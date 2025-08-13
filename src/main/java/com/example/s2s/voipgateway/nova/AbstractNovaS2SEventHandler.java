package com.example.s2s.voipgateway.nova;

import com.example.s2s.voipgateway.nova.event.*;
import com.example.s2s.voipgateway.nova.io.QueuedUlawInputStream;
import com.example.s2s.voipgateway.nova.observer.InteractObserver;
import com.example.s2s.voipgateway.nova.VoiceActivityDetector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Common NovaS2SEventHandler functionality.
 */
public abstract class AbstractNovaS2SEventHandler implements NovaS2SEventHandler {
    private static final Logger log = LoggerFactory.getLogger(AbstractNovaS2SEventHandler.class);
    private static final Base64.Decoder decoder = Base64.getDecoder();
    private static final String ERROR_AUDIO_FILE = "error.wav";
    private final QueuedUlawInputStream audioStream = new QueuedUlawInputStream();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VoiceActivityDetector voiceDetector = new VoiceActivityDetector();
    private final ConversationLogger conversationLogger = new ConversationLogger();
    private InteractObserver<NovaSonicEvent> outbound;
    private String promptName;
    private boolean debugAudioOutput;
    private boolean playedErrorSound = false;
    private volatile boolean isNovaGenerating = false;
    private boolean bargeInEnabled = "true".equalsIgnoreCase(System.getenv().getOrDefault("ENABLE_BARGE_IN", "true"));
    private volatile String currentUserContentName = null;

    public AbstractNovaS2SEventHandler() {
        this(null);
    }

    public AbstractNovaS2SEventHandler(InteractObserver<NovaSonicEvent> outbound) {
        this.outbound = outbound;
        debugAudioOutput = "true".equalsIgnoreCase(System.getenv().getOrDefault("DEBUG_AUDIO_OUTPUT", "false"));
    }

    @Override
    public void handleCompletionStart(JsonNode node) {
        log.info("Completion started for node: {}", node);
        promptName = node.get("promptName").asText();
        log.info("Completion started with promptId: {}", promptName);
        isNovaGenerating = true;
        if (bargeInEnabled) {
            log.debug("Nova started generating, barge-in enabled");
        }
    }

    @Override
    public void handleContentStart(JsonNode node) {

    }

    @Override
    public void handleTextInput(JsonNode node) {
        String content = node.get("content").asText();
        String role = node.get("role").asText();
        
        if ("user".equalsIgnoreCase(role) || "USER".equalsIgnoreCase(role)) {
            // Log user's transcribed speech
            conversationLogger.logUserInput(content);
            log.debug("Logged user input: {}", content);
        }
    }

    @Override
    public void handleTextOutput(JsonNode node) {
        String content = node.get("content").asText();
        String role = node.get("role").asText();
        
        if ("assistant".equalsIgnoreCase(role) || "ASSISTANT".equalsIgnoreCase(role)) {
            // Log Nova's text response
            conversationLogger.logNovaResponse(content);
            log.debug("Logged Nova response: {}", content);
        }
    }

    @Override
    public void handleAudioOutput(JsonNode node) {
        String content = node.get("content").asText();
        String role = node.get("role").asText();
        if (debugAudioOutput) {
            log.info("Received audio output {} from {}", content, role);
        }
        
        // Don't append audio if we're interrupted
        if (audioStream.isInterrupted()) {
            log.debug("Skipping audio output due to interruption");
            return;
        }
        
        byte[] data = decoder.decode(content);
        try {
            audioStream.append(data);
        } catch (InterruptedException e) {
            log.error("Failed to append audio data to queued input stream", e);
        }
    }

    @Override
    public void handleContentEnd(JsonNode node) {
        log.info("Content end for node: {}", node);
        String contentId = node.get("contentId").asText();
        String stopReason = node.has("stopReason") ? node.get("stopReason").asText() : "";
        log.info("Content ended: {} with reason: {}", contentId, stopReason);
    }

    @Override
    public void handleCompletionEnd(JsonNode node) {
        log.info("Completion end for node: {}", node);
        String stopReason = node.has("stopReason") ? node.get("stopReason").asText() : "";
        log.info("Completion ended with reason: {}", stopReason);
        isNovaGenerating = false;
        
        // Resume audio stream and reset voice detector for next interaction
        audioStream.resume();
        voiceDetector.reset();
        
        if (bargeInEnabled) {
            log.debug("Nova finished generating, resuming normal audio flow");
        }
    }

    @Override
    public void onStart() {
        log.info("Session started, playing greeting.");
        conversationLogger.logSystemMessage("Call connected - Nova Sonic session started");
        
        String greetingFilename = System.getenv().getOrDefault("GREETING_FILENAME","hello-how.wav");
        try { playAudioFile(greetingFilename); }
        catch (FileNotFoundException e) {
            log.info("{} not found, no greeting will be sent", greetingFilename);
        }
    }

    @Override
    public void onError(Exception e) {
        log.error("Nova S2S session error", e);
        conversationLogger.logSystemMessage("Error occurred: " + e.getMessage());
        
        // Reset state on error
        isNovaGenerating = false;
        audioStream.resume();
        voiceDetector.reset();
        
        // Send prompt end event to properly close the session on error
        if (outbound != null && promptName != null) {
            log.info("Sending PromptEndEvent due to error for prompt: {}", promptName);
            try {
                outbound.onNext(PromptEndEvent.create(promptName));
            } catch (Exception promptEndException) {
                log.warn("Failed to send PromptEndEvent on error", promptEndException);
            }
        }
        
        if (!playedErrorSound) {
            try {
                playAudioFile(ERROR_AUDIO_FILE);
                playedErrorSound = true;
            } catch (FileNotFoundException ex) {
                log.error("Failed to play error audio file", ex);
            }
        }
    }

    @Override
    public void onComplete() {
        log.info("Stream complete");
        conversationLogger.logConversationEnd();
        
        // Send prompt end event to properly close the session
        if (outbound != null && promptName != null) {
            log.info("Sending PromptEndEvent for prompt: {}", promptName);
            outbound.onNext(PromptEndEvent.create(promptName));
        }
    }

    @Override
    public InputStream getAudioInputStream() {
        return audioStream;
    }

    @Override
    public void setOutbound(InteractObserver<NovaSonicEvent> outbound) {
        this.outbound = outbound;
    }

    /**
     * Handles the actual invocation of a tool.
     * @param toolUseId The tool use id.
     * @param toolName The tool name.
     * @param content Content provided as a parameter to the invocation.
     * @param output The output node.
     */
    protected abstract void handleToolInvocation(String toolUseId, String toolName, String content, Map<String,Object> output);

    @Override
    public void handleToolUse(JsonNode node, String toolUseId, String toolName, String content) {
        log.info("Tool {} invoked with id={}, content={}", toolName, toolUseId, content);
        String contentName = UUID.randomUUID().toString();
        try {
            Map<String, Object> contentNode = new HashMap<>();
            handleToolInvocation(toolUseId, toolName, content, contentNode);

            ToolResultEvent toolResultEvent = new ToolResultEvent();
            Map<String,Object> toolResult = toolResultEvent.getToolResult().getProperties();
            toolResult.put("promptName", promptName);
            toolResult.put("contentName", contentName);
            toolResult.put("role", "TOOL");
            toolResult.put("content", objectMapper.writeValueAsString(contentNode)); // Ensure proper escaping

            sendToolContentStart(toolUseId, contentName);
            outbound.onNext(toolResultEvent);
            outbound.onNext(ContentEndEvent.create(promptName, contentName));
        } catch (Exception e) {
            throw new RuntimeException("Error creating JSON payload for toolResult", e);
        }
    }

    /**
     * Plays an audio file, either relative to the working directory or from the classpath.
     * @param filename The file name of the file to play.
     */
    protected void playAudioFile(String filename) throws FileNotFoundException {
        InputStream is = null;
        File file = new File(filename);
        if (file.exists()) {
            try { is = new FileInputStream(file); }
            catch (FileNotFoundException e) {
                // we already checked if it exists ... this should never happen
            }
        } else {
            is = getClass().getClassLoader().getResourceAsStream(filename);
        }
        if (is != null) {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
                AudioInputStream transcodedStream = AudioSystem.getAudioInputStream(new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, 16, 1, 2, 8000, false), audioInputStream);
                audioStream.append(transcodedStream.readAllBytes());
                log.debug("Wrote audio from {} to output stream ...", filename);
            } catch (RuntimeException e) {
                log.error("Runtime exception while playing audio from {}", filename, e);
            } catch (InterruptedException e) {
                log.error("Interrupted while appending audio to queued input stream", e);
            } catch (IOException | UnsupportedAudioFileException e) {
                log.error("Failed to load {}", filename, e);
            }
        } else {
            throw new FileNotFoundException("Could not find "+filename);
        }
    }

    private void sendToolContentStart(String toolUseId, String contentName) {
        Map<String,Object> toolResultInputConfig=new HashMap<>();
        toolResultInputConfig.put("toolUseId", toolUseId);
        toolResultInputConfig.put("type", "TEXT");
        toolResultInputConfig.put("textInputConfiguration", MediaConfiguration.builder().mediaType("text/plain").build());

        outbound.onNext(ContentStartEvent.builder()
                .contentStart(ContentStartEvent.ContentStart.builder()
                        .promptName(promptName)
                        .contentName(contentName)
                        .interactive(false)
                        .type("TOOL")
                        .property("toolResultInputConfiguration", toolResultInputConfig)
                        .property("role", "TOOL")
                        .build())
                .build());
    }
    
    /**
     * Processes incoming user audio for voice activity detection and barge-in.
     * This should be called by the audio input stream when new audio data arrives.
     * 
     * @param audioData PCM audio data from the user
     */
    public void processUserAudio(byte[] audioData) {
        if (!bargeInEnabled || !isNovaGenerating || audioStream.isInterrupted()) {
            return;
        }
        
        // Check for voice activity
        if (voiceDetector.detectVoiceActivity(audioData)) {
            log.info("Barge-in detected: User started speaking while Nova was generating");
            conversationLogger.logSystemMessage("User interrupted Nova (barge-in detected)");
            handleBargeIn();
        }
    }
    
    /**
     * Handles barge-in when user speech is detected during Nova generation.
     */
    private void handleBargeIn() {
        // Interrupt the current audio output to stop Nova from speaking
        audioStream.interrupt();
        log.info("Barge-in handled: Interrupted Nova's audio output");
        
        // Reset voice detector for next detection
        voiceDetector.reset();
        
        // After a short delay, resume the audio stream so Nova can respond to the user's input
        // We need to do this because Nova might generate a response to the user's barge-in
        new Thread(() -> {
            try {
                Thread.sleep(500); // Brief pause to let the interruption take effect
                audioStream.resume();
                log.info("Audio stream resumed after barge-in pause");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Checks if Nova is currently generating a response.
     * @return true if Nova is generating, false otherwise
     */
    public boolean isNovaGenerating() {
        return isNovaGenerating;
    }
    
    /**
     * Checks if barge-in is enabled.
     * @return true if barge-in is enabled, false otherwise
     */
    public boolean isBargeInEnabled() {
        return bargeInEnabled;
    }
    
    /**
     * Sets the current user content name for tracking active audio content.
     * This is needed for proper barge-in functionality.
     * @param contentName The content name of the current user audio stream
     */
    public void setCurrentUserContentName(String contentName) {
        this.currentUserContentName = contentName;
        log.debug("Set current user content name: {}", contentName);
    }
    
    /**
     * Clears the current user content name when content ends.
     */
    public void clearCurrentUserContentName() {
        log.debug("Cleared current user content name: {}", currentUserContentName);
        this.currentUserContentName = null;
    }
}

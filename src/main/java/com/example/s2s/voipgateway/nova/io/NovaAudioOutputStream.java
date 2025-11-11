package com.example.s2s.voipgateway.nova.io;

import com.example.s2s.voipgateway.constants.MediaTypes;
import com.example.s2s.voipgateway.constants.SonicAudioConfig;
import com.example.s2s.voipgateway.constants.SonicAudioTypes;
import com.example.s2s.voipgateway.nova.event.AudioInputEvent;
import com.example.s2s.voipgateway.nova.event.EndAudioContent;
import com.example.s2s.voipgateway.nova.event.NovaSonicEvent;
import com.example.s2s.voipgateway.nova.event.PromptEndEvent;
import com.example.s2s.voipgateway.nova.event.StartAudioContent;
import com.example.s2s.voipgateway.nova.observer.InteractObserver;
import com.example.s2s.voipgateway.nova.transcode.UlawToPcmTranscoder;
import com.example.s2s.voipgateway.nova.AbstractNovaS2SEventHandler;
import com.example.s2s.voipgateway.nova.ContentLifecycleManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstracts Nova S2S outbound audio as an OutputStream.
 */
public class NovaAudioOutputStream extends OutputStream {
    private final InteractObserver<NovaSonicEvent> observer;
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final String promptName;
    private final String contentName;
    private boolean startSent = false;
    private OutputStream audioFileOutput;
    private boolean debugAudioReceived = System.getenv().getOrDefault("DEBUG_AUDIO_RECEIVED", "false").equalsIgnoreCase("true");
    private AbstractNovaS2SEventHandler eventHandler;
    private ContentLifecycleManager lifecycleManager;

    public NovaAudioOutputStream(InteractObserver<NovaSonicEvent> observer, String promptName, ContentLifecycleManager lifecycleManager) {
        this.observer = observer;
        this.promptName = promptName;
        this.contentName = UUID.randomUUID().toString();
        this.lifecycleManager = lifecycleManager;
    }

    /**
     * Constructor that accepts an event handler for barge-in functionality.
     */
    public NovaAudioOutputStream(InteractObserver<NovaSonicEvent> observer, String promptName, AbstractNovaS2SEventHandler eventHandler, ContentLifecycleManager lifecycleManager) {
        this.observer = observer;
        this.promptName = promptName;
        this.contentName = UUID.randomUUID().toString();
        this.eventHandler = eventHandler;
        this.lifecycleManager = lifecycleManager;
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (b.length != len) {
            byte[] other = new byte[len];
            System.arraycopy(b, off, other, 0, len);
            b = other;
        }
        if (!startSent) {
            sendStart();
            if (debugAudioReceived) {
                audioFileOutput = new FileOutputStream("received.raw");
            }
        }

        byte[] pcmData = UlawToPcmTranscoder.convertByteArray(b);
        if (audioFileOutput != null) {
            audioFileOutput.write(pcmData);
        }
        
        // Process audio for barge-in detection if event handler is available
        if (eventHandler != null) {
            eventHandler.processUserAudio(pcmData);
        }

        observer.onNext(new AudioInputEvent(AudioInputEvent.AudioInput.builder()
                .promptName(promptName)
                .contentName(contentName)
                .role("USER")
                .content(encoder.encodeToString(pcmData))
                .build()));
    }

    /**
     * Sends the StartAudioContent event.
     */
    private void sendStart() {
        // Register content start with lifecycle manager
        if (lifecycleManager != null && lifecycleManager.registerContentStart(contentName)) {
            // Notify event handler about the current content name for barge-in tracking
            if (eventHandler != null) {
                eventHandler.setCurrentUserContentName(contentName);
            }

            observer.onNext(new StartAudioContent(StartAudioContent.ContentStart.builder()
                    .promptName(promptName)
                    .contentName(contentName)
                    .type(StartAudioContent.TYPE_AUDIO)
                    .interactive(true)
                    .audioInputConfiguration(StartAudioContent.AudioInputConfiguration.builder()
                            .mediaType(MediaTypes.AUDIO_LPCM)
                            .sampleRateHertz(SonicAudioConfig.SAMPLE_RATE)
                            .sampleSizeBits(SonicAudioConfig.SAMPLE_SIZE)
                            .channelCount(SonicAudioConfig.CHANNEL_COUNT)
                            .audioType(SonicAudioTypes.SPEECH)
                            .encoding(SonicAudioConfig.ENCODING_BASE64)
                            .build())
                    .build()));

            // Mark content as active after sending start event
            if (lifecycleManager != null) {
                lifecycleManager.markContentActive(contentName);
            }
            startSent = true;
        }
    }

    @Override
    public void write(int b) throws IOException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void close() throws IOException {
        // Only send end event if content was started and lifecycle manager allows it
        if (startSent && lifecycleManager != null && lifecycleManager.registerContentEnd(contentName)) {
            observer.onNext(new EndAudioContent(EndAudioContent.ContentEnd.builder()
                    .promptName(promptName)
                    .contentName(contentName)
                    .build()));

            // Mark content as closed in lifecycle manager
            lifecycleManager.markContentClosed(contentName);
        }

        // Clear the tracked content name since this content is ending
        if (eventHandler != null) {
            eventHandler.clearCurrentUserContentName();
        }

        if (audioFileOutput != null) {
            audioFileOutput.close();
            audioFileOutput = null;
        }

        // CRITICAL FIX: Always send PromptEndEvent when closing the stream
        // This ensures the prompt is properly closed at the Bedrock level
        observer.onNext(PromptEndEvent.create(promptName));

        // Only complete observer if no other content is active
        if (lifecycleManager != null && lifecycleManager.canCompleteObserver()) {
            if (lifecycleManager.markObserverCompleted()) {
                observer.onComplete();
            }
        } else {
            // Fallback: complete observer if no lifecycle manager
            if (lifecycleManager == null) {
                observer.onComplete();
            }
        }
    }
}

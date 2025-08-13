package com.example.s2s.voipgateway.nova;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Logs conversation text to a file with timestamps.
 */
public class ConversationLogger {
    private static final Logger log = LoggerFactory.getLogger(ConversationLogger.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final String logFilePath;
    private final boolean enabled;
    private final ReentrantLock writeLock = new ReentrantLock();
    
    public ConversationLogger() {
        // Check if conversation logging is enabled via environment variable
        this.enabled = "true".equalsIgnoreCase(System.getenv().getOrDefault("ENABLE_CONVERSATION_LOG", "false"));
        this.logFilePath = System.getenv().getOrDefault("CONVERSATION_LOG_FILE", "conversation.log");
        
        if (enabled) {
            log.info("Conversation logging enabled, writing to: {}", logFilePath);
            logConversationStart();
        } else {
            log.debug("Conversation logging disabled");
        }
    }
    
    /**
     * Logs a message from Nova (assistant response).
     * @param content The text content of Nova's response
     */
    public void logNovaResponse(String content) {
        if (!enabled || content == null || content.trim().isEmpty()) {
            return;
        }
        
        logMessage("NOVA", content.trim());
    }
    
    /**
     * Logs a user input message.
     * Note: In the current S2S system, user speech is not transcribed to text,
     * so this method is provided for future use if transcription becomes available.
     * @param content The text content of the user's input
     */
    public void logUserInput(String content) {
        if (!enabled || content == null || content.trim().isEmpty()) {
            return;
        }
        
        logMessage("USER", content.trim());
    }
    
    /**
     * Logs a system message (e.g., call start, call end, errors).
     * @param message The system message to log
     */
    public void logSystemMessage(String message) {
        if (!enabled || message == null || message.trim().isEmpty()) {
            return;
        }
        
        logMessage("SYSTEM", message.trim());
    }
    
    /**
     * Logs the start of a new conversation session.
     */
    private void logConversationStart() {
        logMessage("SYSTEM", "=== New conversation started ===");
    }
    
    /**
     * Logs the end of a conversation session.
     */
    public void logConversationEnd() {
        logMessage("SYSTEM", "=== Conversation ended ===");
        logMessage("SYSTEM", ""); // Empty line for separation
    }
    
    /**
     * Internal method to write a timestamped message to the log file.
     * @param speaker The speaker (NOVA, USER, SYSTEM)
     * @param content The message content
     */
    private void logMessage(String speaker, String content) {
        if (!enabled) {
            return;
        }
        
        writeLock.lock();
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String logEntry = String.format("[%s] %s: %s%n", timestamp, speaker, content);
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
                writer.write(logEntry);
                writer.flush();
            } catch (IOException e) {
                log.error("Failed to write to conversation log file: {}", logFilePath, e);
            }
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Checks if conversation logging is enabled.
     * @return true if logging is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Gets the path to the conversation log file.
     * @return the log file path
     */
    public String getLogFilePath() {
        return logFilePath;
    }
}
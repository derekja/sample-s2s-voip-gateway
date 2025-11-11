package com.example.s2s.voipgateway.sip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Filters and preprocesses SIP messages to reduce noise from malformed packets.
 * Drops obviously invalid messages before they reach the mjSIP parser.
 */
public class SipMessageFilter {
    private static final Logger log = LoggerFactory.getLogger(SipMessageFilter.class);

    // Statistics for monitoring
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final AtomicLong filteredMessages = new AtomicLong(0);
    private final AtomicLong malformedMessages = new AtomicLong(0);

    // Pattern to match valid SIP message start
    private static final Pattern VALID_SIP_START = Pattern.compile(
        "^(INVITE|ACK|BYE|CANCEL|REGISTER|OPTIONS|INFO|PRACK|UPDATE|REFER|NOTIFY|SUBSCRIBE|MESSAGE)\\s+.*SIP/2\\.0",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern to match valid SIP response start
    private static final Pattern VALID_SIP_RESPONSE = Pattern.compile(
        "^SIP/2\\.0\\s+\\d{3}\\s+.*",
        Pattern.CASE_INSENSITIVE
    );

    // Minimum valid SIP message size (in bytes)
    private static final int MIN_SIP_MESSAGE_SIZE = 20;

    // Maximum reasonable SIP message size (in bytes) - prevent DoS attacks
    private static final int MAX_SIP_MESSAGE_SIZE = 65536; // 64KB

    /**
     * Filters a raw SIP message and determines if it should be processed.
     * @param messageBytes The raw message bytes
     * @return true if the message should be processed, false if it should be dropped
     */
    public boolean shouldProcessMessage(byte[] messageBytes) {
        totalMessages.incrementAndGet();

        // Check for null or empty message
        if (messageBytes == null || messageBytes.length == 0) {
            filteredMessages.incrementAndGet();
            logFilteredMessage("Empty message", messageBytes);
            return false;
        }

        // Check message size bounds
        if (messageBytes.length < MIN_SIP_MESSAGE_SIZE) {
            filteredMessages.incrementAndGet();
            logFilteredMessage("Message too small (" + messageBytes.length + " bytes)", messageBytes);
            return false;
        }

        if (messageBytes.length > MAX_SIP_MESSAGE_SIZE) {
            filteredMessages.incrementAndGet();
            log.warn("Dropping oversized message: {} bytes from potential DoS attack", messageBytes.length);
            return false;
        }

        // Convert to string for content analysis
        String messageStr;
        try {
            messageStr = new String(messageBytes, "UTF-8").trim();
        } catch (Exception e) {
            filteredMessages.incrementAndGet();
            logFilteredMessage("Invalid UTF-8 encoding", messageBytes);
            return false;
        }

        // Check for empty or whitespace-only messages
        if (messageStr.isEmpty()) {
            filteredMessages.incrementAndGet();
            logFilteredMessage("Whitespace-only message", messageBytes);
            return false;
        }

        // Check for common malformed patterns
        if (isCommonMalformedMessage(messageStr)) {
            malformedMessages.incrementAndGet();
            logFilteredMessage("Common malformed pattern", messageBytes);
            return false;
        }

        // Validate SIP message structure
        if (!hasValidSipStructure(messageStr)) {
            malformedMessages.incrementAndGet();
            logFilteredMessage("Invalid SIP structure", messageBytes);
            return false;
        }

        // Message passed all filters
        return true;
    }

    /**
     * Checks for common malformed message patterns that waste processing time.
     */
    private boolean isCommonMalformedMessage(String message) {
        // Check for messages that are just line breaks or control characters
        if (message.matches("^[\\r\\n\\s]*$")) {
            return true;
        }

        // Check for single character messages
        if (message.length() < 4) {
            return true;
        }

        // Check for messages that don't start with a valid SIP method or response
        String[] lines = message.split("[\\r\\n]+");
        if (lines.length == 0) {
            return true;
        }

        String firstLine = lines[0].trim();
        if (firstLine.isEmpty()) {
            return true;
        }

        // Must start with either a SIP method or SIP response
        return !VALID_SIP_START.matcher(firstLine).matches() &&
               !VALID_SIP_RESPONSE.matcher(firstLine).matches();
    }

    /**
     * Validates basic SIP message structure.
     */
    private boolean hasValidSipStructure(String message) {
        // SIP messages should have headers separated by CRLF and end with double CRLF
        // At minimum, should contain SIP version identifier
        if (!message.contains("SIP/2.0")) {
            return false;
        }

        // Should contain at least one colon (indicating a header)
        // Valid SIP messages must have headers like Via:, From:, To:, etc.
        if (!message.contains(":")) {
            return false;
        }

        return true;
    }

    /**
     * Logs filtered messages with rate limiting to prevent log spam.
     */
    private void logFilteredMessage(String reason, byte[] messageBytes) {
        // Rate limit logging - only log every 100th filtered message to prevent spam
        long count = filteredMessages.get();
        if (count % 100 == 1 || count <= 10) {
            String preview = getMessagePreview(messageBytes);
            log.debug("Filtered SIP message (#{}) - {}: {}", count, reason, preview);

            // Log statistics periodically
            if (count % 1000 == 0) {
                logStatistics();
            }
        }
    }

    /**
     * Creates a safe preview string of the message for logging.
     */
    private String getMessagePreview(byte[] messageBytes) {
        if (messageBytes == null || messageBytes.length == 0) {
            return "[empty]";
        }

        // Create a safe preview (max 50 chars) with non-printable chars escaped
        int previewLength = Math.min(50, messageBytes.length);
        StringBuilder preview = new StringBuilder();
        for (int i = 0; i < previewLength; i++) {
            byte b = messageBytes[i];
            if (b >= 32 && b <= 126) { // Printable ASCII
                preview.append((char) b);
            } else {
                preview.append(String.format("\\x%02x", b & 0xFF));
            }
        }
        if (messageBytes.length > previewLength) {
            preview.append("...");
        }
        return preview.toString();
    }

    /**
     * Logs current filtering statistics.
     */
    public void logStatistics() {
        long total = totalMessages.get();
        long filtered = filteredMessages.get();
        long malformed = malformedMessages.get();
        long processed = total - filtered;

        if (total > 0) {
            double filterRate = (filtered * 100.0) / total;
            double malformedRate = (malformed * 100.0) / total;
            log.info("SIP Message Filter Stats: {} total, {} processed, {} filtered ({:.1f}%), {} malformed ({:.1f}%)",
                total, processed, filtered, filterRate, malformed, malformedRate);
        }
    }

    /**
     * Gets filtering statistics for monitoring.
     */
    public FilterStatistics getStatistics() {
        return new FilterStatistics(
            totalMessages.get(),
            filteredMessages.get(),
            malformedMessages.get()
        );
    }

    /**
     * Statistics about message filtering.
     */
    public static class FilterStatistics {
        public final long totalMessages;
        public final long filteredMessages;
        public final long malformedMessages;
        public final long processedMessages;
        public final double filterPercentage;

        public FilterStatistics(long total, long filtered, long malformed) {
            this.totalMessages = total;
            this.filteredMessages = filtered;
            this.malformedMessages = malformed;
            this.processedMessages = total - filtered;
            this.filterPercentage = total > 0 ? (filtered * 100.0) / total : 0.0;
        }
    }
}
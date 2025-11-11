package com.example.s2s.voipgateway.sip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors SIP gateway performance and provides statistics about message processing.
 * Helps track and reduce noise from malformed SIP messages.
 */
public class SipMonitor {
    private static final Logger log = LoggerFactory.getLogger(SipMonitor.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicLong incomingCalls = new AtomicLong(0);
    private final AtomicLong successfulCalls = new AtomicLong(0);
    private final AtomicLong failedCalls = new AtomicLong(0);
    private final AtomicLong registrationAttempts = new AtomicLong(0);
    private final AtomicLong registrationSuccesses = new AtomicLong(0);
    private final AtomicLong keepAlivePackets = new AtomicLong(0);

    // Estimated counters for network-level issues (based on log analysis)
    private final AtomicLong estimatedMalformedMessages = new AtomicLong(0);
    private final AtomicLong estimatedValidMessages = new AtomicLong(0);

    private volatile boolean started = false;

    /**
     * Starts the SIP monitor with periodic statistics reporting.
     */
    public void start() {
        if (started) {
            return;
        }
        started = true;

        log.info("Starting SIP Monitor - will report statistics every 5 minutes");

        // Schedule periodic statistics reporting
        scheduler.scheduleAtFixedRate(this::reportStatistics, 5, 5, TimeUnit.MINUTES);

        // Schedule periodic health check
        scheduler.scheduleAtFixedRate(this::performHealthCheck, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Stops the SIP monitor.
     */
    public void stop() {
        if (!started) {
            return;
        }
        started = false;

        log.info("Stopping SIP Monitor");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Records an incoming call.
     */
    public void recordIncomingCall() {
        incomingCalls.incrementAndGet();
        estimatedValidMessages.incrementAndGet();
    }

    /**
     * Records a successful call completion.
     */
    public void recordSuccessfulCall() {
        successfulCalls.incrementAndGet();
    }

    /**
     * Records a failed call.
     */
    public void recordFailedCall() {
        failedCalls.incrementAndGet();
    }

    /**
     * Records a registration attempt.
     */
    public void recordRegistrationAttempt() {
        registrationAttempts.incrementAndGet();
        estimatedValidMessages.incrementAndGet();
    }

    /**
     * Records a successful registration.
     */
    public void recordRegistrationSuccess() {
        registrationSuccesses.incrementAndGet();
    }

    /**
     * Records a keep-alive packet sent.
     */
    public void recordKeepAlive() {
        keepAlivePackets.incrementAndGet();
    }

    /**
     * Estimates malformed message count based on typical patterns.
     * This is called when we detect patterns that suggest malformed messages.
     */
    public void estimateMalformedMessage() {
        estimatedMalformedMessages.incrementAndGet();
    }

    /**
     * Records a valid SIP message processed.
     */
    public void recordValidMessage() {
        estimatedValidMessages.incrementAndGet();
    }

    /**
     * Reports current statistics.
     */
    public void reportStatistics() {
        SipStatistics stats = getStatistics();

        log.info("=== SIP Gateway Statistics (Last 5 minutes) ===");
        log.info("Calls: {} incoming, {} successful, {} failed",
                stats.incomingCalls, stats.successfulCalls, stats.failedCalls);
        log.info("Registration: {} attempts, {} successful",
                stats.registrationAttempts, stats.registrationSuccesses);
        log.info("Keep-alive packets sent: {}", stats.keepAlivePackets);
        log.info("Estimated messages: {} valid, {} malformed",
                stats.estimatedValidMessages, stats.estimatedMalformedMessages);

        if (stats.estimatedValidMessages + stats.estimatedMalformedMessages > 0) {
            double malformedPercentage = (stats.estimatedMalformedMessages * 100.0) /
                    (stats.estimatedValidMessages + stats.estimatedMalformedMessages);
            log.info("Estimated malformed message rate: {:.2f}%", malformedPercentage);

            if (malformedPercentage > 50) {
                log.warn("High malformed message rate detected - consider network-level filtering");
            }
        }

        if (stats.incomingCalls > 0) {
            double successRate = (stats.successfulCalls * 100.0) / stats.incomingCalls;
            log.info("Call success rate: {:.2f}%", successRate);
        }

        if (stats.registrationAttempts > 0) {
            double regSuccessRate = (stats.registrationSuccesses * 100.0) / stats.registrationAttempts;
            log.info("Registration success rate: {:.2f}%", regSuccessRate);
        }

        log.info("================================================");
    }

    /**
     * Performs a health check of the SIP gateway.
     */
    private void performHealthCheck() {
        SipStatistics stats = getStatistics();

        // Check for concerning patterns
        if (stats.registrationAttempts > 0 && stats.registrationSuccesses == 0) {
            log.warn("Health Check: No successful registrations - check SIP server connectivity");
        }

        if (stats.incomingCalls > 10 && stats.successfulCalls == 0) {
            log.warn("Health Check: No successful calls despite incoming calls - check media configuration");
        }

        // Estimate if we're getting too many malformed messages
        long totalEstimated = stats.estimatedValidMessages + stats.estimatedMalformedMessages;
        if (totalEstimated > 100) {
            double malformedRate = (stats.estimatedMalformedMessages * 100.0) / totalEstimated;
            if (malformedRate > 75) {
                log.warn("Health Check: Very high malformed message rate ({:.1f}%) - possible DoS or network issues", malformedRate);
            }
        }
    }

    /**
     * Gets current statistics snapshot.
     */
    public SipStatistics getStatistics() {
        return new SipStatistics(
                incomingCalls.get(),
                successfulCalls.get(),
                failedCalls.get(),
                registrationAttempts.get(),
                registrationSuccesses.get(),
                keepAlivePackets.get(),
                estimatedMalformedMessages.get(),
                estimatedValidMessages.get()
        );
    }

    /**
     * Resets all statistics counters.
     */
    public void resetStatistics() {
        incomingCalls.set(0);
        successfulCalls.set(0);
        failedCalls.set(0);
        registrationAttempts.set(0);
        registrationSuccesses.set(0);
        keepAlivePackets.set(0);
        estimatedMalformedMessages.set(0);
        estimatedValidMessages.set(0);
        log.info("SIP statistics reset");
    }

    /**
     * Statistics snapshot.
     */
    public static class SipStatistics {
        public final long incomingCalls;
        public final long successfulCalls;
        public final long failedCalls;
        public final long registrationAttempts;
        public final long registrationSuccesses;
        public final long keepAlivePackets;
        public final long estimatedMalformedMessages;
        public final long estimatedValidMessages;

        public SipStatistics(long incomingCalls, long successfulCalls, long failedCalls,
                           long registrationAttempts, long registrationSuccesses, long keepAlivePackets,
                           long estimatedMalformedMessages, long estimatedValidMessages) {
            this.incomingCalls = incomingCalls;
            this.successfulCalls = successfulCalls;
            this.failedCalls = failedCalls;
            this.registrationAttempts = registrationAttempts;
            this.registrationSuccesses = registrationSuccesses;
            this.keepAlivePackets = keepAlivePackets;
            this.estimatedMalformedMessages = estimatedMalformedMessages;
            this.estimatedValidMessages = estimatedValidMessages;
        }
    }
}
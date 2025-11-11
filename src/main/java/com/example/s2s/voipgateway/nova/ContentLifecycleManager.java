package com.example.s2s.voipgateway.nova;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages content lifecycle to prevent race conditions in AWS Bedrock interactions.
 * Ensures content IDs are properly tracked and lifecycle events are synchronized.
 */
public class ContentLifecycleManager {
    private static final Logger log = LoggerFactory.getLogger(ContentLifecycleManager.class);

    private final ConcurrentMap<String, ContentState> activeContent = new ConcurrentHashMap<>();
    private final AtomicBoolean observerCompleted = new AtomicBoolean(false);

    /**
     * Represents the state of a content item.
     */
    public enum ContentState {
        STARTING,   // ContentStartEvent sent but not confirmed
        ACTIVE,     // Content is active and can receive data
        ENDING,     // ContentEndEvent sent but not confirmed
        CLOSED      // Content is fully closed
    }

    /**
     * Registers a new content ID that is about to start.
     * @param contentId The content ID to register
     * @return true if registration was successful, false if content was already registered
     */
    public boolean registerContentStart(String contentId) {
        ContentState previousState = activeContent.putIfAbsent(contentId, ContentState.STARTING);
        if (previousState != null) {
            log.warn("Content ID {} was already registered with state {}", contentId, previousState);
            return false;
        }
        log.debug("Registered content start for ID: {}", contentId);
        return true;
    }

    /**
     * Marks content as active after ContentStartEvent is confirmed.
     * @param contentId The content ID to mark as active
     */
    public void markContentActive(String contentId) {
        boolean updated = activeContent.replace(contentId, ContentState.STARTING, ContentState.ACTIVE);
        if (!updated) {
            log.warn("Attempted to mark content {} as active, but it was in state: {}", contentId,
                    activeContent.get(contentId));
        } else {
            log.debug("Marked content as active: {}", contentId);
        }
    }

    /**
     * Registers that a content ID is ending.
     * @param contentId The content ID that is ending
     * @return true if the content was active and can be ended, false otherwise
     */
    public boolean registerContentEnd(String contentId) {
        ContentState currentState = activeContent.get(contentId);
        if (currentState == null) {
            log.warn("Attempted to end content {} that was never registered", contentId);
            return false;
        }

        if (currentState != ContentState.ACTIVE) {
            log.warn("Attempted to end content {} in invalid state: {}", contentId, currentState);
            return false;
        }

        boolean updated = activeContent.replace(contentId, ContentState.ACTIVE, ContentState.ENDING);
        if (updated) {
            log.debug("Registered content end for ID: {}", contentId);
        }
        return updated;
    }

    /**
     * Marks content as fully closed after ContentEndEvent is confirmed.
     * @param contentId The content ID to mark as closed
     */
    public void markContentClosed(String contentId) {
        ContentState removedState = activeContent.remove(contentId);
        if (removedState != ContentState.ENDING) {
            log.warn("Marked content {} as closed, but it was in state: {}", contentId, removedState);
        } else {
            log.debug("Marked content as closed: {}", contentId);
        }
    }

    /**
     * Checks if there are any active content items.
     * @return true if there are active content items, false otherwise
     */
    public boolean hasActiveContent() {
        return !activeContent.isEmpty();
    }

    /**
     * Gets the number of active content items.
     * @return the count of active content items
     */
    public int getActiveContentCount() {
        return activeContent.size();
    }

    /**
     * Checks if the observer can be safely completed.
     * @return true if observer can be completed, false if there's still active content
     */
    public boolean canCompleteObserver() {
        boolean canComplete = !hasActiveContent() && !observerCompleted.get();
        if (!canComplete && hasActiveContent()) {
            log.debug("Cannot complete observer: {} active content items remaining", getActiveContentCount());
        }
        return canComplete;
    }

    /**
     * Marks the observer as completed to prevent duplicate completion.
     * @return true if this call actually completed the observer, false if it was already completed
     */
    public boolean markObserverCompleted() {
        boolean wasCompleted = observerCompleted.getAndSet(true);
        if (!wasCompleted) {
            log.debug("Observer marked as completed");
        }
        return !wasCompleted;
    }

    /**
     * Forces cleanup of all content in case of errors.
     * This should only be used in error scenarios.
     */
    public void forceCleanupAll() {
        int count = activeContent.size();
        activeContent.clear();
        if (count > 0) {
            log.warn("Force cleaned up {} active content items due to error", count);
        }
    }

    /**
     * Gets a snapshot of current active content for debugging.
     * @return a copy of the current active content map
     */
    public ConcurrentMap<String, ContentState> getActiveContentSnapshot() {
        return new ConcurrentHashMap<>(activeContent);
    }
}
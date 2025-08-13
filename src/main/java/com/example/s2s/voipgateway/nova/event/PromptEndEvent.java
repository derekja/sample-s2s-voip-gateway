package com.example.s2s.voipgateway.nova.event;

import com.fasterxml.jackson.annotation.JsonGetter;

/**
 * Represents a prompt end event to properly close Nova Sonic prompts.
 */
public class PromptEndEvent implements NovaSonicEvent {
    private PromptEnd promptEnd;

    public PromptEndEvent() {
    }

    public PromptEndEvent(String promptName) {
        this.promptEnd = new PromptEnd(promptName);
    }

    @JsonGetter
    public PromptEnd getPromptEnd() {
        return promptEnd;
    }

    public void setPromptEnd(PromptEnd promptEnd) {
        this.promptEnd = promptEnd;
    }

    public static class PromptEnd {
        private String promptName;

        public PromptEnd() {
        }

        public PromptEnd(String promptName) {
            this.promptName = promptName;
        }

        @JsonGetter
        public String getPromptName() {
            return promptName;
        }

        public void setPromptName(String promptName) {
            this.promptName = promptName;
        }
    }

    /**
     * Creates a PromptEndEvent for the given prompt name.
     * @param promptName The name of the prompt to end
     * @return A new PromptEndEvent
     */
    public static PromptEndEvent create(String promptName) {
        return new PromptEndEvent(promptName);
    }
}
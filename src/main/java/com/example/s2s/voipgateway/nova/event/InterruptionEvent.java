package com.example.s2s.voipgateway.nova.event;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event to interrupt Nova Sonic's current response for barge-in functionality.
 */
public class InterruptionEvent implements NovaSonicEvent {
    
    @JsonProperty("interruption")
    private final Interruption interruption;
    
    public InterruptionEvent(String promptName) {
        this.interruption = new Interruption(promptName);
    }
    
    public Interruption getInterruption() {
        return interruption;
    }
    
    public static class Interruption {
        @JsonProperty("promptName")
        private final String promptName;
        
        @JsonProperty("type")
        private final String type = "USER_INTERRUPTION";
        
        public Interruption(String promptName) {
            this.promptName = promptName;
        }
        
        public String getPromptName() {
            return promptName;
        }
        
        public String getType() {
            return type;
        }
    }
}
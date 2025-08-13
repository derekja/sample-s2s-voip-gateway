package com.example.s2s.voipgateway.nova.event;

import com.fasterxml.jackson.annotation.JsonGetter;

/**
 * Represents a session start event.
 */
public class SessionStartEvent implements NovaSonicEvent {
    private SessionStart sessionStart=new SessionStart();

    @JsonGetter
    public SessionStart getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(SessionStart sessionStart) {
        this.sessionStart = sessionStart;
    }

    public SessionStartEvent(int maxTokens, float topP, float temperature) {
        sessionStart.inferenceConfiguration.setMaxTokens(maxTokens);
        sessionStart.inferenceConfiguration.setTopP(topP);
        sessionStart.inferenceConfiguration.setTemperature(temperature);
    }

    public SessionStartEvent(int maxTokens, float topP, float temperature, String guardrailIdentifier, String guardrailVersion) {
        this(maxTokens, topP, temperature);
        if (guardrailIdentifier != null && !guardrailIdentifier.trim().isEmpty()) {
            GuardrailConfiguration guardrailConfig = new GuardrailConfiguration();
            guardrailConfig.setGuardrailIdentifier(guardrailIdentifier);
            guardrailConfig.setGuardrailVersion(guardrailVersion != null ? guardrailVersion : "DRAFT");
            guardrailConfig.setTrace(true); // Enable trace to get input/output text
            sessionStart.setGuardrailConfiguration(guardrailConfig);
        }
    }
    public static class SessionStart {
        private InferenceConfiguration inferenceConfiguration = new InferenceConfiguration();
        private GuardrailConfiguration guardrailConfiguration = null;

        public SessionStart() {
        }

        @JsonGetter("inferenceConfiguration")
        public InferenceConfiguration getInferenceConfiguration() {
            return inferenceConfiguration;
        }

        public void setInferenceConfiguration(InferenceConfiguration inferenceConfiguration) {
            this.inferenceConfiguration = inferenceConfiguration;
        }

        @JsonGetter("guardrailConfiguration")
        public GuardrailConfiguration getGuardrailConfiguration() {
            return guardrailConfiguration;
        }

        public void setGuardrailConfiguration(GuardrailConfiguration guardrailConfiguration) {
            this.guardrailConfiguration = guardrailConfiguration;
        }
    }

    public static class InferenceConfiguration {
        private int maxTokens;
        private float topP;
        private float temperature;

        @JsonGetter("maxTokens")
        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        @JsonGetter("topP")
        public float getTopP() {
            return topP;
        }

        public void setTopP(float topP) {
            this.topP = topP;
        }

        @JsonGetter("temperature")
        public float getTemperature() {
            return temperature;
        }

        public void setTemperature(float temperature) {
            this.temperature = temperature;
        }
    }

    public static class GuardrailConfiguration {
        private String guardrailIdentifier;
        private String guardrailVersion;
        private boolean trace = true; // Enable trace to get input/output text

        @JsonGetter("guardrailIdentifier")
        public String getGuardrailIdentifier() {
            return guardrailIdentifier;
        }

        public void setGuardrailIdentifier(String guardrailIdentifier) {
            this.guardrailIdentifier = guardrailIdentifier;
        }

        @JsonGetter("guardrailVersion")
        public String getGuardrailVersion() {
            return guardrailVersion;
        }

        public void setGuardrailVersion(String guardrailVersion) {
            this.guardrailVersion = guardrailVersion;
        }

        @JsonGetter("trace")
        public boolean isTrace() {
            return trace;
        }

        public void setTrace(boolean trace) {
            this.trace = trace;
        }
    }
}

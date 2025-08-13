package com.example.s2s.voipgateway.nova;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple voice activity detector for barge-in functionality.
 * Uses energy-based detection to identify when speech is present.
 */
public class VoiceActivityDetector {
    private static final Logger log = LoggerFactory.getLogger(VoiceActivityDetector.class);
    
    // Configuration parameters
    private static final double ENERGY_THRESHOLD = 1000.0; // Minimum energy level to consider as speech
    private static final int MIN_SPEECH_FRAMES = 3; // Minimum consecutive frames to confirm speech
    private static final int SILENCE_FRAMES_TO_RESET = 10; // Frames of silence needed to reset speech detection
    
    // State variables
    private int speechFrames = 0;
    private int silenceFrames = 0;
    private boolean speechDetected = false;
    private long lastSpeechTime = 0;
    
    /**
     * Analyzes audio data to detect voice activity.
     * 
     * @param audioData The PCM audio data to analyze
     * @return true if speech is detected, false otherwise
     */
    public boolean detectVoiceActivity(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return false;
        }
        
        double energy = calculateEnergy(audioData);
        
        if (energy > ENERGY_THRESHOLD) {
            speechFrames++;
            silenceFrames = 0;
            
            if (speechFrames >= MIN_SPEECH_FRAMES && !speechDetected) {
                speechDetected = true;
                lastSpeechTime = System.currentTimeMillis();
                log.debug("Speech detected with energy: {}", energy);
                return true;
            }
        } else {
            silenceFrames++;
            speechFrames = Math.max(0, speechFrames - 1);
            
            if (silenceFrames >= SILENCE_FRAMES_TO_RESET && speechDetected) {
                speechDetected = false;
                log.debug("Speech ended after {} ms", System.currentTimeMillis() - lastSpeechTime);
            }
        }
        
        return false;
    }
    
    /**
     * Calculates the energy (RMS) of the audio signal.
     * 
     * @param audioData PCM audio data (16-bit samples)
     * @return The calculated energy
     */
    private double calculateEnergy(byte[] audioData) {
        long sum = 0;
        int sampleCount = audioData.length / 2; // 16-bit samples
        
        for (int i = 0; i < audioData.length - 1; i += 2) {
            // Convert bytes to 16-bit sample
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            sum += sample * sample;
        }
        
        if (sampleCount == 0) {
            return 0.0;
        }
        
        return Math.sqrt((double) sum / sampleCount);
    }
    
    /**
     * Resets the voice activity detector state.
     */
    public void reset() {
        speechFrames = 0;
        silenceFrames = 0;
        speechDetected = false;
        lastSpeechTime = 0;
    }
    
    /**
     * Checks if speech is currently being detected.
     * @return true if speech is currently detected
     */
    public boolean isSpeechActive() {
        return speechDetected;
    }
}
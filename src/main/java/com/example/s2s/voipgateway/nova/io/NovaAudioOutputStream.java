package com.example.s2s.voipgateway.nova.io;

import com.example.s2s.voipgateway.constants.MediaTypes;
import com.example.s2s.voipgateway.constants.SonicAudioConfig;
import com.example.s2s.voipgateway.constants.SonicAudioTypes;
import com.example.s2s.voipgateway.nova.event.AudioInputEvent;
import com.example.s2s.voipgateway.nova.event.EndAudioContent;
import com.example.s2s.voipgateway.nova.event.NovaSonicEvent;
import com.example.s2s.voipgateway.nova.event.StartAudioContent;
import com.example.s2s.voipgateway.nova.observer.InteractObserver;
import com.example.s2s.voipgateway.nova.transcode.UlawToPcmTranscoder;
import com.example.s2s.voipgateway.nova.AbstractNovaS2SEventHandler;

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

    public NovaAudioOutputStream(InteractObserver<NovaSonicEvent> observer, String promptName) {
        this.observer = observer;
        this.promptName = promptName;
        this.contentName = UUID.randomUUID().toString();
    }
    
    /**
     * Constructor that accepts an event handler for barge-in functionality.
     */
    public NovaAudioOutputStream(InteractObserver<NovaSonicEvent> observer, String promptName, AbstractNovaS2SEventHandler eventHandler) {
        this.observer = observer;
        this.promptName = promptName;
        this.contentName = UUID.randomUUID().toString();
        this.eventHandler = eventHandler;
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
        startSent=true;
    }

    @Override
    public void write(int b) throws IOException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void close() throws IOException {
        observer.onNext(new EndAudioContent(EndAudioContent.ContentEnd.builder()
                .promptName(promptName)
                .contentName(contentName)
                .build()));
        
        // Clear the tracked content name since this content is ending
        if (eventHandler != null) {
            eventHandler.clearCurrentUserContentName();
        }
        
        if (audioFileOutput!=null) {
            audioFileOutput.close();
            audioFileOutput=null;
        }
        observer.onComplete();
    }
}

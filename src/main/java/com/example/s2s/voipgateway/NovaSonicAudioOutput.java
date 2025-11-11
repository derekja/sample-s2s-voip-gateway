package com.example.s2s.voipgateway;

import com.example.s2s.voipgateway.nova.event.NovaSonicEvent;
import com.example.s2s.voipgateway.nova.io.NovaAudioOutputStream;
import com.example.s2s.voipgateway.nova.observer.InteractObserver;
import com.example.s2s.voipgateway.nova.AbstractNovaS2SEventHandler;
import com.example.s2s.voipgateway.nova.ContentLifecycleManager;
import org.mjsip.media.RtpStreamReceiver;
import org.mjsip.media.RtpStreamReceiverListener;
import org.mjsip.media.rx.*;
import org.mjsip.rtp.RtpPayloadFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zoolu.net.UdpSocket;
import org.zoolu.sound.CodecType;
import org.zoolu.util.Encoder;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;

/**
 * mjSIP AudioReceiver implementation for Nova Sonic
 */
public class NovaSonicAudioOutput implements AudioReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(AudioFileReceiver.class);
    private final InteractObserver<NovaSonicEvent> inputObserver;
    private final String promptName;
    private final AbstractNovaS2SEventHandler eventHandler;
    private final ContentLifecycleManager lifecycleManager;

    public NovaSonicAudioOutput(InteractObserver<NovaSonicEvent> inputObserver, String promptName, ContentLifecycleManager lifecycleManager) {
        this.inputObserver = inputObserver;
        this.promptName = promptName;
        this.eventHandler = null;
        this.lifecycleManager = lifecycleManager;
    }

    public NovaSonicAudioOutput(InteractObserver<NovaSonicEvent> inputObserver, String promptName, AbstractNovaS2SEventHandler eventHandler, ContentLifecycleManager lifecycleManager) {
        this.inputObserver = inputObserver;
        this.promptName = promptName;
        this.eventHandler = eventHandler;
        this.lifecycleManager = lifecycleManager;
    }

    @Override
    public AudioRxHandle createReceiver(RtpReceiverOptions options, UdpSocket socket, AudioFormat audio_format,
                                        CodecType codec, int payload_type, RtpPayloadFormat payloadFormat,
                                        int sample_rate, int channels, Encoder additional_decoder,
                                        RtpStreamReceiverListener listener) throws IOException {
        NovaAudioOutputStream outputStream = eventHandler != null ?
            new NovaAudioOutputStream(inputObserver, promptName, eventHandler, lifecycleManager) :
            new NovaAudioOutputStream(inputObserver, promptName, lifecycleManager);
        RtpStreamReceiver receiver = new RtpStreamReceiver(options, outputStream, additional_decoder, payloadFormat, socket, listener) {
            protected void onRtpStreamReceiverTerminated(Exception error) {
                super.onRtpStreamReceiverTerminated(error);
                try {
                    outputStream.close();
                } catch (IOException ex) {
                    LOG.error("Closing audio stream failed: {}", outputStream, ex);
                }

            }
        };
        return new RtpAudioRxHandler(receiver);
    }
}


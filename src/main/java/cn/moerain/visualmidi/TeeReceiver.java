package cn.moerain.visualmidi;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

public class TeeReceiver implements Receiver {
    private final MidiVisualizer visualizer;
    private Receiver target;
    private volatile boolean closed = false;

    public TeeReceiver(MidiVisualizer visualizer) {
        this.visualizer = visualizer;
    }

    public synchronized void setTarget(Receiver target) {
        this.target = target;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (closed) return;
        // always forward to visualization
        visualizer.onMidi(message, timeStamp);
        // conditionally forward to actual target device (respect mute/solo for channel voice messages)
        Receiver t;
        synchronized (this) { t = target; }
        if (t == null) return;
        try {
            if (message instanceof javax.sound.midi.ShortMessage sm) {
                int cmd = sm.getCommand();
                int ch = sm.getChannel();
                boolean channelVoice = (cmd >= 0x80 && cmd <= 0xE0);
                if (channelVoice) {
                    boolean anySolo = visualizer.anySolo();
                    boolean allowed;
                    synchronized (visualizer) { // use visualizer monitors for consistency
                        boolean isMuted = visualizer.isMuted(ch);
                        boolean isSolo = visualizer.isSolo(ch);
                        allowed = !isMuted && (!anySolo || isSolo);
                    }
                    if (!allowed) return; // suppress audio
                }
            }
        } catch (Throwable ignore) {}
        t.send(message, timeStamp);
    }

    @Override
    public void close() {
        closed = true;
        Receiver t;
        synchronized (this) { t = target; }
        if (t != null) t.close();
    }
}

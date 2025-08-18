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
        // forward to visualization
        visualizer.onMidi(message, timeStamp);
        // forward to actual target device
        Receiver t;
        synchronized (this) { t = target; }
        if (t != null) {
            t.send(message, timeStamp);
        }
    }

    @Override
    public void close() {
        closed = true;
        Receiver t;
        synchronized (this) { t = target; }
        if (t != null) t.close();
    }
}

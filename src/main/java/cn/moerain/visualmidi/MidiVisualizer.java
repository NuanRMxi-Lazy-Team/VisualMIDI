package cn.moerain.visualmidi;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MidiVisualizer {
    public static final int CHANNELS = 16;

    private final ChannelState[] channels = new ChannelState[CHANNELS];

    public MidiVisualizer() {
        for (int i = 0; i < CHANNELS; i++) channels[i] = new ChannelState();
    }

    public void onMidi(MidiMessage message, long timeStamp) {
        if (message instanceof ShortMessage sm) {
            int ch = sm.getChannel();
            switch (sm.getCommand()) {
                case ShortMessage.NOTE_ON -> {
                    int note = sm.getData1();
                    int vel = sm.getData2();
                    if (vel == 0) {
                        channels[ch].noteOff(note);
                    } else {
                        channels[ch].noteOn(note, vel);
                    }
                }
                case ShortMessage.NOTE_OFF -> channels[ch].noteOff(sm.getData1());
                case ShortMessage.PROGRAM_CHANGE -> channels[ch].setProgram(sm.getData1());
                case ShortMessage.CONTROL_CHANGE -> {
                    // sustain pedal could be handled here if desired
                }
            }
        }
    }

    public ChannelState getChannel(int ch) { return channels[ch]; }

    public static class ChannelState {
        private static final int BUFFER_SIZE = 2048; // for drawing
        private final float[] buffer = new float[BUFFER_SIZE];
        private int writePos = 0;
        private final Map<Integer, ActiveNote> activeNotes = new HashMap<>();
        private int program = 0;
        private String instrumentName = GMInstruments.getName(0);
        private double phaseAccum = 0;
        private double lastTimeNs = System.nanoTime();

        public synchronized void noteOn(int note, int velocity) {
            ActiveNote an = new ActiveNote(note, velocity);
            activeNotes.put(note, an);
        }

        public synchronized void noteOff(int note) {
            activeNotes.remove(note);
        }

        public synchronized void setProgram(int program) {
            this.program = program;
            this.instrumentName = GMInstruments.getName(program);
        }

        public synchronized String getInstrumentName() {
            return instrumentName;
        }

        public synchronized float[] getRecentWaveform(int length) {
            // advance synthesis a bit to keep buffer live
            synthAdvance();
            if (length > buffer.length) length = buffer.length;
            float[] out = new float[length];
            int start = (writePos - length + buffer.length) % buffer.length;
            for (int i = 0; i < length; i++) {
                out[i] = buffer[(start + i) % buffer.length];
            }
            return out;
        }

        private void synthAdvance() {
            double now = System.nanoTime();
            double dt = (now - lastTimeNs) / 1_000_000_000.0; // seconds
            if (dt <= 0) return;
            lastTimeNs = now;
            // simulate generation at 8kHz for visualization simplicity
            double sampleRate = 8000.0;
            int samples = (int)Math.max(1, Math.min(512, Math.round(dt * sampleRate)));
            for (int i = 0; i < samples; i++) {
                double sample = 0.0;
                // sum simple sines for active notes
                Iterator<ActiveNote> it = activeNotes.values().iterator();
                while (it.hasNext()) {
                    ActiveNote an = it.next();
                    sample += an.nextSample(sampleRate);
                    if (an.isFinished()) it.remove();
                }
                // soft clip
                float s = (float)(sample);
                if (s > 1) s = 1; else if (s < -1) s = -1;
                buffer[writePos] = s;
                writePos = (writePos + 1) % buffer.length;
            }
        }
    }

    private static class ActiveNote {
        private final int note;
        private final double freq;
        private double phase = 0;
        private double amp;
        private double env = 1.0;
        private int lifeSamples = 0;

        ActiveNote(int note, int velocity) {
            this.note = note;
            this.freq = 440.0 * Math.pow(2, (note - 69) / 12.0);
            this.amp = velocity / 127.0 * 0.3; // modest amplitude
        }

        double nextSample(double sampleRate) {
            lifeSamples++;
            // simple AD envelope: first 10ms attack, then decay depending on time
            double attackSamples = sampleRate * 0.01;
            double decayStart = attackSamples;
            if (lifeSamples < attackSamples) {
                env = lifeSamples / attackSamples;
            } else {
                env = Math.max(0.0, env * 0.9995); // slow decay
            }
            phase += (2 * Math.PI * freq) / sampleRate;
            double s = Math.sin(phase) * amp * env;
            return s;
        }

        boolean isFinished() {
            return env < 0.0005;
        }
    }
}

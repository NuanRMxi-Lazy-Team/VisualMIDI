package cn.moerain.visualmidi;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;

public class MidiEngine {
    private final MidiVisualizer visualizer = new MidiVisualizer();
    private Sequencer sequencer;
    private Receiver targetReceiver; // selected output receiver
    private TeeReceiver teeReceiver; // forwards to targetReceiver and visualizer
    private Synthesizer softwareSynth; // for software output
    private DeviceOption currentDevice = DeviceOption.softwareSynth();
    private Soundbank loadedSoundbank;

    public MidiEngine() {
        try {
            sequencer = MidiSystem.getSequencer(false); // we provide our Receiver
            sequencer.open();
            teeReceiver = new TeeReceiver(visualizer);
            sequencer.getTransmitter().setReceiver(teeReceiver);
            // default to software synth
            setOutputDevice(DeviceOption.softwareSynth());
        } catch (MidiUnavailableException e) {
            throw new RuntimeException("MIDI unavailable: " + e.getMessage(), e);
        }
    }

    public MidiVisualizer getVisualizer() { return visualizer; }

    public void loadMidi(File midiFile) throws InvalidMidiDataException, IOException {
        Sequence seq = MidiSystem.getSequence(midiFile);
        sequencer.setSequence(seq);
    }

    public void loadSf2(File sf2File) throws Exception {
        // Attempt to load SF2 soundbank using Java internal class if available
        try {
            Soundbank sb = MidiSystem.getSoundbank(sf2File);
            if (sb == null) {
                // Fallback to com.sun.media.sound.SF2Soundbank if not detected
                sb = (Soundbank)Class.forName("com.sun.media.sound.SF2Soundbank").getConstructor(File.class).newInstance(sf2File);
            }
            this.loadedSoundbank = sb;
            if (softwareSynth != null && softwareSynth.isOpen()) {
                softwareSynth.unloadAllInstruments(softwareSynth.getDefaultSoundbank());
                softwareSynth.loadAllInstruments(sb);
            }
        } catch (Throwable t) {
            throw new Exception("Failed to load SF2: " + t.getMessage(), t);
        }
    }

    public void setOutputDevice(DeviceOption option) throws MidiUnavailableException {
        this.currentDevice = option;
        // Close previous receiver/synth
        if (targetReceiver != null) {
            targetReceiver.close();
            targetReceiver = null;
        }
        if (softwareSynth != null) {
            softwareSynth.close();
            softwareSynth = null;
        }
        // Open new device
        if (option.getType() == DeviceOption.Type.SOFTWARE_SYNTH) {
            softwareSynth = MidiSystem.getSynthesizer();
            softwareSynth.open();
            if (loadedSoundbank != null) {
                softwareSynth.unloadAllInstruments(softwareSynth.getDefaultSoundbank());
                softwareSynth.loadAllInstruments(loadedSoundbank);
            }
            targetReceiver = softwareSynth.getReceiver();
        } else {
            MidiDevice device = MidiSystem.getMidiDevice(option.getInfo());
            device.open();
            targetReceiver = device.getReceiver();
        }
        // Update tee receiver to forward to target
        teeReceiver.setTarget(targetReceiver);
    }

    public void play() {
        sequencer.start();
    }

    public void pause() {
        if (sequencer.isRunning()) {
            sequencer.stop();
        } else {
            sequencer.start();
        }
    }

    public void stop() {
        sequencer.stop();
        sequencer.setTickPosition(0);
    }
}

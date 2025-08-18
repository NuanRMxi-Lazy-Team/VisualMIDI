package cn.moerain.visualmidi;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VisualMIDIApp extends JFrame {
    private final MidiEngine midiEngine;
    private final JComboBox<DeviceOption> deviceCombo;
    private final JButton playBtn;
    private final JButton pauseBtn;
    private final JButton stopBtn;
    private final JLabel statusLabel;
    private final ChannelsPanel channelsPanel;

    private File currentMidiFile;

    public VisualMIDIApp() {
        super("Visual MIDI Player - Waveform");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        this.midiEngine = new MidiEngine();
        this.channelsPanel = new ChannelsPanel(midiEngine.getVisualizer());

        setLayout(new BorderLayout());
        setJMenuBar(createMenuBar());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        deviceCombo = new JComboBox<>();
        refreshDevices();
        JButton refreshBtn = new JButton("Refresh Devices");
        refreshBtn.addActionListener(e -> refreshDevices());

        JButton openMidiBtn = new JButton("Open MIDI...");
        openMidiBtn.addActionListener(this::openMidi);
        JButton openSf2Btn = new JButton("Load SF2...");
        openSf2Btn.addActionListener(this::openSf2);

        playBtn = new JButton("Play");
        pauseBtn = new JButton("Pause");
        stopBtn = new JButton("Stop");
        playBtn.addActionListener(e -> play());
        pauseBtn.addActionListener(e -> pause());
        stopBtn.addActionListener(e -> stop());

        top.add(new JLabel("Output:"));
        top.add(deviceCombo);
        top.add(refreshBtn);
        top.add(openSf2Btn);
        top.add(openMidiBtn);
        top.add(playBtn);
        top.add(pauseBtn);
        top.add(stopBtn);

        add(top, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(channelsPanel);
        add(scroll, BorderLayout.CENTER);

        statusLabel = new JLabel("Ready");
        add(statusLabel, BorderLayout.SOUTH);

        // Device selection behavior
        deviceCombo.addActionListener(e -> {
            DeviceOption opt = (DeviceOption) deviceCombo.getSelectedItem();
            if (opt != null) {
                try {
                    midiEngine.setOutputDevice(opt);
                    status("Output set to " + opt.getDisplayName());
                } catch (Exception ex) {
                    error("Failed to set device: " + ex.getMessage());
                }
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem openMidi = new JMenuItem("Open MIDI...");
        openMidi.addActionListener(this::openMidi);
        JMenuItem openSf2 = new JMenuItem("Load SF2...");
        openSf2.addActionListener(this::openSf2);
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> dispose());
        file.add(openMidi);
        file.add(openSf2);
        file.addSeparator();
        file.add(exit);
        bar.add(file);
        return bar;
    }

    private void openMidi(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose MIDI file");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MIDI Files", "mid", "midi") );
        if (currentMidiFile != null) chooser.setCurrentDirectory(currentMidiFile.getParentFile());
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            currentMidiFile = chooser.getSelectedFile();
            try {
                midiEngine.loadMidi(currentMidiFile);
                status("Loaded MIDI: " + currentMidiFile.getName());
            } catch (Exception ex) {
                error("Failed to load MIDI: " + ex.getMessage());
            }
        }
    }

    private void openSf2(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose SoundFont (.sf2)");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SoundFont 2", "sf2"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File sf2 = chooser.getSelectedFile();
            try {
                midiEngine.loadSf2(sf2);
                status("Loaded SF2: " + sf2.getName());
            } catch (Exception ex) {
                error("Failed to load SF2: " + ex.getMessage());
            }
        }
    }

    private void play() {
        try {
            midiEngine.play();
            status("Playing" + (currentMidiFile != null ? (": " + currentMidiFile.getName()) : ""));
        } catch (Exception ex) {
            error("Play failed: " + ex.getMessage());
        }
    }

    private void pause() {
        try {
            midiEngine.pause();
            status("Paused");
        } catch (Exception ex) {
            error("Pause failed: " + ex.getMessage());
        }
    }

    private void stop() {
        try {
            midiEngine.stop();
            status("Stopped");
        } catch (Exception ex) {
            error("Stop failed: " + ex.getMessage());
        }
    }

    private void status(String text) {
        statusLabel.setText(text);
    }

    private void error(String text) {
        statusLabel.setText(text);
        JOptionPane.showMessageDialog(this, text, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void refreshDevices() {
        List<DeviceOption> options = new ArrayList<>();
        options.add(DeviceOption.softwareSynth());
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice dev = MidiSystem.getMidiDevice(info);
                if (dev.getMaxReceivers() != 0) {
                    options.add(DeviceOption.hardware(info));
                }
            } catch (Exception ignore) {
            }
        }
        DefaultComboBoxModel<DeviceOption> model = new DefaultComboBoxModel<>(options.toArray(new DeviceOption[0]));
        deviceCombo.setModel(model);
        deviceCombo.setSelectedIndex(0);
    }

    // Container that lays out channel waveform panels
    private static class ChannelsPanel extends JPanel {
        private final MidiVisualizer visualizer;
        private final List<WaveformPanel> panels = new ArrayList<>();

        public ChannelsPanel(MidiVisualizer visualizer) {
            super(null);
            this.visualizer = visualizer;
            setBackground(Color.DARK_GRAY);
            setPreferredSize(new Dimension(1100, 16 * 120));
            setLayout(new GridLayout(16, 1, 2, 2));
            for (int ch = 0; ch < 16; ch++) {
                WaveformPanel panel = new WaveformPanel(visualizer, ch);
                panels.add(panel);
                add(panel);
            }
            Timer t = new Timer(16, e -> repaint());
            t.start();
        }
    }
}

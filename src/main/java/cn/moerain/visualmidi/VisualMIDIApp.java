package cn.moerain.visualmidi;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class VisualMIDIApp extends JFrame {
    private static final ResourceBundle msgs = ResourceBundle.getBundle("messages", Locale.getDefault(), new UTF8Control());
    private enum ViewMode { WAVEFORM, BARS, WMP, SC88 }
    private final MidiEngine midiEngine;
    private final JComboBox<DeviceOption> deviceCombo;
    private final JButton playBtn;
    private final JButton pauseBtn;
    private final JButton stopBtn;
    private final JLabel statusLabel;
    private final ChannelsPanel channelsPanel;
    private final JToggleButton viewToggle;

    private File currentMidiFile;

    public VisualMIDIApp() {
        super(MessageFormat.format(msgs.getString("app.title"), msgs.getString("mode.waveform")));
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
        JButton refreshBtn = new JButton(msgs.getString("btn.refreshDevices"));
        refreshBtn.addActionListener(e -> refreshDevices());

        JButton openMidiBtn = new JButton(msgs.getString("btn.openMidi"));
        openMidiBtn.addActionListener(this::openMidi);
        JButton openSf2Btn = new JButton(msgs.getString("btn.loadSf2"));
        openSf2Btn.addActionListener(this::openSf2);

        playBtn = new JButton(msgs.getString("btn.play"));
        pauseBtn = new JButton(msgs.getString("btn.pause"));
        stopBtn = new JButton(msgs.getString("btn.stop"));
        playBtn.addActionListener(e -> play());
        pauseBtn.addActionListener(e -> pause());
        stopBtn.addActionListener(e -> stop());

        viewToggle = new JToggleButton(msgs.getString("mode.waveform"));
        viewToggle.setToolTipText(msgs.getString("view.tooltip"));
        viewToggle.addActionListener(e -> {
            // cycle through modes on each click
            ViewMode next;
            switch (channelsPanel.getMode()) {
                case WAVEFORM -> next = ViewMode.BARS;
                case BARS -> next = ViewMode.WMP;
                case WMP -> next = ViewMode.SC88;
                default -> next = ViewMode.WAVEFORM;
            }
            channelsPanel.setMode(next);
            setTitle(MessageFormat.format(msgs.getString("app.title"), switch (next) {
                case WAVEFORM -> msgs.getString("mode.waveform");
                case BARS -> msgs.getString("mode.bars");
                case WMP -> msgs.getString("mode.wmp");
                case SC88 -> msgs.getString("mode.sc88");
            }));
            viewToggle.setText(switch (next) {
                case WAVEFORM -> msgs.getString("mode.waveform");
                case BARS -> msgs.getString("mode.bars");
                case WMP -> msgs.getString("mode.wmp");
                case SC88 -> msgs.getString("mode.sc88");
            });
        });

        top.add(new JLabel(msgs.getString("label.output")));
        top.add(deviceCombo);
        top.add(refreshBtn);
        top.add(openSf2Btn);
        top.add(openMidiBtn);
        top.add(playBtn);
        top.add(pauseBtn);
        top.add(stopBtn);
        top.add(viewToggle);

        add(top, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(channelsPanel);
        add(scroll, BorderLayout.CENTER);

        statusLabel = new JLabel(msgs.getString("status.ready"));
        add(statusLabel, BorderLayout.SOUTH);

        // Device selection behavior
        deviceCombo.addActionListener(e -> {
            DeviceOption opt = (DeviceOption) deviceCombo.getSelectedItem();
            if (opt != null) {
                try {
                    midiEngine.setOutputDevice(opt);
                    status(MessageFormat.format(msgs.getString("status.outputSet"), opt.getDisplayName()));
                } catch (Exception ex) {
                    error(MessageFormat.format(msgs.getString("error.setDevice"), ex.getMessage()));
                }
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu(msgs.getString("menu.file"));
        JMenuItem openMidi = new JMenuItem(msgs.getString("menu.openMidi"));
        openMidi.addActionListener(this::openMidi);
        JMenuItem openSf2 = new JMenuItem(msgs.getString("menu.loadSf2"));
        openSf2.addActionListener(this::openSf2);
        JMenuItem exit = new JMenuItem(msgs.getString("menu.exit"));
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
        chooser.setDialogTitle(msgs.getString("dialog.chooseMidi"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(msgs.getString("filter.midi"), "mid", "midi") );
        if (currentMidiFile != null) chooser.setCurrentDirectory(currentMidiFile.getParentFile());
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            currentMidiFile = chooser.getSelectedFile();
            try {
                midiEngine.loadMidi(currentMidiFile);
                status(MessageFormat.format(msgs.getString("status.loadedMidi"), currentMidiFile.getName()));
            } catch (Exception ex) {
                error(MessageFormat.format(msgs.getString("error.loadMidi"), ex.getMessage()));
            }
        }
    }

    private void openSf2(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(msgs.getString("dialog.chooseSf2"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(msgs.getString("filter.sf2"), "sf2"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File sf2 = chooser.getSelectedFile();
            try {
                midiEngine.loadSf2(sf2);
                status(MessageFormat.format(msgs.getString("status.loadedSf2"), sf2.getName()));
            } catch (Exception ex) {
                error(MessageFormat.format(msgs.getString("error.loadSf2"), ex.getMessage()));
            }
        }
    }

    private void play() {
        try {
            midiEngine.play();
            status(MessageFormat.format(msgs.getString("status.playing"), (currentMidiFile != null ? (": " + currentMidiFile.getName()) : "")));
        } catch (Exception ex) {
            error("Play failed: " + ex.getMessage());
        }
    }

    private void pause() {
        try {
            midiEngine.pause();
            status(msgs.getString("status.paused"));
        } catch (Exception ex) {
            error("Pause failed: " + ex.getMessage());
        }
    }

    private void stop() {
        try {
            midiEngine.stop();
            status(msgs.getString("status.stopped"));
        } catch (Exception ex) {
            error("Stop failed: " + ex.getMessage());
        }
    }

    private void status(String text) {
        statusLabel.setText(text);
    }

    private void error(String text) {
        statusLabel.setText(text);
        JOptionPane.showMessageDialog(this, text, msgs.getString("error.title"), JOptionPane.ERROR_MESSAGE);
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

    // Container that lays out channel panels (waveform or bar graph)
    private static class ChannelsPanel extends JPanel {
        private final MidiVisualizer visualizer;
        private final java.util.List<JPanel> panels = new ArrayList<>();
        private ViewMode mode = ViewMode.WAVEFORM;

        public ChannelsPanel(MidiVisualizer visualizer) {
            super(null);
            this.visualizer = visualizer;
            setBackground(Color.DARK_GRAY);
            setPreferredSize(new Dimension(1100, 16 * 120));
            setLayout(new GridLayout(16, 1, 2, 2));
            rebuild();
            Timer t = new Timer(16, e -> repaint());
            t.start();
        }

        public void setMode(ViewMode mode) {
            if (this.mode == mode) return;
            this.mode = mode;
            rebuild();
        }

        public ViewMode getMode() {
            return this.mode;
        }

        private void rebuild() {
            removeAll();
            panels.clear();
            for (int ch = 0; ch < 16; ch++) {
                JPanel panel = switch (mode) {
                    case WAVEFORM -> new WaveformPanel(visualizer, ch);
                    case BARS -> new BarGraphPanel(visualizer, ch);
                    case WMP -> new WMPSpectrumPanel(visualizer, ch);
                    case SC88 -> new SC88ProPanel(visualizer, ch);
                };
                panels.add(panel);
                add(panel);
            }
            revalidate();
            repaint();
        }
    }
}

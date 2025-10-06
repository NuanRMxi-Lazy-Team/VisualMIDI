package cn.moerain.visualmidi;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Roland Sound Canvas SC-88Pro-like channel strip.
 * Not an exact replica, but mimics:
 * - Part number, instrument name, program number
 * - Level meter with peak, simple activity LEDs for notes
 * - Mute/Solo per channel and program up/down buttons (track controls)
 */
public class SC88ProPanel extends JPanel {
    private final MidiVisualizer visualizer;
    private final int channel;

    // simple track controls state (mute/solo at UI level only)
    private boolean muted = false;
    private boolean solo = false;

    private final JToggleButton muteBtn = new JToggleButton("M");
    private final JToggleButton soloBtn = new JToggleButton("S");
    private final JButton progDecBtn = new JButton("-");
    private final JButton progIncBtn = new JButton("+");

    public SC88ProPanel(MidiVisualizer visualizer, int channel) {
        this.visualizer = visualizer;
        this.channel = channel;
        setPreferredSize(new Dimension(1000, 110));
        setBackground(new Color(25, 25, 25));
        setLayout(new BorderLayout());

        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 6, 2, 6);
        c.anchor = GridBagConstraints.WEST;

        JLabel part = label(String.format("PART %02d", channel + 1));
        part.setFont(part.getFont().deriveFont(Font.BOLD, 12f));
        c.gridx = 0; c.gridy = 0; left.add(part, c);

        JLabel instr = new JLabel();
        instr.setForeground(new Color(210, 255, 210));
        instr.setFont(new Font("Monospaced", Font.PLAIN, 12));
        c.gridy = 1; left.add(instr, c);

        JLabel pgm = new JLabel();
        pgm.setForeground(new Color(190, 220, 255));
        pgm.setFont(new Font("Monospaced", Font.PLAIN, 11));
        c.gridy = 2; left.add(pgm, c);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.setOpaque(false);
        styleButton(muteBtn, new Color(255, 90, 90));
        styleButton(soloBtn, new Color(120, 210, 255));
        styleButton(progDecBtn, new Color(200, 200, 200));
        styleButton(progIncBtn, new Color(200, 200, 200));
        btns.add(muteBtn); btns.add(soloBtn); btns.add(progDecBtn); btns.add(progIncBtn);
        c.gridy = 3; left.add(btns, c);

        // wire controls
        muteBtn.addActionListener(e -> visualizer.setMute(channel, muteBtn.isSelected()));
        soloBtn.addActionListener(e -> visualizer.setSolo(channel, soloBtn.isSelected()));
        // Program +/- only update label locally by simulating program change in visualizer state
        progDecBtn.addActionListener(e -> {
            int p = visualizer.getChannel(channel).getProgram();
            visualizer.getChannel(channel).setProgram(Math.max(0, p - 1));
        });
        progIncBtn.addActionListener(e -> {
            int p = visualizer.getChannel(channel).getProgram();
            visualizer.getChannel(channel).setProgram(Math.min(127, p + 1));
        });

        add(left, BorderLayout.WEST);

        // Update labels timer
        Timer t = new Timer(100, e -> {
            String name = visualizer.getChannel(channel).getInstrumentName();
            instr.setText(name);
            // Program number unknown directly; parse from GM name index if possible
            // We store it implicitly; display just the name as primary.
            pgm.setText("  ");
            repaint();
        });
        t.start();
    }

    private static JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(new Color(255, 240, 200));
        return l;
    }

    private static void styleButton(AbstractButton b, Color fg) {
        b.setFocusPainted(false);
        b.setMargin(new Insets(2,6,2,6));
        b.setBackground(new Color(40,40,40));
        b.setForeground(fg);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();

        // bezel
        g2.setColor(new Color(35,35,35));
        g2.fillRect(0,0,w,h);
        g2.setColor(new Color(60,60,60));
        g2.drawRect(0,0,w-1,h-1);

        // meter area on right mimicking SC-88 style 2 rows of LEDs
        int top = 10;
        int meterH = h - top - 12;
        int meterW = Math.min(520, w - 340);
        int meterX = w - meterW - 10;
        int meterY = top;

        float[] wf = visualizer.getChannel(channel).getRecentWaveform(Math.max(256, meterW));
        float peak = 0f; double sum = 0;
        for (float v : wf) { float a = Math.abs(v); if (a>peak) peak=a; sum += v*v; }
        float rms = wf.length>0 ? (float)Math.sqrt(sum / wf.length) : 0f;

        // segments
        int segments = 24;
        int segGap = 2;
        int segW = (meterW - (segments-1)*segGap) / segments;
        int segH = (meterH - 8) / 2; // two rows

        // convert rms/peak to number of segments
        int rmsSegs = Math.round(clamp(rms,0,1) * segments);
        int peakSegs = Math.round(clamp(peak,0,1) * segments);

        // draw two rows L/R style (we only have mono visual; duplicate)
        drawLedRow(g2, meterX, meterY, segW, segGap, segH, segments, rmsSegs, peakSegs);
        drawLedRow(g2, meterX, meterY + segH + 6, segW, segGap, segH, segments, rmsSegs, peakSegs);

        g2.dispose();
    }

    private void drawLedRow(Graphics2D g2, int x, int y, int segW, int gap, int h, int segments, int rmsSegs, int peakSegs) {
        for (int i = 0; i < segments; i++) {
            int sx = x + i * (segW + gap);
            Color c = ledColor(i, segments);
            // background slot
            g2.setColor(new Color(25,25,25));
            g2.fillRoundRect(sx, y, segW, h, 4, 4);
            // fill if under rms
            if (i < rmsSegs) {
                g2.setColor(c);
                g2.fillRoundRect(sx+1, y+1, segW-2, h-2, 4, 4);
            }
        }
        // peak marker
        int px = x + Math.min(segments-1, peakSegs) * (segW + gap);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(px, y, segW, h, 4, 4);
    }

    private static Color ledColor(int idx, int total) {
        float t = idx / (float)(total-1);
        // green to yellow to red
        if (t < 0.75f) {
            return new Color(100, 255, 120);
        } else if (t < 0.9f) {
            return new Color(255, 230, 100);
        } else {
            return new Color(255, 120, 100);
        }
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

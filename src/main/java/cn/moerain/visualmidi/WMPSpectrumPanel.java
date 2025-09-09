package cn.moerain.visualmidi;

import javax.swing.*;
import java.awt.*;

/**
 * A simple spectrum-style visualization inspired by classic Windows Media Player bars.
 * It renders a set of vertical bands with peak hold and smooth decay per channel.
 * Note: This computes a pseudo-spectrum by sampling the recent waveform buffer
 * with a small bank of resonant filters (cheap approximation, not a real FFT).
 */
public class WMPSpectrumPanel extends JPanel {
    private final MidiVisualizer visualizer;
    private final int channel;
    // number of bands
    private static final int BANDS = 20;
    private final float[] levels = new float[BANDS];
    private final float[] peaks = new float[BANDS];

    public WMPSpectrumPanel(MidiVisualizer visualizer, int channel) {
        this.visualizer = visualizer;
        this.channel = channel;
        setPreferredSize(new Dimension(1000, 110));
        setBackground(new Color(20, 20, 20));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();

        // Title
        String title = String.format("Ch %02d - %s", channel + 1, visualizer.getChannel(channel).getInstrumentName());
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
        g2.drawString(title, 8, 16);

        // Get samples (more than width for some resolution)
        float[] wf = visualizer.getChannel(channel).getRecentWaveform(Math.max(512, w));

        // Analyze into BANDS using a simple bank of rectified moving-average windows at different scales
        // This mimics low-to-high frequency bands roughly exponentially spaced.
        if (wf.length > 0) {
            for (int b = 0; b < BANDS; b++) {
                // window size grows smaller for higher bands
                // map band to frequency window using exponential mapping
                double t = b / (double)(BANDS - 1);
                int win = (int) (wf.length * (0.25 * Math.pow(0.5, t))); // from ~25% down to small
                if (win < 4) win = 4;
                // compute average rectified energy over sliding window samples
                int step = Math.max(1, win / 4);
                double maxAvg = 0;
                for (int i = 0; i + win <= wf.length; i += step) {
                    double sum = 0;
                    for (int j = 0; j < win; j++) {
                        sum += Math.abs(wf[i + j]);
                    }
                    double avg = sum / win;
                    if (avg > maxAvg) maxAvg = avg;
                }
                float level = (float) Math.min(1.0, maxAvg * 1.5); // boost a bit
                // smooth with attack/decay
                float prev = levels[b];
                float a = 0.4f; // attack
                float d = 0.08f; // decay
                float target = Math.max(level, prev * (1 - d));
                levels[b] = prev + a * (target - prev);
                // peak hold with slow fall
                peaks[b] = Math.max(peaks[b] * 0.96f, levels[b]);
            }
        } else {
            // decay when no data
            for (int b = 0; b < BANDS; b++) {
                levels[b] *= 0.9f;
                peaks[b] *= 0.96f;
            }
        }

        int top = 22;
        int bottomPadding = 8;
        int availH = h - top - bottomPadding;
        int gap = 4;
        int bandWidth = Math.max(6, (w - 16 - (BANDS - 1) * gap) / BANDS);
        int x = 8;

        // background grid lines
        g2.setColor(new Color(45,45,45));
        for (int i = 0; i <= 5; i++) {
            int yy = top + (availH * i / 5);
            g2.drawLine(8, yy, w - 8, yy);
        }

        // Colors
        Color base = Color.getHSBColor((channel / 16f), 0.7f, 0.95f);
        for (int b = 0; b < BANDS; b++) {
            float lv = clamp(levels[b], 0f, 1f);
            int bh = Math.max(2, Math.round(availH * lv));
            int y = top + (availH - bh);

            // gradient bar
            GradientPaint gp = new GradientPaint(x, y, base.brighter(), x, y + bh, base.darker());
            g2.setPaint(gp);
            g2.fillRoundRect(x, y, bandWidth, bh, 4, 4);

            // peak marker
            int ph = Math.max(2, Math.round(availH * clamp(peaks[b], 0f, 1f)));
            int py = top + (availH - ph);
            g2.setColor(new Color(255, 255, 255, 180));
            g2.fillRect(x, py, bandWidth, 2);

            x += bandWidth + gap;
        }

        g2.dispose();
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

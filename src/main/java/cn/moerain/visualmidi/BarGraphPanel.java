package cn.moerain.visualmidi;

import javax.swing.*;
import java.awt.*;

public class BarGraphPanel extends JPanel {
    private final MidiVisualizer visualizer;
    private final int channel;

    public BarGraphPanel(MidiVisualizer visualizer, int channel) {
        this.visualizer = visualizer;
        this.channel = channel;
        setPreferredSize(new Dimension(1000, 110));
        setBackground(new Color(30, 30, 30));
    }

    private static Color channelColor(int ch) {
        // Distinct-ish colors for channels
        float hue = (ch / 16.0f);
        return Color.getHSBColor(hue, 0.6f, 0.95f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();

        // background grid
        g2.setColor(new Color(50, 50, 50));
        for (int i = 0; i < 10; i++) {
            int y = i * h / 10;
            g2.drawLine(0, y, w, y);
        }

        String title = String.format("Ch %02d - %s", channel + 1, visualizer.getChannel(channel).getInstrumentName());
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
        g2.drawString(title, 8, 16);

        // Compute volume over recent waveform as RMS and peak
        float[] wf = visualizer.getChannel(channel).getRecentWaveform(Math.max(64, w));
        float peak = 0f;
        double sumSq = 0.0;
        for (float v : wf) {
            float av = Math.abs(v);
            if (av > peak) peak = av;
            sumSq += v * v;
        }
        float rms = wf.length > 0 ? (float) Math.sqrt(sumSq / wf.length) : 0f;

        int barLeft = 8;
        int barTop = 24;
        int barWidth = w - 16;
        int barHeight = h - barTop - 16;

        // Frame
        g2.setColor(new Color(70, 70, 70));
        g2.fillRoundRect(barLeft, barTop, barWidth, barHeight, 8, 8);

        // Draw volume bars stacked: RMS (darker) then Peak (lighter) to its height
        int rmsHeight = Math.round(barHeight * clamp(rms, 0f, 1f));
        int peakHeight = Math.round(barHeight * clamp(peak, 0f, 1f));

        int baseY = barTop + barHeight;
        Color col = channelColor(channel);
        g2.setColor(col.darker());
        g2.fillRect(barLeft + 2, baseY - rmsHeight, barWidth - 4, rmsHeight);
        g2.setColor(col);
        g2.fillRect(barLeft + 2, baseY - peakHeight, barWidth - 4, Math.max(2, peakHeight));

        // Scale marks
        g2.setColor(new Color(200, 200, 200, 140));
        g2.setFont(g2.getFont().deriveFont(11f));
        for (int i = 0; i <= 5; i++) {
            int yy = baseY - (i * barHeight / 5);
            g2.drawLine(barLeft, yy, barLeft + barWidth, yy);
            String label = switch (i) {
                case 0 -> "0%";
                case 1 -> "20%";
                case 2 -> "40%";
                case 3 -> "60%";
                case 4 -> "80%";
                default -> "100%";
            };
            g2.drawString(label, barLeft + 6, Math.max(barTop + 14, yy - 2));
        }

        g2.dispose();
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

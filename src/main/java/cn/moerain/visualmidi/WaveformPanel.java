package cn.moerain.visualmidi;

import javax.swing.*;
import java.awt.*;

public class WaveformPanel extends JPanel {
    private final MidiVisualizer visualizer;
    private final int channel;

    public WaveformPanel(MidiVisualizer visualizer, int channel) {
        this.visualizer = visualizer;
        this.channel = channel;
        setPreferredSize(new Dimension(1000, 110));
        setBackground(new Color(30, 30, 30));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();

        // background grid
        g2.setColor(new Color(50,50,50));
        for (int i=0;i<10;i++) {
            int y = i * h / 10;
            g2.drawLine(0, y, w, y);
        }

        // title (instrument name)
        String title = String.format("Ch %02d - %s", channel+1, visualizer.getChannel(channel).getInstrumentName());
        g2.setColor(Color.WHITE);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
        g2.drawString(title, 8, 16);

        // waveform
        float[] wf = visualizer.getChannel(channel).getRecentWaveform(w);
        int mid = h/2;
        g2.setColor(new Color(0x55FF77));
        int prevX = 0;
        int prevY = mid;
        for (int x=0; x<w; x++) {
            float s = wf.length>0 ? wf[Math.min(x, wf.length-1)] : 0f;
            int y = mid - (int)(s * (h/2 - 20));
            if (x>0) g2.drawLine(prevX, prevY, x, y);
            prevX = x; prevY = y;
        }

        g2.dispose();
    }
}

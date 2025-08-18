package cn.moerain.visualmidi;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            VisualMIDIApp app = new VisualMIDIApp();
            app.setVisible(true);
        });
    }
}

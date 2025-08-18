package cn.moerain.visualmidi;

import javax.sound.midi.MidiDevice;

public class DeviceOption {
    public enum Type { SOFTWARE_SYNTH, HARDWARE }
    private final Type type;
    private final MidiDevice.Info info; // for hardware

    private DeviceOption(Type type, MidiDevice.Info info) {
        this.type = type;
        this.info = info;
    }

    public static DeviceOption softwareSynth() {
        return new DeviceOption(Type.SOFTWARE_SYNTH, null);
    }

    public static DeviceOption hardware(MidiDevice.Info info) {
        return new DeviceOption(Type.HARDWARE, info);
    }

    public Type getType() {
        return type;
    }

    public MidiDevice.Info getInfo() {
        return info;
    }

    public String getDisplayName() {
        if (type == Type.SOFTWARE_SYNTH) return "Java Software Synth";
        return info.getName() + " - " + info.getDescription();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}

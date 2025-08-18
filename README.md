[![Gradle Package](https://github.com/NuanRMxi-Lazy-Team/VisualMIDI/actions/workflows/gradle-publish.yml/badge.svg)](https://github.com/NuanRMxi-Lazy-Team/VisualMIDI/actions/workflows/gradle-publish.yml)

# VisualMIDI

Project Introduction
VisualMIDI is an audio software that can visualize the waveforms of MIDI tracks and support custom output devices and sound libraries. It is designed to provide music producers, arrangers and audio engineers with an intuitive and flexible tool to better understand and edit MIDI data.

## Functional Features

- **MIDI Track Waveform Visualization ** : Visually view the waveforms of each track in the MIDI file, helping to quickly locate and edit MIDI data.
- ** Support for custom output devices ** : Connect to external synthesizers, audio interfaces or other hardware devices to adapt to different music production and performance scenarios.
- ** Support for custom sound libraries ** : Load your own sound library (such as the SF2 sound library) and use specific sounds when playing MIDI files.

## Usage scenarios

- ** Music Production ** : Enhance creative efficiency and better understand and edit MIDI data.
- "Audio Analysis" : Analyze the structure and rhythm of MIDI files.
- ** Live Performance ** : Supports custom output devices and sound libraries, suitable for live performance scenarios.

## Installation and Operation

System Requirements

- "Operating System" : Windows, macOS or Linux

- **Java** : Java 17 or a higher version needs to be installed

Installation steps

1. ** Clone Repository **

```bash
git clone https://github.com/NuanRMxi-Lazy-Team/VisualMIDI.git
cd VisualMIDI
```

2. "Build the Project"

Build a project using Gradle

```bash
./gradlew build
```

3. Run the program:

Run the built JAR file

```bash
java-jar build/libs/VisualMIDI-1.0-BETA.jar
```

## Usage Method

### Open the MIDI file

After launching VisualMIDI, open MIDI files through the menu or by dragging and dropping.

The software will automatically load and display the waveform of the MIDI track.

### bConfigure the output device

In the Settings menu, select the output device, which supports connection to external synthesizers or other audio devices.

### Load the sound library

Load a custom sound library (such as an SF2 file) in the Settings menu to use specific sounds during playback.

### Edit MIDI data

- Directly edit MIDI tracks through the visual interface to adjust waveforms, notes, etc.

## Open source license

VisualMIDI is open-source software and follows the [GNU General Public License v3.0](LICENSE). You are free to use, modify and distribute the software, but you must comply with the requirements of the license, such as providing the source code and retaining the copyright notice, etc.
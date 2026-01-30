# A20 SOM Carrier Board - Audio Device Tree Configuration

## Overview

This document describes the audio device tree configuration for the Olimex A20 SOM Carrier Board (Rev-C). The configuration enables audio playback through the TPA2012D2 amplifier and microphone input via the A20's internal codec.

## Hardware Summary

### Audio Signal Path

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────┐
│   A20 SoC       │     │   TPA2012D2     │     │   Speaker   │
│                 │     │   Amplifier     │     │             │
│  HPOUTL ────────┼────►│ INL      OUTL ──┼────►│ Left        │
│  HPOUTR ────────┼────►│ INR      OUTR ──┼────►│ Right       │
│                 │     │                 │     │             │
│  PH15 ──────────┼────►│ SD (Enable)     │     │             │
│  PH14 ──────────┼────►│ GAIN1           │     │             │
└─────────────────┘     └─────────────────┘     └─────────────┘

┌─────────────────┐
│   Microphone    │
│                 │
│  MIC ───────────┼────► MICIN1 (A20 Internal Codec)
│                 │
└─────────────────┘
```

### Key Components

| Component | Part Number | Function |
|-----------|-------------|----------|
| Audio Codec | A20 Internal | DAC/ADC, analog outputs |
| Amplifier | TPA2012D2RTJ (U11) | Class-D stereo amplifier |
| Microphone | Electret | Audio input |

### GPIO Assignments

| GPIO | Pin | Function | Logic |
|------|-----|----------|-------|
| PH15 | SOM-GPIO Pin 13 | Amplifier Enable (AUDIO_SD) | Active Low (LOW=enabled, HIGH=shutdown) |
| PH14 | SOM-GPIO Pin 11 | Amplifier Gain (GAIN1) | Active High |

### Amplifier Enable Circuit

The TPA2012D2 shutdown pin is controlled through a MOSFET (Q2):
- When **PH15 is HIGH**: Q2 conducts, pulling AUDIO_SD to ground → Amplifier **OFF**
- When **PH15 is LOW**: Q2 is off, R37 pulls AUDIO_SD high → Amplifier **ON**

This inverted logic is why `GPIO_ACTIVE_LOW` is used in the device tree.

### Gain Configuration

The TPA2012D2 gain is set by GAIN0 and GAIN1 pins:

| GAIN1 (PH14) | GAIN0 (Hardwired LOW) | Gain |
|--------------|----------------------|------|
| 0 | 0 | 6 dB |
| 1 | 0 | 12 dB |

GAIN0 is hardwired to ground via R43, so PH14 controls between 6dB and 12dB gain.

---

## Device Tree Configuration

### 1. Speaker Amplifier Node

```dts
speaker_amp: speaker-amp {
    compatible = "simple-audio-amplifier";
    /* PH15: HIGH=shutdown, LOW=enabled */
    enable-gpios = <&pio 7 15 GPIO_ACTIVE_LOW>;
    sound-name-prefix = "Speaker Amp";
};
```

**Explanation:**
- `compatible = "simple-audio-amplifier"` - Uses the generic Linux amplifier driver
- `enable-gpios = <&pio 7 15 GPIO_ACTIVE_LOW>` - PH15 (Port H pin 15) controls enable
- `sound-name-prefix` - ALSA widget naming prefix

### 2. Sound Card Node

```dts
sound {
    compatible = "simple-audio-card";
    simple-audio-card,name = "A20-SOM-Carrier";

    simple-audio-card,aux-devs = <&speaker_amp>;

    simple-audio-card,widgets =
        "Speaker", "Speaker",
        "Microphone", "Microphone";

    simple-audio-card,routing =
        "Speaker Amp INL", "HP Left",
        "Speaker Amp INR", "HP Right",
        "Speaker", "Speaker Amp OUTL",
        "Speaker", "Speaker Amp OUTR",
        "MIC1", "Microphone";

    simple-audio-card,cpu {
        sound-dai = <&codec>;
    };

    simple-audio-card,codec {
        sound-dai = <&codec>;
    };
};
```

**Explanation:**
- `simple-audio-card` - Generic sound card driver for simple audio setups
- `aux-devs` - References the amplifier as an auxiliary device
- `widgets` - Declares external audio widgets (speaker, microphone)
- `routing` - Defines the DAPM (Dynamic Audio Power Management) routes:
  - Codec HP outputs → Amplifier inputs
  - Amplifier outputs → Speaker
  - Microphone → Codec MIC1 input

### 3. Gain Control Node

```dts
audio_gain: audio-gain {
    compatible = "regulator-fixed";
    regulator-name = "audio-gain";
    /* PH14 controls GAIN1 pin of TPA2012D2 */
    gpio = <&pio 7 14 GPIO_ACTIVE_HIGH>;
    regulator-min-microvolt = <3300000>;
    regulator-max-microvolt = <3300000>;
    enable-active-high;
    regulator-always-on;
    status = "okay";
};
```

**Explanation:**
- Uses `regulator-fixed` to control PH14 GPIO
- `regulator-always-on` keeps the GPIO in a defined state
- Can be controlled via sysfs or userspace to toggle between 6dB/12dB gain

### 4. Internal Codec Configuration

```dts
&codec {
    #sound-dai-cells = <0>;
    pinctrl-names = "default";
    pinctrl-0 = <&audio_pins>;
    allwinner,audio-routing =
        "HP Left", "HPOUTL",
        "HP Right", "HPOUTR",
        "MIC1", "Mic Bias",
        "Mic Bias", "Microphone";
    status = "okay";
};
```

**Explanation:**
- `#sound-dai-cells = <0>` - Required for sound-dai reference
- `allwinner,audio-routing` - Internal codec DAPM routes:
  - HPOUTL/R → HP Left/Right (headphone outputs)
  - Microphone → Mic Bias → MIC1 (microphone with bias voltage)

### 5. Pin Control

```dts
&pio {
    audio_pins: audio-pins {
        pins = "PH14", "PH15";
        function = "gpio_out";
        drive-strength = <20>;
    };
};
```

**Explanation:**
- Configures PH14 and PH15 as GPIO outputs
- Sets drive strength for reliable signal levels

---

## Kernel Configuration

The following kernel configs are required (already set in `linux-mainline_%.bbappend`):

```
CONFIG_SOUND=y
CONFIG_SND=y
CONFIG_SND_SOC=y
CONFIG_SND_SUN4I_CODEC=y
```

---

## Testing Audio

### Check Sound Card

```bash
# List sound cards
aplay -l

# Expected output:
# card 0: A20SOMCarrier [A20-SOM-Carrier], device 0: ...
```

### Test Playback

```bash
# Play test tone
speaker-test -c 2 -t sine

# Play audio file
aplay /path/to/audio.wav
```

### Test Recording

```bash
# Record 5 seconds of audio
arecord -d 5 -f cd test.wav

# Play back recording
aplay test.wav
```

### ALSA Mixer Controls

```bash
# Show all mixer controls
amixer contents

# Enable microphone capture
amixer set "Mic1" Capture on
amixer set "Mic1 Boost" 2

# Set playback volume
amixer set "HP" 80%
```

---

## Troubleshooting

### No Sound Output

| Check | Command | Expected |
|-------|---------|----------|
| Sound card detected | `aplay -l` | Shows "A20-SOM-Carrier" |
| Codec loaded | `dmesg \| grep codec` | sun4i-codec probed |
| Amplifier GPIO | `cat /sys/kernel/debug/gpio` | PH15 = low (enabled) |
| Mixer unmuted | `amixer get HP` | Playback not muted |

### No Microphone Input

| Check | Command | Expected |
|-------|---------|----------|
| Capture enabled | `amixer get "Mic1" Capture` | [on] |
| Mic bias | `amixer get "Mic Bias"` | Enabled |
| Recording works | `arecord -vv test.wav` | Shows VU meter activity |

### Amplifier Not Enabling

```bash
# Check GPIO state
cat /sys/kernel/debug/gpio | grep PH15

# Manually toggle (for testing)
echo 495 > /sys/class/gpio/export  # PH15 = 7*32 + 15 = 239, adjust for your system
echo out > /sys/class/gpio/gpio495/direction
echo 0 > /sys/class/gpio/gpio495/value  # Enable amp
```

---

## Changes from Previous Configuration

### What Was Fixed

| Issue | Before | After |
|-------|--------|-------|
| Amplifier GPIO | PE5 (incorrect) | PH15 (correct per schematic) |
| Node type | `gpio-leds` (wrong) | `simple-audio-amplifier` |
| Codec routing | Missing | Full DAPM routing added |
| Gain control | Not present | PH14 GPIO control added |
| Microphone | Not configured | MIC1 with bias configured |

### Files Modified

1. `layers/meta-balena-allwinner/recipes-kernel/linux/files/0001-fix-device-tree-for-olimex-a20-som-carrier-board.patch`

### Reference Files

1. `sun7i-a20-olinuxino-micro.dts.upstream` - Original upstream kernel DTS for reference

---

## References

- [TPA2012D2 Datasheet](https://www.ti.com/product/TPA2012D2) - Amplifier IC
- [Allwinner A20 User Manual](https://linux-sunxi.org/A20) - SoC documentation
- [Linux simple-audio-card Documentation](https://www.kernel.org/doc/Documentation/devicetree/bindings/sound/simple-card.txt)
- [Linux sun4i-codec Driver](https://www.kernel.org/doc/Documentation/devicetree/bindings/sound/allwinner,sun4i-a10-codec.yaml)
- Olimex A20-SOM Carrier Board Rev-C Schematic

# A20 Audio Device-Tree Abstraction – Carrier Board

## 1. Scope
This document maps the **Audio Out & Microphone** hardware block of the A20 SOM Carrier Board to a **Linux Device Tree (DTS) abstraction**.

It is intended for:
- Firmware / BSP developers
- AI agents generating or validating DTS files
- Audio bring-up and debugging

This is a **logical mapping**, not a copy‑paste DTS from a vendor kernel.

---

## 2. Hardware Audio Architecture (Recap)

### 2.1 Functional Topology

```
A20 SoC
 ├─ I²S / PCM (digital audio)
 ├─ I²C (control)
 │
 └── External Audio Codec
      ├─ DAC → Line Out / Speaker Amp
      ├─ ADC ← Microphone Input
      └─ Analog bias & filtering
```

Key points:
- The A20 **does not output analog audio directly**
- All analog paths are external to the SoM
- Clocking is master/slave configurable

---

## 3. Device-Tree Building Blocks

Audio support is composed of **four logical DT nodes**:

1. A20 Audio Interface (I²S)
2. External Audio Codec
3. Simple-Audio-Card glue
4. Optional Audio Amplifier / GPIO controls

---

## 4. A20 I²S / PCM Controller Node

### 4.1 Logical Role
- Digital audio source/sink
- Provides bit clock (BCLK), LRCLK, and data

### 4.2 DT Abstraction

```dts
i2s0: i2s@01c22000 {
    compatible = "allwinner,sun7i-a20-i2s";
    reg = <0x01c22000 0x400>;
    interrupts = <GIC_SPI 13 IRQ_TYPE_LEVEL_HIGH>;
    clocks = <&ccu CLK_I2S0>, <&ccu CLK_APB_I2S0>;
    clock-names = "mod", "bus";
    dmas = <&dma 3>, <&dma 3>;
    dma-names = "tx", "rx";
    status = "okay";
};
```

AI agent reasoning:
- If audio is silent → verify this node is enabled
- If clocks missing → codec will not lock

---

## 5. External Audio Codec Node

### 5.1 Logical Role
- Converts I²S digital audio ↔ analog signals
- Controlled via I²C

### 5.2 Typical Codec Representation
(Exact `compatible` depends on the actual chip)

```dts
audio_codec: codec@1a {
    compatible = "vendor,codec-model";
    reg = <0x1a>;
    #sound-dai-cells = <0>;
    clocks = <&ccu CLK_I2S0>;
    clock-names = "mclk";
    status = "okay";
};
```

AI agent reasoning:
- I²C probe success ≠ audio works
- Codec needs clocks + routing

---

## 6. Simple-Audio-Card (Glue Layer)

### 6.1 Purpose
Binds:
- CPU DAI (A20 I²S)
- Codec DAI
- Defines signal roles and direction

### 6.2 DT Representation

```dts
sound {
    compatible = "simple-audio-card";
    simple-audio-card,name = "A20-Carrier-Audio";
    simple-audio-card,format = "i2s";
    simple-audio-card,bitclock-master = <&codec_dai>;
    simple-audio-card,frame-master = <&codec_dai>;

    simple-audio-card,cpu {
        sound-dai = <&i2s0>;
    };

    codec_dai: simple-audio-card,codec {
        sound-dai = <&audio_codec>;
    };
};
```

Clocking rules:
- Codec is often master (cleaner analog clocking)
- Can be reversed if hardware requires

---

## 7. Microphone Path Mapping

### 7.1 Hardware Reality
- Electret mic
- Bias provided by codec
- ADC channel inside codec

### 7.2 DT Implications

- No separate DT node for mic
- Mic enable handled via codec mixer controls

Example (userspace):
```
amixer set Mic Capture on
amixer set Mic Boost 2
```

AI agent reasoning:
> If playback works but capture does not → codec routing issue, not I²S

---

## 8. Speaker / Amplifier Control (Optional)

If speaker amp is GPIO‑enabled:

```dts
speaker_amp: speaker-amp {
    compatible = "simple-audio-amplifier";
    enable-gpios = <&pio 7 3 GPIO_ACTIVE_HIGH>;
    sound-name-prefix = "Speaker";
};
```

Then referenced by the sound card.

---

## 9. Common Failure Modes (AI-Useful)

| Symptom | Likely Cause |
|------|------------|
| Codec probes, no sound | I²S disabled or clock mismatch |
| Playback OK, mic dead | Codec mixer / bias config |
| Loud noise | Missing MCLK or wrong master |
| Sound only one channel | I²S format mismatch |

---

## 10. AI Agent Summary Model

To reason about audio:

1. I²C sees codec?
2. I²S node enabled?
3. MCLK present?
4. simple-audio-card binds both?
5. Mixer paths enabled?

If **any step fails**, audio fails.

---

## 11. Optional Next Steps
I can generate:
- A **fully merged DTS snippet** for your kernel version
- An **ALSA control map** (what mixers must be set)
- An **AI troubleshooting decision tree**
- A **machine-readable YAML DT model**

Tell me which direction you want to go.


# A20 Audio – ALSA Mixer Dependency Graph

## 1. Purpose
This document describes the **ALSA mixer dependency graph** for the A20 carrier board audio subsystem.

It explains:
- Which ALSA controls exist
- How they depend on each other
- The minimum set of controls required for **speaker playback** and **microphone capture**

This is optimized for **AI reasoning**, scripted bring-up, and automated diagnostics.

---

## 2. Conceptual Model

ALSA audio is a **directed graph**, not a flat list of switches.

```
PCM Stream
  ↓
Digital Volume / Switch
  ↓
DAC / ADC
  ↓
Analog Mixers
  ↓
Output Drivers / Mic Bias
```

If **any node in the path is disabled**, audio fails silently.

---

## 3. Playback (Speaker / Line-Out) Dependency Graph

### 3.1 Logical Flow

```
PCM Playback
 └─ DAC Enable
     └─ DAC Mixer
         └─ Line / Speaker Mixer
             └─ Output Driver
                 └─ External Amplifier (optional GPIO)
```

---

### 3.2 ALSA Control Dependencies (Playback)

| Order | ALSA Control | Required State | Notes |
|-----|-------------|---------------|------|
| 1 | PCM Playback Switch | ON | Created by sound card |
| 2 | DAC Playback Switch | ON | Enables DAC core |
| 3 | DAC Playback Volume | > 0 | Digital attenuation |
| 4 | Mixer DAC → Output | ON | Routes DAC signal |
| 5 | Line / Speaker Switch | ON | Selects physical output |
| 6 | Output Volume | > 0 | Analog gain |
| 7 | Speaker Amp Enable | ON | If GPIO-controlled |

AI agent rule:
> If PCM is active but no sound → check steps 2–5 first

---

### 3.3 Minimal Playback Bring-Up Script

```bash
amixer set 'DAC Playback Switch' on
amixer set 'DAC Playback Volume' 80%
amixer set 'Line Playback Switch' on
amixer set 'Line Playback Volume' 80%
```

If speaker amp exists:

```bash
amixer set 'Speaker Switch' on
```

---

## 4. Capture (Microphone) Dependency Graph

### 4.1 Logical Flow

```
Microphone
 └─ Mic Bias
     └─ Mic Boost / PGA
         └─ ADC Mixer
             └─ ADC Enable
                 └─ PCM Capture
```

---

### 4.2 ALSA Control Dependencies (Capture)

| Order | ALSA Control | Required State | Notes |
|-----|-------------|---------------|------|
| 1 | Mic Bias | ON | Powers electret mic |
| 2 | Mic Boost | 1–3 | Analog gain stage |
| 3 | Mic Capture Switch | ON | Enables routing |
| 4 | ADC Capture Switch | ON | Enables ADC |
| 5 | ADC Capture Volume | > 0 | Digital gain |
| 6 | PCM Capture Switch | ON | ALSA stream |

AI agent rule:
> Playback works but capture fails → bias or ADC mixer missing

---

### 4.3 Minimal Capture Bring-Up Script

```bash
amixer set 'Mic Bias' on
amixer set 'Mic Boost' 2
amixer set 'Mic Capture Switch' on
amixer set 'ADC Capture Switch' on
amixer set 'ADC Capture Volume' 80%
```

---

## 5. Clock & Power Dependencies (Hidden but Critical)

Some dependencies are **not visible in ALSA**, but still mandatory.

### 5.1 Hidden Preconditions

| Dependency | Why it matters |
|---------|---------------|
| I²S MCLK running | Codec analog blocks need clock |
| Codec PLL locked | Prevents noise / silence |
| I²S bitclock active | ADC/DAC won’t start |
| Codec powered | ALSA controls still appear otherwise |

AI agent rule:
> ALSA controls visible ≠ hardware functional

---

## 6. Common Failure Signatures (Pattern-Based)

| Symptom | Likely Missing Node |
|------|------------------|
| Silent playback | DAC mixer or output switch |
| Loud hiss | Missing MCLK or wrong master |
| Capture flat zero | Mic Bias OFF |
| Very low mic level | Mic Boost too low |
| One channel only | I²S format mismatch |

---

## 7. AI-Friendly Dependency Summary

### 7.1 Playback Must-Have Set

```
DAC ON
↓
Mixer Route ON
↓
Output Driver ON
```

### 7.2 Capture Must-Have Set

```
Mic Bias ON
↓
ADC ON
↓
PCM Capture ON
```

Any missing edge breaks audio.

---

## 8. Diagnostic Algorithm (For AI Agents)

1. Does ALSA card exist?
2. Is PCM stream active?
3. Are DAC/ADC switches ON?
4. Are mixer routes enabled?
5. Is output/input selected?
6. Are clocks running?

Fail fast at the first NO.

---

## 9. Optional Extensions
I can generate:
- A **machine-readable mixer graph (YAML/JSON)**
- A **one-command bring-up script**
- A **self-test audio loopback procedure**
- An **AI prompt template** for automated debugging

Just tell me how this will be consumed.


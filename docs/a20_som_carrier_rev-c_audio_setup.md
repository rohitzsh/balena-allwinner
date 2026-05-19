# A20-SOM Carrier Rev C - Audio Output Hardware Reference

## Overview

This document describes the audio output path on the A20-SOM Carrier Board Rev C. The design uses the Allwinner A20 SoC's internal audio codec feeding a TPA3112D1PWP Class-D amplifier (U11, 28-pin HTSSOP) to drive a speaker in Bridge-Tied Load (BTL) configuration.

All amplifier control GPIOs reside on Port E of the A20 SoC (PE5, PE7, PE8).

---

## Hardware Block Diagram

```
A20 SoC Internal Codec
    │
    │                              TPA3112D1PWP (U11, 28-pin)
    │                                                    │
    │                                              Speaker Output
    │
    │
    │   SHUTDOWN CONTROL
    │   ════════════════
    │
    │       3.3V
    │        │
    │      R28 (100k) ← pull-up, boot-safe default = HIGH
    │        │
    PE5 ─────┼──── Gate
    │              │
    │           BSS138 (Q2)
    │              │
    │           Drain ───┬── R30 (10k) ── VIN (6-16V)
    │              │     │
    │           Source   SD pin (Pin 1, TPA3112D1)
    │              │
    │             GND
    │
    │   PE5 HIGH → MOSFET ON  → SD pulled to GND → Shutdown
    │   PE5 LOW  → MOSFET OFF → R30 pulls SD to VIN → Enabled
    │
    │
    │   GAIN CONTROL
    │   ════════════
    │
    PE7 ── R32 (1k) ──→ GAIN0 (Pin 5)
    PE8 ── R32 (1k) ──→ GAIN1 (Pin 6)
    │
    │   GAIN1  GAIN0  │  Gain
    │     0      0    │  20 dB
    │     0      1    │  26 dB
    │     1      0    │  32 dB
    │     0      0    │  20 dB  ← current setting (both LOW)
    │
    │
    │   ANALOG SUPPLY
    │   ══════════════
    │
    AUDIO_AVCC ── R48 (100k) ──→ AVCC (Pin 14)
    │
    │
    │   BTL OUTPUT STAGE (LC Filter + Zobel Networks)
    │   ══════════════════════════════════════════════
    │
    │   NEGATIVE OUTPUT
    │   ────────────────
    │
    │   BSN(22) ──┬── BSN(26)
    │             │
    │           C47 (470nF/16V)  ← bootstrap cap
    │             │
    │   OUTN(23) ─┼── OUTN(25) ──── L11 ──┬── SPKR-
    │             │                        │
    │             ├── R34 (10R)            C48 (1nF/50V/COG)
    │             │      │                 │
    │             │   C49 (1nF/50V/COG)   GND
    │             │      │
    │             │     GND
    │             │
    │   PGND(24) ─── GND
    │
    │
    │   POSITIVE OUTPUT
    │   ────────────────
    │
    │   BSP(21) ──┬── BSP(17)
    │             │
    │           C58 (470nF/16V)  ← bootstrap cap
    │             │
    │   OUTP(20) ─┼── OUTP(18) ──── L13 ──┬── SPKR+
    │             │                        │
    │             ├── R45 (10R)           C59 (1nF/50V/COG)
    │             │      │                 │
    │             │   C62 (1nF/50V/COG)   GND
    │             │      │
    │             │     GND
    │             │
    │   PGND(19) ─── GND
```

---

## Circuit Details

### Shutdown Control (PE5)

The shutdown circuit uses an inverted logic scheme via a BSS138 N-channel MOSFET (Q2):

- **R28 (100k pull-up to 3.3V)** ensures the MOSFET gate is HIGH at boot, keeping the amplifier in shutdown as a safe default. This prevents speaker pops or unexpected audio output during power-on and boot sequences.
- **PE5 LOW** (amp enabled): MOSFET is OFF, R30 (10k) pulls the SD pin up to VIN (6-16V), bringing the amplifier out of shutdown.
- **PE5 HIGH** (amp shutdown): MOSFET is ON, pulling the SD pin to GND, putting the amplifier into low-power shutdown mode.

From a software perspective: writing `0` to PE5 enables the amplifier, writing `1` shuts it down.

### Gain Control (PE7, PE8)

Gain is set via two GPIO lines connected to the TPA3112D1's GAIN0 (pin 5) and GAIN1 (pin 6) inputs through 1k series resistors (R32) for current limiting and protection:

| GAIN1 (PE8) | GAIN0 (PE7) | Gain |
|:-----------:|:-----------:|:----:|
| 0           | 0           | 20 dB |
| 0           | 1           | 26 dB |
| 1           | 0           | 32 dB |
| 1           | 1           | 36 dB |

The current configuration drives both pins LOW for 20 dB gain.

Unlike the shutdown pin, the gain pins have no pull-up or pull-down resistors for a defined boot default. Their state is indeterminate until software configures them. This is harmless because the amplifier remains in shutdown (via R28) until PE5 is explicitly driven LOW.

### Analog Supply (AVCC)

The AUDIO_AVCC rail feeds the amplifier's analog supply pin (AVCC, pin 14) through R48 (100k). This resistor acts as a filter element, isolating the codec's analog reference from noise on the main power rail.

### BTL Output Stage

The amplifier drives the speaker in a Bridge-Tied Load (BTL) configuration. The output stage is symmetric across the positive and negative channels:

#### Output Filter (per channel)

Each H-bridge output pair feeds through an LC low-pass filter to reconstruct the analog audio waveform from the Class-D PWM output:

- **Positive channel:** OUTP (pins 18, 20) → L13 → SPKR+
- **Negative channel:** OUTN (pins 23, 25) → L11 → SPKR-

#### Bootstrap Capacitors

Each channel has a bootstrap capacitor between the BSx and OUTx pins to provide gate drive voltage for the high-side FETs in the H-bridge:

- **Positive:** C58 (470nF/16V) between BSP (pins 17, 21) and OUTP
- **Negative:** C47 (470nF/16V) between BSN (pins 22, 26) and OUTN

#### Zobel Networks (pre-inductor)

A series R-C Zobel network on each output (before the inductor) provides impedance matching and ensures amplifier stability with varying speaker loads:

- **Positive:** R45 (10R) → C62 (1nF/50V/COG) → GND
- **Negative:** R34 (10R) → C49 (1nF/50V/COG) → GND

#### EMI Filter Capacitors (post-inductor)

A capacitor from each speaker terminal to ground filters high-frequency EMI on the output:

- **Positive:** C59 (1nF/50V/COG) from SPKR+ to GND
- **Negative:** C48 (1nF/50V/COG) from SPKR- to GND

---

## Pin Summary

| TPA3112D1 Pin | Function | Connection |
|:-------------:|:--------:|:-----------|
| 1             | SD       | Shutdown control via BSS138 (Q2), R30 pull-up to VIN |
| 5             | GAIN0    | PE7 via R32 (1k) |
| 6             | GAIN1    | PE8 via R32 (1k) |
| 14            | AVCC     | AUDIO_AVCC via R48 (100k) |
| 17, 21        | BSP      | Tied together, C58 to OUTP |
| 18, 20        | OUTP     | Tied together, L13 to SPKR+ |
| 19            | PGND     | GND |
| 22, 26        | BSN      | Tied together, C47 to OUTN |
| 23, 25        | OUTN     | Tied together, L11 to SPKR- |
| 24            | PGND     | GND |

## GPIO Summary

| GPIO | Function | Active State | Notes |
|:----:|:--------:|:------------:|:------|
| PE5  | Shutdown | LOW = amp ON | Inverted by BSS138; R28 pull-up defaults to shutdown at boot |
| PE7  | GAIN0   | HIGH = +6 dB | No default pull resistor; indeterminate until configured |
| PE8  | GAIN1   | HIGH = +6 dB | No default pull resistor; indeterminate until configured |

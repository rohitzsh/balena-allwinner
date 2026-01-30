# A20-SOM Carrier Board – Functional & Interface Documentation

## 1. Purpose of This Document
This document is a **machine-readable, structured explanation** of the A20-SOM Carrier Board schematic (Rev C). It is intended for an **AI agent** (or new engineer) to quickly understand:
- Major functional blocks
- Power architecture
- External interfaces
- Signal roles and dependencies
- System-level assumptions

The goal is **system comprehension**, not schematic re‑creation.

---

## 2. System Overview

The board is a **carrier/baseboard** for an **Allwinner A20 System‑on‑Module (SoM)**. The SoM provides:
- Dual‑core ARM CPU
- DDR memory (on-module)
- PMU (power management)
- High‑speed peripherals (USB, Ethernet MAC, Audio, LCD, UARTs, etc.)

The carrier board:
- Breaks out SoM signals
- Adds power input & regulation
- Adds physical connectors (USB, Ethernet, RS232, Audio)
- Adds system control & monitoring

---

## 3. Major Functional Blocks

### 3.1 A20 SOM Connector (Central Block)
- Multiple high‑density board‑to‑board connectors
- Carries:
  - Power rails
  - GPIOs
  - USB, Ethernet, UART, Audio, LCD, SPI, I²C
- The SoM is the **only processor**; all logic is peripheral support

Key assumptions:
- DDR, PMU, and CPU clocking are **on the SoM**
- Carrier board does **no high‑speed memory routing**

---

## 4. Power Supply Architecture

### 4.1 Input Power
- External input: **6–16 V DC**
- Reverse polarity protection + fuse

### 4.2 Power Regulation
- Primary buck regulator converts VIN → system rail
- Generates:
  - +5V (USB, Ethernet, audio amp, peripherals)
  - +3.3V (logic, level translators, PHY IO)
- Additional LDOs for noise‑sensitive domains (audio, analog)

### 4.3 Power Distribution Philosophy
- SoM handles CPU core voltages
- Carrier supplies **clean, current‑capable rails** only

AI agent note:
> Power faults, brownouts, or USB issues often trace back to VIN quality or 5V rail stability.

---

## 5. USB Subsystem

### 5.1 USB OTG
- One USB OTG port
- Micro‑USB or header based
- Supports:
  - Device mode
  - Host mode (with ID pin)

### 5.2 USB Host Ports
- Two USB Host ports
- Each port includes:
  - ESD protection
  - Power switching / current limiting
  - EMI filtering

Control:
- USB power enable controlled by A20 GPIO

---

## 6. Ethernet Subsystem

### 6.1 MAC–PHY Architecture
- A20 provides **Ethernet MAC**
- External **10/100 PHY** on carrier board

### 6.2 Interface
- MII / RMII signals from SoM
- Magnetics integrated or external
- RJ45 with:
  - Link/activity LEDs
  - Integrated transformer

Clocks:
- PHY clock sourced either internally or from SoM (configurable)

---

## 7. Audio Subsystem

### 7.1 Audio Codec
- External audio codec IC
- Connected via:
  - I²S / PCM digital audio
  - I²C control bus

### 7.2 Outputs
- Line‑out / Speaker‑out
- Amplifier stage present
- EMI filtering and AC coupling capacitors

### 7.3 Microphone Input
- Bias provided
- Analog path filtered

AI agent note:
> Audio path includes both digital and analog domains; grounding and power noise are critical.

---

## 8. UART & RS‑232 Interfaces

### 8.1 Native UARTs
- A20 UART signals routed to level translators

### 8.2 RS‑232 Channels
- Multiple RS‑232 ports (IN, 1, 2, 3)
- Each includes:
  - MAX3232‑type transceiver
  - ESD protection
  - DB9 / header connectors

Logic:
- TTL ↔ ±RS‑232 level conversion

---

## 9. Display / Expansion Interfaces

### 9.1 UEXT Connector
- Standard UEXT (used by Olimex‑style modules)
- Carries:
  - SPI
  - I²C
  - UART
  - Power (3.3V)

### 9.2 PCB Headers
- Raw signal breakouts
- Intended for:
  - Debug
  - Expansion
  - Factory testing

---

## 10. System Control & Monitoring

### 10.1 Watchdog
- External hardware watchdog IC
- Monitors heartbeat from A20 GPIO
- Can reset system on failure

Purpose:
- Recovery from software lockups

### 10.2 DIP Switches
- Boot configuration
- Mode selection
- GPIO strapping at startup

AI agent note:
> DIP switch states may affect boot source, console routing, or debug modes.

---

## 11. LEDs & User Button

### 11.1 LEDs
- Status LEDs (power, activity)
- Driven by GPIO via current‑limit resistors

### 11.2 Button
- User or reset button
- Pulled up/down correctly
- Debounce handled in software or RC

---

## 12. Signal Integrity & Protection

- USB: ESD diodes + common‑mode chokes
- Ethernet: magnetics + termination
- RS‑232: TVS diodes
- Power: bulk + ceramic decoupling

Design intent:
> Robust operation in noisy industrial environments

---

## 13. AI Agent Mental Model (Summary)

Think of the board as:

> **A20 SOM (brain)** + **Carrier Board (nervous system & organs)**

- SoM = compute, memory, PMU
- Carrier = power entry, IO translation, physical connectors

Most debugging questions reduce to:
1. Is the rail powered?
2. Is the interface enabled in software?
3. Is the external transceiver powered and clocked?

---

## 14. Suggested Extensions
If you want, I can also generate:
- A **JSON/YAML hardware description** for an AI agent
- A **device‑tree‑style abstraction**
- A **troubleshooting decision tree**
- A **block‑diagram‑only simplified view**

Just tell me how the AI agent will be used (QA, firmware, diagnostics, LLM tool, etc.).


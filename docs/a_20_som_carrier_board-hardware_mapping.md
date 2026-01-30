This document provides the hardware mapping and logical constraints required for an AI agent to generate a valid **Device Tree (DTS/DTSI)** file for the **A20-SOM-Carrier Rev-C** board.

***

# Device Tree Specification: A20-SOM-Carrier Rev-C

## 1. Core SoC Information
*   **Target SoC:** Allwinner A20 (sun7i).
*   **Architecture:** Dual-core ARM Cortex-A7.
*   **Root Compatible:** `olimex,a20-som-carrier`, `allwinner,sun7i-a20`.

## 2. Audio Subsystem (Internal Codec + External Amp)
The board uses the SoC's internal analog codec routed through an external Class-D amplifier.

*   **Internal Codec Node (`&codec`):**
    *   **Status:** `okay`.
    *   **Pins Used:** `HPOUTL` (Pin 39), `HPOUTR` (Pin 37), `MICIN1` (Pin 40), `MICIN2` (Pin 38), and `MBIAS` (Pin 36) on the `A20_CON-LCD` connector.
*   **External Amplifier (U11 - TPA2012D2):**
    *   **Shutdown/Enable Control:** Pin **PH15**. 
    *   **Logic:** PH15 drives MOSFET Q2. A **High** signal on PH15 pulls `AUDIO_SD` low (Shutdown). Therefore, the amplifier is **Active Low** in the device tree (`GPIO_ACTIVE_LOW`).
    *   **Gain Control:** Pin **PH14** is connected to `GAIN1`.
*   **Sound Card Routing (`simple-audio-card`):**
    *   `"Speaker", "HPOUTL"`
    *   `"Speaker", "HPOUTR"`
    *   `"MIC1", "Mic Bias"`
    *   `"Mic Bias", "Microphone"`

## 3. USB Configuration
The board features three USB ports with individual power switching via **SY6280** ICs.

*   **USB-OTG (U8):** Power enabled by signal `USB0-DRV` found on **A20_CON-SOM-GPIO Pin 1**.
*   **USB-Host 1 (U9):** Power enabled by signal `USB1-DRV` found on **A20_CON-SOM-GPIO Pin 3**.
*   **USB-Host 2 (U10):** Power enabled by signal `USB2-DRV` found on **A20_CON-SOM-GPIO Pin 5**.
*   **Logic:** These power switches typically use `GPIO_ACTIVE_HIGH` to enable VBUS.

## 4. UART & RS232 Mapping
The board exposes several UARTs through transceivers for RS232 compatibility.

| UART Instance | Function | Signals | SOM Connector Pins |
| :--- | :--- | :--- | :--- |
| **UART0** | Debug Console | TX0, RX0 | Dedicated `UART` header |
| **UART3** | RS232-1 | TX3, RX3, RTS3, CTS3 | GPIO Pins 17, 19, 21, 23 |
| **UART4** | RS232-2 | TX4, RX4 | GPIO Pins 25, 27 |
| **UART7** | RS232-3 | TX7, RX7 | GPIO Pins 29, 31 |

## 5. Ethernet (RGMII/MII)
*   **Interface:** Connected via the `A20_CON-EPHY` block.
*   **PHY:** The carrier provides the RJ45 jack and magnetics; the PHY logic is typically managed by the A20 internal EMAC or an external PHY on the SOM.

## 6. GPIO & User Interface
*   **Status LED (LED_ST):** 
    *   **Pin:** **PH02**.
    *   **Logic:** Connected via MOSFET Q3; set as `linux,default-trigger = "heartbeat"` or `default-state = "on"`.
*   **Reset Button:** Hardwired to `RESET_N` (Pin 58 of `A20_CON-SOM-GPIO`), usually handled by the A20 hardware reset logic rather than a GPIO-key.
*   **DIP Switches:** Connected to various GPIOs (labeled DIP1-DIP4) on the `A20_CON-SOM-GPIO` connector.

## 7. Power Tree
*   **Main Input:** 6-16VDC.
*   **Regulators:**
    *   **U2 (5V):** Main peripheral power.
    *   **U3 (3.3V):** I/O power.
    *   **U1 (1.2V):** Core/Logic power.

***

### Instructions for AI Agent:
1.  Use the **sun7i-a20.dtsi** as the base include.
2.  Define the `reg_vcc5v0`, `reg_vcc3v3`, and specific USB VBUS regulators using the `regulator-fixed` compatible.
3.  In the `&pio` node, create pinmux settings (`pinctrl-0`) for the UARTs, Audio control pins (PH14, PH15), and the Status LED (PH02).
4.  Ensure the `&codec` node includes the proper routing for a `simple-audio-card` implementation.
5.  Set the `status = "okay"` for `&ehci0`, `&ohci0`, `&ehci1`, `&ohci1`, and `&usbphy`.
# Olimex A20-SOM Carrier Board - Changes Required for balena-allwinner

This document captures all changes made to the `balena-allwinner` fork to support the **Olimex A20-SOM Carrier Board (Rev-C)**. Use this as a reference when syncing with upstream `balena-os/balena-allwinner`.

## Branch Information

- **Branch**: `olimex-a20som-initial-work`
- **Base**: Fork of `balena-os/balena-allwinner`
- **Purpose**: Add BSP support for Olimex A20-SOM on custom carrier board

---

## Summary of Changes

### Files Added (New)

| File | Purpose |
|------|---------|
| `layers/meta-balena-allwinner/conf/machine/olinuxino-a20som.conf` | Machine configuration |
| `layers/meta-balena-allwinner/recipes-bsp/u-boot/u-boot_2023.01.bb` | U-Boot 2023.01 recipe |
| `layers/meta-balena-allwinner/recipes-bsp/u-boot/files/0001-Balena-specific-bootcmd-changes.patch` | Boot command patch |
| `layers/meta-balena-allwinner/recipes-bsp/u-boot/files/0002-Update-CONFIG_SYS_BOOTM_LEN-to-64M.patch` | Bootm length patch |
| `layers/meta-balena-allwinner/recipes-bsp/u-boot/files/0003-Balena-env-on-uboot.patch` | Balena env patch |
| `layers/meta-balena-allwinner/recipes-kernel/linux/files/0001-fix-device-tree-for-olimex-a20-som-carrier-board.patch` | Device tree patch |
| `layers/meta-balena-allwinner/recipes-kernel/linux/files/0001-Add-support-for-enabling-RS232-FET-via-GPIO.patch` | RS232 FET enable (not used currently) |
| `olinuxino-a20som.coffee` | Balena device type definition |
| `olinuxino-a20som.svg` | Device type logo |
| `A20-SOM-Carrier_Rev-C_board_diagram.png` | Board schematic reference |

### Files Modified

| File | Changes |
|------|---------|
| `layers/meta-balena-allwinner/recipes-bsp/u-boot/u-boot_%.bbappend` | New patch references |
| `layers/meta-balena-allwinner/recipes-kernel/linux/linux-mainline_%.bbappend` | DT patch + kernel configs |
| `layers/meta-balena-allwinner/recipes-core/images/balena-image.inc` | olinuxino-a20som image config |
| `layers/meta-balena-allwinner/conf/samples/local.conf.sample` | Added debug tools |
| `.gitignore` | Added `/tmp` |

### Files Deleted (from upstream)

| File | Reason |
|------|--------|
| `layers/meta-balena-allwinner/recipes-bsp/u-boot/files/0001-Add-Resin-specific-boot-command.patch` | Replaced with new patches |
| `layers/meta-balena-allwinner/recipes-bsp/u-boot/files/0002-Change_CONFIG_SYS_BOOTM_LEN_to_64M.patch` | Replaced with new patches |
| `layers/meta-balena-allwinner/recipes-bsp/u-boot/files/resin-specific-env-integration-kconfig_reworked.patch` | Replaced with new patches |

---

## Detailed Changes

### 1. Machine Configuration

**File**: `layers/meta-balena-allwinner/conf/machine/olinuxino-a20som.conf`

```bitbake
#@TYPE: Machine
#@NAME: olinuxino-a20som
#@DESCRIPTION: Machine configuration for the Olimex A20-SOM Evaluation Board, based on Allwinner A20 CPU
#https://github.com/OLIMEX/SOM

require conf/machine/include/sun7i.inc

KERNEL_DEVICETREE = "sun7i-a20-olinuxino-micro.dtb"
UBOOT_MACHINE = "A20-OLinuXino_MICRO_config"
SUNXI_FEX_FILE = "sys_config/a20/olimex_a20_som.fex"
```

---

### 2. U-Boot Configuration

#### 2.1 U-Boot Recipe

**File**: `layers/meta-balena-allwinner/recipes-bsp/u-boot/u-boot_2023.01.bb`

```bitbake
DESCRIPTION = "U-Boot port for sunxi"

require recipes-bsp/u-boot/u-boot.inc
require recipes-bsp/u-boot/u-boot-common.inc

DEPENDS += "bc-native dtc-native python3-setuptools-native"

LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://Licenses/gpl-2.0.txt;md5=b234ee4d69f5fce4486a80fdaf4a4263"

SRCREV = "62e2ad1ceafbfdf2c44d3dc1b6efc81e768a96b9"
SRC_URI = " \
	git://github.com/u-boot/u-boot.git;protocol=https \
	"

S = "${WORKDIR}/git"

UBOOT_LOCALVERSION = "-yocto"
```

#### 2.2 U-Boot bbappend

**File**: `layers/meta-balena-allwinner/recipes-bsp/u-boot/u-boot_%.bbappend`

```bitbake
UBOOT_KCONFIG_SUPPORT = "1"
inherit resin-u-boot
FILESEXTRAPATHS_append := ":${THISDIR}/files"

SRC_URI_remove = " \
		file://resin-specific-env-integration-kconfig.patch \
		file://0001-nanopi_neo_air_defconfig-Enable-eMMC-support.patch \
		"

SRC_URI_append = " \
		file://0001-Balena-specific-bootcmd-changes.patch \
 		file://0002-Update-CONFIG_SYS_BOOTM_LEN-to-64M.patch \
 		file://0003-Balena-env-on-uboot.patch \
 		"
```

#### 2.3 U-Boot Patches

##### Patch 1: Balena Boot Command

**File**: `layers/meta-balena-allwinner/recipes-bsp/u-boot/files/0001-Balena-specific-bootcmd-changes.patch`

Modifies `include/configs/sunxi-common.h`:
- Undefines `CONFIG_BOOTCOMMAND`
- Defines custom boot sequence that:
  - Sets `resin_kernel_load_addr`
  - Runs `resin_set_kernel_root`
  - Sets bootargs with root, rootfstype, console
  - Loads uimage from FAT partition
  - Loads DTB from `/dtb/` directory
  - Boots with `bootm`

##### Patch 2: Bootm Length

**File**: `layers/meta-balena-allwinner/recipes-bsp/u-boot/files/0002-Update-CONFIG_SYS_BOOTM_LEN-to-64M.patch`

```c
#define CONFIG_SYS_BOOTM_LEN   (64 << 20)      /* Increase max gunzip size 64M */
```

##### Patch 3: Balena Environment

**File**: `layers/meta-balena-allwinner/recipes-bsp/u-boot/files/0003-Balena-env-on-uboot.patch`

Modifies `include/env_default.h`:
- Includes `<env_resin.h>`
- Adds `BALENA_ENV` to default environment

---

### 3. Linux Kernel Configuration

#### 3.1 Kernel bbappend

**File**: `layers/meta-balena-allwinner/recipes-kernel/linux/linux-mainline_%.bbappend`

**Additions for olinuxino-a20som**:

```bitbake
SRC_URI_append = " \
    file://general-add-configfs-overlay.patch \
    file://general-add-overlay-compilation-support.patch \
    file://general-sunxi-overlays.patch \
    file://0001-arch-arm-Makefile-Partial-revert-of-https-github.com.patch \
    file://0001-fix-device-tree-for-olimex-a20-som-carrier-board.patch \
"

# Sound support for A20 internal codec
BALENA_CONFIGS_append = " snd"
BALENA_CONFIGS[snd] ="\
    CONFIG_SOUND=y \
    CONFIG_SND=y \
    CONFIG_SND_SOC=y \
    CONFIG_SND_SUN4I_CODEC=y \
"

# AXP209 PMIC power monitoring
BALENA_CONFIGS_append = " axp_power"
BALENA_CONFIGS_DEPS[axp_power] = "\
    CONFIG_TOUCHSCREEN_SUN4I=n \
    CONFIG_IIO=y \
    CONFIG_REGMAP_IRQ=y \
    CONFIG_MFD_SUN4I_GPADC=y \
    CONFIG_MFD_AXP20X=y \
    CONFIG_MFD_AXP20X_I2C=y \
"
BALENA_CONFIGS[axp_power] ="\
    CONFIG_AXP20X_POWER=y \
"

# ConfigFS for device tree overlays
BALENA_CONFIGS_append = " configfs"
BALENA_CONFIGS[configfs] = " \
    CONFIG_OF_CONFIGFS=y \
    CONFIG_OF_OVERLAY=y \
    CONFIG_CONFIGFS_FS=y \
"
```

#### 3.2 Device Tree Patch

**File**: `layers/meta-balena-allwinner/recipes-kernel/linux/files/0001-fix-device-tree-for-olimex-a20-som-carrier-board.patch`

**Changes to `sun7i-a20-olinuxino-micro.dts`**:

| Feature | Details |
|---------|---------|
| **Serial Aliases** | serial2=uart2, serial3=uart4, serial4=uart3, serial5=uart7 |
| **HDMI** | Removed (hdmi-connector, &hdmi, &hdmi_out nodes) |
| **Audio Amplifier** | TPA2012D2 on PH15 (enable, active low) |
| **Audio Gain** | PH14 controls GAIN1 |
| **Sound Card** | simple-audio-card with routing |
| **Codec** | Internal A20 codec with HP out + MIC1 in |
| **UART2** | Enabled with RTS/CTS on PI pins |
| **UART3** | Enabled with RTS/CTS on PG pins |
| **UART4** | Enabled on PG pins |
| **CSI0** | Camera interface on PE0-PE11 |
| **Pinctrl** | audio_pins (PH14, PH15), csi0_8bits_pins |

---

### 4. Balena Image Configuration

**File**: `layers/meta-balena-allwinner/recipes-core/images/balena-image.inc`

**Added section for olinuxino-a20som**:

```bitbake
#
# olinuxino-a20som
#

IMAGE_FSTYPES_append_olinuxino-a20som = " balenaos-img"

# Customize balenaos-img
BALENA_IMAGE_BOOTLOADER_olinuxino-a20som = "u-boot"
BALENA_BOOT_PARTITION_FILES_olinuxino-a20som = " \
    ${KERNEL_IMAGETYPE}${KERNEL_INITRAMFS}-${MACHINE}.bin:/${KERNEL_IMAGETYPE} \
    sun7i-a20-olinuxino-micro.dtb:/dtb/sun7i-a20-olinuxino-micro.dtb \
    u-boot-sunxi-with-spl.bin: \
"
IMAGE_CMD_balenaos-img_append_olinuxino-a20som () {
    # olinuxino-a20som needs uboot written at a specific location
    dd if=${DEPLOY_DIR_IMAGE}/u-boot-sunxi-with-spl.bin of=${BALENA_RAW_IMG} conv=notrunc seek=8 bs=1024
}
```

---

### 5. Local Configuration Sample

**File**: `layers/meta-balena-allwinner/conf/samples/local.conf.sample`

**Added line**:
```bitbake
IMAGE_INSTALL_append = " inotify-tools i2c-tools tcpdump"
```

---

### 6. Balena Device Type Definition

**File**: `olinuxino-a20som.coffee`

```coffee
deviceTypesCommon = require '@resin.io/device-types/common'
{ networkOptions, commonImg, instructions } = deviceTypesCommon

module.exports =
	version: 1
	slug: 'orange-pi-one'  # Note: slug needs to be updated for production
	name: 'orange-pi-one'
	aliases: [ 'orange-pi-one' ]
	arch: 'armv7hf'
	state: 'new'
	community: true
	private: false

	instructions: commonImg.instructions
	gettingStartedLink:
		windows: 'http://docs.resin.io/#/pages/installing/gettingStarted.md#windows'
		osx: 'http://docs.resin.io/#/pages/installing/gettingStarted.md#on-mac-and-linux'
		linux: 'http://docs.resin.io/#/pages/installing/gettingStarted.md#on-mac-and-linux'
	supportsBlink: true

	options: [ networkOptions.group ]

	yocto:
		machine: 'olinuxino-a20som'
		image: 'balena-image'
		fstype: 'balenaos-img'
		version: 'yocto-dunfell'
		deployArtifact: 'balena-image-olinuxino-a20som.balenaos-img'
		compressed: true

	configuration:
		config:
			partition:
				primary: 1
			path: '/config.json'

	initialization: commonImg.initialization
```

**Note**: The `slug` and `name` fields should be updated to `olinuxino-a20som` for production use.

---

### 7. Git Ignore

**File**: `.gitignore`

**Added**:
```
/tmp
```

---

## Commit History

For reference, here are the commits on this branch:

```
74b9649 fixing olimex audio enable issue
12062a5 fixup: cleanup
279fec8 fixup: add gpio
41ad8b3 fixup: add csi0
4caa013 Add sound codec in defconfig
ba3540a fixup: only rx
cf50e13 Add rs232 port to serial
e181f0f Enable FET via DTS
55745fd Add A20-SOM carrier board diagram
f2ebb89 Add tmp to ignore
168d0b2 fix netlink issue
7b02218 Masking device-type for open-balena
392cac5 Add tcpdump for debug network
a41c897 patch device tree for olimex a20 som carrier board
03f003e remove overlay as upcoming changes will be inside the device tree files
d57d00f revert overlay
8d274ea fix serial 2
0e1ccc7 Using github u-boot with branch v2023.01 to solve ftdoverlay addr
e5b6d80 add support for manual load of dtbo files
8d15c20 change u-boot source to olimex olinuxino
caa4dc0 install i2c and related device tree debug tools
f809dba increase bootm len to 64M
a3a744a add balena specific env and bootcmd
3569ea5 fixup: remove non existing files
e6647e8 Change to A20-OLinuXino_MICRO_config
8107528 fixup: add overlay to linux kernel
43907cd Add sun7i a20 supported overlays to kernel device tree
d3c89f0 Add olimex A20-SOM
c34b3c5 Initial work
```

---

## Hardware Reference

### GPIO Assignments (Carrier Board)

| GPIO | Function | Notes |
|------|----------|-------|
| PH15 | Audio Amp Enable | Active Low (via Q2 MOSFET) |
| PH14 | Audio Gain (GAIN1) | High = 12dB, Low = 6dB |
| PH11 | RS232 FET Enable | Active High |
| PE0-PE11 | CSI0 Camera | 8-bit parallel |

### UART Mapping

| Alias | UART | Pins | Features |
|-------|------|------|----------|
| serial0 | uart0 | PB | Console |
| serial1 | uart6 | PI | - |
| serial2 | uart2 | PI | RTS/CTS |
| serial3 | uart4 | PG | - |
| serial4 | uart3 | PG | RTS/CTS |
| serial5 | uart7 | PI | - |

---

## Re-applying Changes After Sync

After syncing with upstream `balena-os/balena-allwinner`:

1. **Create new files** listed in "Files Added" section
2. **Apply modifications** to files listed in "Files Modified" section
3. **Remove** any conflicting old patches that were deleted
4. **Verify** device tree patch applies to new kernel version
5. **Test build** with `MACHINE=olinuxino-a20som`

### Potential Conflicts

- U-Boot patches may need rebasing if upstream sunxi-common.h changed
- Device tree patch may need rebasing if kernel version changed significantly
- Kernel configs may need adjustment for new kernel versions

---

## Reference Files

- `sun7i-a20-olinuxino-micro.dts.upstream` - Original kernel DTS for reference
- `A20-SOM-Carrier_Rev-C_board_diagram.png` - Board schematic
- `docs/a20-som-carrier-audio-device-tree.md` - Audio configuration details

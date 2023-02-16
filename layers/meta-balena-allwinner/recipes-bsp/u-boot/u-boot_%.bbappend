UBOOT_KCONFIG_SUPPORT = "1"
inherit resin-u-boot
FILESEXTRAPATHS_append := ":${THISDIR}/files"

SRC_URI_remove = " \
		file://resin-specific-env-integration-kconfig.patch \
		file://0001-nanopi_neo_air_defconfig-Enable-eMMC-support.patch \
		"

SRC_URI_append = " \
		file://0001-Add-dtbo-to-memory-for-dynamic-loading-of-device-tre.patch \
 		file://0002-Update-CONFIG_SYS_BOOTM_LEN-to-64M.patch \
 		file://0003-Balena-env-on-uboot.patch \
 		"

UBOOT_KCONFIG_SUPPORT = "1"
inherit resin-u-boot
FILESEXTRAPATHS_append := ":${THISDIR}/files"

SRC_URI_remove = " \
		file://resin-specific-env-integration-kconfig.patch \
		file://0001-nanopi_neo_air_defconfig-Enable-eMMC-support.patch \
		"

SRC_URI_append = " \
 		file://0001-Subject-PATCH-Add-Resin-specific-boot-command.patch \
 		file://0002-Balena-env-on-uboot.patch \
 		"

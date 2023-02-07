DESCRIPTION = "U-Boot port for sunxi"

require recipes-bsp/u-boot/u-boot.inc
require recipes-bsp/u-boot/u-boot-common.inc

DEPENDS += "bc-native dtc-native python3-setuptools-native"


LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://Licenses/gpl-2.0.txt;md5=b234ee4d69f5fce4486a80fdaf4a4263"

SRCREV = "62e2ad1ceafbfdf2c44d3dc1b6efc81e768a96b9"
SRC_URI = " \
	git://gitlab.com/u-boot/u-boot.git;protocol=https \
	"

S = "${WORKDIR}/git"

UBOOT_LOCALVERSION = "-yocto"

DESCRIPTION = "U-Boot port for sunxi"

require recipes-bsp/u-boot/u-boot.inc
require recipes-bsp/u-boot/u-boot-common.inc

DEPENDS += "bc-native dtc-native python3-setuptools-native"


LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://Licenses/gpl-2.0.txt;md5=b234ee4d69f5fce4486a80fdaf4a4263"

SRCREV = "release-20230110"
SRC_URI = " \
	git://github.com/OLIMEX/u-boot-olinuxino.git;branch=release-20230110;protocol=https \
	"

S = "${WORKDIR}/git"

UBOOT_LOCALVERSION = "-yocto"

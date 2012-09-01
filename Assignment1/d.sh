#!/bin/bash
/sbin/fdisk /dev/sdf < fd.in
mkfs.ext3 /dev/sdf1
mount /dev/sdf1 /usr/sandeep
partprobe /dev/sdf1

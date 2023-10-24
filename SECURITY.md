# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.5.7   | :white_check_mark: |
| 0.6.6.21 |  :white_check_mark: |

## Reporting a Vulnerability

Please visit the manjaro Forum to contact me, if you see anything security-related in this project.

## Security in BackSnap

BackSnap was designed to be used only as root. This is necessary because the Btrfs command needs to be used as root.
To prevent Backsnap from causing any damage to your system, the commands used internally are subject to restrictions.

* BackSnap does mount or unmount filesystems (btrfs volumes) to /tmp/... only to make a backup of them
    * This is restricted to ***/tmp/BtrfsRoot***
* BackSnap does not delete any snapshots
    * Exception: If the GUI (-g) is used for maintenance, the backups requested for deletion are deleted individually.
    * This is restricted to ***/tmp/BackSnap***
* BackSnap does not modify files 
    * Exception 1: It copies the description of the snapshot to the backup
    * Exception 2: It creates and mantains configfiles under /etc/backsnap.d/*.conf

### Using ssh

To make backups of a computer over ssh you need to be able to issue btrfs-commands (as root or with sudo). 
This can be achieved by using ssh-keys and by restricting their usage to some of the btrfs commands. 

#### On the computer reading the snapshots you need permission to
 * id
 * mount 
 * mount -t btrfs
 * btrfs send
 * btrfs subvolume show
 * btrfs subvolume list
 * btrfs filesystem show
 * btrfs property set
 * btrfs property get
 * rsync -vcptgo
 * rw-access to /etc/backsnap.d/*.conf

#### On the computer wrtiting the backup you need permission to
 * btrfs receive
 * btrfs subvolume delete -Cv (only if you want to delete outdated snapshots)
 * sync
 * pv -f
 * mkdir -pv
 

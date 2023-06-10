# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.5.7   | :white_check_mark: |

## Reporting a Vulnerability

Please visit the manjaro Forum to contact me, if you see anything security-related in this project.

## Security in BackSnap

BackSnap was designed to be only usable as root. This is nessecary because it needs to use the btrfs-command as root.

### Using ssh

To make backups of a computer over ssh you need to be able to issue btrfs-commands (as root or with sudo). 
This can be achieved by using ssh-keys and by restricting their usage to some of the btrfs commands. 

#### You need permission to
 * id
 * mount
 * mount|grep btrfs
 * btrfs send
 * /bin/btrfs send
 * btrfs subvolume show
 * btrfs subvolume list
 * btrfs filesystem show
 * /bin/rsync -vcptgo
 * 

#### On the computer wrtiting the backup you need permission to
 * btrfs receive
 * btrfs subvolume delete -Cv (only if you want to delete outdated snapshots)
 * /bin/sync
 * /bin/pv -f
 * mkdir -pv
 

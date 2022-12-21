# [HowTo] Backup btrfs snapshots with send/receive

## Goals:

Simply a backup of a complete system

* Back up all snapshots
* Differential backup of each snapshot (is faster)
* Use as little storage space on the backup medium as possible
* Differential backup (when repeating the backup after a few days/weeks/months)
* The backup medium can be used for backups of many different computers
* Option to cleanup expired snapshots (to free some space)

## No goal:

* Automatic management of backups by age
* Archive of snapshots with separate management 
* Automatically delete old backups when there is not enough space
* Backup of the current state of the subvolume

## Requirements:

* BTRFS on both the computer and the backup media
* snapper layout of snapshots
* snapshots have to be readonly (requirement for send)
* Java 11 or newer on the computer
* Recommended: pv installed

## Back snap:

The Java program Backsnap saves ALL snapshots that have been specified onto a backup medium. It uses 
**btrfs send** and **btrfs receive** to do this.

The 1st passed parameter points to the **SOURCE path** where the readonly snapshots reside.
Snapper puts all snapshots in directories with ascending numbering.
The actual snapshot there is simply called "snapshot".

* /.snapshots
* /home/.snapshots

The 2nd parameter points to the point at which the snapshots are to be saved. 
To do this, the backup medium is mounted to /mnt/BACKUP (special (subvol=/)). 
It needs a subvolume called **@snapshots** and a directory named with this PC's **hostname**. 
The path to this directory will be specified as the **TARGET path** for the backup.

* /mnt/BACKUP/@snapshots/manjaro
* /mnt/BACKUP/@snapshots/manjaro.home

Backsnap goes through all the directories in the source path in ascending order and checks if that
respective directory already exists at the destination. If it exists, the backup of this snapshot is asumed to be present
If it does not already exist, the directory will be created, and the snapshot will be **send** there.

If possible, a previous snapshot is used as a "parent" to speedup the process and to use less space.

Each time the program is called, all snapshots of ONE subvolume can be backed up. 
This corresponds to ONE configuration of Snapper.

If started with --cleanup the backups on the backupmedium will be reduced to what the snapper-config tells.

## Recommendations:

* Create a shell script called /usr/local/bin/backup that handles the entire backup.
* Mount backup volume with option compress=zst:9

#### P.S. The responsibility for backups never lies with a program, but always with the user!
# [HowTo] Backup btrfs snapshots with send/receive
## How to use BackSnap

* In the **[master](../master/gallery/gallery.md)** branch is a version for use with "snapper"
* In the **[timeshift](../timeshift/gallery/gallery.md)** branch is a beta version for use with "timeshift"

## Prologue
###### A snapshot is not a backup!


I've read that before. Basically correct, but a btrfs snapshot is **as good as any other in-system backup**.
Btrfs can bring a high level of security against data loss for home users.
With some additional work, the external backup for btrfs can also be achieved. Then backup 3-2-1 with btrfs is easily possible

## The responsibility for backups never lies with a program, but always with the user!

##### In-System Backup (n-snapshots 1)
Btrfs with RAID0 and **readonly snapshots** by snapper are like a backup of the original files housed in the same system. It protects against:
* minor problems, like accidentally deleting a file
* delete an entire file tree (e.g. /home/hans/* )
* from unintentionally changing file permissions of many files
* unintentionally modified files

##### In-System Backup with RAID (+1)
Running btrfs with `RAID1` on at least 2 different devices is as good as **a local backup**.
This also protects against:
* Failure of a complete device
* Corruption of the file system on a device
* Loss of partition table on a device
* overwrite a device with dd ;-)

##### Out Of System Backup (+1)
If the snapshots are also backed up on an external disk, this is another REAL **external backup**.
At best, the external disk should only be connected to the computer for a short time. This also protects against:
* Complete failure of the computer
* Loss of past backups up to capacity of external disk
* Targeted deletion of the internal backups (e.g. by Mallware)

Btrfs then corresponds to 3-2-1 Backup (Near CDP)

## Goals of BackSnap:
**Simple external backup** of a complete (btrfs) subvolume

* Back up **all** snapshots
* **Differential backup** of each snapshot (fast). If the backup is repeated after a few days/weeks/months
* As **little space consumption** on the backup medium as possible
* The backup medium can be used for **backups of different computers**
* Command line program without GUI
* KISS
* with GUI -g clear representation of snapshots and existing backups
* GUI controlled deletion of outdated backups
* GUI controlled pruning of backups

##### No Goal:
* Automatic management of backups by age
* Automatically delete old backups when there is not enough space
* Backup of the current state of a subvolume

##### Desirable side effects
* The **backup strategy** is already defined in snapper, and is involved here
* The backup is compressed, but at the same time all snapshots in the backup are **always fully read-only accessible**

## Requirements:
* **Java 17** or newer on the computer
* Recommended: **pv** installed
* BTRFS both on the computer (recommended as **RAID 1** with 2 devices) and on the backup medium (single)
* **snapper** layout of the snapshots 
* The snapshots must be **readonly** otherwise btrfs cannot transfer them.
* Recommended: external backup medium e.g. USB3 hard drive
* Recommended: your own bash script to start the backup easily

# BackSnap:
The Java program BackSnap backs up ALL snapshots from a specified directory to another directory on a
backup medium. To do this, it uses **btrfs send** and **btrfs receive**.

##### Source (snapshots)
The 1st passed parameter points to the **SOURCE path** where the snapshots are reachable by Snapper. Snapper creates all snapshots in directories with ascending numbering. The actual snapshot there is simply called "snapshot".

* **/**
* **/home**
* /.snapshots (alternative)
* /home/.snapshots (alternative)

##### Destination (backups)
The 2nd parameter points to the **DESTINATION path** at which the snapshots are to be saved. To do this, the **backup medium** must be mounted before the program is called. A backup subvolume named **/@BackSnap** and a directory with individual Names for each subvolume of each PC are suggested. The path to this mounted directory will be specified as the **TARGET path** for the backup.

* **/mnt/BackSnap/manjaro21**
* **/mnt/BackSnap/manjaro21.home** 

Backsnap goes through all the directories in the source path in **ascending time order** and checks if that respective directory already exists at the destination. If not, the snapshot will be saved there. 

If possible, a previous snapshot is used as **"parent"**.

Each time the program is called, all snapshots of ONE subvolume can be backed up, which corresponds to ONE configuration of Snapper.

## Furter reading:
* In the **[master](../master/gallery/gallery.md)** branch is a version for use with "snapper"
* In the **[timeshift](../timeshift/gallery/gallery.md)** branch is a beta version for use with "timeshift"

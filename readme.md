# [HowTo] Backup btrfs snapshots with send/receive
[How it works](./HowItWorks.md),  [deutsch](./backsnap.md) 
## Prologue
A snapshot is not a backup!

I've read that before. Correct in principle, but a btrfs snapshot is as good as any other
In-system backup. Btrfs can bring a high level of security against data loss for home users.
With some additional work, an external backup for btrfs can also be achieved.

# The responsibility for backups never lies with a program, but always with the user!

##### In system backup
Btrfs with RAID0 and readonly snapshots by snapper are like a backup of the original files housed in the same system. It protects against:
* minor problems, like accidentally deleting a file
* delete an entire file tree (e.g. /home/hans/** )
* from unintentionally changing file permissions of many files

##### In-system backup with RAID
If btrfs is operated with `RAID1` on at least 2 different devices, this is as good as a local backup on a RAID system. This also protects against:
* Failure of a complete device
* Corruption of the file system on a device
* Loss of partition table on a device
* overwrite a device with dd ;-)

##### Out-Of-System Backup
If the snapshots are also copied to an external disk, this is `a REAL backup`. At best this
external disk should be connected to the computer only for a short time. This also protects against:
* Complete failure of the computer
* Loss of past backups up to capacity of external disk
* Targeted deletion of the internal backups by Mallware

(btrfs then corresponds to Near CDP)

## Goals of backsnap:
Simple external backup of a complete (btrfs) subvolume

* Back up **all snapshots**
* Differential backup of each snapshot (**fast**)
* Use as little storage space on the backup medium as possible (**compressed**)
* **Differential** backup (when repeating the backup after a few days/weeks/months)
* The backup medium can be used for backups of **different computers**
* Command line program without GUI
* **Keep It Simple Superuser**

##### No Goal:
* Automatic management of backups by age
* Automatically delete old backups when there is not enough space
* Backup of the current state of a subvolume     

##### Desirable side effects:
* The **backup strategy** is already defined in snapper/Timeshift, and is involved here
* The backup is compressed, but at the same time all snapshots in the backup are read-only, but **fully accessible**

## Requirements:
* **Java 17** or newer on the computer
* **BTRFS** both on the computer (recommended as RAID1 with 2 devices) and on the backup medium (single)
* The snapshots must be **read-only**, and they are **numbered sequentially** (gaps are allowed)
* Recommended: pv installed
* Recommended: snapper layout of snapshots
* Recommended: external backup medium e.g. USB3 hard drive
* Recommended: your own bash script to start the backup

# Backsnap:
The Java program `backsnap` backs up ALL snapshots from a specified directory to another directory on a
backup medium. To do this, it uses **btrfs send** and **btrfs receive**.

##### Source
The 1st passed parameter points to the **SOURCE path** where the snapshots
are reachable for Snapper. Snapper puts all snapshots in directories with ascending numbering.
The actual snapshot there is simply called "snapshot".

* /.snapshots
* /home/.snapshots

##### Destination
The 2nd parameter points to the **DESTINATION path** at which the snapshots are to be saved. To do this, the backup medium needs to 
be mounted "special (subvol=/)". It needs a subvolume called **@snapshots** and a directory
with an individual name for each subvolume.
The path to this directory will be specified as the **TARGET path** for the backup.

* /mnt/BACKUP/@snapshots/manjaro21
* /mnt/BACKUP/@snapshots/manjaro21.home

Backsnap goes through all the directories in the source path in numerically ascending order and checks if that
respective directory already exists at the destination. If not, the snapshot will be saved there.
If possible, a previous snapshot is used as a "parent".

Each time the program is called, all snapshots of ONE subvolume can be backed up
corresponding to ONE configuration of Snapper.

##### SSH
A computer accessible via `ssh` can also be specified as the source path. This is `ssh`, `ssh-keys` and a
`ssh-agent` required. The ssh agent, the keys and the ssh connection should be **tested separately beforehand**.
Since `backsnap` must also be started locally as `root`, the ssh connection must be set up for `root` (local and remote)

* root@192.168.1.23:/.snapshots
* root@192.168.1.23:/home/.snapshots

###### Warnings:
SSH doesn't work miracles. It can (despite compression and differential backup) only be as fast as your network.
 (1GB/s / 100MB/s / WLAN ??? )

I will not give any help with SSH problems!
As long as the program works locally but not over SSH, SSH doesn't seem to be fully set up.
SSH isn't for everyone, but there are plenty of good guides out there if you look for it.

### Preparations

###### external hard drive
* GPT partition table recommended
* A partition formatted with btrfs
* create a subvolume `@snapshots` there later!

###### fstab
* Create a mount point `sudo mkdir /mnt/BACKUP`
* Enter the external hard drive in the fstab. Example:

> UUID=03417033-3745-4ae7-9451-efafcbb9124e /mnt/BACKUP btrfs noauto,rw,noatime,compress=zstd:9,subvol=/ 0 0

You can get the uid with e.g. `lsblk -o name,uuid`

##### Install
The backsnap.jar file must be stored "somewhere". (Recommended: /usr/local/bin/backsnap.jar). The call is then made as root.

`java -jar /usr/local/bin/backsnap.jar /.snapshots /mnt/BACKUP/manjaro21`

##### Test
  * become root `sudo su -`
  * Mount backup media `mount /mnt/BACKUP`
  * create subvolume once `btrfs subvolume create /mnt/BACKUP/@snapshots`
  * Create target path once `mkdir /mnt/BACKUP/@snapshots/manjaro21`
  * Create target path once `mkdir /mnt/BACKUP/@snapshots/manjaro21.home`
  * Create additional target paths for computers/subvolumes
  * Create backup `java -jar /usr/local/bin/backsnap.jar /home/.snapshots /mnt/BACKUP/@snapshots/manjaro21.home`
 
> ATTENTION: this can take quite a long time, so it's best to start with the smallest subvolume ;-)

  * try again after an hour!

##### backup script
After initial manual experiments, a backup script should be created (e.g. in `/usr/local/bin/backup` ).
The desired subvolumes / computers are then backed up there.
Btrfs needs root access. The script must therefore be executable and started with sudo!

###### Example:
```
   #!/bin/zsh
   # Sichern aller aufgelisteten Rechner auf ein angestecktes Laufwerk
   # BS_UUID=03417033-3745-4ae7-9451-efafcbb9124e
   BS_MOUNT=/mnt/BACKUP
   BS_JAR=/usr/local/bin/backsnap.jar
   
   # mount aus fstab ;-)
   
   [ -d $BS_MOUNT/@snapshots ] || mount $BS_MOUNT
   #sudo mount -o noatime,subvol=/,compress=zstd:9 UUID=$BS_UUID $BS_MOUNT
   # wenn das mounten nicht geklappt hat, ABBRECHEN 
   [ -d $BS_MOUNT/@snapshots ] || { echo "Das mounten war nicht erfolgreich"; exit; }
   [ -d $BS_MOUNT/@snapshots/$BS_HOST ] || { echo "Für $BS_HOST gibt es keinen mountpoint"; exit; }
   
   function backup {
       BS_SOURCE="$1/.snapshots"
       BS_DEST="$BS_MOUNT/@snapshots/$2"
   #    echo "java -jar $BS_JAR $BS_SOURCE $BS_DEST"
       java -jar $BS_JAR $BS_SOURCE $BS_DEST
       echo -n " Y" 
   }
   
   # lokal sichern
   backup "" "manjaro18"
   backup "/home" "manjaro18.home"
   
   # server sichern
   backup "root@server:" "server"
   backup "root@server:/home" "server.home"
   
   # laptop sichern 
   backup "root@notebook:" "notebook"
   backup "root@notebook:/home" "notebook.home"
   
   # gast sichern
   backup "root@gast:" "gast"
   backup "root@gast:/home" "gast"
   umount $BS_MOUNT
   echo "fertig"
```
   
#### Upcoming 

There is a functional, but incomplete version in gui-branch <https://github.com/andreaskielkopf/BackSnap/tree/gui> 
 



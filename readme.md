# [HowTo] Backup btrfs snapshots with send/receive
[How it works](./HowItWorks.md),  [deutsch](./backsnap.md) 
## How to use backsnap
You find most recent info in [./gallery/gallery.md](.gallery/gallery.md) 
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

## Goals of backsnap:
**Simple external backup** of a complete (btrfs) subvolume

* Back up **all** snapshots
* **Differential backup** of each snapshot (fast). If the backup is repeated after a few days/weeks/months
* As **little space consumption** on the backup medium as possible
* The backup medium can be used for **backups of different computers**
* Command line program without GUI
* KISS

* with GUI -g clear representation of the existing backups
* GUI controlled deletion of old backups
* GUI controlled pruning of backups
* View snapshots and backups

##### No Goal:
* Automatic management of backups by age
* Automatically delete old backups when there is not enough space
* Backup of the current state of a subvolume

##### Desirable side effects
* The **backup strategy** is already defined in snapper/Timeshift, and is involved here
* The backup is compressed, but at the same time all snapshots in the backup are **always fully read-only accessible**

## Requirements:
* **Java 17** or newer on the computer
* Recommended: **pv** installed
* BTRFS both on the computer (recommended as **RAID 1** with 2 devices) and on the backup medium (single)
* **snapper** layout of the snapshots (the program can also work with other layouts)
* The snapshots must be **readonly** otherwise btrfs cannot transfer them.
* Recommended: external backup medium e.g. USB3 hard drive
* Recommended: your own bash script to start the backup easily

# Backsnap:
The Java program Backsnap backs up ALL snapshots from a specified directory to another directory on a
backup medium. To do this, it uses **btrfs send** and **btrfs receive**.

##### Source (snapshots)
The 1st passed parameter points to the **SOURCE path** where the snapshots
are reachable by Snapper. Snapper creates all snapshots in directories with ascending numbering.
The actual snapshot there is simply called "snapshot".

* **/**
* **/home**
* /.snapshots (alternative)
* /home/.snapshots (alternative)

##### Destination (backups)
The 2nd parameter points to the **DESTINATION path** at which the snapshots are to be saved.
To do this, the **backup medium** must be mounted before the program is called.
A backup subvolume named **/@BackSnap** and a directory with individual
Names for each subvolume of each PC.
The path to this mounted directory will be specified as the **TARGET path** for the backup.

* **/mnt/BackSnap/manjaro21** (if **/@BackSnap** was mounted to /mnt/BackSnap)
* **/mnt/BackSnap/manjaro21.home** (if **/@BackSnap** was mounted to /mnt/BackSnap)
* /mnt/BACKUP/@BackSnap/manjaro21 (alternative if btrfs-**/** was mounted to /mnt/BACKUP)
* /mnt/BACKUP/@BackSnap/manjaro21.home (alternative if btrfs-**/** was mounted to /mnt/BACKUP)

Backsnap goes through all the directories in the source path in **ascending time order** and checks if that
respective directory already exists at the destination. If not, the snapshot will be saved there.
If possible, a previous snapshot is used as **"parent"**.

Each time the program is called, all snapshots of ONE subvolume can be backed up,
which corresponds to ONE configuration of Snapper.

##### SSH
A computer accessible via `ssh` can also be specified as the source path. 
Therefore `ssh-keys`, `ssh`, and a `ssh-agent` is required. The ssh agent, the keys and the ssh connection should be **tested separately** beforehand.
Since `backsnap` must also be started locally as `root`, the ssh connection must be set up for `root` (local and remote)

* root@192.168.1.23:/
* root@192.168.1.23:/home

###### Warnings:
SSH doesn't work miracles. That can (despite compression and differential backup) only be as fast as
your network. (10Gbit/s, 1GB/s, 100MB/s, / WLAN ??? )

I will not be able to give any help with SSH problems!
As long as the program works locally but not over SSH, SSH doesn't seem to be fully set up.
SSH isn't for everyone, but there are plenty of good guides out there if you look for it.

### Preparations

###### external hard drive
* GPT partition table recommended
* A partition formatted with btrfs
* Create a subvolume `/@BackSnap` there!

###### fstab
* Create a mount point `sudo mkdir /mnt/BackSnap`
* Enter the external hard drive in the fstab. Example:

> UUID=03417033-3745-4ae7-9451-efafcbb9124e /mnt/BackSnap btrfs noauto,rw,noatime,compress=zstd:9,subvol=/@BackSnap 0 0

You can get the uuid with e.g. `lsblk -o name,uuid`

##### To install
It is recommended to check the Java version:
`java -version`

The backsnap file must be stored "somewhere". (Recommended: /usr/local/bin/backsnap).
The call is then made as root.

`backsnap / /mnt/BackSnap/manjaro21`
or

`sudo backsnap / /mnt/BackSnap/manjaro21`
(recomended) or

`ssh root@localhost backsnap.jar / /mnt/BackSnap/manjaro21`
or

`backsnap root@localhost:/ root@localhost:/mnt/BackSnap/manjaro21`

##### Prepare (one time)
  * become root `sudo su -`
  * Mount backup medium btrfs volume **/** (!) to /mnt
  * create subvolume `btrfs subvolume create /mnt/@BackSnap`
  * unmount (!) `umount /mnt`
  * Create mount point for /@BackSnap `mkdir /mnt/BackSnap`
 
##### Prepare (per PC/Subvolume)
  * become root `sudo su -`
  * Mount backup medium btrfs subvolume **/@BackSnap** (!) to /mnt/BackSnap
  * Create target path for **manjaro21** `mkdir /mnt/BackSnap/manjaro21`
  * Create target path for **home** `mkdir /mnt/BackSnap/manjaro21.home`
  * Create additional target paths for computers/subvolumes as required
  * Create first backup `backsnap -s /home /mnt/BackSnap/manjaro21.home`
 
> ATTENTION: this can take quite a long time, so try the smallest subvolume first ;-)

  * try again after an hour!

##### backup script
After the first manual experiments, a backup script should be created (e.g. in `/usr/local/bin/backup.sh` ).
The desired subvolumes / computers are then mentioned there.
Btrfs needs root access. So the script must be **executable** and started with **sudo**!


###### Example:

```
#!/bin/zsh
# Sichern aller aufgelisteten Rechner auf ein angestecktes Laufwerk

# your mountpoint for backsnap
BS_MOUNT=/mnt/BackSnap
# start of the uuid of your backup partition
BS_ID=03417033

# search for the backup device:
BS_UUID=$(lsblk -o uuid | grep -E $BS_ID)
#BS_UUID=03417033-3745-4ae7-9451-efafcbb912..
# echo "UUID=$BS_UUID LEN=${#BS_UUID}"
[ ${#BS_UUID} -le 35 ] && 
    echo "error: backup disk with UUID $BS_ID... is not connected" && exit

# mount backup device:
WAS_MOUNTED=$(mount | grep -E " $BS_MOUNT ")
# echo "was mounted=$WAS_MOUNTED"
[ ${#WAS_MOUNTED} -le 10 ] && 
    echo "i need to mount $BS_UUID to $BS_MOUNT" && 
    mount -t btrfs -o subvol=@BackSnap,compress=zstd:9 /dev/disk/by-uuid/$BS_UUID $BS_MOUNT
mount | grep -E " $BS_MOUNT " ||
    { echo "error: could not mount $BS_UUID to $BS_MOUNT" && exit }

# activate ssh-askpass for gui usage
export SSH_ASKPASS_REQUIRE="prefer"

function backup {
# This script has to be run in a terminal with sudo to get the grapical version !
    backsnap -g -v=1 -a=15 $1 $BS_MOUNT/$2 || exit
# alternative cli-version is called:    
#   backsnap -v3 $1 $BS_MOUNT/$2 || exit    
    echo  "OK"
}

# lokal sichern
backup /     manjaro18
backup /home manjaro18.home

# server sichern
backup root@server:/ server
backup root@server:/home server.home
backup root@server:/srv server.srv

# jitsi sichern
backup root@jitsim1:/ jitsim1
backup root@jitsim1:/home jitsim1.home
backup root@jitsim1:/opt/hst jitsim1.hst
backup root@jitsim1:/opt/hugo jitsim1.hugo

# laptop sichern 
backup root@notebook: notebook
backup root@notebook:/home notebook.home

# gast sichern
backup root@gast:/ gast
backup root@gast:/home gast.home

# Laufwerk unmounten:
sync
[ ${#WAS_MOUNTED} -le 10 ] && 
    echo "i need to umount $BS_MOUNT" && 
    umount $BS_MOUNT &&
    sync
mount | grep -E " $BS_MOUNT "
echo "fertig mit den Backups"
```

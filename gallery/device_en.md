# Prepare a backup medium
BackSnap has specific requirements for the backup medium used. But these are not difficult to achieve.

### The backup medium:
* Should definitely be an external medium (e.g. USB hard drive)
* Should definitely be significantly larger than the medium in your pc
* Must be on a different device than the snapshots to be backed up
* Must be **btrfs formatted**

### Select device
It is recommended to use a device that has **significantly** more storage space than all the computers to be backed up combined. That may then be an external **hard drive with 2TB**, or even more.

### Partition and format the device
It is recommended to provide the device with a `GPT` partition table. The partition to be used for backup must be formatted with `btrfs`. I have had good experiences with `gparted`. The label of the btrfs partition can be anything. In my example it is `Backup`

### Further preparation

Backsnap can carry out the further preparation itself if it is started in a terminal with the `-gi` option. 

Please connect your backup-device, so that btrfs will be able to detect it. (You do not need to mount it)

`sudo backsnap -gi`

### [next step ->](config_en.md)
----
[More info on btrfs](https://wiki.manjaro.org/index.php/Btrfs)
Sonntag, 29. Oktober 2023 11:49 


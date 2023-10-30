# Prepare a backup drive
BackSnap has specific requirements for the backup drive used. But these are not difficult to achieve.

### The backup drive:
* Should definitely be an **external** drive (e.g. USB hard drive)
* Should definitely be significantly **larger** than the drive to be backed up in your PC
* Must be on a **different drive** than the snapshots being backed up
* Must be **btrfs formatted**

### Select drive
It is recommended to use a drive that has **significantly** more storage space than all of the computers being backed up combined. This could be an external **hard drive with 2TB**, or even more.

### Partition and format the backup drive
It is recommended to equip the device with a `GPT` partition table. The partition to be used for backup must be formatted with `btrfs`. I have had good experiences with `gparted`. The name of the BTRFS partition can be anything.

### Further preparation
Backsnap can carry out the further preparation itself if it is started in a terminal with the `-gi` option.
Please connect your backup device so Btrfs can detect it. (You don't need to mount it)

`sudo backsnap -gi`

### [next step ->](config_en.md)
----
[More info about btrfs](https://wiki.manjaro.org/index.php/Btrfs)

Montag, 30. Oktober 2023 09:00 
# Try BackSnap CLI
The following assumes that everything has been [installed](install_en.md) and the backup media and required mount point have been [prepared](device_en.md).
### Mount backup medium
The subvolume **@BackSnap** of the backup device with said **UUID** will be mounted to **/mnt/BackSnap**. The compression zstd is set to level 9. Using the UUID reliably prevents mounting something wrong.

`sudo mount -t btrfs -o subvol=@BackSnap,compress=zstd:9 /dev/disk/by-uuid/03417033-3745-4be7-9451-efafcbb9124e /mnt/BackSnap`

### Back up a single snapshot
Save a single snapshot (-s) of the root (/) to the backup media in the manjaro23 folder. The oldest snapshot that has not yet been backed up will be backed up.

`sudo backsnap -s / /mnt/BackSnap/manjaro23 `

If this is the very first snapshot being backed up, it may take some time. The progress bar shows every minute how much data has already been copied. The program does not show percentages because the total size is not known beforehand.

`sudo backsnap -s / /mnt/BackSnap/manjaro23 `

If this is the second snapshot being backed up, this one is significantly faster. It builds on the previous snapshot, and only needs space for the differences from the first snapshot.

### Back up all snapshots
Back up all snapshots (-s) from the main directory (/) to the backup medium in the folder manjaro23 one after the other.

`sudo backsnap / /mnt/BackSnap/manjaro23 `

When this is done, all snapshots have been coppied to the backup device

### Testing the backup

Now it's time to go to /mnt/BackSnap/manjaro23/ and look at the backups. In each folder there is a snapshot named "snapshot". In it, all files of the snapshot should be accessible readonly.

`sudo ls -lA /mnt/Backsnap/manjaro23/`

If you have mc installed, this is a good way to check if the files are there in the backup

`sudo mc`

### Complete backup
Don't forget to remove the backup device again

`sudo umount /mnt/BackSnap `

----
Saturday, June 03, 2023 06:27
# Restore from backup
Depending on what broke, there are different ways to repair the damage. A repair method is recommended below depending on the type of damage. If the damage is more serious, there is no shame in getting help from the manjaro forum.
However, anyone who immediately advises you to throw everything away, tear down the house and build a new one is obviously not the best advisor. Either he doesn't know btrfs, or he loves to set up new systems all the time, or he just thinks he's glad he doesn't have to do this work himself..
## Before restore
If possible, a snapshot (and also a backup) should be made of the current status. After all, the repair is based on the current subvolume, and we don't want to go from bad to worse.

## Issues:
#### Deleted single file, deleted directory tree, corrupted permissions
The easiest way to restore a single file/directory is to **mount** the backup and rsync the file back. **Rsync** also automatically repairs the file permissions.

#### Configuration corrupted
If you know which configuration you accidentally corrupted, you can use **meld** to compare the working configuration in the backup with the current one and make necessary changes

#### File system readonly, file system not mountable, file system total loss
With all of these problems, it is very important to **fix** the cause of the problem first. It doesn't really make sense to set up a new system on a **broken hard drive** by formatting the partition and importing the backup.
There are many good ways to repair a btrfs file system. Even if there are only a few people who can do it at the moment. If you don't know about btrfs, you can't fix it either.
see also:
* [Rescue files from btrfs (even if backup is missing)](https://forum.manjaro.org/t/how-to-rescue-data-from-a-damaged-btrfs-volume/79400)
* [manjaro - wiki on btrfs](https://wiki.manjaro.org/index.php/Btrfs)
* [arch-wiki on btrfs](https://wiki.archlinux.org/title/Btrfs)
* [Btrfs Documentation(Read the docs)](https://btrfs.readthedocs.io/en/latest/index.html btrfs.readthedocs.io)

## Tools for solutions:

### rsync
Rsync is a tool to **copy** individual files. But it also allows copying entire **directories**. To do this, the snapshot with the desired files is simply mounted somewhere and then the files are copied back with rsync.
Of course, programs like cp, mc, or any other file manager can also be used.

##### The special thing about rsync
* Rsync also takes care to set the **file permissions** right again.
* Rsync is **especially gentle** on btrfs file systems because it only replaces the mismatched parts of a file instead of just rewriting the whole file. This saves a lot of space in the snapshot.
* Rsync can of course **also be used via ssh**.
* Additionally, rsync can also **remove** files that are no longer needed

### meld
**Meld** is a popular program to compare files and copy individual passages from one file (which worked) to the other file. Various other programs such as **mc** can also be used for this

### New file system ? rollback ?
If there is any snapshot left on the btrfs volume that corresponds to the subvolume at any point in time (even if it is a year old), then that snapshot is a good basis for repair.
1. Make a new writable **Snapshot** of the "Base".
2. **Mount** the backup somewhere
3. **Copy** the backup into this writable snapshot with rsync.
In doing so, rsync will only change the files that do not match the base. In this case, you should definitely ask rsync to **delete** files that are no longer used, if necessary.
4. **Roll back** to this writable snapshot.
see also:
* [manual rollback with btrfs](https://forum.manjaro.org/t/howto-rollback-mit-btrfs-by-hand/80209/7)

#### I wish you every success.

Saturday, June 03, 2023 06:30
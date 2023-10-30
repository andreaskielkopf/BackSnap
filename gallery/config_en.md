# Create Configfiles
The following assumes that everything has been [installed](install_en.md) and that the backup drive has been [prepared](device_en.md).

## Configure backsnap
Backsnap needs to know **which** snapshots to back up and **where** to store the backups. This is set in the configuration files at `/etc/backsnap.d/`

When you started `sudo backsnap -gi`, Backsnap first created `/etc/backsnap.d/` and then prepared a simple configuration file for the local PC.

### `/etc/backsnap.d/local.conf`

#### 1) Select the **PC**
```
# backup local pc per sudo
pc = localhost
# backup local pc per ssh
#pc = root@localhost
```
One of:
* **pc = localhost** = This PC (Backsnap uses **sudo** to invoke btrfs)
* **User@IP** or **User@Name** = The pc is accessible via SSH (Backsnap uses **ssh** to call btrfs there)

#### 2) Select the UUID for the **backup volume**
```
# detect and mount backupvolume by scanning for this id (as part of the uuid)
backup_id = 03417033-3645-4ae7-9451-efafcbb9124e
```
Only in **one** configuration are you allowed to specify the UUID of the partition where backups should be stored.
The subvolume **@BackSnap** of the backup partition with the said **UUID** is mounted in **/tmp/BackupRoot** during the backup. The compression zstd is set to level 9. Using a UUID reliably prevents anything from being mounted incorrectly.

#### 3) Select options and a name for the first subvolume
```
# use these flags for the following backups (optional)
#flags = -gtc -v=1 -a=12 
# backuplabel = manjaro18 for snapshots of /
manjaro18 = /
```
All snapshots of the subvolume mounted at **/** will be included in the backup at /tmp/BackupRoot/**manjaro18**/

#### 4) Select options and a name for the next subvolume
```
#flags = -gtc -v=1 -a=12 
# backuplabel = manjaro18.home for snapshots of /home
manjaro18.home = /home
```
All snapshots of the subvolume mounted at **/home** will be included in the backup at /tmp/BackupRoot/**manjaro18.home**/

#### 5) And so forth ...
You can add additional subvolumes here. Each with its own mount point and its own name.

### /etc/backsnap.d/laptop.conf
For each additional PC that is to be included in the backup, you must create its own configuration file here.
```
# This pc is reachable via ssh with root@192.168.178.88
#pc = root@192.168.178.88
# This pc is reachable via ssh with root@notebook
pc = root@notebook

# flags are unchanged ;-)
# Backup snapshots of / to label notebook
notebook = /
# Backup snapshots of /home to label notebook.home
notebook.home = /home
```
Backsnap will try to reach the PC via ssh. If that fails, this pc will be skipped.

### Here we go
##### You can do a test run
In a terminal: **`sudo backsnap -gcd -v=6 -a=20`**
* -g with gui
* -c compressed btrfs send format
* -d dry run
* -v=6 verbose
* -a=20 automatically continue with the next subvolume after 20 seconds

##### You can make a proper backup
In a terminal: **`sudo backsnap -gc -v=6 -a=10`**
* -g with gui
* -c compressed btrfs send format
* -v=6 be verbose
* -a=10 automatically continue with the next subvolume after 10 seconds

Please keep in mind that a backup can take a really long time depending on the number of snapshots.

----

Sonntag, 29. Oktober 2023 17:01 
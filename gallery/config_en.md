# Create Configfiles
The following assumes that everything has been [installed](install_en.md) and that the backup media has been [prepared](device_en.md).
## Config backsnap
Backsnap needs to know what snapshots to backup, and where to store the backups. This is set in configfiles under `/etc/backsnap.d/`

When starting `sudo backsnap -gi` backsnap will first create `/etc/backsnap.d/` and then it will create a configfile for the local pc

### `/etc/backsnap.d/local.conf`

#### 1) Select a location for the **pc**
```
# backup local pc per sudo
pc = localhost
# backup local pc per ssh
#pc = root@localhost
```
One of:
* **localhost** = this pc (backsnap uses **sudo**)
* **user@ip** or **user@name** = reachable per ssh (backsnap uses **ssh**)

#### 2) Select the uuid for the **backupvolume** 
```
# detect and mount backupvolume by scanning for this id (as part of the uuid)
backup_id = 03417033-3645-4ae7-9451-efafcbb9124e
```
Only in one configuration you need to specify the uuid of the partition where backups should go
The subvolume **@BackSnap** of the backup partition with said **UUID** will be mounted to **/tmp/BackupRoot**. The compression zstd is set to level 9. Using a UUID reliably prevents mounting something wrong.

#### 3) Select options and a name for the first subvolume
```
# use these flags for the following backups (optional)
#flags = -gtc -v=1 -a=12 
# backuplabel = manjaro18 for snapshots of /
manjaro18 = /
```
All snapshots of the subvolume mounted at **/** will be included in the backup at Backupvolume/BackSnap/**manjaro18**/

#### 4) select aoptions and a name for the next subvolume
```
#flags = -gtc -v=1 -a=12 
# backuplabel = manjaro18.home for snapshots of /home
manjaro18.home = /home
```
All snapshots of the subvolume mounted at **/home** will be included in the backup at Backupvolume/BackSnap/**manjaro18.home**/

#### 5) and so on
You may specify furter subvolumes to be included into the backup (each with its own name)

### /etc/backsnap.d/laptop.conf
For each pc to be included into backups you need to create another configfile
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
Backsnap will try to make backups of the notebook using **ssh root@notebook**.
If the notebook is not reachable, backsnap will skip it.

----
Sonntag, 29. Oktober 2023 04:31 

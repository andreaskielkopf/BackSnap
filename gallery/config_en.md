# Create Configfiles
The following assumes that everything has been [installed](install_en.md) and that the backup drive has been 
[prepared](device_en.md).

## Configure backsnap
Backsnap needs to know **which** snapshots to back up and **where** to store the backups. This is set in the 
configuration files at `/etc/backsnap.d/`

When you started `sudo backsnap -gi`, Backsnap first created `/etc/backsnap.d/` and then prepared a simple 
configuration file for this PC.

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
Only **one** configuration is allowed to specify the UUID of the partition where backups should be stored. The subvolume 
**@BackSnap** of the backup partition with this **UUID** is mounted in **/tmp/BackupRoot** during the backup. 
The compression zstd is set to level 9. Using a UUID reliably prevents anything from being mounted incorrectly.

#### 3) Select options and a name for the first subvolume
```
# use these flags for the following backups (optional)
#flags = -gtc -v=1 -a=12  
# backuplabel = manjaro17 for snapshots of /
manjaro17 = /
```
All snapshots of the subvolume mounted at **/** will be included in the backup at `/tmp/BackupRoot/**manjaro17**/`

#### 4) Select options and a name for the next subvolume
```
#flags = -gtc -v=1 -a=12 
# backuplabel = manjaro17.home for snapshots of /home
manjaro17.home = /home
```
All snapshots of the subvolume mounted at **/home** will be included in the backup at `/tmp/BackupRoot/**manjaro17.home**/`

#### 5) And so forth ...
You can add additional subvolumes here. Each with its own mount point and its own name.

### /etc/backsnap.d/laptop.conf
For each additional PC that is to be included in the backup, you must create its own configuration file.
```
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
In a terminal:
```
sudo backsnap -gcd -v=6 -a=20
```
* -g with gui
* -c compressed btrfs send format
* -d dry run
* -v=6 verbose
* -a=20 automatically continue with the next subvolume after 20 seconds

##### You can make a proper backup
In a terminal:
```
sudo backsnap -gc -v=6 -a=10
```
* -g with gui
* -c compressed btrfs send format
* -v=6 be verbose
* -a=10 automatically continue with the next subvolume after 10 seconds

Please keep in mind that a backup can take a really long time depending on the number of snapshots. Additionally it 
depends on the speed of your backup drive.

## Done
As long as the config files exist in `/etc/backsnap`, one command is enough to create a backup to the external drive.
```
sudo backsnap -gc 
```
or
```
sudo backsnap
```

#### The available (and configured) options are displayed with
```
sudo backsnap -h
```
```
BackSnap Version 0.6.8.27 (2024/06/22)
args >  -h 
java [version=23-ea, major=23, minor=null, patch=null]
using ThreadPerTaskExecutor
Pc[sudo ] & Id:03417033-3745-4ae7-9451-efafcbb9124e
OneBackup[/etc/backsnap.d/gast.conf, Pc[root@gast]:/ (gast) flags=-c -a=5 -v=5 -o=500 -m=200]
OneBackup[/etc/backsnap.d/gast.conf, Pc[root@gast]:/home (gast.home) flags=-c -a=5 -v=5 -o=500 -m=200]
OneBackup[/etc/backsnap.d/jitsim1.conf, Pc[root@jitsim1]:/ (jitsim1) flags=-c -a=5 -v=5 -o=3500 -m=1000]
OneBackup[/etc/backsnap.d/jitsim1.conf, Pc[root@jitsim1]:/home (jitsim1.home) flags=-c -a=5 -v=5 -o=3500 -m=1000]
OneBackup[/etc/backsnap.d/jitsim1.conf, Pc[root@jitsim1]:/opt/hst (jitsim1.hst) flags=-c -a=5 -v=5 -o=3500 -m=1000]
OneBackup[/etc/backsnap.d/jitsim1.conf, Pc[root@jitsim1]:/opt/hugo (jitsim1.hugo) flags=-c -a=5 -v=5 -o=3500 -m=1000]
OneBackup[/etc/backsnap.d/local.conf, Pc[sudo ]:/ (manjaro19) flags=-c -v=5 -a=5 -o=4000 -m=1000]
OneBackup[/etc/backsnap.d/local.conf, Pc[sudo ]:/home (manjaro19.home) flags=-c -v=5 -a=5 -o=4000 -m=1000]
OneBackup[/etc/backsnap.d/laptop.conf, Pc[root@notebook]:/ (notebook) flags=null]
OneBackup[/etc/backsnap.d/laptop.conf, Pc[root@notebook]:/home (notebook.home) flags=null]
OneBackup[/etc/backsnap.d/0_server.conf, Pc[root@server]:/ (server) flags=-c -v=5 -a=5 -o=3000 -m=1000]
OneBackup[/etc/backsnap.d/0_server.conf, Pc[root@server]:/home (server.home) flags=-c -v=5 -a=5 -o=3000 -m=1000]
OneBackup[/etc/backsnap.d/0_server.conf, Pc[root@server]:/srv (server.srv) flags=-c -v=5 -a=5 -o=3000 -m=1000]
OneBackup[/etc/backsnap.d/timeshift.conf, Pc[root@timeshift]:/ (timeshift) flags=-c -v=5 -a=5 -o=3000 -m=1000]
OneBackup[/etc/backsnap.d/timeshift.conf, Pc[root@timeshift]:/home (timeshift.home) flags=-c -v=5 -a=5 -o=3000 -m=1000]
2 ==>
BackSnap is made for making backups of btrfs snapshots on linux

Usage:
------
/usr/local/bin/backsnap [OPTIONS]

 -h --help           show usage
 -x --version        show date and version
 -d --dryrun         do not do anything ;-)
 -v --verbose        be more verbose (-v=9)
 -s --singlesnapshot backup exactly one snapshot
 -g --gui            enable gui (works only with sudo)
 -a --auto           auto-close gui when ready
 -c --compressed     use protokoll version2 for send/receive (if possible)
 -i --init           init /etc/backsnap.d/local.conf (only with -g)
 -o --deleteold      mark old backups for deletion in gui (-o=500)
 -m --keepminimum    mark all but minimum backups for deletion in gui (-m=250)  
 
 -o,-m,        need  manual confirmation in the gui to delete marked snapshots
 -i            needs gui to confirm uuid of backup-medium
  
 For sources see@ https://github.com/andreaskielkopf/BackSnap and inside this file
 For help go to   https://forum.manjaro.org/t/howto-hilfsprogramm-fur-backup-btrfs-snapshots-mit-send-recieve
```

----
Sontag, 23. Juni 2024 14:46


































----

Sonntag, 29. Oktober 2023 17:01 
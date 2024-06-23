# Konfigurationsdateien erstellen
Im Folgenden wird davon ausgegangen, dass alles [installiert](install_de.md) und das Sicherungslaufwerk 
[vorbereitet](device_de.md) wurde.

## Backsnap konfigurieren
Backsnap muss wissen, **welche** Snapshots gesichert werden sollen und **wo** die Sicherungen gespeichert werden sollen. 
Dies wird in den Konfigurationsdateien unter `/etc/backsnap.d/` festgelegt.

Als Sie „sudo backsnap -gi“ gestartet haben, hat Backsnap zuerst „/etc/backsnap.d/“ erstellt und dann eine einfache 
Konfigurationsdatei für den lokalen PC vorbereitet.

### /etc/backsnap.d/local.conf

#### 1) Wählen Sie den **PC**
```
# backup local pc per sudo
pc = localhost
# backup local pc per ssh
#pc = root@localhost
```
Einer von:
* **pc = localhost** ==> Dieser Pc (Backsnap verwendet **sudo** um btrfs aufzurufen)
* **pc = user@ip** or **pc = user@name** = Der Pc ist per SSH erreichbar (Backsnap verwendet **ssh** um btrfs dort aufzurufen)

#### 2) Wählen Sie die UUID für das **Backupvolume**
```
# detect and mount backupvolume by scanning for this id (as part of the uuid)
backup_id = 03417033-3645-4ae7-9451-efafcbb9124e
```
Nur in **einer** Konfiguration dürfen Sie die UUID der Partition angeben, auf der Backups gespeichert werden sollen.
Das Subvolume **@BackSnap** der Backup-Partition mit der besagten **UUID** wird während des Backups 
in **/tmp/BackupRoot** gemountet. Die Komprimierung zstd ist auf Stufe 9 eingestellt. Durch die Verwendung einer 
UUID wird zuverlässig verhindert, dass etwas falsch gemountet wird.

#### 3) Wählen Sie die Optionen und einen Namen für das erste Subvolume
```
# use these flags for the following subvolume (optional)
#flags = -gtc -v=1 -a=12
# backuplabel = manjaro17 for snapshots of /
manjaro17 = /
```
Alle Snapshots des unter **`/`** bereitgestellten Subvolumes werden in die Sicherung unter 
`/tmp/BackupRoot/**manjaro17**/` einbezogen.

#### 4) Wählen Sie Optionen und einen Namen für das nächste Subvolume
```
#flags = -gtc -v=1 -a=12 
#backuplabel = manjaro17.home for snapshots of /home
manjaro17.home = /home
```
Alle Snapshots des unter **/home** bereitgestellten Subvolumes werden in die Sicherung unter 
`/tmp/BackupRoot/**manjaro17.home**/` einbezogen.

#### 5) Und so weiter ...
Sie können weitere Subvolumes hinzufügen. Jedes mit seinem Mountpoint und einem eigenen Namen.

### /etc/backsnap.d/laptop.conf
Für jeden weiteren Pc der ins Backup eingebunden werden soll, müssen sie eine eigene Konfigurationsdatei erstellen.
```
# This pc is reachable via ssh with root@notebook
pc = root@notebook 
# flags are unchanged ;-)
# Backup snapshots of / to label notebook
notebook = /
# Backup snapshots of /home to label notebook.home
notebook.home = /home
```
Backsnap wird versuchen den PC per **ssh root@notebook**. zu erreichen. Wenn das fehlschlägt wird dieser pc übersprungen. 

### Jetzt gehts los

##### Sie können einen Probelauf machen
In einem Terminal:
```
sudo backsnap -gcd -v=6 -a=20
```
* -g mit gui
* -c compressed btrfs send-format
* -d dry-run
* -v=6 ausführlich
* -a=20 automatsch weitermachen mit dem nächsten Subvolume nach 20 Sekunden

##### Sie können ein richtiges Backup machen
In einem Terminal: 
```
sudo backsnap -gc -v=6 -a=10
```
* -g mit gui
* -c compressed btrfs send-format
* -v=6 ausführlich
* -a=10 automatsch weitermachen mit dem nächsten Subvolume nach 10 Sekunden

Bitte denken Sie daran, dass das je nach Anzahl der Snapshots wirklich lange dauern kann. Das hängt natürlich auch 
davon ab, wie schnell ihr Backup-Laufwerk ist.

## Fertig
Solange die Configdateien in /etc/backsnap existieren, reicht ein 
```
sudo backsnap -gc 
```
oder ein
```
sudo backsnap
```
um ein backup auf das externe Laufwerk zu erstellen.
Die verfügbaren (und configurierten Optionen werden angezeigt mit  
```
sudo backsnap -h
```
```
BackSnap Version 0.6.8.27 (2024/06/22)
args >  -h 
java [version=23-ea, major=23, minor=null, patch=null]
using ThreadPerTaskExecutor
Pc![sudo ] & Id:03417033-3745-4ae7-9451-efafcbb9124e
OneBackup[/etc/backsnap.d/gast.conf, Pc[root@gast]:/ (gast) flags=-c -a=5 -v=5 -o=500 -m=200]
OneBackup[/etc/backsnap.d/gast.conf, Pc[root@gast]:/home (gast.home) flags=-c -a=5 -v=5 -o=500 -m=200]
OneBackup[/etc/backsnap.d/jitsim1.conf, Pc[root@jitsim1]:/ (jitsim1) flags=-c -a=5 -v=5 -o=3500 -m=1000]
OneBackup[/etc/backsnap.d/jitsim1.conf, Pc[root@jitsim1]:/home (jitsim1.home) flags=-c -a=5 -v=5 -o=3500 -m=1000]
OneBackup[/etc/backsnap.d/jitsim1.conf, Pc[root@jitsim1]:/opt/hst (jitsim1.hst) flags=-c -a=5 -v=5 -o=3500 -m=1000]
OneBackup[/etc/backsnap.d/jitsim1.conf, Pc[root@jitsim1]:/opt/hugo (jitsim1.hugo) flags=-c -a=5 -v=5 -o=3500 -m=1000]
OneBackup[/etc/backsnap.d/local.conf, Pc[sudo ]:/ (manjaro18) flags=-c -v=5 -a=5 -o=4000 -m=1000]
OneBackup[/etc/backsnap.d/local.conf, Pc[sudo ]:/home (manjaro18.home) flags=-c -v=5 -a=5 -o=4000 -m=1000]
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
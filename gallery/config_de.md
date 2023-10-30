# Konfigurationsdateien erstellen
Im Folgenden wird davon ausgegangen, dass alles [installiert](install_de.md) und das Sicherungslaufwerk [vorbereitet](device_de.md) wurde.

## Backsnap konfigurieren
Backsnap muss wissen, **welche** Snapshots gesichert werden sollen und **wo** die Sicherungen gespeichert werden sollen. Dies wird in den Konfigurationsdateien unter `/etc/backsnap.d/` festgelegt.

Als Sie „sudo backsnap -gi“ gestartet haben, hat Backsnap zuerst „/etc/backsnap.d/“ erstellt und dann eine einfache Konfigurationsdatei für den lokalen PC vorbereitet.

### `/etc/backsnap.d/local.conf`

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
Das Subvolume **@BackSnap** der Backup-Partition mit der besagten **UUID** wird während des Backups in **/tmp/BackupRoot** gemountet. Die Komprimierung zstd ist auf Stufe 9 eingestellt. Durch die Verwendung einer UUID wird zuverlässig verhindert, dass etwas falsch gemountet wird.

#### 3) Wählen Sie die Optionen und einen Namen für das erste Subvolume
```
# use these flags for the following subvolume (optional)
#flags = -gtc -v=1 -a=12
# backuplabel = manjaro18 for snapshots of /
manjaro18 = /
```
Alle Snapshots des unter **`/`** bereitgestellten Subvolumes werden in die Sicherung unter /tmp/BackupRoot/**manjaro18**/ einbezogen.

#### 4) Wählen Sie Optionen und einen Namen für das nächste Subvolume
```
#flags = -gtc -v=1 -a=12 
#backuplabel = manjaro18.home for snapshots of /home
manjaro18.home = /home
```
Alle Snapshots des unter **/home** bereitgestellten Subvolumes werden in die Sicherung unter /tmp/BackupRoot/**manjaro18.home**/ einbezogen.

#### 5) Und so weiter ...
Sie können weitere Subvolumes hier hinzufügen. Jedes mit seinem Mountpoint und einem eigenen Namen.

### /etc/backsnap.d/laptop.conf
Für jeden weiteren Pc der ins Backup eingebunden werden soll, müssen sie hier eine eigene Konfigurationsdatei erstellen.
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
Backsnap wird versuchen den PC per **ssh root@notebook**. zu erreichen. Wenn das fehlschlägt wird dieser pc übersprungen. 

### Jetzt gehts los

##### Sie können einen Probelauf machen
In einem Terminal: **`sudo backsnap -gcd -v=6 -a=20`**
* -g mit gui
* -c compressed btrfs send-format
* -d dry-run
* -v=6 ausführlich
* -a=20 automatsch weitermachen mit dem nächsten Subvolume nach 20 Sekunden

##### Sie können ein richtiges Backup machen
In einem Terminal: **`sudo backsnap -gc -v=6 -a=10`**
* -g mit gui
* -c compressed btrfs send-format
* -v=6 ausführlich
* -a=10 automatsch weitermachen mit dem nächsten Subvolume nach 10 Sekunden

Bitte denken Sie daran, dass das je nach Anzahl der Snapshots wirklich lange dauern kann.

----

Sonntag, 29. Oktober 2023 17:01 
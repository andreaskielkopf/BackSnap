# [HowTo] Backup btrfs snapshots mit send/recieve
[english](./readme.md), [Wie es funktioniert](./WieEsFunktioniert.md) 
## Prolog

###### Ein Snapshot ist kein Backup !

Das hab ich schon öfters gelesen. Im Prinzip richtig, aber ein btrfs-snapshot ist **so gut wie jedes andere In-System-Backup**.
Btrfs kann für Privatanwender ein hohes Maß an Sicherheit vor Datenverlust bringen. 
Mit etwas zusätzlicher Arbeit kann auch das externe Backup für btrfs erreicht werden. Dann ist Backup 3-2-1 mit btrfs einfach möglich

## Die Verantwortung für Backups liegt nie bei einem Programm, sondern immer beim Nutzer !

##### In-System Backup (n-snapshots 1)
Btrfs mit RAID0 und **readonly snapshots** durch snapper sind wie ein im selben System untergebrachtes Backup der Original-Dateien. Es schützt vor:
* kleineren Problemen, wie dem versehentlichen Löschen einer Datei 
* löschen eines ganzen Dateibaums (z.B. /home/hans/* )
* vor dem ungewollten Ändern von Dateirechten vieler Dateien
* ungewollt veränderten Dateien

##### In-System Backup mit RAID (+1)
Wird btrfs mit `RAID1` auf mindestens 2 verschiedenen Devices betrieben, ist das so gut wie **ein lokales Backup**. 
Das schützt zusätzlich vor:
* Ausfall eines kompletten Device
* Beschädigung des Dateisystems auf einem Device
* Verlust der Partitionstabelle auf einem Device
* überschreiben eines Device mit dd ;-)

##### Out-Of-System Backup (+1)
Werden die snapshots zusätzlich auf einer externen Platte gesichert, ist das ein weiteres ECHTES **externes Backup**. 
Bestenfalls sollte die externe Platte nur kurzzeitig am Rechner angeschlossen werden. Das schützt zusätzlich vor:
* Komplettausfall des Rechners
* Verlust vergangener Backups bis zur Kapazitätsgrenze der extenen Platte
* gezieltem Löschen der internen Backups (z.B. durch Mallware)

Btrfs entspricht dann 3-2-1 Backup (Near CDP)

## Ziele von backsnap: 
**Einfaches externes Backup** eines kompletten (btrfs-)Subvolumes

* **Alle** Snapshots sichern
* **Differentielle Sicherung** der einzelnen Snapshots (schnell). Bei wiederholung des Backups nach einigen Tagen/Wochen/Monaten
* So **wenig Speicherplatzverbrauch** auf dem Sicherungsmedium wie möglich
* Das Backupmedium kann für **Backups verschiedener Computer** benutzt werden
* Kommandozeilenprogramm ohne GUI
* KISS

* mit GUI -g anschaliche Darstellung der vorhandenen Backups
* GUI-gesteuertes löschen alter Backups
* GUI-gesteuertes ausdünnen der Backups
* Anzeige der Snapshots und Backups

##### Nicht Ziel:
* Automatische Verwaltung der Backups nach Alter
* Automatisches löschen alter Backups bei Platznot
* Sicherung des aktuellen Zustands eines Subvolumes

##### Erwünschte Nebeneffekte
* Die **Backup-Strategie** ist bereits in snapper/Timeshift festgelegt, und wirkt hier mit
* Das Backup ist zwar komprimiert, aber gleichzeitig sind alle Snapshots im Backup **immer voll readonly zugreifbar**

## Vorraussetzungen:
* **Java 17** oder neuer auf dem Computer
* Empfohlen: **pv** installiert
* BTRFS sowohl auf dem Computer (empfohlen als **RAID 1** mit 2 devices) als auch auf dem Backupmedium (single)
* **snapper**-Layout der Snapshots (Das programm kann auch mit anderen Layouts arbeiten)
* Die Snapshots müssen **readonly** sein sonst kann btrfs sie nicht übertragen.
* Empfohlen: externes Backupmedium z.B. USB3-Festplatte
* Empfohlen: ein eigenes bash-script um das backup leicht zu starten

# Backsnap:
Das Java-Programm Backsnap sichert ALLE Snapshots aus einem angegebenen Verzeichnis in ein anderes Verzeichnis auf einem
Backupmedium. Dazu benutzt es **btrfs send** und **btrfs receive**.

##### Quelle (snapshots)
Der 1. übergebene Parameter zeigt auf den **QUELL-Pfad** an dem die Snapshots 
von Snapper erreichbar sind. Snapper legt alle Snapshots in Verzeichnisse mit aufsteigender Nummerierung an. 
Der eigentliche Snapshot dort heißt dann einfach "snapshot".  

* **/**
* **/home**
* /.snapshots (alternativ)
* /home/.snapshots (alternativ)

##### Ziel (backups)
Der 2. Parameter zeigt auf die Stelle an der die Snapshots gesichert werden sollen. 
Dazu muß vor dem Programmaufruf das **Backupmedium gemountet** werden. 
Ideal ist ein Backup-Subvolume mit Namen **/@BackSnap** und ein Verzeichnis mit einem individuellen 
Namen für jedes Subvolume von jedem PC. 
Der Pfad zu diesem gemounteten Verzeichnis wird als **ZIEL-Pfad** für die Sicherung angegeben.

* **/mnt/BackSnap/manjaro21** (wenn **/@BackSnap** nach /mnt/BackSnap gemounted wurde)
* **/mnt/BackSnap/manjaro21.home** (wenn **/@BackSnap** nach /mnt/BackSnap gemounted wurde)
* /mnt/BACKUP/@BackSnap/manjaro21 (alternativ, wenn btrfs-**/** nach /mnt/BACKUP gemounted wurde)
* /mnt/BACKUP/@BackSnap/manjaro21.home (alternativ, wenn btrfs-**/** nach /mnt/BACKUP gemounted wurde)

Backsnap geht alle Verzeichnisse im Quellpfad in **zeitlich aufsteigender Reihenfolge** durch, und prüft ob das 
jeweilige Verzeichniss am Ziel bereits existiert. Wenn nicht, wird der snapshot dorthin gesichert.
Wenn möglich wird dabei auf einem vorigen Snapshot als **"Parent"** aufgebaut.

Mit jedem Aufruf des Programms können alle Snapshots EINES Subvolumes gesichert werden,
das EINER Konfiguration von Snapper entspricht. 

##### SSH
Als Quellpfad kann auch ein per `ssh` erreichbarer Rechner angegeben werden. Dazu sind `ssh-keys`, `ssh`, und ein 
`ssh-agent` erforderlich. Der ssh-Agent, die keys sowie die ssh-Verbindung sollten **vorher separat getestet** werden. 
Da `backsnap` auch lokal als `root` gestartet werden muß, muß die ssh-verbindung für `root` eingerichtet sein (lokal und entfernt) 

* root@192.168.1.23:/
* root@192.168.1.23:/home

###### Warnungen:
SSH vollbringt keine Wunder. Das kann (trotz kompression und differentellem Backup) nur so schnell sein wie 
dein Netzwerk. (1GB/s / 100MB/s / Wlan ??? )

Ich werde bei SSH-Problemen keine Hilfe geben können!
Solange das Programm zwar lokal funktioniert, über SSH aber nicht, scheint SSH nicht vollständig eingerichtet zu sein.
SSH ist nicht jedermanns Stärke, aber es gibt eine Menge gute Anleitungen dazu wenn man danach sucht.

### Vorbereitungen

###### externe Festplatte
* Partitionstabelle GPT empfohlen
* Eine Partition mit btrfs formatiert
* Dort ein Subvolume `/@BackSnap` anlegen !

###### fstab 
* Einen Mountpoint anlegen `sudo mkdir /mnt/BackSnap`
* Die externe Festplatte in der fstab eintragen. Beispiel:

> UUID=03417033-3745-4ae7-9451-efafcbb9124e /mnt/BackSnap  btrfs noauto,rw,noatime,compress=zstd:9,subvol=/@BackSnap 0 0

Die uuid bekommt man z.B. mit `lsblk -o name,uuid` 

##### Installieren
Es empfiehlt sich die Java-version zu prüfen:
`java -version`

Die Datei backsnap.jar muß "irgendwo" abgelegt werden. (Empfohlen: /usr/local/bin/backsnap.jar). 
Der Aufruf erfolgt dann als root.

`java -jar /usr/local/bin/backsnap.jar / /mnt/BackSnap/manjaro21` 
oder

`sudo java -jar /usr/local/bin/backsnap.jar / /mnt/BackSnap/manjaro21` 
oder

`ssh root@localhost java -jar /usr/local/bin/backsnap.jar / /mnt/BackSnap/manjaro21` 
oder

`java -jar /usr/local/bin/backsnap.jar root@localhost:/ root@localhost:/mnt/BackSnap/manjaro21` 

##### Vorbereiten (einmalig)
 * root werden `sudo su -`
 * Backupmedium btrfs-Volume **/** (!) mounten nach /mnt 
 * subvolume anlegen `btrfs subvolume create /mnt/@BackSnap`
 * unmounten (!) `umount /mnt`
 * Mountpoint für /@BackSnap anlegen `mkdir /mnt/BackSnap`
 
##### Vorbereiten (pro PC/Subvolume)
 * root werden `sudo su -`
 * Backupmedium btrfs-Subvolume **/BackSnap** (!) mounten nach /mnt/BackSnap 
 * Zielpfad für **manjaro21** anlegen `mkdir /mnt/BackSnap/manjaro21`
 * Zielpfad für **home** anlegen `mkdir /mnt/BackSnap/manjaro21.home`
 * weitere Zielpfade für Rechner/Subvolumes nach bedarf anlegen
 * Erstes Backup erzeugen `java -jar /usr/local/bin/backsnap.jar /home /mnt/BackSnap/manjaro21.home`
 
>    ACHTUNG: das kann ganz schön lange dauern, also am besten das kleinste subvolume zuerst probieren ;-)

 * nach einer Stunde probeweise wiederholen !

##### backup script
Nach ersten manuellen Experimenten sollte ein backup-script angelegt werden (z.B. in `/usr/local/bin/backsnap.sh` ). 
Darin werden dann die gewünschten Subvolumes / Rechner eingetragen. 
Btrfs braucht root-zugriff. Das Script muß also **ausführbar** sein und mit **sudo** gestartet werden!

###### Beispiel:
```
   #!/bin/zsh
   # Sichern aller aufgelisteten Rechner auf ein angestecktes Laufwerk
   # BS_UUID=03417033-3745-4ae7-9451-efafcbb9124e
   BS_MOUNT=/mnt/BackSnap
   BS_JAR=/usr/local/bin/backsnap.jar
   
   # mount aus fstab ;-)
   
   # [ -d $BS_MOUNT ] || mount $BS_MOUNT
   # sudo mount -o noatime,subvol=/,compress=zstd:9 UUID=$BS_UUID $BS_MOUNT
   # wenn das mounten nicht geklappt hat, ABBRECHEN 
   # [ -d $BS_MOUNT/@snapshots ] || { echo "Das mounten war nicht erfolgreich"; exit; }
   [ -d $BS_MOUNT/$BS_HOST ] || { echo "Für $BS_HOST gibt es kein Verzeichnis"; exit; }
   
   function backup {
       BS_SOURCE="$1"
       BS_DEST="$BS_MOUNT/$2"
   #    echo "java -jar $BS_JAR $BS_SOURCE $BS_DEST"
       java -jar $BS_JAR $BS_SOURCE $BS_DEST
       echo -n " Y" 
   }
   
   # lokal sichern
   backup "/" "manjaro18"
   backup "/home" "manjaro18.home"
   
   # server sichern
   backup "root@server:/" "server"
   backup "root@server:/home" "server.home"
   
   # laptop sichern 
   backup "root@notebook:/" "notebook"
   backup "root@notebook:/home" "notebook.home"
   
   # gast sichern
   backup "root@gast:/" "gast"
   backup "root@gast:/home" "gast.home"
   umount $BS_MOUNT
   echo "fertig"
```
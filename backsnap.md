# [HowTo] Backup btrfs snapshots mit send/recieve

## Prolog

Ein Snapshot ist kein Backup !

Das hab ich schon öfters gelesen. Im Prinzip richtig, aber ein btrfs-snapshot ist so gut wie jedes andere 
In-System-Backup. Btrfs kann für Privatanwender ein hohes Maß an Sicherheit vor Datenverlust bringen. 
Mit etwas zusätzlicher Arbeit kann auch das externe Backup für btrfs nachgerüstet werden.

# Die Verantwortung für Backups liegt nie bei einem Programm, sondern immer beim Nutzer !

##### In-System Backup
Btrfs mit RAID0 und readonly snapshots per snapper sind wie ein im selben System untergebrachtes Backup der Original-Dateien. Es schützt vor:
* kleineren Problemen, wie dem versehentlichen Löschen einer Datei 
* löschen eines ganzen Dateibaums (z.B. /home/hans/** )
* vor dem ungewollten Ändern von Dateirechten vieler Dateien

##### In-System Backup mit RAID
Wird btrfs mit `RAID1` auf mindestens 2 verschiedenen Devices betrieben, ist das so gut wie ein lokales Backup auf ein RAID-System. Das schützt zusätzlich vor:
* Ausfall eines kompletten Device
* Beschädigung des Dateisystems auf einem Device
* Verlust der Partitionstabelle auf einem Device
* überschreiben eines Device mit dd ;-)

##### Out-Of-System Backup 
Werden die snapshots zusätzlich auf einer externen Platte kopiert, ist das ein ECHTES Backup. Am besten sollte diese 
externe Platte nur kurzzeitig am Rechner angeschlossen werden. Das schützt zusätzlich vor:
* Komplettausfall des Rechners
* Verlust vergangener Backups bis zur Kapazitätsgrenze der extenen Platte
* gezieltem Löschen der internen Backups durch Mallware

(btrfs entspricht dann Near CDP)

## Ziele von backsnap: 
Einfaches externes Backup eines kompletten (btrfs-)Subvolumes

* Alle Snapshots sichern
* Differentielle Sicherung der einzelnen Snapshots (schnell)
* So wenig Speicherplatzverbrauch auf dem Sicherungsmedium wie möglich
* Differentielle Sicherung (Bei wiederholung des Backups nach einigen Tagen/Wochen/Monaten)
* Das Backupmedium kann für Backups verschiedener Computer benutzt werden
* Kommandozeilenprogramm ohne GUI
* KISS

##### Keine Ziele:
* Automatische Verwaltung der Backups nach Alter
* Automatisches löschen alter Backups bei Platznot
* Sicherung des aktuellen Zustands eines Subvolumes

## Vorraussetzungen:
* Java 17 oder neuer auf dem Computer
* Empfohlen: pv installiert
* BTRFS sowohl auf dem Computer (empfohlen als RAID1 mit 2 devices) als auch auf dem Backupmedium (single)
* snapper-Layout der Snapshots
* Die Snapshots müssen readonly sein, und sie sind fortlaufend numeriert (Lücken sind erlaubt)
* Empfohlen: externes Backupmedium z.B. USB3-Festplatte
* Empfohlen: ein eigenes bash-script um das backup zu starten

# Backsnap:
Das Java-Programm Backsnap sichert ALLE Snapshots aus einem angegebenen Verzeichnis in ein anderes Verzeichnis auf einem
Backupmedium. Dazu benutzt es **btrfs send**  und **btrfs receive**.

##### Quelle
Der 1. übergebene Parameter zeigt auf den **QUELL-Pfad** an dem die Snapshots 
von Snapper erreichbar sind. Snapper legt alle Snapshots in Verzeichnisse mit aufsteigender Nummerierung. 
Der eigentliche Snapshot dort heißt dann einfach "snapshot".  

* /.snapshots
* /home/.snapshots

##### Ziel
Der 2. Parameter zeigt auf die Stelle an der die Snapshots gesichert werden sollen. Dazu muß das Backupmedium 
"speziell (subvol=/)" gemountet werden. Es braucht ein Subvolume mit Namen **@snapshots** und ein Verzeichnis 
mit einem individuellen Namen für jedes Subvolume. 
Der Pfad zu diesem Verzeichnis wird als **ZIEL-Pfad** für die Sicherung angegeben.

* /mnt/BACKUP/@snapshots/manjaro21
* /mnt/BACKUP/@snapshots/manjaro21.home

Backsnap geht alle Verzeichnisse im Quellpfad in numerisch aufsteigender Reihenfolge durch, und prüft ob das 
jeweilige Verzeichniss am Ziel bereits existiert. Wenn nicht, wird der snapshot dorthin gesichert.
Wenn möglich wird dabei auf einem vorigen Snapshot als "Parent" aufgebaut.

Mit jedem Aufruf des Programms können alle Snapshots EINES Subvolumes gesichert werden das 
EINER Konfiguration von Snapper entspricht. 

##### SSH
Als Quellpfad kann auch ein per `ssh` erreichbarer Rechner angegeben werden. Dazu ist `ssh`, `ssh-keys` und ein 
`ssh-agent` erforderlich. Der ssh-Agent, die keys sowie die ssh-Verbindung sollten vorher **unbedingt separat getestet**  werden. 
Da `backsnap` auch lokal als `root` gestartet werden muß, muß die ssh-verbindung für `root` eingerichtet sein (lokal und entfernt) 

* root@192.168.1.23:/.snapshots
* root@192.168.1.23:/home/.snapshots


### Vorbereitungen

###### externe Festplatte
* Partitionstabelle GPT empfohlen
* Eine Partition mit btrfs formatiert
* später dort ein subvolume `@snapshots` anlegen !

###### fstab
* Einen Mountpoint anlegen `sudo mkdir /mnt/BACKUP`
* Die externe Festplatte in der fstab eintragen. Beispiel:

> UUID=03417033-3745-4ae7-9451-efafcbb9124e /mnt/BACKUP    btrfs noauto,rw,noatime,compress=zstd:9,subvol=/         0 0

Die uid bekommt man z.B. mit `lsblk -o name,uuid` 

##### Installieren
Die datei backsnap.jar muß "irgendwo" abgelegt werden. (Empfohlen: /usr/local/bin/backsnap.jar). Der Aufruf erfolgt dann als root.

`java -jar /usr/local/bin/backsnap.jar /.snapshots /mnt/BACKUP/manjaro21` 

##### Test
 * root werden `sudo su -`
 * Backupmedium mounten `mount /mnt/BACUP`
 * subvolume anlegen `btrfs subvolume create /mnt/BACKUP/@snapshots`
 * Zielpfad anlegen `mkdir /mnt/BACKUP/@snapshots/manjaro21`
 * Zielpfad anlegen `mkdir /mnt/BACKUP/@snapshots/manjaro21.home`
 * weitere Zielpfade für Rechner/Subvolumes anlegen
 * Backup erzeugen `java -jar /usr/local/bin/backsnap.jar /home/.snapshots /mnt/BACKUP/@snapshots/manjaro21.home`
 
>    ACHTUNG: das kann ganz schön lange dauern, also am besten das kleinste subvolume zuerst ;-)

 * nach einer Stunde probeweise wiederholen !

##### backup script
Nach ersten manuellen Experimenten sollte ein backup-script angelegt werden (z.B. in `/usr/local/bin/backup` ). 
Darin werden dann die gewünschten Subvolumes / Rechner gesichert. 
Btrfs braucht root-zugriff. Das Script muß also ausführbar sein und mit sudo gestartet werden!

   `#!/bin/zsh`
   `# Sichern aller aufgelisteten Rechner auf ein angestecktes Laufwerk`
   `# BS_UUID=03417033-3745-4ae7-9451-efafcbb9124e`
   `BS_MOUNT=/mnt/BACKUP`
   `BS_JAR=/usr/local/bin/backsnap.jar`
   ``
   `# mount aus fstab ;-)`
   ``
   `[ -d $BS_MOUNT/@snapshots ] || mount $BS_MOUNT`
   `#sudo mount -o noatime,subvol=/,compress=zstd:9 UUID=$BS_UUID $BS_MOUNT`
   `# wenn das mounten nicht geklappt hat, ABBRECHEN `
   `[ -d $BS_MOUNT/@snapshots ] || { echo "Das mounten war nicht erfolgreich"; exit; }`
   `[ -d $BS_MOUNT/@snapshots/$BS_HOST ] || { echo "Für $BS_HOST gibt es keinen mountpoint"; exit; }`
   ``
   `function backup {`
   `    BS_SOURCE="$1/.snapshots"`
   `    BS_DEST="$BS_MOUNT/@snapshots/$2"`
   `#    echo "java -jar $BS_JAR $BS_SOURCE $BS_DEST"`
   `    java -jar $BS_JAR $BS_SOURCE $BS_DEST`
   `    echo -n " Y" `
   `}`
   ``
   `# lokal sichern`
   `backup "" "manjaro18"`
   `backup "/home" "manjaro18.home"`
   ``
   `# server sichern`
   `backup "root@server:" "server"`
   `backup "root@server:/home" "server.home"`
   ``
   `# laptop sichern `
   `backup "root@notebook:" "notebook"`
   `backup "root@notebook:/home" "notebook.home"`
   ``
   `# gast sichern`
   `backup "root@gast:" "gast"`
   `backup "root@gast:/home" "gast"`
   `umount $BS_MOUNT`
   `echo "fertig"`
   
 

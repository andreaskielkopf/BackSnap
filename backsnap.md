# [HowTo] Backup btrfs snapshots mit send/recieve

## Ziele: 

Einfach ein Backup eines kompletten Systems

* Alle Snapshots sichern
* Differentielle Sicherung der einzelnen Snapshots (schnell)
* So wenig Speicherplatzverbrauch auf dem Sicherungsmedium wie möglich
* Differentielle Sicherung (Bei wiederholung des Backups nach einigen Tagen/Wochen/Monaten)
* Das Backupmedium kann für Backups verschiedener Computer benutzt werden

## Kein Ziel:

* Automatische Verwaltung der Backups nach Alter
* Automatisches löschen alter Backups bei Platznot
* Sicherung des aktuellen Zustands des Subvolumes

## Vorraussetzungen:

* BTRFS sowohl auf dem Computer als auch auf dem Backupmedium
* snapper-Layout der Snapshots
* Java 11 oder neuer auf dem Computer
* Empfohlen: pv installiert

## Backsnap:

Das Java-Programm Backsnap sichert ALLE Snapshots die angegeben wurden auf ein Backupmedium. Dazu benutzt es 
**btrfs send**  und **btrfs receive**.

Der 1. übergebene Parameter zeigt auf den **QUELL-Pfad** an dem die Snapshots 
von Snapper erreichbar sind. Snapper legt alle Snapshots in Verzeichnisse mit aufsteigender Nummerierung. 
Der eigentliche Snapshot dort heißt einfach "snapshot".  

* /.snapshots
* /home/.snapshots

Der 2. Parameter zeigt auf die Stelle an der die Snapshots gesichert werden sollen. Dazu muß das Backupmedium 
"speziell (subvol=/)" gemountet werden. Es braucht ein Subvolume mit Namen **@snapshots** und ein Verzeichnis 
mit dem Namen diese PC. Der Pfad zu diesem Verzeichnis wird als **ZIEL-Pfad** für die Sicherung angegeben.

* /mnt/BACKUP/@snapshots/manjaro
* /mnt/BACKUP/@snapshots/manjaro.home

Backsnap geht alle Verzeichnisse im Quellpfad in aufsteigender Reihenfolge durch, und prüft ob das 
jeweilige Verzeichniss am Ziel bereits existiert. Wenn nicht, wird der snapshot dorthin gesichert.
Wenn möglich wird dabei auf einem vorigen Snapshot als "Parent" aufgebaut.

Mit jedem Aufruf des Programms können alle Snapshots EINES Subvolumes gesichert werden das 
EINER Konfiguration von Snapper entspricht. 

### Erweiterungen:
 1. Mounten des Backupmediums (falls erforderlich)
   BS_MOUNT /mnt/BACKUP
   BS_UUID 03417033-3745-4ae7-9451-efafcbb9124e
   Prüfen des Mountens
 

## Empfehlungen:

* Erstelle ein Shellscript mit Namen /usr/local/bin/backup das das gesamte Backup abwickelt.  
* Mounte das Backupvolume mit der Option compress=zst:9 

#### P.S. Die Verantwortung für Backups liegt niemals bei einem Programm, sondern immer beim Nutzer !
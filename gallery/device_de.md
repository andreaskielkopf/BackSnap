# Backup-medium vorbereiten
BackSnap stellt spezifische Anforderungen an das verwendete Backup-medium. Diese sind aber nicht schwer zu erfüllen. 

### Das Backupmedium:
* sollte unbedingt ein externes Medium sein (z.B. USB-Festplatte)
* sollte unbedingt deutlich größer sein als die zu sichernden Daten
* muß auf einem anderen Device liegen wie die Snapshots die gesichert werden sollen
* muß mit **btrfs formatiert** sein

### Device aussuchen
Es ist Empfehlenswert ein Device zu verwenden, das **deutlich** mehr Speicherplatz hat als alle zu sichernden Rechner zusammen. Das mag dann eine externe **Festplatte mit 5TB** sein, oder auch mehr.

### 2. Device partitionieren und formatieren
Es ist empfehlenswert das Device mit einer `GPT`-Partitionstabelle zu versehen. Die Partition die zum Backup verwendet werden soll, muß mit `btrfs` formatiert werden. Ich habe mit `gparted` gute Erfahrungen gemacht. Das Label der btrfs-Partition darf beliebig lauten. In meinem Beispiel ist es `Backup`

#### Weitere Vorbereitung

Die weitere Vorbereitung kann backsnap inzwichen selbst durchführen, wenn es mit der option `-gi` gestartet wird.

----
[Weitere Infos zu btrfs](https://wiki.manjaro.org/index.php/Btrfs) 
Sonntag, 30. April 2023 07:01 


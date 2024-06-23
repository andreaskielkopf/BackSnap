# Bereiten Sie ein Sicherungslaufwerk vor
BackSnap stellt besondere Anforderungen an das verwendete Sicherungslaufwerk. Aber diese sind nicht schwer zu erreichen.

### Das Backup-Laufwerk:
* Sollte auf jeden Fall ein **externes** Laufwerk sein (z. B. USB-Festplatte)
* Sollte auf jeden Fall deutlich **größer** sein als das zu sichernde Laufwerk in Ihrem PC
* Muss sich auf einem **anderen** Laufwerk befinden als die zu sichernden Snapshots
* Muss **btrfs-formatiert sein**

### Laufwerk auswählen
Es wird empfohlen, ein Laufwerk zu verwenden, das über **deutlich** mehr Speicherplatz verfügt als alle zu sichernden 
Computer zusammen. Das könnte dann eine externe **Festplatte mit 2TB** sein, oder auch mehr.

### Partitionieren und formatieren Sie das Backup-Laufwerk
Es wird empfohlen, das Gerät mit einer **GPT-Partitionstabelle** auszustatten. Die für die Sicherung zu verwendende 
Partition muss mit **btrfs** formatiert sein. Ich habe gute Erfahrungen mit `gparted` gemacht. Die Bezeichnung der 
BTRFS-Partition kann beliebig sein. 

#### Weitere Vorbereitung
Die weitere Vorbereitung kann backsnap selbst durchführen, wenn es in einem `terminal` mit der option `-gi` gestartet wird.
Bitte schließen Sie Ihr Backup-Laufwerk an, so dass Btrfs es erkennen kann. (Sie müssen es nicht mounten)
```
sudo backsnap -gi
```

### [nächster Schritt ->](config_de.md)
----
[Weitere Infos zu btrfs](https://wiki.manjaro.org/index.php/Btrfs)

Sontag, 23. Juni 2024 14:46


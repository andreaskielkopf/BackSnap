# BackSnap CLI ausprobieren
Das folgende setzt vorraus dass das Backupmedium und der erforderliche Mountpunkt vorbereitet wurden.
### Backupmedium mounten
Das Subvolume @BackSnap des Backup-device mit der besagten UUID wird nach /mnt/BackSnap gemountet. Dabei wird die Kompression zstd mit Level 9 eingestellt. Das verwenden der UUID verhindert zuverlässig, dass etwas falsches gemountet wird.

`sudo mount -t btrfs -o subvol=@BackSnap,compress=zstd:9 /dev/disk/by-uuid/03417033-3745-4be7-9451-efafcbb9124e /mnt/BackSnap`

### Einen einzelnen Snapshot sichern
Sichere einen einzelnen Snapshot (-s) des Hauptverzeichnis (/) auf das Backupmedium in den Ordner manjaro23. Dabei wird der älteste Snapshot gesichert, der noch nicht gesichert wurde. 

`sudo java -jar backsnap.jar -s / /mnt/BackSnap/manjaro23 `

Wenn das der allererste Snapshot ist, der gesichert wird, kann das einige Zeit dauern. Der Fortschrittsbalken zeigt minütlich an, wie viele Daten bereits kopiert sind. Das Programm zeigt keine Prozentangaben, weil die Gesamtgröße vorher nicht bekannt ist.

`sudo java -jar backsnap.jar -s / /mnt/BackSnap/manjaro23 `

Wenn das der zweite Snapshot ist, der gesichert wird, geht dieser hier erheblich schneller. Er baut auf dem vorherigen Snapshot auf, und braucht nur Platz für die Unterschiede zum ersten Snapshot .

### Alle Snapshots sichern
Sichere nacheinander alle Snapshots (-s) vomHauptverzeichnis (/) auf das Backupmedium in den Ordner manjaro23.

`sudo java -jar backsnap.jar / /mnt/BackSnap/manjaro23 `

Wenn das durchgelaufen ist, sind alle Snapshots auf das Backup-device übertragen

### Test des Backup

Jetzt ist es an der Zeit einmal nach /mnt/BackSnap/manjaro23/ zu gehen und die Backups anzusehen. In jedem Ordener befindet sich ein Snapshot mit dem Namen "snapshot". Darin sollten alle Dateien des Snapshot readonly zugreifbar sein.

`sudo ls -lA /mnt/Backsnap/manjaro23/`

Wenn sie mc installiert haben, ist das eine gute Möglichkeit nachzusehen ob die Dateien im Backup da sind

`sudo mc`

### Backup abschließen
Nicht vergessen das Backup-device wieder zu entfernen

`sudo umount /mnt/BackSnap `

----
Montag, 29. Mai 2023 17:46 



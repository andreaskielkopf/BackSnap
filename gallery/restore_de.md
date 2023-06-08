# Restore vom backup
Je nach dem was kaputt gegangen ist, gibt es verschiedene Wege für eine Reparatur des Schadens. In Folgenden wird je nach Schadensart eine Reparaturmethode empfohlen. Bei gravierenderen Schäden ist es keine Schande sich Hilfe aus dem manjaro-forum zu holen.
Wer Ihnen jedoch sofort rät alles hinzuschmeißen, das Haus abzureißen und neu zu bauen, ist ganz offensichtlich nicht der beste Ratgeber. Entweder er kennt btrfs nicht, oder er liebt es systeme ständig neu aufzusetzen, oder er denkt einfach, dass er froh ist diese Arbeit nicht selbst nachen zu müssen..
## Vor dem Restore
Wenn möglich sollte vom aktuellen Zustand ein Snapshot (und auch ein Backup) gemacht werden. Die Reparatur setzt schließlich auf dem aktuellen Subvolume auf, und wir wollen ja nicht vom Regen in die Traufe kommen.

## Probleme:
#### Einzelne Datei gelöscht, Verzeichnisbaum gelöscht,  Berechtigungen beschädigt
Um eine einzelne Datei/Verzeichnis wiederherzustellen ist es am einfachsten, das **backup zu mounten**, und die Datei mit rsync zurückzukopieren. **Rsync** repariert dabei automatisch auch die Dateirechte.

#### Konfiguration beschädigt
Wenn Du weißt, welche Konfiguration du aus versehen beschädigt hast, kannst du **meld** einsetzen, um die funktionierende Konfiguration im Backup mit der aktuellen zu vergleichen, und notwendige Änderungen vorzunehmen

#### Dateisystem readonly, Dateisystem nicht mountbar, Dateisystem Totalschaden
Bei all diesen Problemen ist es sehr wichtig zuerst **die Ursache** für das Problem zu **beseitigen**. Es macht nicht wirklich Sinn, auf einer **kaputten Festplatte** wieder ein neues System aufzusetzen indem man die Partition formatiert und das Backup einspielt.
Es gibt viele gute Möglichkeiten ein btrfs-Dateisystem zu reparieren. Auch wenn es nur wenige Leute gibt, die das im Moment können. Wer sich mit btrfs nicht auskennt, kann es halt auch nicht reparieren.
siehe auch:
* [Dateien retten aus btrfs (auch wenn das Backup fehlt)](https://forum.manjaro.org/t/how-to-daten-von-einem-beschadigten-btrfs-volume-retten/79400) 
* [manjaro - wiki zu btrfs](https://wiki.manjaro.org/index.php/Btrfs) 
* [arch-wiki zu btrfs](https://wiki.archlinux.org/title/Btrfs) 
* [Btrfs Dokumentation(Read the docs)](https://btrfs.readthedocs.io/en/latest/index.html)

## Hilfsmittel für Lösungen:

### rsync
Rsync ist ein Werkzeug mit dem einzelne **Dateien kopiert** werden können. Aber es erlaubt auch ganze **Verzeichnisse** zu kopieren. Dazu wird der Snapshot mit den gewünschten Dateien einfach irgendwo gemountet, und dann die Dateien mit rsync zurück-kopiert.
Es können natürlich auch Programme wie cp, mc, oder jeder andere Dateimanager verwendet werden.

##### Das Besondere an rsync
* Rsync achtet darauf auch die **Dateirechte** wieder richtig zu stellen.
* Rsync arbeitet **besonders schonend** auf btrfs-Dateisystemen, weil es nur die nicht übereinstimmenden Teile einer Datei ersetzt, anstatt einfach die ganze Datei neu zu schreiben. Das spart enorm Platz im Snapshot.
* Rsync kann natürlich **auch über ssh** eingesetzt werden.
* Nebenbei kann rsync auch nicht mehr benötigte Dateien **entfernen**

### meld
**Meld** ist ein beliebtes Programm um Dateien zu vergleichen, und einzelne Passagen aus der einen Datei (die funktioniert hatte), in die andere Datei zu übernehmen. Auch dafür können verschiedene andere Programme wie z.B. **mc** eingesetzt werden

### Neues Dateisystem ? Rollback ?
Wenn es noch irgend einen Snapshot auf dem btrfs-Volume gibt, der dem Subvolume zu irgendeinem Zeitpunkt entspricht (auch wenn er ein Jahr alt sein sollte), dann ist dieser Snapshot eine gute Basis für die Reparatur.
1. Einen neuen schreibbaren **Snapshot** von der "Basis" anfertigen.
2. Das Backup irgendwo **mounten**
3. Mit rsync das Backup in diesen beschreibbaren Snapshot **hineinkopieren**. 
Dabei wird rsync nur die Dateien ändern, die mit der Basis nicht übereinstimmen. In diesem Fall sollten sie rsync auf jeden fall auch beauftragen nötigenfalls dateien zu **löschen**, die nicht mehr verwendet werden.
4. Einen **Rollback** auf diesen beschreibbaren Snapshot machen.
siehe auch:
* [manueller Rollback mit btrfs](https://forum.manjaro.org/t/howto-rollback-mit-btrfs-von-hand/80209/7) 

#### Ich wünsche gutes Gelingen.

Samstag, 03. Juni 2023 06:30 


# Wie es funktioniert:
[ReadMe](./backsnap.md),  [english](./HowItWorks.md) 


#### per CLI
`backsnap / manjaro21`
`backsnap /home manjaro21.home`

oder

`sudo java -jar backsnap.jar / manjaro21`
`sudo java -jar backsnap.jar /home manjaro21.home`

1. starte BackSnap (aus /usr/local/bin)
4. Pfad zu Quell-Snapshots (sourcedir)
5. Pfad zum Speichern von Backups der Snapshots (destdir)


#### per /etc/backsnap.d/local.conf

`sudo backsnap -gi`

oder 

`sudo java -jar backsnap.jar -gi`

1. starte BackSnap (aus /usr/local/bin)
4. option -gi ==> unterstützt durch GUI und automatische Initialisierung nach /etc/backsnap.d/local.conf

# Was es macht

## Informationen über sourcedir sammeln
* Suche Snapshots im angegebenen Subvolume
* Sortieren in eine source-map

## Informationen zum Zielverzeichnis sammeln
* Suche bereits vorhandene Backups
* Vergleiche das mit den aktuell vorhandenen Snapshots
* Sortieren in eine dest-map

## Snapshots in Source-Map durchlaufen
### Versuche jeden einzelnen dieser Snapshots an destdir zu senden
* Wenn er bereits vorhanden ist, überspringen
* btrfs send(compressed) dieses Snapshot mit Pipe 
* durch pv an
* btrfs receive in destdir

#### Führe dabei fortlaufend eine Fehlerprüfung durch
* Unterbrechen, wenn ein Fehler auftritt

#### Nebenher wird grafisch angezeigt was vorgeht

Genaueres zur Installation [hier](gallery/gallery.md)
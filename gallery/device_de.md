# Backup-medium vorbereiten
BackSnap stellt spezifische Anforderungen an das verwendete Backup-medium. Diese sind aber nicht schwer zu erfüllen. 

#### Das Backupmedium:
* sollte unbedingt ein externes Medium sein (z.B. USB-Festplatte)
* sollte unbedingt deutlich größer sein
* muß auf einem anderen Device liegen wie die Snapshots die gesichert werden sollen
* muß mit **btrfs formatiert** sein
* muß ein Subvolume **@BackSnap** enthalten
* muß für jedes zu sichernde Subvolume ein eignes Verzeichnis enthalten 

### 1. Device aussuchen
Es ist Empfehlenswert ein Device zu verwenden, das **deutlich** mehr Speicherplatz hat als alle zu sichernden Rechner zusammen. Das mag dann eine externe **Festplatte mit 5TB** sein, oder auch mehr.

### 2. Device partitionieren und formatieren
Es ist empfehlenswert das Device mit einer `GPT`-Partitionstabelle zu versehen. Die Partition die zum Backup verwendet werden soll, muß mit `btrfs` formatiert werden. Ich habe mit `gparted` gute Erfahrungen gemacht. Das Label der btrfs-Partition darf beliebig lauten. In meinem Beispiel ist es `Backup`

### 3. Subvolume @BackSnap anlegen
Bitte achte genau darauf wo **`BackSnap`** verwendet wird, und wo **`@Backsnap`** verwendet werden muß. Das sind keine Tippfehler.
#### UUID ermitteln
```
sudo btrfs filesystem show -d`

Label: none  uuid: 34a7ba3d-4cdc-a043-1cba-c420ebca2aca
	Total devices 2 FS bytes used 598.86GiB
	devid    1 size 900.00GiB used 621.03GiB path /dev/sda2
	devid    3 size 900.00GiB used 621.03GiB path /dev/nvme0n1p3

Label: 'Backup'  uuid: 03417033-4ae7-9451-3745-efafcbb9124e
	Total devices 1 FS bytes used 1.33TiB
	devid    1 size 2.93TiB used 1.40TiB path /dev/sdc3

Label: '4'  uuid: eb6a5c31-0cd4-43a5-90b4-077b6d98cd70
	Total devices 1 FS bytes used 144.00KiB
	devid    1 size 1000.00GiB used 2.02GiB path /dev/sdc6
```
Mein Device ist offensichtlich das mit `Label: 'Backup'`und die uuid: ist **`03417033-4ae7-9451-3745-efafcbb9124e`**

#### btrfs-root mounten
##### Wir mounten jetzt btrfs-root nach /mnt
```
sudo mount -t btrfs -o subvol=/,compress=zstd:9 --uuid 03417033-4ae7-9451-3745-efafcbb9124e  /mnt`
```

#### Subvolumes anlegen
##### Für den Fall dass auch andere Daten auf das Volume sollen, legen wir ein default btrfs-Subvolume `/@` an.
```
sudo btrfs subvolume create /mnt/@
sudo btrfs subvolume set-default /mnt/@
```
##### Für BackSnap legen wir ein Subvolume `/@BackSnap` an.
```
sudo btrfs subvolume create /mnt/@BackSnap
```

### 4. Verzeichnisse für die Sicherung der Volumes anlegen
Um auf unserem Backup eine gewisse Ordnung einzuhalten, ist es dringend angeraten für jeden PC und jedes Subvolume das gesichert wird einen eigenen Bereich anzulegen.

Ich verwendet für jeden Rechner einen Namen, und für jedes Subvolume den Namen des Mountpoints, und verbinde die zwei durch einen Punkt. Das kann jeder so machen wie er will. Aber später müssen diese Pfade beim Aufruf von `BackSnap` angegeben werden. Es lohnt sich also hier eine gewisse Ordnung zu halten.

Mit den folgenden Befehlen werden die Verzeichnisse für die verschiedenen PCs angelegt. (Man kann das aber auch mit `mc` tun). Meine Pcs sind *manjaro23, server, jitsim1, notebook, laptop* und *gast*
```
sudo su -
cd /mnt/@BackSnap
mkdir -v manjaro23 manjaro23.home 
mkdir -v server server.home server.srv 
mkdir -v jitsim1 jitsim1.home jitsim1.hugo jitsim1.hst
mkdir -v notebook notebook.home
mkdir -v laptop laptop.home
mkdir -v gast gast.home
```
Weitere Verzeichnisse können später jederzeit ergänzt werden.

### 5. Den Betrieb vorbereiten

##### Unmounten  (auf keinen Fall vergessen ! ) 
```
sudo umount /mnt
```
##### Den Mountpoint `/mnt/Backsnap` (ohne @ !) für den späteren Betrieb anlegen
```
sudo mkdir -v /mnt/Backsnap
```
###### optional Eintrag in der fstab
Eine Zeile in die /etc/fstab einfügen die einen mout erleichtert. Die fstab kann z.B. mit `nano` oder `mc` editiert werden.
Die Zeile könnte wie folgt aussehen. (wobei du natürlich die UUID deiner eigenen Partition verwenden mußt. ;-) (siehe oben). 
```
UUID=03417033-4ae7-9451-3745-efafcbb9124e /mnt/BackSnap	btrfs	noauto,rw,noatime,compress=zstd:9,subvol=/@BackSnap	0 0
```
* Jetzt mußt du dich auch für einen Kompressionslevel entscheiden. Alles zwischen 3 und 13 ist OK. **Ich habe mit `9` gute Erfahrungen gemacht.**.
* `noauto` verhindert dass die Platte beim Booten automatisch gemountet wird.

### 6. Abschließen und Kontrolle
##### Muß leer sein:
```
sudo ls -lA /mnt/BackSnap
```
##### Bringt keine Fehlermeldung:
```
sudo mount /mnt/BackSnap
```
##### Muß die angelegten Verzeichnisse zeigen:
```
sudo ls -lA /mnt/BackSnap
```
##### Bringt keine Fehlermeldung:
```
sudo umount /mnt/BackSnap
```
----
[Weitere Infos zu btrfs](https://wiki.manjaro.org/index.php/Btrfs) 
Sonntag, 30. April 2023 07:01 


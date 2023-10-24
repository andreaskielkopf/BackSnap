# Beispielscripte zum Backup mit backsnap
Ein Beispielscript für den lokalen Rechner
Das Script muss in einem **grafischen Terminal** mit **sudo** gestartet werden !

#### Alternative Konfiguration in /etc/backsnap.d
Die inzwischen bessere Alternative ist eine Konfigurationsdatei /etc/backsnap.d/local.conf. Muster dafür sind hier im Projekt abgelegt. Wenn das Programm mit `sudo backsnap -gi` gestartet wird, kann das menügeführt vorbereitet werden.

#### Anpassen nicht vergessen!
Falls sie doch die Scripte verwenden, müssen diese an die lokalen Gegebenheiten angepasst werden. Insbesondere folgende Punkte:

`BS_ID=03417033`
 Das muß ein erkennbarer Teil der UUID der Backup-Partition sein
`backup /     manjaro18`
 Der erste Parameter zeigt auf den mountpoint des Subvolumes dessen Snapshots gesichert werden sollen
 Der zweite Parameter benennt den Namen der Sicherung
### lokal

```
#!/bin/sh
# Alle Snapshots des lokalen Rechners auf ein angestecktes Laufwerk sichern

# Vorgaben für backsnap
BS_MOUNT='/mnt/BackSnap'
# Teil der UUID der  backup partition
BS_ID='0341703'
# activate ssh-askpass for gui usage with ssh
# export SSH_ASKPASS_REQUIRE="prefer"

# Partition suchen
BS_UUID=$( lsblk -no FSTYPE,UUID | grep "$BS_ID" | grep -Po '(?<=btrfs  ).{36}$' ) 
# BS_UUID="03417033-3745-4ae7-9451-efafcbb9124...."
if [ ${#BS_UUID} -ne 36 ]; then 
    echo "error: backup disk with UUID $BS_ID... is not connected" 
    exit
fi

# Partition mounten
WAS_MOUNTED=$(mount | grep -E " $BS_MOUNT ")
[ ${#WAS_MOUNTED} -le 10 ] && 
    echo "i need to mount $BS_UUID to $BS_MOUNT" && 
    mount -t btrfs -o subvol=@BackSnap,compress=zstd:9 /dev/disk/by-uuid/$BS_UUID $BS_MOUNT
mount | grep -E " $BS_MOUNT " ||
    { echo "error: could not mount $BS_UUID to $BS_MOUNT" && exit }

function backup {
    backsnap -g -v=1 -a=15 $1 $BS_MOUNT/$2 || exit
# alternative cli-version
# backsnap -v=3 $1 $BS_MOUNT/$2 || exit    
    echo  "OK"
}

# lokale Snapshots sichern
backup /     manjaro18
backup /home manjaro18.home
# ... hier erweitern

# Laufwerk unmounten:
sync
[ ${#WAS_MOUNTED} -le 10 ] && 
    echo "i need to umount $BS_MOUNT" && 
    umount $BS_MOUNT &&
    sync
mount | grep -E " $BS_MOUNT "
echo "All backups done"
```
### per ssh
Ein Beispielscript zum Backup mehrerer Rechner über ssh
Das Script muss in einem **grafischen Terminal** mit **sudo** gestartet werden !
ssh muß vorher passend eingerichtet worden sein !
durch SSH_ASKPASS_REQUIRE wird ein Dialog angezeigt der die Passphrase für den eingesetzten Schlüssel abfragt sobald dieser gebraucht wird.
```
#!/bin/sh
# Alle Snapshots des lokalen Rechners auf ein angestecktes Laufwerk sichern

# Vorgaben für backsnap
BS_MOUNT=/mnt/BackSnap
# Teil der UUID der  backup partition
BS_ID=03417033
# activate ssh-askpass for gui usage
export SSH_ASKPASS_REQUIRE="prefer"

# Partition suchen
BS_UUID=$(lsblk -o uuid | grep -E $BS_ID)
#BS_UUID=03417033-3745-4ae7-9451-efafcbb912..
[ ${#BS_UUID} -le 35 ] && 
    echo "error: backup disk with UUID $BS_ID... is not connected" && exit

# Partition mounten
WAS_MOUNTED=$(mount | grep -E " $BS_MOUNT ")
[ ${#WAS_MOUNTED} -le 10 ] && 
    echo "i need to mount $BS_UUID to $BS_MOUNT" && 
    mount -t btrfs -o subvol=@BackSnap,compress=zstd:9 /dev/disk/by-uuid/$BS_UUID $BS_MOUNT
mount | grep -E " $BS_MOUNT " ||
    { echo "error: could not mount $BS_UUID to $BS_MOUNT" && exit }

function backup {
    backsnap -g -v=1 -a=15 $1 $BS_MOUNT/$2 || exit
# alternative cli-version
# backsnap -v=3 $1 $BS_MOUNT/$2 || exit    
    echo  "OK"
}

# lokale Snapshots sichern
backup /     manjaro18
backup /home manjaro18.home

# server sichern
backup root@server:/ server
backup root@server:/home server.home
backup root@server:/srv server.srv

# jitsi sichern
backup root@jitsim1:/ jitsim1
backup root@jitsim1:/home jitsim1.home
backup root@jitsim1:/opt/hst jitsim1.hst
backup root@jitsim1:/opt/hugo jitsim1.hugo

# laptop sichern 
backup root@notebook: notebook
backup root@notebook:/home notebook.home

# gast sichern
backup root@gast:/ gast
backup root@gast:/home gast.home

# Laufwerk unmounten:
sync
[ ${#WAS_MOUNTED} -le 10 ] && 
    echo "i need to umount $BS_MOUNT" && 
    umount $BS_MOUNT &&
    sync
mount | grep -E " $BS_MOUNT "
echo "All backups done"
```

#!/usr/bin/bash

# Falls die Variabel leer ist, dann setze einen neuen Wert:
[ -z "$XDG_RUNTIME_DIR" ] && XDG_RUNTIME_DIR='/tmp'

# Füge hier die UUID deiner Backup Festplatte bzw. Partition ein.
# lsblk --fs
#BACKSNAP_UUID='2dc648d2-720f-48a5-9eff-e2da37294ff7'
BACKSNAP_UUID='85100417-a8ae-478a-8fba-40365ae3ced6'
# Das ist der Standard Einhängeunkt. Nur verändern, wenn man wirklich weiß, was man tut.
BACKSNAP_MOUNT="$XDG_RUNTIME_DIR/BackSnap-${BACKSNAP_UUID//-/}"
BACKSNAP_SUBVOL="@BackSnap"

RunBackSnap() {
# Backup Kommando
BackSnap() { 
RunAsRoot mkdir -p "$BACKSNAP_MOUNT/$2"
RunAsRoot "$PWD"/backsnap -d -v=8 "$1" "$BACKSNAP_MOUNT/$2"
}
# Hier wird das Backup durchgeführt:
BackSnap '/' 'manjaro'
BackSnap '/home' 'manjaro.home'
# Mehr hier hinzufügen
	
}

##########################################
### AB HIER KEIN KONFIGURATIONSBEREICH ###
##########################################

# Auf Root wechseln
RunAsRoot() { sudo --login --user root "$@";}

# Farbige Nachrichten
PrintInfo() { printf '\e[32m[INFO]\e[0m \e[33m%s\e[0m\n' "$1"; }
PrintError() { printf '\e[31m[ERROR]\e[0m \e[33m%s\e[0m\n' "$1"; }
PrintQuestion() { printf '\e[35m[QUESTION]\e[0m \e[33m%s\e[0m\n' "$1"; }

# Prüfe ob die Partition mit der angegebene UUID verfügbar ist
check_uuid_is_connected() {
while read -r line; do
	FSTYPE="$(cut -d' ' -f1 <<<"$line")"
	[[ "$FSTYPE" != "btrfs" ]] && continue
	UUID="$(cut -d' ' -f2 <<<"$line")"
	[[ "$1" != "$UUID" ]] && continue
	[[ "$1" == "$UUID" ]] && return 0
done < <(lsblk -nro FSTYPE,UUID,MOUNTPOINTS)
}

# Prüfe ob die Partition mit der angegebene UUID eingebunden ist
check_disk_is_mounted() {
status=1
while read -r line; do
	FSTYPE="$(cut -d' ' -f1 <<<"$line")"
	[[ "$FSTYPE" != "btrfs" ]] && continue
	UUID="$(cut -d' ' -f2 <<<"$line")"
	[[ "$UUID" != "$BACKSNAP_UUID" ]] && continue
	MNTPNTS="$(cut -d' ' -f3 <<<"$line")"
	for mnt in ${MNTPNTS//\\x0a/ }; do
		[[ "$mnt" != "$BACKSNAP_MOUNT" ]] && continue
		[[ "$mnt" == "$BACKSNAP_MOUNT" ]] && status=0 && break
	done
done <  <(lsblk -nro FSTYPE,UUID,MOUNTPOINTS)
return $status
}

# Prüfe ob die Partition mit der angegebene UUID eingebunden das Subvolume @BackSnap hat
check_subvol_exists() {
while read -r line; do
	SUBVOL="$(cut -d' ' -f9 <<<"$line")"
	[[ "$BACKSNAP_SUBVOL" != "$SUBVOL" ]] && continue
	[[ "$BACKSNAP_SUBVOL" == "$SUBVOL" ]] && return 0
done < <(RunAsRoot btrfs subvolume list "$BACKSNAP_MOUNT")
return 1
}

# Synchronisiere noch nicht geschrieben Daten  
btrfs_sync() { PrintInfo "Syncing $BACKSNAP_MOUNT" ; btrfs filesystem sync "$BACKSNAP_MOUNT"; }

# Prüfe ob die Partition mit der angegebene UUID eingebunden mit Subvolume @BackSnap eingebunden wurde
check_subvol_mount() {
status=1
while read -r line; do
	MNTPNT="$(cut -d' ' -f2 <<<"$line")"
	[[ "$MNTPNT" != "$BACKSNAP_MOUNT" ]] && continue
	OPTIONS="$(cut -d' ' -f4 <<<"$line")"
	for opt in ${OPTIONS//,/ }; do
		[[ 'subvol=/@BackSnap' != "$opt" ]] && continue
		[[ 'subvol=/@BackSnap' == "$opt" ]] && status=0 && break
	done
done </proc/mounts
return $status
}

# Hänge die Partition mit der angegebene UUID ein. Jeweils mit ROOTFS oder Subvolume.
mount_disk() {
mkdir -p "$BACKSNAP_MOUNT"
case $1 in
	rootfs)
		PrintInfo "Mounting $BACKSNAP_UUID on $BACKSNAP_MOUNT as ROOTFS" 
		RunAsRoot mount -t btrfs -o compress=zstd:9 -U "$BACKSNAP_UUID" "$BACKSNAP_MOUNT"
	;;
	subvol) 
		PrintInfo "Mounting $BACKSNAP_UUID on $BACKSNAP_MOUNT as Subvolume"
		RunAsRoot mount -t btrfs -o subvol=@BackSnap,compress=zstd:9 -U "$BACKSNAP_UUID" "$BACKSNAP_MOUNT"
	;;
esac
}

# Hänge Partition aus
unmount_disk() { PrintInfo "Unmounting $BACKSNAP_MOUNT" && btrfs_sync && RunAsRoot umount "$BACKSNAP_MOUNT" && sync; }

# Erstelle das Subvolume
create_subvol() { 
if unmount_disk; then
	PrintInfo "Unmount of $BACKSNAP_MOUNT was successfull"
else
	PrintError "Unmount of $BACKSNAP_MOUNT was not successfull"
	exit
fi
if mount_disk rootfs; then
	PrintInfo "Mounting of $BACKSNAP_MOUNT on ROOTFS was succssfull."
else
	PrintError "Mounting of $BACKSNAP_MOUNT on ROOTFS was not succssfull."
	exit
fi
CreateSubvolume() { 
PrintInfo "Creating subvolume $BACKSNAP_MOUNT/$BACKSNAP_SUBVOL"
RunAsRoot btrfs --quiet subvolume create "$BACKSNAP_MOUNT/$BACKSNAP_SUBVOL"
}
if CreateSubvolume; then
	PrintInfo "Creating of the subvolume $BACKSNAP_MOUNT/$BACKSNAP_SUBVOL was succssfull."
else
	PrintError "Creating of the subvolume $BACKSNAP_MOUNT/$BACKSNAP_SUBVOL was not succssfull."
	exit
fi
if btrfs_sync; then
	PrintInfo "Syncing $BACKSNAP_MOUNT was succssfull."
else
	PrintError "Syncing $BACKSNAP_MOUNT was not succssfull."
	exit
fi
if unmount_disk; then
	PrintInfo "Unmount of $BACKSNAP_MOUNT was successfull"
else
	PrintError "Unmount of $BACKSNAP_MOUNT was not successfull"
	exit
fi
}


# Prüfe, ob die angegebene UUID im System bekannt ist, andernsfalls Abbruch
if check_uuid_is_connected "$BACKSNAP_UUID"; then
	PrintInfo "Backup Disk with UUID $UUID is connected"
else
	PrintError "Backup Disk with UUID $UUID is not connected"
	exit
fi

# Prüfe, ob die Partition mit der angegebenen UUID eingehängt ist
if check_disk_is_mounted; then
	PrintInfo "Backup Disk is mounted on $BACKSNAP_MOUNT"
else
	PrintInfo "Backup Disk is not mounted on $BACKSNAP_MOUNT"
	# Hänge die Partition mit der angegebenen UUID als ROOTFS ein.
	if mount_disk rootfs; then
		PrintInfo "Mounting $BACKSNAP_UUID on $BACKSNAP_MOUNT as ROOTFS was successfull."
	else
		PrintError "Mounting $BACKSNAP_UUID on $BACKSNAP_MOUNT as ROOTFS was not successfull."
		exit
	fi	
fi

# Prüfe, ob das Subvolume bereits erstellt wurde und falls nicht, dann erstelle es.
if check_subvol_exists; then
	PrintInfo "Subvolume $BACKSNAP_SUBVOL exists"
	unmount_disk
else
	PrintInfo "Subvolume $BACKSNAP_SUBVOL doesn't exist"
	create_subvol
fi

# Hänge das Subvolume ein
if mount_disk subvol; then
	PrintInfo "Mounting $BACKSNAP_UUID on $BACKSNAP_MOUNT as Subvolume was successfull."
else
	PrintError "Mounting $BACKSNAP_UUID on $BACKSNAP_MOUNT as Subvolume was not successfull."
	exit
fi 

# Hier läuft das Backup
PrintQuestion "Does RunBackSnap run?"
RunBackSnap

# Aushängen der Partition 
unmount_disk 

PrintInfo "All backups done."



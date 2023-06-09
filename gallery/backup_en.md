# Example scripts for backup with backsnap
#### An example script for the local computer
The script must be started in a **graphical terminal** with **sudo** !
#### Don't forget to adapt!
The scripts must be adapted to the local conditions. In particular the following points:

`BS_ID=03417033`

  This must be a recognizable part of the UUID of the backup partition
  
`backup / manjaro18`

* The first parameter points to the mountpoint of the subvolume whose snapshots are to be backed up
* The second parameter names the name of the backup
### local

```
#!/bin/sh
# Back up all snapshots of the local machine to an attached drive

# Defaults for backsnap
BS_MOUNT=/mnt/BackSnap
# Part of the UUID of the backup partition
BS_ID=03417033
# activate ssh-askpass for gui usage
# export SSH_ASKPASS_REQUIRE="prefer"

# Find partition
BS_UUID=$(lsblk -o uuid | grep -E $BS_ID)
#BS_UUID=03417033-3745-4ae7-9451-efafcbb912..
[ ${#BS_UUID} -le 35 ] && 
    echo "error: backup disk with UUID $BS_ID... is not connected" && exit

# Mount partition
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

# back up local snapshots
backup /     manjaro18
backup /home manjaro18.home
# ... hier erweitern

# Unmount drive:
sync
[ ${#WAS_MOUNTED} -le 10 ] && 
    echo "i need to umount $BS_MOUNT" && 
    umount $BS_MOUNT &&
    sync
mount | grep -E " $BS_MOUNT "
echo "All backups done"
```
### ssh
#### An example script for backing up multiple machines
The script must be started in a **graphical terminal** with **sudo** !

* ssh must have been set up appropriately beforehand!
* SSH_ASKPASS_REQUIRE displays a dialog that requests the passphrase for the key as soon as it is needed.
```
#!/bin/sh
# Back up all snapshots to an attached drive

# Defaults for backsnap
BS_MOUNT=/mnt/BackSnap
# Part of the UUID of the backup partition
BS_ID=03417033
# activate ssh-askpass for gui usage
export SSH_ASKPASS_REQUIRE="prefer"

# Find partition
BS_UUID=$(lsblk -o uuid | grep -E $BS_ID)
#BS_UUID=03417033-3745-4ae7-9451-efafcbb912..
[ ${#BS_UUID} -le 35 ] && 
    echo "error: backup disk with UUID $BS_ID... is not connected" && exit

# Mount partition
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

# back up local snapshots
backup /     manjaro18
backup /home manjaro18.home

# backup server 
backup root@server:/ server
backup root@server:/home server.home
backup root@server:/srv server.srv

# backup jitsi 
backup root@jitsim1:/ jitsim1
backup root@jitsim1:/home jitsim1.home
backup root@jitsim1:/opt/hst jitsim1.hst
backup root@jitsim1:/opt/hugo jitsim1.hugo

# backuplaptop 
backup root@notebook: notebook
backup root@notebook:/home notebook.home

# backup gast
backup root@gast:/ gast
backup root@gast:/home gast.home

# Unmount drive:
sync
[ ${#WAS_MOUNTED} -le 10 ] && 
    echo "i need to umount $BS_MOUNT" && 
    umount $BS_MOUNT &&
    sync
mount | grep -E " $BS_MOUNT "
echo "All backups done"
```

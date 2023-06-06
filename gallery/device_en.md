# Prepare backup medium
BackSnap has specific requirements for the backup medium used. But these are not difficult to achieve.

#### The backup medium:
* should definitely be an external medium (e.g. USB hard drive)
* should definitely be significantly larger than the medium in your pc
* must be on a different device than the snapshots to be backed up
* must be **btrfs formatted**
* must contain a subvolume **@BackSnap**
* must contain a separate directory for each subvolume to be backed up

### 1. Select device
It is recommended to use a device that has **significantly** more storage space than all the computers to be backed up combined. That may then be an external **hard drive with 5TB**, or even more.

### 2. Partition and format device
It is recommended to provide the device with a `GPT` partition table. The partition to be used for backup must be formatted with `btrfs`. I have had good experiences with `gparted`. The label of the btrfs partition can be anything. In my example it is `Backup`

### 3. Create subvolume @BackSnap
Please pay close attention to where **`BackSnap`** is used and where **`@Backsnap`** must be used. These are not typos.
#### Get UUID
```
sudo btrfs filesystem show -d`

Label: none uuid: 34a7ba3d-4cdc-a043-1cba-c420ebca2aca
Total devices 2 FS bytes used 598.86GiB
devid 1 size 900.00GiB used 621.03GiB path /dev/sda2
devid 3 size 900.00GiB used 621.03GiB path /dev/nvme0n1p3

Label: 'Backup' uuid: 03417033-4ae7-9451-3745-efafcbb9124e
Total devices 1 FS bytes used 1.33TiB
devid 1 size 2.93TiB used 1.40TiB path /dev/sdc3

Label: '4' uuid: eb6a5c31-0cd4-43a5-90b4-077b6d98cd70
Total devices 1 FS bytes used 144.00KiB
devid 1 size 1000.00GiB used 2.02GiB path /dev/sdc6
```
My device is obviously the one with `Label: 'Backup'` and the uuid: is **`03417033-4ae7-9451-3745-efafcbb9124e`**

#### Mount btrfs root
##### We are now mounting btrfs-root to /mnt
```
sudo mount -t btrfs -o subvol=/,compress=zstd:9 --uuid 03417033-4ae7-9451-3745-efafcbb9124e /mnt`
```

#### Create subvolumes
##### In case other data should also get onto the volume, we create a default btrfs subvolume `/@`.
```
sudo btrfs subvolume create /mnt/@
sudo btrfs subvolume set-default /mnt/@
```
##### For BackSnap we create a subvolume `/@BackSnap`.
```
sudo btrfs subvolume create /mnt/@BackSnap
```

### 4. Create directories for backing up the volumes
In order to maintain a certain order on our backup, it is strongly recommended to create a separate area for each PC and each subvolume that is backed up.

I use a name for each host and a mount point name for each subvolume, and connect the two with a period. Everyone can do that as they want. But later these paths must be specified when calling `BackSnap`. It is therefore worthwhile to keep a certain order here.

The directories for the various PCs are created with the following commands. (You can also do this with `mc`). My PCs are *manjaro23, server, jitsim1, notebook, laptop* and *guest*
```
sudo su -
cd /mnt/@BackSnap
mkdir -v manjaro23 manjaro23.home
mkdir -v server server.home server.srv
mkdir -v jitsim1 jitsim1.home jitsim1.hugo jitsim1.hst
mkdir -v notebook notebook.home
mkdir -v laptop laptop.home
mkdir -v guest guest.home
```
Further directories can be added later at any time.

### 5. Prepare for operation

##### Unmount (do not forget ! )
```
sudo umount /mnt
```
##### Create the mount point `/mnt/Backsnap` (without @ !) for later operation
```
sudo mkdir -v /mnt/Backsnap
```
###### optional entry in fstab
Put a line in /etc/fstab that facilitates a mout. The fstab can be edited with e.g. `nano` or `mc`. The line could look like this. Of course, you must use the UUID of your own partition. ;-) (see above).
```
UUID=03417033-4ae7-9451-3745-efafcbb9124e /mnt/BackSnap btrfs noauto,rw,noatime,compress=zstd:9,subvol=/@BackSnap 0 0
```
* Now you also have to decide on a compression level. Anything between 3 and 13 is OK. **I have had good experiences with `9`.**.
* `noauto` prevents the disk from being automatically mounted at boot time.

### 6. Closing and control
##### Must be empty:
```
sudo ls -lA /mnt/BackSnap
```
##### Brings no error message:
with fstab entry:
```
sudo mount /mnt/BackSnap
```
##### Must show the created directories:
```
sudo ls -lA /mnt/BackSnap
```
##### Brings no error message:
```
sudo umount /mnt/BackSnap
```
----
[More info on btrfs](https://wiki.manjaro.org/index.php/Btrfs)
Sunday, April 30, 2023 07:01
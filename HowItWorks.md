# How it works:
[ReadMe](./readme.md), [deutsch](./WieEsFunktioniert.md)

`java -jar /usr/local/bin/backsnap.jar /.snapshots /mnt/BACKUP/manjaro21`

1. starts java
2. Use a JAR file
3. Path to backsnap.jar
4. Path to source snapshots (sourcedir)
5. Path to store backups of snapshots (destdir)

# What it does

## Collect information about sourcedir
* List all snapshots in the source directory 
* Save in a sorted source-map

## Gather information about the target directory
* List all snaüshots in backup
* Filter for real backups (received-uuid exists)
* Save in a sorted dest-map

## Loop through snapshots in source map
### Try sending this one snapshot to destdir
* If it already exists, skip it
* btrfs send this snapshot with pipe
* btrfs receive in destdir

#### Do an error check while doing this
* Break when an error occurs

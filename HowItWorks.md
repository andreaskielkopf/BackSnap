# How it works:

`java -jar /usr/local/bin/backsnap.jar /.snapshots /mnt/BACKUP/manjaro21`

1. starts java
2. Use a JAR file
3. Path to backsnap.jar
4. Path to source snapshots (sourcedir)
5. Path to store backups of snapshots (destdir)

# What it does

## Collect information about sourcedir
* List all files in the source directory (/bin/ls sourcedir)
* Filter for numeric filenames
* Save in a numerically sorted source-map

## Gather information about the target directory
* List all files in backup (/bin/ls target directory)
* Filter for numeric filenames
* Save in a numerically sorted dest-map

## Loop through snapshots in source map
### Try sending this one snapshot to destdir
* If it already exists, skip it
* btrfs send this snapshot with pipe
* btrfs receive in destdir

#### Do an error check while doing this
* Break when an error occurs

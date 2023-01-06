# Examples of commandline usage
## root
### be root
login as root, then

```
java -jar /usr/local/bin/backsnap.jar /.snapshots /mnt/BACKUP/@snapshots/manjaro18
...
```
### become root
```
sudo su -
[sudo] Passwort für andreas: 
root@manjaro18 ...
java -jar /usr/local/bin/backsnap.jar /.snapshots /mnt/BACKUP/@snapshots/manjaro18         
...
```
### execute as root (recommended)
```
sudo java -jar /usr/local/bin/backsnap.jar /.snapshots /mnt/BACKUP/@snapshots/manjaro18
[sudo] Passwort für andreas:
...
```
### with sudo per every single command [not recommended]
You will have to give your Password for the first command. Then sudo may chache it. When sudo times out, you 
will be prompted again

```
java -jar /usr/local/bin/backsnap.jar sudo:/.snapshots sudo:/mnt/BACKUP/@snapshots/manjaro18
```

## ssh [for experts only]
For ssh to work with backsnap you need to work with `ssh-keys`. You best test your ssh-keys without using backsnap before you try to use backsnap with ssh.

You also need to setup an `ssh-agent` to hold the keys while the programm performs backups. Otherwise you would need to unlock your key for every single transfer.

Be aware that a backup via ssh may take some time. If the key expires while a backup is in progress, the backup will 
finish the backup of the actual snapshot. But it will not be able to progress to the next one. You will need to 
unlock the key again, to progress to the next snapshot.

### localhost [recommended]
This is an alternative to use sudo. You log into your root account using ssh with `root@localhost` 
```
java -jar /usr/local/bin/backsnap.jar root@localhost:/.snapshots root@localhost:/mnt/BACKUP/@snapshots/manjaro18 
ssh root@localhost 'btrfs subvolume list -spcguqR /'
Host key fingerprint is SHA257:Nrfr0...
+--[ED25519 257]--+
|          ^^     |
|         o  O:   |
|        o =  ++  |
|.O . . o = .     |
|o+=.@ . S.o .    |
|%=.o.  ..o!. :   |
|@=.o     =.+.    |
| E=.      Bo     |
| ++.     +Bo.    |
+----[SHA256]-----+
Enter passphrase for key '/home/andreas/.ssh/...': 
...
```

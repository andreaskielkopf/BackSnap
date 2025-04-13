# Installation
BackSnap is easy to install (and uninstall).

## 1. Install `JAVA 21`
This varies greatly depending on the distribution used. Please refer to your **distribution guide** on how to 
install java 21 so you do it right from the start. I recommend using **`Java OpenJDK 21`**.

#### Manjaro or Arch:
```
pamac install jdk-openjdk
```
 or 
```
pacman -S jdk-openjdk
```
 or 
```
trizen -S jdk-openjdk
```
##### Test as `user` and as `root`:
```
java -version
```
```
openjdk version "21" 2023-09-19
OpenJDK Runtime Environment (build 21+35)
OpenJDK 64-Bit Server VM (build 21+35, mixed mode, sharing)
```

## 2. Install `pv`
**`pv`** shows the progress and speed of the snapshot transfer. It is not required but recommended.

#### Manjaro or Arch:
```
pamac install pv
```
```
pacman -S pv
```
```
trizen -S pv
```

##### Test as `user` and as `root`:
```
pv -V
```
```
pv 1.8.0
Copyright 2023 Andrew Wood
License: GPLv3+ <https://www.gnu.org/licenses/gpl-3.0.html>
This is free software: you are free to change and redistribute it.
There is NO WARRANTY, to the extent permitted by law.
Project web site: <https://www.ivarch.com/programs/pv.shtml>
```

## 3. Install `backsnap`
The installation must be done as root (or with sudo). It should be done in such a way that `backsnap` is accessible in 
both **root**'s `$PATH` and **user**'s `$PATH`.
```
echo $PATH
```

#### in `/usr/local/bin`
If `/usr/local/bin` is in your path, it's easiest to install backsnap there.
```
sudo wget https://github.com/andreaskielkopf/BackSnap/raw/master/backsnap -O /usr/local/bin/backsnap
```
Make backsnap executable: 
```
sudo chmod a+x /usr/local/bin/backsnap
```

#### or in `/usr/bin`
If `/usr/local/bin` is **not** in your path, the easiest way is to install backsnap in `/usr/bin`.
```
sudo wget https://github.com/andreaskielkopf/BackSnap/raw/master/backsnap -O /usr/bin/backsnap
```
Make backsnap executable: 
```
sudo chmod a+x /usr/bin/backsnap
```

### Test as `user` and as `root`:
```
backsnap -x
```
```
BackSnap Version 0.6.8.27 (2024/06/22)
args >  -x 
java [version=23-ea, major=23, minor=null, patch=null]
using ThreadPerTaskExecutor
```
----
## Uninstall
You can uninstall Java 21 **as usual for your distribution**.
Only do this if you no longer need Java.
#### Manjaro or Arch:
```
pacman -R jdk-openjdk
```
#### You can remove pv with:
```
pacman -R pv
```
#### You can remove backsnap by simply deleting it
```
sudo rm /usr/local/bin/backsnap
```
or
```
sudo rm /usr/bin/backsnap
```
----
### [next step ->](device_en.md)

----

See also: [archlinux wiki java](https://wiki.archlinux.org/title/java) 
, [manjaro pamac](https://wiki.manjaro.org/index.php/Pamac) 
, [manjaro pacman](https://wiki.manjaro.org/index.php/Pacman_Overview) 
, [manjaro trizen](https://wiki.archlinux.de/title/Trizen)

Sontag, 23. Juni 2024 14:46


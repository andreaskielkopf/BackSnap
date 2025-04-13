# Installation
BackSnap ist bewusst einfach zu installieren (und zu deinstallieren).

## 1. Installiere „JAVA 21“.
Je nach verwendeter Distribution ist das ganz unterschiedlich. Bitte schau in der **Anleitung zu deiner Distribution** 
nach wie du java 21 installieren kannst, damit du das von Anfang an richtig machst. Ich empfehle die Verwendung 
von **`Java OpenJDK 21`**.

#### Manjaro oder Arch:
```
pamac install jdk-openjdk
```
oder
```
pacman -S jdk-openjdk
``` 
oder 
```
trizen -S jdk-openjdk
```

##### Test als `user` und als `root`:
```
java -version
```
```
openjdk version "21" 2023-09-19
OpenJDK Runtime Environment (build 21+35)
OpenJDK 64-Bit Server VM (build 21+35, mixed mode, sharing)
```

## 2. Installiere `pv`
**`pv`** zeigt den Fortschritt und die Geschwindigkeit der Snapshot-Übertragung an. Es ist nicht erforderlich, 
aber dringend empfohlen.

#### Manjaro oder Arch:
```
pamac install pv
```
```
pacman -S pv
```
```
trizen -S pv
```

##### Test als `user` und als `root`:
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

## 3. Installiere `backsnap`
Die Installation muß als root erfolgen (oder mit sudo). Sie sollte so erfolgen, dass `backsnap` sowohl im `$PATH` 
von **root** als auch im `$PATH` deines **users** erreichbar ist.
```
echo $PATH
```

### in `/usr/local/bin` (empfohlen)
Wenn `/usr/local/bin` in deinem Pfad enthalten ist, ist es das einfachste `backsnap` dort zu installieren.
```
sudo wget https://github.com/andreaskielkopf/BackSnap/raw/master/backsnap -O /usr/local/bin/backsnap
```
Backsnap ausführbar machen: 
```
sudo chmod a+x /usr/local/bin/backsnap
```

### oder in `/usr/bin`
Wenn `/usr/local/bin` nicht in deinem Pfad enthalten ist, ist es das einfachste BackSnap in `/usr/bin` zu installieren.
```
sudo wget https://github.com/andreaskielkopf/BackSnap/raw/master/backsnap -O /usr/bin/backsnap
```
Backsnap ausführbar machen:
```
sudo chmod a+x /usr/bin/backsnap
```

#### Test als `user` und als `root`:
```
backsnap -x
```
```
BackSnap Version 0.6.8.27 (2024/06/22)
args >  -x 
java [version=23-ea, major=23, minor=null, patch=null]
using ThreadPerTaskExecutor
```

### [weiter ->](device_de.md)
----
## Deinstallieren
Du kannst java 21 so deinstallieren, wie es **bei deiner Distribution üblich** ist. 
Mach das nur wenn du Java auch sonst nicht mehr brauchst. 
#### Manjaro oder Arch:
```
pacman -R jdk-openjdk
``` 
#### Du kannst pv entfernen mit:
```
pacman -R pv
```
#### Du kannst backsnap entfernen indem du es einfach löschst
```
sudo rm /usr/local/bin/backsnap
```
oder
```
sudo rm /usr/bin/backsnap
```
----

Siehe auch [archlinux wiki java](https://wiki.archlinux.org/title/java) 
, [manjaro pamac](https://wiki.manjaro.org/index.php/Pamac) 
, [manjaro pacman](https://wiki.manjaro.org/index.php/Pacman_Overview) 
, [manjaro trizen](https://wiki.archlinux.de/title/Trizen)

Sontag, 23. Juni 2024 14:46 
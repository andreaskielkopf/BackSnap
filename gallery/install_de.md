# Installation
Es ist bewusst einfach gehalten BackSnap zu installieren.
## 1. `JAVA` installieren
Je nach verwendeter Distribution ist das ganz unterschiedlich. Bitte schau in der **Anleitung zu deiner Distribution** nach wie du java installieren kannst, damit du das von Anfang an richtig machst. Ich empfehle `java Openjdk 17 oder 20` zu verwenden, aber alle Versionen ab 17 sollten gut funktionieren.
#### Für manjaro oder arch:
`pamac install jdk-openjdk`

oder 
`pacman -S jdk-openjdk`

oder 
`trizen -S jdk-openjdk`
#### Test als `user` und als `root`:
`java -version`
```
openjdk version "19.0.2" 2023-01-17
OpenJDK Runtime Environment (build 19.0.2+7)
OpenJDK 64-Bit Server VM (build 19.0.2+7, mixed mode)
```
Siehe auch: [archlinux wiki java](https://wiki.archlinux.org/title/java) , [manjaro pamac](https://wiki.manjaro.org/index.php/Pamac) , [manjaro pacman](https://wiki.manjaro.org/index.php/Pacman_Overview) , [manjaro trizen](https://wiki.archlinux.de/title/Trizen) 

## 2. `pv` installieren
`pv` zeigt während der Übertragung des Snapshots den Fortschritt und die Geschwindigkeit an. Es ist nicht erforderlich, aber empfohlen.
#### für ARCH oder manjaro:
`pamac install pv`

oder 
`pacman -S pv`

oder 
`trizen -S pv`

## 3. `BackSnap` installieren
Die Installation muß als root erfolgen (oder mit sudo). Sie sollte so erfolgen, dass BackSnap sowohl im `$PATH` von **root** als auch im `$PATH` des **users** erreichbar ist.

`echo $PATH`
#### in /usr/local/bin
Wenn `/usr/local/bin` in deinem Pfad enthalten ist, ist es das einfachste BackSnap dort zu installieren.

`sudo wget https://github.com/andreaskielkopf/BackSnap/raw/master/backsnap -O /usr/local/bin/backsnap`

BackSnap ausführbar machen

`sudo chmod a+x /usr/local/bin/backsnap`
#### oder in /usr/bin
Wenn `/usr/local/bin` nicht in deinem Pfad enthalten ist, ist es das einfachste BackSnap in `/usr/bin` zu installieren.

`sudo wget https://github.com/andreaskielkopf/BackSnap/raw/master/backsnap -O /usr/bin/backsnap`

BackSnap ausführbar machen

`sudo chmod a+x /usr/bin/backsnap`
### Test als `user` und als `root`:
`backsnap -x`
```
args >  -x
<html> BackSnap <br> Version 0.5.1 <br> (2023/04/22)
```
----
Samstag, 03. Juni 2023 06:22 


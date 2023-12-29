#!/bin/env -S bash
# ©2023 Andreas Kielkopf
# License: `GNU General Public License v3.0`
export SSH_ASKPASS_REQUIRE=prefer   
sudo java -jar "$0" "$@"
EC="$?"
[ "$EC" = 0 ] && exit;
echo -n "$EC ==>"
[ "$EC" = 127 ] && echo "$0 needs a java runtime"
[ "$EC" = 1 ] && echo "$0 needs a java runtime with version 21"
{ cat; exit; } <<EOF

BackSnap is made for making backups of btrfs snapshots on linux

Usage:
------
$0 [OPTIONS]

 -h --help           show usage
 -x --version        show date and version
 -d --dryrun         do not do anything ;-)
 -v --verbose        be more verbose (-v=9)
 -s --singlesnapshot backup exactly one snapshot
 -g --gui            enable gui (works only with sudo)
 -a --auto           auto-close gui when ready
 -c --compressed     use protokoll version2 for send/receive (if possible)
 -i --init           init /etc/backsnap.d/local.conf (only with -g)
 -o --deleteold      mark old backups for deletion in gui (-o=500)
 -m --keepminimum    mark all but minimum backups for deletion in gui (-m=250)  
 
 -o,-m,        need  manual confirmation in the gui to delete marked snapshots
 -i            needs gui to confirm uuid of backup-medium
  
 For sources see@ https://github.com/andreaskielkopf/BackSnap and inside this file
 For help go to   https://forum.manjaro.org/t/howto-hilfsprogramm-fur-backup-btrfs-snapshots-mit-send-recieve
EOF

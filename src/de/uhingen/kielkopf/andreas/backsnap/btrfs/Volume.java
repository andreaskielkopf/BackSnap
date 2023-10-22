/**
 * 
 */
package de.uhingen.kielkopf.andreas.backsnap.btrfs;

import static de.uhingen.kielkopf.andreas.beans.RecordParser.getPathMap;
import static de.uhingen.kielkopf.andreas.beans.RecordParser.getString;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import de.uhingen.kielkopf.andreas.backsnap.config.Log;
import de.uhingen.kielkopf.andreas.backsnap.config.Log.LEVEL;
import de.uhingen.kielkopf.andreas.beans.shell.CmdStreams;

/**
 * @author Andreas Kielkopf
 *
 */
public record Volume(Pc pc, ArrayList<String> lines, ConcurrentSkipListMap<String, Path> devices, String label,
         String uuid) {
   static final Pattern VOLUMELABEL=Pattern.compile("^Label: ('.+'|none)");
   static final Pattern UUID=Pattern.compile("uuid: ([-0-9a-f]{36})");
   static final Pattern DEVICE=Pattern.compile("devid .+ path (/dev/.+)");
   static final Pattern SIZE_USED=Pattern.compile("size (?<size>[0-9.MTGiB]+) used (?<used>[0-9.MTGiB]+) ");
   static final Pattern DEVICE_MAP=Pattern.compile(".*devid +(?<key>.+) size.+path (?<value>/dev/.+)");
   static private final String UDEVADM="udevadm info ";
   /**
    * @param pc2
    * @param tmpList
    * @return
    */
   public static Volume getVolume(Pc pc2, ArrayList<String> lineList) {
      return new Volume(pc2, lineList);
   }
   private Volume(Pc pc2, ArrayList<String> lines) {
      this(pc2, new ArrayList<>(lines), getPathMap(DEVICE_MAP, lines), getString(VOLUMELABEL, lines),
               getString(UUID, lines));
   }
   public Mount mount() {
      try {
         for (Path mine:devices().values())
            if (pc.getMountList(false).values().stream().filter(m -> m.devicePath().equals(mine)).findFirst()
                     .get() instanceof Mount mount)
               return mount;
      } catch (IOException | java.util.NoSuchElementException ignore) { /* */ }
      return null;
   }
   public String listName() {
      StringBuilder sb=new StringBuilder().append(devices().sequencedValues().getFirst());
      sb.append(" ".repeat(Math.max(15 - sb.length(), 1))).append(Usage.IB.getText(getFree(false))).append(" free => ")
               .append((long) getFree(true)).append("%");//
      return sb.insert(0, pc.extern() + ":").toString();
   }
   public Color listColor() {
      return ((getFree(false) < Usage.IB.GiB.f * 20) || (getFree(true) < 10d)) ? Color.RED : // 20GB ,10%
               ((getFree(false) < Usage.IB.GiB.f * 50) || (getFree(true) < 20d)) ? Color.ORANGE : // 50GB ,25%
                        isUSB() ? Color.GREEN : Color.YELLOW;
   }
   @Override
   public String toString() {
      return new StringBuilder("Volume [")//
               .append("uuid=").append(uuid()).append(" ")//
               .append(pc.extern()).append(":").append(devices())//
               .append(" ").append(label()).append("]").toString();
   }
   public boolean isUSB() {
      String isUSBCmd=pc.getCmd(new StringBuilder(UDEVADM).append(devices().sequencedValues().getFirst()), false);
      Log.logln(isUSBCmd, LEVEL.BTRFS);
      boolean treffer=false;
      try (CmdStreams isUSBStream=CmdStreams.getCachedStream(isUSBCmd)) {
         treffer=isUSBStream.outBGerr().anyMatch(line -> line.contains("ID_BUS=usb"));
      } catch (IOException e1) {
         e1.printStackTrace();
      }
      return treffer;
   }
   private double getFree(boolean inPercent) {
      double size=1;
      double used=1;
      for (String line:lines) {
         Matcher m=SIZE_USED.matcher(line);
         if (m.find()) {
            // System.out.println(line);
            String s=m.group("size");
            String u=m.group("used");
            size+=Usage.getZahl(s);
            used+=Usage.getZahl(u);
         }
      }
      double free=size - used;
      return inPercent ? 100d * free / size : free;
   }
}

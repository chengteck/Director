 package com.ingres.util;
 
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public class NumericUtil
 {
   private static final Pattern valueMatch = Pattern.compile("^([0-9]+)([kKmMgG])$");
   private static final long MULT_KB = 1024L;
   private static final long MULT_MB = 1048576L;
   private static final long MULT_GB = 1073741824L;
   private static final long MULT_TB = 1099511627776L;
   private static final long MULT_PB = 1125899906842624L;
   
   public static enum Range
   {
     BYTES(1L, "", "bytes", "Bytes"),  KILOBYTES(1024L, "K", "Kbytes", "Kilobytes"),  MEGABYTES(1048576L, "M", "Mbytes", "Megabytes"),  GIGABYTES(1073741824L, "G", "Gbytes", "Gigabytes"),  TERABYTES(1099511627776L, "T", "Tbytes", "Terabytes"),  PETABYTES(1125899906842624L, "P", "Pbytes", "Petabytes");
     
     private long multiplier;
     private String prefix;
     private String shortName;
     private String longName;
     
     private Range(long multiplier, String prefix, String shortName, String longName)
     {
       this.multiplier = multiplier;
       this.prefix = prefix;
       this.shortName = shortName;
       this.longName = longName;
     }
     
     public long getMultiplier()
     {
       return this.multiplier;
     }
     
     public String getShortName()
     {
       return this.shortName;
     }
     
     public String getLongName()
     {
       return this.longName;
     }
     
     public static Range getRangeByPrefix(String prefix)
     {
       //@modify zhengxb add values()
       for (Range r : values()) {
         if (r.prefix.equalsIgnoreCase(prefix)) {
           return r;
         }
       }
       return null;
     }
     
     public static Range getRangeOfValue(long value)
     {
       long absValue = Math.abs(value);
       if (absValue <= 1L) {
         return BYTES;
       }
       for (Range r : values()) {
         if (absValue / r.multiplier < 800L) {
           return r;
         }
       }
       return BYTES;
     }
   }
   
   public static long convertKMGValue(String input)
   {
     Matcher m = valueMatch.matcher(input);
     if (m.matches())
     {
       long value = Long.parseLong(m.group(1));
       Range range = Range.getRangeByPrefix(m.group(2));
       if (range != null) {
         value *= range.getMultiplier();
       }
       return value;
     }
     throw new NumberFormatException("Improper input format: must be 'nnK' or 'nnM' or 'nnG'");
   }
   
   public static String formatToRange(long value)
   {
     Range r = Range.getRangeOfValue(value);
     if (r == Range.BYTES) {
       return String.format("%1$d %2$s", new Object[] { Long.valueOf(value), r.getShortName() });
     }
     double scaledValue = value / r.getMultiplier();
     return String.format("%1$3.2f %2$s", new Object[] { Double.valueOf(scaledValue), r.getShortName() });
   }
   
   public static String getLongRangeName(long value)
   {
     return Range.getRangeOfValue(value).getLongName();
   }
 }

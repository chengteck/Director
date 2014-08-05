 package com.ingres.util;
 
 import java.text.DecimalFormat;
 import java.text.FieldPosition;
 import java.text.NumberFormat;
 
 public class Num
 {
   private static NumberFormat f1 = null;
   private static NumberFormat f2 = null;
   
   static
   {
     f1 = NumberFormat.getIntegerInstance();
     f1.setGroupingUsed(true);
     f2 = NumberFormat.getInstance();
     if ((f2 instanceof DecimalFormat))
     {
       DecimalFormat df = (DecimalFormat)f2;
       df.setGroupingUsed(true);
       df.setDecimalSeparatorAlwaysShown(true);
       df.setMinimumFractionDigits(3);
     }
   }
   
   public static long lpow(long base, int n)
   {
     int bitMask = n;
     long evenPower = base;
     long result = 1L;
     while (bitMask != 0)
     {
       if ((bitMask & 0x1) != 0) {
         result *= evenPower;
       }
       evenPower *= evenPower;
       bitMask >>>= 1;
     }
     return result;
   }
   
   public static double dpow(double base, int n)
   {
     int bitMask = n;
     double evenPower = base;
     double result = 1.0D;
     while (bitMask != 0)
     {
       if ((bitMask & 0x1) != 0) {
         result *= evenPower;
       }
       evenPower *= evenPower;
       bitMask >>>= 1;
     }
     return result;
   }
   
   public static String fmt1(long value, int scale, int places)
   {
     StringBuffer buf = new StringBuffer();
     FieldPosition pos = new FieldPosition(0);
     f1.format(value / lpow(10L, scale), buf, pos);
     int pad = places - pos.getEndIndex();
     while (pad-- > 0) {
       buf.insert(0, ' ');
     }
     return buf.toString();
   }
   
   public static String fmt1(long value, int places)
   {
     return fmt1(value, 0, places);
   }
   
   public static String fmt2(double value, int scale, int places)
   {
     StringBuffer buf = new StringBuffer();
     FieldPosition pos = new FieldPosition(0);
     f2.format(value / dpow(10.0D, scale), buf, pos);
     int pad = places - pos.getEndIndex();
     while (pad-- > 0) {
       buf.insert(0, ' ');
     }
     return buf.toString();
   }
 }


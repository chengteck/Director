 package com.ingres.util;
 
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.security.MessageDigest;
 import java.security.SecureRandom;
 import javax.crypto.Cipher;
 import javax.crypto.SecretKeyFactory;
 import net.iharder.b64.Base64;
 
 public class SecurityUtil
 {
   public static final String __OBFUSCATE = "OBF:";
   public static final String __HASH = "SHS:";
   private static final String UTF_8 = "UTF-8";
   private static final String PBE_ALG = "PBEWithSHA1AndDESede";
   private static final char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
   private static MessageDigest __md = null;
   private static SecureRandom __random = null;
   private static SecretKeyFactory __factory = null;
   private static Cipher __cipher = null;
   
   static
   {
     try
     {
       __md = MessageDigest.getInstance("SHA-384");
       
       __random = SecureRandom.getInstance("SHA1PRNG");
       __random.setSeed(__random.generateSeed(256));
       
       __factory = SecretKeyFactory.getInstance("PBEWithSHA1AndDESede");
       
       __cipher = Cipher.getInstance("PBEWithSHA1AndDESede");
     }
     catch (Exception e)
     {
       e.printStackTrace();
     }
   }
   
   public static String hexEncode(byte[] input)
   {
     StringBuilder buf = new StringBuilder(input.length * 2);
     for (byte b : input)
     {
       buf.append(hexChars[(b >>> 4 & 0xF)]);
       buf.append(hexChars[(b & 0xF)]);
     }
     return buf.toString();
   }
   
   public static String obfuscate(String s)
   {
     StringBuilder buf = new StringBuilder();
     byte[] b = Base64.getStringBytes(s);
     
     buf.append("OBF:");
     for (int i = 0; i < b.length; i++)
     {
       byte b1 = b[i];
       byte b2 = b[(s.length() - (i + 1))];
       int i1 = 127 + b1 + b2;
       int i2 = 127 + b1 - b2;
       int i0 = i1 * 256 + i2;
       String x = Integer.toString(i0, 36);
       switch (x.length())
       {
       case 1: 
         buf.append('0');
       case 2: 
         buf.append('0');
       case 3: 
         buf.append('0');
       }
       buf.append(x);
     }
     return buf.toString();
   }
   
   public static String deobfuscate(String s)
   {
     if (s.startsWith("OBF:")) {
       s = s.substring("OBF:".length());
     }
     byte[] b = new byte[s.length() / 2];
     int l = 0;
     for (int i = 0; i < s.length(); i += 4)
     {
       String x = s.substring(i, i + 4);
       int i0 = Integer.parseInt(x, 36);
       int i1 = i0 / 256;
       int i2 = i0 % 256;
       b[(l++)] = ((byte)((i1 + i2 - 254) / 2));
     }
     return Base64.bytesToString(b, 0, l);
   }
   
   private static String hashValue(String s)
   {
     synchronized (__md)
     {
       __md.reset();
       __md.update(Base64.getStringBytes(s));
       byte[] digest = __md.digest();
       return hexEncode(digest);
     }
   }
   
   public static String hash(String s)
   {
     StringBuilder buf = new StringBuilder("SHS:");
     buf.append(hashValue(s));
     return buf.toString();
   }
   
   public static String read(InputStream is, int delim)
     throws IOException
   {
     ByteArrayOutputStream bos = new ByteArrayOutputStream();
     int byt = is.read();
     while ((byt > 0) && (byt != delim))
     {
       bos.write(byt);
       byt = is.read();
     }
     return Base64.bytesToString(bos.toByteArray());
   }
   
   public static String read(InputStream is)
     throws IOException
   {
     ByteArrayOutputStream bos = new ByteArrayOutputStream();
     int byt = is.read();
     while (byt > 0)
     {
       bos.write(byt);
       byt = is.read();
     }
     return Base64.bytesToString(bos.toByteArray());
   }
   
   public static void write(OutputStream os, String s)
     throws IOException
   {
     byte[] bytes = Base64.getStringBytes(s);
     os.write(bytes);
   }
   
   public static void write(OutputStream os, char c)
     throws IOException
   {
     byte b = (byte)(c & 0x7F);
     os.write(b);
   }
 }

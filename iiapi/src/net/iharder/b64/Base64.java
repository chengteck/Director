 package net.iharder.b64;
 
 import java.io.BufferedInputStream;
 import java.io.BufferedOutputStream;
 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.FilterInputStream;
 import java.io.FilterOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;
 import java.io.OutputStream;
 import java.io.PrintStream;
 import java.io.Serializable;
 import java.io.UnsupportedEncodingException;
 import java.util.zip.GZIPInputStream;
 import java.util.zip.GZIPOutputStream;
 
 public class Base64
 {
   public static final int NO_OPTIONS = 0;
   public static final int ENCODE = 1;
   public static final int DECODE = 0;
   public static final int GZIP = 2;
   public static final int DONT_BREAK_LINES = 8;
   public static final int URL_SAFE = 16;
   public static final int ORDERED = 32;
   private static final int MAX_LINE_LENGTH = 76;
   private static final byte EQUALS_SIGN = 61;
   private static final byte NEW_LINE = 10;
   private static final String PREFERRED_ENCODING = "UTF-8";
   private static final byte WHITE_SPACE_ENC = -5;
   private static final byte EQUALS_SIGN_ENC = -1;
   private static final byte[] _STANDARD_ALPHABET = { 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 47 };
   private static final byte[] _STANDARD_DECODABET = { -9, -9, -9, -9, -9, -9, -9, -9, -9, -5, -5, -9, -9, -5, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -5, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, 62, -9, -9, -9, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -9, -9, -9, -1, -9, -9, -9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -9, -9, -9, -9, -9, -9, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -9, -9, -9, -9 };
   private static final byte[] _URL_SAFE_ALPHABET = { 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 45, 95 };
   private static final byte[] _URL_SAFE_DECODABET = { -9, -9, -9, -9, -9, -9, -9, -9, -9, -5, -5, -9, -9, -5, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -5, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, 62, -9, -9, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -9, -9, -9, -1, -9, -9, -9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -9, -9, -9, -9, 63, -9, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -9, -9, -9, -9 };
   private static final byte[] _ORDERED_ALPHABET = { 45, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 95, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122 };
   private static final byte[] _ORDERED_DECODABET = { -9, -9, -9, -9, -9, -9, -9, -9, -9, -5, -5, -9, -9, -5, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -5, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, 0, -9, -9, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -9, -9, -9, -1, -9, -9, -9, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, -9, -9, -9, -9, 37, -9, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, -9, -9, -9, -9 };
   
   private static final byte[] getAlphabet(int options)
   {
     if ((options & 0x10) == 16) {
       return _URL_SAFE_ALPHABET;
     }
     if ((options & 0x20) == 32) {
       return _ORDERED_ALPHABET;
     }
     return _STANDARD_ALPHABET;
   }
   
   private static final byte[] getDecodabet(int options)
   {
     if ((options & 0x10) == 16) {
       return _URL_SAFE_DECODABET;
     }
     if ((options & 0x20) == 32) {
       return _ORDERED_DECODABET;
     }
     return _STANDARD_DECODABET;
   }
   
   public static final void main(String[] args)
   {
     if (args.length < 3)
     {
       usage("Not enough arguments.");
     }
     else
     {
       String flag = args[0];
       String infile = args[1];
       String outfile = args[2];
       if (flag.equals("-e")) {
         encodeFileToFile(infile, outfile);
       } else if (flag.equals("-d")) {
         decodeFileToFile(infile, outfile);
       } else {
         usage("Unknown flag: " + flag);
       }
     }
   }
   
   private static final void usage(String msg)
   {
     System.err.println(msg);
     System.err.println("Usage: java Base64 -e|-d inputfile outputfile");
   }
   
   private static byte[] encode3to4(byte[] b4, byte[] threeBytes, int numSigBytes, int options)
   {
     encode3to4(threeBytes, 0, numSigBytes, b4, 0, options);
     return b4;
   }
   
   private static byte[] encode3to4(byte[] source, int srcOffset, int numSigBytes, byte[] destination, int destOffset, int options)
   {
     byte[] ALPHABET = getAlphabet(options);
     
     int inBuff = (numSigBytes > 0 ? source[srcOffset] << 24 >>> 8 : 0) | (numSigBytes > 1 ? source[(srcOffset + 1)] << 24 >>> 16 : 0) | (numSigBytes > 2 ? source[(srcOffset + 2)] << 24 >>> 24 : 0);
     switch (numSigBytes)
     {
     case 3: 
       destination[destOffset] = ALPHABET[(inBuff >>> 18)];
       destination[(destOffset + 1)] = ALPHABET[(inBuff >>> 12 & 0x3F)];
       destination[(destOffset + 2)] = ALPHABET[(inBuff >>> 6 & 0x3F)];
       destination[(destOffset + 3)] = ALPHABET[(inBuff & 0x3F)];
       return destination;
     case 2: 
       destination[destOffset] = ALPHABET[(inBuff >>> 18)];
       destination[(destOffset + 1)] = ALPHABET[(inBuff >>> 12 & 0x3F)];
       destination[(destOffset + 2)] = ALPHABET[(inBuff >>> 6 & 0x3F)];
       destination[(destOffset + 3)] = 61;
       return destination;
     case 1: 
       destination[destOffset] = ALPHABET[(inBuff >>> 18)];
       destination[(destOffset + 1)] = ALPHABET[(inBuff >>> 12 & 0x3F)];
       destination[(destOffset + 2)] = 61;
       destination[(destOffset + 3)] = 61;
       return destination;
     }
     return destination;
   }
   
   public static String encodeObject(Serializable serializableObject)
   {
     return encodeObject(serializableObject, 0);
   }
   
   public static String bytesToString(byte[] bytes, int offset, int len)
   {
     try
     {
       return new String(bytes, offset, len, "UTF-8");
     }
     catch (UnsupportedEncodingException uue) {}
     return new String(bytes, offset, len);
   }
   
   public static String bytesToString(byte[] bytes)
   {
     try
     {
       return new String(bytes, "UTF-8");
     }
     catch (UnsupportedEncodingException uue) {}
     return new String(bytes);
   }
   
   public static byte[] getStringBytes(String s)
   {
     try
     {
       return s.getBytes("UTF-8");
     }
     catch (UnsupportedEncodingException uue) {}
     return s.getBytes();
   }
   
   public static String encodeObject(Serializable serializableObject, int options)
   {
	 //@modify zhengxb
     ByteArrayOutputStream baos = null;
     OutputStream b64os = null;
     ObjectOutputStream oos = null;
     GZIPOutputStream gzos = null;
     
     int gzip = options & 0x2;
     int dontBreakLines = options & 0x8;
     try
     {
       baos = new ByteArrayOutputStream();
       b64os = new OutputStream(baos, 0x1 | options);
       if (gzip == 2)
       {
         gzos = new GZIPOutputStream(b64os);
         oos = new ObjectOutputStream(gzos);
       }
       else
       {
         oos = new ObjectOutputStream(b64os);
       }
       oos.writeObject(serializableObject);
       
       return bytesToString(baos.toByteArray());
     }
     catch (IOException e)
     {
       e.printStackTrace();
       return null;
     }
     finally
     {
       try
       {
         oos.close();
       }
       catch (Exception e) {}
       try
       {
         gzos.close();
       }
       catch (Exception e) {}
       try
       {
         b64os.close();
       }
       catch (Exception e) {}
       try
       {
         baos.close();
       }
       catch (Exception e) {}
     }
   }
   
   public static String encode(String source)
   {
     byte[] bytes = source.getBytes();
     return encodeBytes(bytes, 0, bytes.length, 0);
   }
   
   public static String encode(String source, int options)
   {
     byte[] bytes = source.getBytes();
     return encodeBytes(bytes, 0, bytes.length, options);
   }
   
   public static String encodeUTF8(String source)
   {
     byte[] bytes = getStringBytes(source);
     return encodeBytes(bytes, 0, bytes.length, 0);
   }
   
   public static String encodeUTF8(String source, int options)
   {
     byte[] bytes = getStringBytes(source);
     return encodeBytes(bytes, 0, bytes.length, options);
   }
   
   public static String encodeBytes(byte[] source)
   {
     return encodeBytes(source, 0, source.length, 0);
   }
   
   public static String encodeBytes(byte[] source, int options)
   {
     return encodeBytes(source, 0, source.length, options);
   }
   
   public static String encodeBytes(byte[] source, int off, int len)
   {
     return encodeBytes(source, off, len, 0);
   }
   
   public static String encodeBytes(byte[] source, int off, int len, int options)
   {
     int dontBreakLines = options & 0x8;
     int gzip = options & 0x2;
     ByteArrayOutputStream baos;
     if (gzip == 2)
     {
       baos = null;
       GZIPOutputStream gzos = null;
       OutputStream b64os = null;
       try
       {
         baos = new ByteArrayOutputStream();
         b64os = new OutputStream(baos, 0x1 | options);
         gzos = new GZIPOutputStream(b64os);
         
         gzos.write(source, off, len);
         gzos.close();

         return bytesToString(baos.toByteArray());
       }
       catch (IOException e)
       {
         e.printStackTrace();
         return null;
       }
       finally
       {
         try
         {
           gzos.close();
         }
         catch (Exception e) {}
         try
         {
           b64os.close();
         }
         catch (Exception e) {}
         try
         {
           baos.close();
         }
         catch (Exception e) {}
       }
     }
     boolean breakLines = dontBreakLines == 0;
     
     int len43 = len * 4 / 3;
     byte[] outBuff = new byte[len43 + (len % 3 > 0 ? 4 : 0) + (breakLines ? len43 / 76 : 0)];
     
 
     int d = 0;
     int e = 0;
     int len2 = len - 2;
     int lineLength = 0;
     for (; d < len2; e += 4)
     {
       encode3to4(source, d + off, 3, outBuff, e, options);
       
       lineLength += 4;
       if ((breakLines) && (lineLength == 76))
       {
         outBuff[(e + 4)] = 10;
         e++;
         lineLength = 0;
       }
       d += 3;
     }
     if (d < len)
     {
       encode3to4(source, d + off, len - d, outBuff, e, options);
       e += 4;
     }
     return bytesToString(outBuff, 0, e);
   }
   
   private static int decode4to3(byte[] source, int srcOffset, byte[] destination, int destOffset, int options)
   {
     byte[] DECODABET = getDecodabet(options);
     if (source[(srcOffset + 2)] == 61)
     {
       int outBuff = (DECODABET[source[srcOffset]] & 0xFF) << 18 | (DECODABET[source[(srcOffset + 1)]] & 0xFF) << 12;
       
 
       destination[destOffset] = ((byte)(outBuff >>> 16));
       return 1;
     }
     if (source[(srcOffset + 3)] == 61)
     {
       int outBuff = (DECODABET[source[srcOffset]] & 0xFF) << 18 | (DECODABET[source[(srcOffset + 1)]] & 0xFF) << 12 | (DECODABET[source[(srcOffset + 2)]] & 0xFF) << 6;
       
 
 
       destination[destOffset] = ((byte)(outBuff >>> 16));
       destination[(destOffset + 1)] = ((byte)(outBuff >>> 8));
       return 2;
     }
     try
     {
       int outBuff = (DECODABET[source[srcOffset]] & 0xFF) << 18 | (DECODABET[source[(srcOffset + 1)]] & 0xFF) << 12 | (DECODABET[source[(srcOffset + 2)]] & 0xFF) << 6 | DECODABET[source[(srcOffset + 3)]] & 0xFF;
       
       destination[destOffset] = ((byte)(outBuff >> 16));
       destination[(destOffset + 1)] = ((byte)(outBuff >> 8));
       destination[(destOffset + 2)] = ((byte)outBuff);
       
       return 3;
     }
     catch (Exception e)
     {
       System.out.println("" + source[srcOffset] + ": " + DECODABET[source[srcOffset]]);
       System.out.println("" + source[(srcOffset + 1)] + ": " + DECODABET[source[(srcOffset + 1)]]);
       System.out.println("" + source[(srcOffset + 2)] + ": " + DECODABET[source[(srcOffset + 2)]]);
       System.out.println("" + source[(srcOffset + 3)] + ": " + DECODABET[source[(srcOffset + 3)]]);
     }
     return -1;
   }
   
   public static byte[] decode(byte[] source, int off, int len, int options)
   {
     byte[] DECODABET = getDecodabet(options);
     
     int len34 = len * 3 / 4;
     byte[] outBuff = new byte[len34];
     int outBuffPosn = 0;
     
     byte[] b4 = new byte[4];
     int b4Posn = 0;
     int i = 0;
     byte sbiCrop = 0;
     byte sbiDecode = 0;
     for (i = off; i < off + len; i++)
     {
       sbiCrop = (byte)(source[i] & 0x7F);
       sbiDecode = DECODABET[sbiCrop];
       if (sbiDecode >= -5)
       {
         if (sbiDecode >= -1)
         {
           b4[(b4Posn++)] = sbiCrop;
           if (b4Posn > 3)
           {
             outBuffPosn += decode4to3(b4, 0, outBuff, outBuffPosn, options);
             b4Posn = 0;
             if (sbiCrop == 61) {
               break;
             }
           }
         }
       }
       else
       {
         System.err.println("Bad Base64 input character at " + i + ": " + source[i] + "(decimal)");
         return null;
       }
     }
     byte[] out = new byte[outBuffPosn];
     System.arraycopy(outBuff, 0, out, 0, outBuffPosn);
     return out;
   }
   
   public static String decodeUTF8(String s)
   {
     return bytesToString(decode(s, 0));
   }
   
   public static byte[] decode(String s)
   {
     return decode(s, 0);
   }
   
   public static byte[] decode(String s, int options)
   {
	 //@modify zhengxb
	 byte[] bytes = getStringBytes(s);
     
     bytes = decode(bytes, 0, bytes.length, options);
     if ((bytes != null) && (bytes.length >= 4))
     {
       int head = bytes[0] & 0xFF | bytes[1] << 8 & 0xFF00;
       if (35615 == head)
       {
         ByteArrayInputStream bais = null;
         GZIPInputStream gzis = null;
         ByteArrayOutputStream baos = null;
         byte[] buffer = new byte[2048];
         int length = 0;
         try
         {
           baos = new ByteArrayOutputStream();
           bais = new ByteArrayInputStream(bytes);
           gzis = new GZIPInputStream(bais);
           while ((length = gzis.read(buffer)) >= 0) {
             baos.write(buffer, 0, length);
           }
           return baos.toByteArray();
         }
         catch (IOException e) {
        	 
         }finally
         {
           try
           {
             baos.close();
           }
           catch (Exception e) {
        	   
           }
           
           try
           {
             gzis.close();
           }
           catch (Exception e) {
        	   
           }
           
           try
           {
             bais.close();
           }
           catch (Exception e) {
        	   
           }
           
          
         }
       }
     }
     
     //@modify zhengxb
     return null;
   }
   
   public static Object decodeToObject(String encodedObject)
   {
     byte[] objBytes = decode(encodedObject);
     
     ByteArrayInputStream bais = null;
     ObjectInputStream ois = null;
     Object obj = null;
     try
     {
       bais = new ByteArrayInputStream(objBytes);
       ois = new ObjectInputStream(bais);
       
       return ois.readObject();
     }
     catch (IOException e)
     {
       e.printStackTrace();
       obj = null;
     }
     catch (ClassNotFoundException e)
     {
       e.printStackTrace();
       obj = null;
     }
     finally
     {
       try
       {
         bais.close();
       }
       catch (Exception e) {}
       try
       {
         ois.close();
       }
       catch (Exception e) {}
     }
     
     return null;
   }
   
   public static boolean encodeToFile(byte[] dataToEncode, String filename)
   {
     boolean success = false;
     OutputStream bos = null;
     try
     {
       bos = new OutputStream(new FileOutputStream(filename), 1);
       
       bos.write(dataToEncode);
       return true;
     }
     catch (IOException e)
     {
       success = false;
     }
     finally
     {
       try
       {
         bos.close();
       }
       catch (Exception e) {}
     }
     
     return false;
   }
   
   public static boolean decodeToFile(String dataToDecode, String filename)
   {
     boolean success = false;
     OutputStream bos = null;
     try
     {
       bos = new OutputStream(new FileOutputStream(filename), 0);
       
       bos.write(dataToDecode.getBytes("UTF-8"));
       return true;
     }
     catch (IOException e)
     {
       success = false;
     }
     finally
     {
       try
       {
         bos.close();
       }
       catch (Exception e) {}
     }
     
     return false;
   }
   
   public static byte[] decodeFromFile(String filename)
   {
     byte[] decodedData = null;
     InputStream bis = null;
     try
     {
       File file = new File(filename);
       byte[] buffer = null;
       int length = 0;
       int numBytes = 0;
       if (file.length() > 2147483647L)
       {
         System.err.println("File is too big for this convenience method (" + file.length() + " bytes).");
         return null;
       }
       buffer = new byte[(int)file.length()];
       bis = new InputStream(new BufferedInputStream(new FileInputStream(file)), 0);
       while ((numBytes = bis.read(buffer, length, 4096)) >= 0) {
         length += numBytes;
       }
       decodedData = new byte[length];
       System.arraycopy(buffer, 0, decodedData, 0, length);
       return decodedData;
     }
     catch (IOException e)
     {
       System.err.println("Error decoding from file " + filename);
     }
     finally
     {
       try
       {
         bis.close();
       }
       catch (Exception e) {}
     }
     
     return null;
   }
   
   public static String encodeFromFile(String filename)
   {
     byte[] encodedData = null;
     InputStream bis = null;
     try
     {
       File file = new File(filename);
       byte[] buffer = new byte[Math.max((int)(file.length() * 1.4D), 40)];
       int length = 0;
       int numBytes = 0;
      
       bis = new InputStream(new BufferedInputStream(new FileInputStream(file)), 1);
       while ((numBytes = bis.read(buffer, length, 4096)) >= 0) {
         length += numBytes;
       }
       return new String(buffer, 0, length, "UTF-8");
     }
     catch (IOException e)
     {
       System.err.println("Error encoding from file " + filename);
     }
     finally
     {
       try
       {
         bis.close();
       }
       catch (Exception e) {}
     }
     
     return null;
   }
   
   public static boolean encodeFileToFile(String infile, String outfile)
   {
     boolean success = false;
     InputStream in = null;
     
     //@modify zhengxb OutputStream to java.io.OutputStream
     java.io.OutputStream out = null;
     try
     {
       in = new InputStream(new BufferedInputStream(new FileInputStream(infile)), 1);
       out = new BufferedOutputStream(new FileOutputStream(outfile));
       byte[] buffer = new byte[65536];
       int read = -1;
       while ((read = in.read(buffer)) >= 0) {
         out.write(buffer, 0, read);
       }
       return true;
     }
     catch (IOException exc)
     {
       exc.printStackTrace();
     }
     finally
     {
       try
       {
         in.close();
       }
       catch (Exception exc) {}
       try
       {
         out.close();
       }
       catch (Exception exc) {}
     }
     
     return false;
   }
   
   public static boolean decodeFileToFile(String infile, String outfile)
   {
     boolean success = false;
     InputStream in = null;
     
     //@modify zhengxb
     java.io.OutputStream out = null;
     try
     {
       in = new InputStream(new BufferedInputStream(new FileInputStream(infile)), 0);
       out = new BufferedOutputStream(new FileOutputStream(outfile));
       byte[] buffer = new byte[65536];
       int read = -1;
       while ((read = in.read(buffer)) >= 0) {
         out.write(buffer, 0, read);
       }
       return true;
     }
     catch (IOException exc)
     {
       exc.printStackTrace();
     }
     finally
     {
       try
       {
         in.close();
       }
       catch (Exception exc) {}
       try
       {
         out.close();
       }
       catch (Exception exc) {}
     }
     
     return false;
   }
   
   public static class InputStream
     extends FilterInputStream
   {
     private boolean encode;
     private int position;
     private byte[] buffer;
     private int bufferLength;
     private int numSigBytes;
     private int lineLength;
     private boolean breakLines;
     private int options;
     private byte[] alphabet;
     private byte[] decodabet;
     
     //@modify zhengxb param InputStream change to java.io.InputStream
     public InputStream(java.io.InputStream in)
     {
       this(in, 0);
     }
     
     //@modify zhengxb param InputStream change to java.io.InputStream
     public InputStream(java.io.InputStream in, int options)
     {
       //@modify zhengxb super() changed to super(in)
       super(in);
       this.breakLines = ((options & 0x8) != 8);
       this.encode = ((options & 0x1) == 1);
       this.bufferLength = (this.encode ? 4 : 3);
       this.buffer = new byte[this.bufferLength];
       this.position = -1;
       this.lineLength = 0;
       this.options = options;
       this.alphabet = Base64.getAlphabet(options);
       this.decodabet = Base64.getDecodabet(options);
     }
     
     public int read()
       throws IOException
     {
       if (this.position < 0) {
         if (this.encode)
         {
           byte[] b3 = new byte[3];
           int numBinaryBytes = 0;
           for (int i = 0; i < 3; i++) {
             try
             {
               int b = this.in.read();
               if (b >= 0)
               {
                 b3[i] = ((byte)b);
                 numBinaryBytes++;
               }
             }
             catch (IOException e)
             {
               if (i == 0) {
                 throw e;
               }
             }
           }
           if (numBinaryBytes > 0)
           {
             Base64.encode3to4(b3, 0, numBinaryBytes, this.buffer, 0, this.options);
             this.position = 0;
             this.numSigBytes = 4;
           }
           else
           {
             return -1;
           }
         }
         else
         {
           byte[] b4 = new byte[4];
           int i = 0;
           for (i = 0; i < 4; i++)
           {
             int b = 0;
             do
             {
               b = this.in.read();
             } while ((b >= 0) && (this.decodabet[(b & 0x7F)] <= -5));
             if (b < 0) {
               break;
             }
             b4[i] = ((byte)b);
           }
           if (i == 4)
           {
             this.numSigBytes = Base64.decode4to3(b4, 0, this.buffer, 0, this.options);
             this.position = 0;
           }
           else
           {
             if (i == 0) {
               return -1;
             }
             throw new IOException("Improperly padded Base64 input.");
           }
         }
       }
       if (this.position >= 0)
       {
         if (this.position >= this.numSigBytes) {
           return -1;
         }
         if ((this.encode) && (this.breakLines) && (this.lineLength >= 76))
         {
           this.lineLength = 0;
           return 10;
         }
         this.lineLength += 1;
         
 
 
         int b = this.buffer[(this.position++)];
         if (this.position >= this.bufferLength) {
           this.position = -1;
         }
         return b & 0xFF;
       }
       throw new IOException("Error in Base64 code reading stream.");
     }
     
     public int read(byte[] dest, int off, int len)
       throws IOException
     {
       for (int i = 0; i < len; i++)
       {
         int b = read();
         if (b >= 0)
         {
           dest[(off + i)] = ((byte)b);
         }
         else
         {
           if (i != 0) {
             break;
           }
           return -1;
         }
       }
       return 0;
       //return i;
     }
   }
   
   public static class OutputStream
     extends FilterOutputStream
   {
     private boolean encode;
     private int position;
     private byte[] buffer;
     private int bufferLength;
     private int lineLength;
     private boolean breakLines;
     private byte[] b4;
     private boolean suspendEncoding;
     private int options;
     private byte[] alphabet;
     private byte[] decodabet;
     
     //@modify zhengxb param InputStream change to java.io.InputStream
     public OutputStream(java.io.OutputStream out)
     {
       this(out, 1);
     }
     
     //@modify zhengxb param InputStream change to java.io.InputStream
     public OutputStream(java.io.OutputStream out, int options)
     {
       //@modify zhengxb super() changed to super(out)
       super(out);
       this.breakLines = ((options & 0x8) != 8);
       this.encode = ((options & 0x1) == 1);
       this.bufferLength = (this.encode ? 3 : 4);
       this.buffer = new byte[this.bufferLength];
       this.position = 0;
       this.lineLength = 0;
       this.suspendEncoding = false;
       this.b4 = new byte[4];
       this.options = options;
       this.alphabet = Base64.getAlphabet(options);
       this.decodabet = Base64.getDecodabet(options);
     }
     
     public void write(int theByte)
       throws IOException
     {
       if (this.suspendEncoding)
       {
         this.out.write(theByte);
         return;
       }
       if (this.encode)
       {
         this.buffer[(this.position++)] = ((byte)theByte);
         if (this.position >= this.bufferLength)
         {
           this.out.write(Base64.encode3to4(this.b4, this.buffer, this.bufferLength, this.options));
           
           this.lineLength += 4;
           if ((this.breakLines) && (this.lineLength >= 76))
           {
             this.out.write(10);
             this.lineLength = 0;
           }
           this.position = 0;
         }
       }
       else if (this.decodabet[(theByte & 0x7F)] > -5)
       {
         this.buffer[(this.position++)] = ((byte)theByte);
         if (this.position >= this.bufferLength)
         {
           int len = Base64.decode4to3(this.buffer, 0, this.b4, 0, this.options);
           this.out.write(this.b4, 0, len);
           
           this.position = 0;
         }
       }
       else if (this.decodabet[(theByte & 0x7F)] != -5)
       {
         throw new IOException("Invalid character in Base64 data.");
       }
     }
     
     public void write(byte[] theBytes, int off, int len)
       throws IOException
     {
       if (this.suspendEncoding)
       {
         this.out.write(theBytes, off, len);
         return;
       }
       for (int i = 0; i < len; i++) {
         write(theBytes[(off + i)]);
       }
     }
     
     public void flushBase64()
       throws IOException
     {
       if (this.position > 0) {
         if (this.encode)
         {
           this.out.write(Base64.encode3to4(this.b4, this.buffer, this.position, this.options));
           this.position = 0;
         }
         else
         {
           throw new IOException("Base64 input not properly padded.");
         }
       }
     }
     
     public void close()
       throws IOException
     {
       flushBase64();
       
 
 
       super.close();
       
       this.buffer = null;
       this.out = null;
     }
     
     public void suspendEncoding()
       throws IOException
     {
       flushBase64();
       this.suspendEncoding = true;
     }
     
     public void resumeEncoding()
     {
       this.suspendEncoding = false;
     }
   }
 }

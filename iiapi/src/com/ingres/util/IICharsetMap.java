 package com.ingres.util;
 
 import java.nio.charset.UnsupportedCharsetException;
 import java.util.HashMap;
 
 public class IICharsetMap
 {
   private static final HashMap<String, String> csMap = new HashMap<String, String>();
   private static final String[][] csInfo = { { "ISO88591", "ISO-8859-1" }, { "ISO88592", "ISO8859_2" }, { "ISO88595", "ISO8859_5" }, { "ISO88599", "ISO8859_9" }, { "IS885915", "ISO8859_15" }, { "IBMPC_ASCII_INT", "Cp850" }, { "IBMPC_ASCII", "Cp850" }, { "IBMPC437", "Cp437" }, { "ELOT437", "Cp737" }, { "SLAV852", "Cp852" }, { "IBMPC850", "Cp850" }, { "CW", "Cp1251" }, { "ALT", "Cp855" }, { "PC857", "Cp857" }, { "WIN1250", "Cp1250" }, { "KOI8", "KOI8_R" }, { "IBMPC866", "Cp866" }, { "WIN1252", "Cp1252" }, { "ASCII", "US-ASCII" }, { "DECMULTI", "ISO-8859-1" }, { "HEBREW", "Cp424" }, { "THAI", "Cp874" }, { "GREEK", "Cp875" }, { "HPROMAN8", "ISO-8859-1" }, { "ARABIC", "Cp420" }, { "WHEBREW", "Cp1255" }, { "PCHEBREW", "Cp862" }, { "WARABIC", "Cp1256" }, { "DOSASMO", "Cp864" }, { "WTHAI", "Cp874" }, { "EBCDIC_C", "Cp500" }, { "EBCDIC_ICL", "Cp500" }, { "EBCDIC_USA", "Cp037" }, { "EBCDIC", "Cp500" }, { "CHINESES", "Cp1383" }, { "KOREAN", "Cp949" }, { "KANJIEUC", "EUC_JP" }, { "SHIFTJIS", "SJIS" }, { "CHTBIG5", "Big5" }, { "CHTEUC", "EUC_TW" }, { "CHTHP", "EUC_TW" }, { "CSGBK", "GBK" }, { "CSGB2312", "EUC_CN" }, { "UTF8", "UTF-8" } };
   
   static
   {
     for (int i = 0; i < csInfo.length; i++) {
       csMap.put(csInfo[i][0], csInfo[i][1]);
     }
   }
   
   public static String getJavaNameForIngresName(String ingresName)
   {
     String javaName = (String)csMap.get(ingresName.toUpperCase());
     if (javaName == null) {
       throw new UnsupportedCharsetException(String.format("Unknown character set name: %1$s", new Object[] { ingresName }));
     }
     return javaName;
   }
 }

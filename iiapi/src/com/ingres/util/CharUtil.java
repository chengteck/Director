package com.ingres.util;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharUtil
{
  private static Pattern regularIdentifierPattern = Pattern.compile("[_a-zA-Z][_a-zA-Z0-9$@#]*");
  private static Pattern upperIdentifierPattern = Pattern.compile("[_A-Z][_A-Z0-9$@#]*");
  private static Pattern specialCharsPattern = Pattern.compile("[0-9$@#].*|.*[&*:,\"=/<>()\\-%.+?;' |\\\\^{}!`~].*");
  private static Pattern delimitedIdentifierPattern = Pattern.compile("\"[_a-zA-Z0-9$@#&*:,\"=/<>()\\-%.+?;' |\\\\^{}!`~]+\"");
  private static Pattern runsOfSpacesPattern = Pattern.compile("  +");
  private static Pattern rtrimPattern = Pattern.compile(" +$");
  private static Pattern winCmdArgNeedsQuotingPattern = Pattern.compile("[\\s\"\\\\]+");
  private static Pattern backslashesBeforeDoubleQuotePattern = Pattern.compile("\\\\*\"|\\\\+$");
  private static Charset utf8Charset = Charset.forName("UTF-8");
  public static final String Digits = "(\\p{Digit}+)";
  public static final String HexDigits = "(\\p{XDigit}+)";
  public static final String HexConstant = "0[xX](\\p{XDigit}+)";
  private static final String Exp = "[eE][+-]?(\\p{Digit}+)";
  public static final String fpRegex = "(NaN|Infinity|((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)([eE][+-]?(\\p{Digit}+))?))[fFdD]?))";
  public static final String fpRegex2 = "(((\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?)[fFdD]?))";
  public static final String fpRegex3 = "(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?(\\.)(\\p{XDigit}+)))[pP][+-]?(\\p{Digit}+))[fFdD]?";
  
  public static enum Justification
  {
    LEFT,  CENTER,  RIGHT;
    
    private Justification() {}
  }
  
  public static String stripQuotes(String value)
  {
    String trimmedValue = value.trim();
    if ((!trimmedValue.startsWith("'")) && (!trimmedValue.endsWith("'"))) {
      return value;
    }
    StringBuilder buf = new StringBuilder(trimmedValue);
    if (buf.charAt(0) == '\'') {
      buf.deleteCharAt(0);
    }
    if (buf.charAt(buf.length() - 1) == '\'') {
      buf.deleteCharAt(buf.length() - 1);
    }
    int ix = buf.indexOf("''");
    while (ix >= 0)
    {
      buf.deleteCharAt(ix);
      ix = buf.indexOf("''", ix + 1);
    }
    ix = buf.indexOf("\\'");
    while (ix >= 0)
    {
      buf.deleteCharAt(ix);
      ix = buf.indexOf("\\'", ix + 1);
    }
    return buf.toString();
  }
  
  public static String addQuotes(String value)
  {
    StringBuilder buf = new StringBuilder();
    addQuotes(value, buf);
    return buf.toString();
  }
  
  public static void addQuotes(String value, StringBuilder buf)
  {
    if (value != null)
    {
      buf.append('\'');
      int ix = buf.length();
      buf.append(value);
      ix = buf.indexOf("'", ix);
      while (ix >= 0)
      {
        buf.insert(ix, '\'');
        ix = buf.indexOf("'", ix + 2);
      }
      buf.append('\'');
    }
  }
  
  public static String doubleQuoteIfNeeded(String input)
  {
    if ((input.indexOf(' ') >= 0) || (input.indexOf(',') >= 0) || (input.indexOf(';') >= 0) || (input.indexOf('(') >= 0) || (input.indexOf(')') >= 0))
    {
      StringBuilder buf = new StringBuilder(input);
      buf.insert(0, '"');
      buf.append('"');
      return buf.toString();
    }
    return input;
  }
  
  public static String delimitIdentifier(String value, boolean mixedcase)
  {
    int len = value.length();
    boolean isDelimited = (len > 1) && (value.charAt(0) == '"') && (value.charAt(len - 1) == '"');
    boolean needsDelimiting = false;
    if (isDelimited)
    {
      needsDelimiting = true;
      value = value.substring(1, len - 1);
    }
    else
    {
      if (mixedcase) {
        needsDelimiting = !value.toUpperCase().equals(value);
      }
      if (!needsDelimiting)
      {
        Matcher m = specialCharsPattern.matcher(value);
        needsDelimiting = m.matches();
      }
      if (!needsDelimiting) {
        needsDelimiting = Reserved.isReserved(value);
      }
    }
    if (needsDelimiting)
    {
      StringBuilder buf = new StringBuilder(value);
      
      int ix = buf.indexOf("\"");
      while (ix >= 0)
      {
        buf.insert(ix, '"');
        ix = buf.indexOf("\"", ix + 2);
      }
      buf.insert(0, '"');
      buf.append('"');
      return buf.toString();
    }
    return value;
  }
  
  public static String undelimitIdentifier(String input)
  {
    int len = input.length();
    boolean leading = (len > 1) && (input.charAt(0) == '"');
    boolean trailing = (len > 1) && (input.charAt(len - 1) == '"');
    if ((leading) || (trailing))
    {
      StringBuilder buf = new StringBuilder(input);
      if (leading) {
        buf.deleteCharAt(0);
      }
      if (trailing) {
        buf.deleteCharAt(buf.length() - 1);
      }
      int ix = buf.indexOf("\"\"");
      while (ix >= 0)
      {
        buf.replace(ix, ix + 2, "\"");
        ix = buf.indexOf("\"\"", ix + 1);
      }
      return buf.toString();
    }
    return input;
  }
  
  public static String getJSONForm(String input)
  {
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < input.length(); i++)
    {
      char ch = input.charAt(i);
      if (!Character.isJavaIdentifierPart(ch)) {
        buf.append(Integer.toString(ch, 16));
      } else {
        buf.append(ch);
      }
    }
    return buf.toString();
  }
  
  public static String padToWidth(String input, int width)
  {
    return padToWidth(input, width, ' ');
  }
  
  public static StringBuilder padToWidth(StringBuilder buf, String input, int width)
  {
    return padToWidth(buf, input, width, ' ', Justification.LEFT);
  }
  
  public static String padToWidth(String input, int width, Justification just)
  {
    return padToWidth(input, width, ' ', just);
  }
  
  public static StringBuilder padToWidth(StringBuilder buf, String input, int width, Justification just)
  {
    return padToWidth(buf, input, width, ' ', just);
  }
  
  public static String padToWidth(String input, int width, char pad)
  {
    return padToWidth(input, width, pad, Justification.LEFT);
  }
  
  public static StringBuilder padToWidth(StringBuilder buf, String input, int width, char pad)
  {
    return padToWidth(buf, input, width, pad, Justification.LEFT);
  }
  
  public static String padToWidth(String input, int width, char pad, Justification just)
  {
    if (input.length() >= width) {
      return input;
    }
    return padToWidth(new StringBuilder(), input, width, pad, just).toString();
  }
  
  public static StringBuilder padToWidth(StringBuilder buf, String input, int width, char pad, Justification just)
  {
    //switch (1.$SwitchMap$com$ingres$util$CharUtil$Justification[just.ordinal()])
    switch(just.ordinal())
	  {
    case 1: 
      buf.append(input);
      for (int i = input.length(); i < width; i++) {
        buf.append(pad);
      }
      break;
    case 2: 
      int left = (width - input.length()) / 2;
      int right = width - left;
      for (int i = 0; i < left; i++) {
        buf.append(pad);
      }
      buf.append(input);
      for (int i = 0; i < right; i++) {
        buf.append(pad);
      }
      break;
    case 3: 
      for (int i = input.length(); i < width; i++) {
        buf.append(pad);
      }
      buf.append(input);
    }
    return buf;
  }
  
  public static boolean isValidIdentifier(String value, boolean uppercase)
  {
    int len = value.length();
    Matcher m;
    if ((len > 1) && (value.charAt(0) == '"') && (value.charAt(len - 1) == '"'))
    {
      m = delimitedIdentifierPattern.matcher(value);
    }
    else
    {
      if (uppercase) {
        m = upperIdentifierPattern.matcher(value);
      } else {
        m = regularIdentifierPattern.matcher(value);
      }
    }
    return m.matches();
  }
  
  public static String regularizeSpaces(String value)
  {
    return runsOfSpacesPattern.matcher(value.trim()).replaceAll(" ");
  }
  
  public static String delimRtrim(Object input)
  {
    String result = rtrimPattern.matcher((String)input).replaceAll("");
    if (result.length() == 0) {
      result = " ";
    }
    return result;
  }
  
  public static String rtrim(Object input)
  {
    return rtrimPattern.matcher((String)input).replaceAll("");
  }
  
  public static String toCamelCase(String enumName)
  {
    StringBuilder buf = new StringBuilder(enumName.toLowerCase());
    int ix;
    while ((ix = buf.indexOf("_")) >= 0)
    {
      buf.deleteCharAt(ix);
      if (ix < buf.length()) {
        buf.setCharAt(ix, Character.toUpperCase(buf.charAt(ix)));
      }
    }
    return buf.toString();
  }
  
  public static String windowsEscapeForCmdLine(String arg)
  {
    Matcher m = winCmdArgNeedsQuotingPattern.matcher(arg);
    if (m.find())
    {
      StringBuffer sb = new StringBuffer();
      m = backslashesBeforeDoubleQuotePattern.matcher(arg);
      while (m.find()) {
        if (!m.hitEnd())
        {
          String backslashes = m.group().substring(0, m.group().length() - 1);
          String replacement = String.format("%1$s%1$s\\\"", new Object[] { backslashes });
          m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        else
        {
          String replacement = String.format("%1$s%1$s", new Object[] { m.group() });
          m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
      }
      m.appendTail(sb);
      


      //arg = '"';
					arg = "";
     }
     return arg;
   }
   
   public static String quoteValue(String value, char delimiter)
   {
     if (value.indexOf(delimiter) >= 0)
     {
       char quoteChar;
       switch (delimiter)
       {
       case '\'': 
         quoteChar = '"';
         break;
       case '"': 
         quoteChar = '\'';
         break;
       default: 
         quoteChar = '"';
       }
       StringBuilder buf = new StringBuilder(value.length() + 2);
       buf.append(quoteChar);
       buf.append(value);
       buf.append(quoteChar);
       return buf.toString();
     }
     return value;
   }
   
   public static String makeStringList(String[] values)
   {
     StringBuilder buf = new StringBuilder();
     makeStringList(values, buf);
     return buf.toString();
   }
   
   public static void makeStringList(String[] values, StringBuilder buf)
   {
     buf.append('(');
     for (String value : values)
     {
       addQuotes(value, buf);
       buf.append(',');
     }
     buf.setCharAt(buf.length() - 1, ')');
   }
   
   public static String makeStringList(Object[] values)
   {
     StringBuilder buf = new StringBuilder();
     makeStringList(values, buf);
     return buf.toString();
   }
   
   public static void makeStringList(Object[] values, StringBuilder buf)
   {
     buf.append('(');
     for (Object value : values)
     {
       addQuotes(value == null ? null : value.toString(), buf);
       buf.append(',');
     }
     buf.setCharAt(buf.length() - 1, ')');
   }
   
   public static String makeSimpleStringList(List<String> values)
   {
     StringBuilder buf = new StringBuilder();
     for (String value : values) {
       buf.append(value).append(',');
     }
     int len = buf.length();
     if (len > 0) {
       buf.deleteCharAt(len - 1);
     }
     return buf.toString();
   }
   
   public static byte[] getUtf8Bytes(String s)
   {
     return s == null ? null : s.getBytes(utf8Charset);
   }
   
   public static String getUtf8String(byte[] bytes)
   {
     return bytes == null ? null : new String(bytes, utf8Charset);
   }
   
   public static void quoteForCSV(String input, StringBuilder buf)
   {
     if ((input != null) && (!input.isEmpty())) {
       if ((input.indexOf("\"") >= 0) || (input.indexOf(",") >= 0) || (input.indexOf("\n") >= 0) || (input.indexOf("\r") >= 0) || (input.startsWith(" ")) || (input.endsWith(" ")))
       {
         buf.append('"');
         int ix = buf.length();
         buf.append(input);
         ix = buf.indexOf("\"", ix);
         while (ix >= 0)
         {
           buf.insert(ix, '"');
           ix = buf.indexOf("\"", ix + 2);
         }
         buf.append('"');
       }
       else
       {
         buf.append(input);
       }
     }
   }
   
   public static void appendToCSV(String input, StringBuilder buf)
   {
     quoteForCSV(input, buf);
     buf.append(',');
   }
   
   public static String getFromCSV(StringBuilder buf)
   {
     String result = null;
     if (buf.length() == 0) {
       return null;
     }
     int ix = 0;
     if (buf.charAt(0) == '"')
     {
       buf.deleteCharAt(0);
       while ((ix < buf.length()) && (buf.charAt(ix) != '"'))
       {
         if ((ix + 1 < buf.length()) && (buf.charAt(ix + 1) == '"')) {
           buf.deleteCharAt(ix);
         }
         ix++;
       }
       result = buf.substring(0, ix);
       if (ix < buf.length()) {
         ix++;
       }
     }
     else
     {
       while ((ix < buf.length()) && (buf.charAt(ix) != ',')) {
         ix++;
       }
       result = buf.substring(0, ix);
     }
     if ((ix < buf.length()) && (buf.charAt(ix) == ',')) {
       ix++;
     }
     buf.delete(0, ix);
     return result;
   }
   
   public static String substituteEnvValue(String input, Map<String, String> symbols)
   {
     StringBuilder buf = new StringBuilder(input);
     int ix = buf.indexOf("%");
     while (ix >= 0)
     {
       int iy = buf.indexOf("%", ix + 1);
       if (iy >= 0)
       {
         String var = buf.substring(ix + 1, iy);
         
         String[] values = var.split("\\|");
         String defaultValue = null;
         int len = values.length;
         if (len > 1) {
           defaultValue = values[(--len)];
         }
         String value = null;
         for (int i = 0; i < len; i++)
         {
           String key = values[i];
           value = System.getenv(key);
           if ((value == null) && (symbols != null)) {
             value = (String)symbols.get(key);
           }
           if (value != null) {
             break;
           }
         }
         if (value != null)
         {
           buf.replace(ix, iy + 1, value);
         }
         else if (defaultValue != null)
         {
           buf.replace(ix, iy + 1, defaultValue);
         }
         else
         {
           buf.deleteCharAt(iy);
           buf.deleteCharAt(ix);
         }
       }
       ix = buf.indexOf("%");
     }
     return buf.toString();
   }
   
   public static String changeJavaNameToWords(String input)
   {
     if ((input != null) && (!input.isEmpty()))
     {
       StringBuilder buf = new StringBuilder();
       buf.append(Character.toTitleCase(input.charAt(0)));
       CharSequence seq = input.subSequence(1, input.length());
       for (int i = 0; i < seq.length(); i++)
       {
         char ch = seq.charAt(i);
         if (Character.isUpperCase(ch))
         {
           buf.append(' ');
           buf.append(Character.toLowerCase(ch));
         }
         else
         {
           buf.append(ch);
         }
       }
       return buf.toString();
     }
     return input;
   }
 }

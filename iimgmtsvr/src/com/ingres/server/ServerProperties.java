 package com.ingres.server;
 
 import com.ingres.util.CharUtil;
 import com.ingres.util.Logging;
 import com.ingres.util.SecurityUtil;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.Enumeration;
 import java.util.Map;
 import java.util.Properties;
 
 public class ServerProperties
 {
   private static Properties props = new Properties();
   public static final String FILE_NAME = "server.properties";
   public static final String RESOURCE_NAME = "com/ingres/server/server.properties";
   
   public static enum Property
   {
     ServerConnectLimit,  MonitorStatisticsInterval,  RespondToHTTP;
     
     private Property() {}
   }
   
   public static boolean load()
   {
     return load((Map)null);
   }
   
   public static boolean load(Map<String, String> symbols)
   {
     Logging.Debug("ServerProperties.load()", new Object[0]);
     InputStream is = ServerProperties.class.getClassLoader().getResourceAsStream("com/ingres/server/server.properties");
     return load(is, symbols);
   }
   
   public static boolean load(File f)
   {
     return load(f, null);
   }
   
   public static boolean load(File f, Map<String, String> symbols)
   {
     try
     {
       return load(new FileInputStream(f), symbols);
     }
     catch (FileNotFoundException fnfe)
     {
       Logging.Error("Input file '%1$s' was not found to load properties from!%n", new Object[] { f.getPath() });
     }
     return false;
   }
   
   public static boolean load(InputStream is)
   {
     return load(is, null);
   }
   
   public static boolean load(InputStream is, Map<String, String> symbols)
   {
     try
     {
       props.load(is);
       is.close();
       Logging.Debug("ServerProperties: done with props load", new Object[0]);
       
       //@modify zhengxb
       for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements();)
       {
         String p = (String)e.nextElement();
         try
         {
           if (p.indexOf('.') >= 0)
           {
             String v = (String)props.remove(p);
             Logging.Debug("ServerProperties: setting system property '%1$s' to '%2$s'...", new Object[] { p, v });
             if (v.startsWith("OBF:")) {
               v = SecurityUtil.deobfuscate(v);
             }
             System.setProperty(p, CharUtil.substituteEnvValue(v, symbols));
           }
           else
           {
             Property pn = (Property)Enum.valueOf(Property.class, p);
             String v = props.getProperty(p);
             if (v.startsWith("OBF:")) {
               v = SecurityUtil.deobfuscate(v);
             }
             props.setProperty(p, CharUtil.substituteEnvValue(v, symbols));
           }
         }
         catch (IllegalArgumentException iae)
         {
           Logging.Error("Server properties key '%1$s' is not a known one, removing it.%n", new Object[] { p });
           props.remove(p);
         }
       }
     }
     catch (IOException ioe)
     {
       Enumeration<?> e;
       Logging.Except("Loading Server properties", ioe);
       return false;
     }
     Logging.Debug("ServerProperties: done setting local properties in 'load'", new Object[0]);
     return true;
   }
   
   public static String getValue(Property p)
   {
     return props.getProperty(p.name());
   }
   
   public static String getValue(Property p, String defaultValue)
   {
     return props.getProperty(p.name(), defaultValue);
   }
   
   public static int getIntValue(Property p)
   {
     String v = props.getProperty(p.name());
     if (v == null)
     {
       Logging.Error("Value of '%1$s' property is null!", new Object[] { p.name() });
       return 0;
     }
     try
     {
       return Integer.parseInt(v);
     }
     catch (NumberFormatException nfe)
     {
       Logging.Error("Value of '%1$s' property (\"%2$s\") should be a number and is not!", new Object[] { p.name(), v });
     }
     return 0;
   }
   
   public static int getIntValue(Property p, int defaultValue)
   {
     String v = props.getProperty(p.name());
     if (v == null) {
       return defaultValue;
     }
     try
     {
       return Integer.parseInt(v);
     }
     catch (NumberFormatException nfe)
     {
       Logging.Error("Value of '%1$s' property (\"%2$s\") should be a number and is not!", new Object[] { p.name(), v });
     }
     return 0;
   }
   
   public static boolean getBooleanValue(Property p)
   {
     String v = props.getProperty(p.name());
     if (v == null)
     {
       Logging.Error("Value of '%1$s' property is null!", new Object[] { p.name() });
       return false;
     }
     if ((v.equalsIgnoreCase(Boolean.TRUE.toString())) || (v.equalsIgnoreCase("yes")) || (v.equals("1"))) {
       return true;
     }
     if ((v.equalsIgnoreCase(Boolean.FALSE.toString())) || (v.equalsIgnoreCase("no")) || (v.equals("0"))) {
       return false;
     }
     Logging.Error("Value of '%1$s' property (\"%2$s\") should be a boolean value and is not!", new Object[] { p.name(), v });
     return false;
   }
   
   public static boolean getBooleanValue(Property p, boolean defaultValue)
   {
     String v = props.getProperty(p.name());
     if (v == null) {
       return defaultValue;
     }
     if ((v.equalsIgnoreCase(Boolean.TRUE.toString())) || (v.equalsIgnoreCase("yes")) || (v.equals("1"))) {
       return true;
     }
     if ((v.equalsIgnoreCase(Boolean.FALSE.toString())) || (v.equalsIgnoreCase("no")) || (v.equals("0"))) {
       return false;
     }
     Logging.Error("Value of '%1$s' property (\"%2$s\") should be a boolean value and is not!", new Object[] { p.name(), v });
     return false;
   }
 }
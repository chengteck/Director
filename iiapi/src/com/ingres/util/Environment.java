 package com.ingres.util;
 
 import java.io.File;
 import java.lang.management.ManagementFactory;
 import java.lang.management.RuntimeMXBean;
 import java.util.Locale;
 import java.util.concurrent.TimeUnit;
 
 public class Environment
 {
   private static final String USER_NAME = System.getProperty("user.name");
   private static final String USER_DIR = System.getProperty("user.dir");
   private static final String USER_HOME = System.getProperty("user.home");
   private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
   private static final String OS_VERSION = System.getProperty("os.version");
   private static final String JAVA_RUNTIME_VERSION = System.getProperty("java.runtime.version");
   private static final String LINE_SEPARATOR = System.getProperty("line.separator");
   private static final int DATA_MODEL = Integer.parseInt(System.getProperty("sun.arch.data.model"));
   public static final int DATA_MODEL_32 = 32;
   public static final int DATA_MODEL_64 = 64;
   private static File currentDirectory = null;
   private static boolean runningAsDesktop = false;
   private static boolean osIsWindows = OS_NAME.startsWith("windows");
   private static boolean osIsLinux = OS_NAME.startsWith("linux");
   private static TimeUnit timeUnit = osIsLinux ? TimeUnit.MILLISECONDS : TimeUnit.NANOSECONDS;
   
   public static String currentUser()
   {
     return USER_NAME;
   }
   
   public static File userDirectory()
   {
     return new File(USER_DIR);
   }
   
   public static File currentDirectory()
   {
     if (currentDirectory != null) {
       return currentDirectory;
     }
     return userDirectory();
   }
   
   public static void setCurrentDirectory(File dir)
   {
     currentDirectory = dir;
   }
   
   public static File userHomeDir()
   {
     return new File(USER_HOME);
   }
   
   public static boolean isWindows()
   {
     return osIsWindows;
   }
   
   public static boolean isOSX()
   {
     return OS_NAME.startsWith("mac os x");
   }
   
   public static boolean isLinux()
   {
     return osIsLinux;
   }
   
   public static String osVersion()
   {
     return OS_VERSION;
   }
   
   public static boolean isDesktopApp()
   {
     return runningAsDesktop;
   }
   
   public static void setDesktopApp(boolean desktop)
   {
     runningAsDesktop = desktop;
   }
   
   public static String javaVersion()
   {
     return JAVA_RUNTIME_VERSION;
   }
   
   public static String hostName()
   {
     String hostName = null;
     if (osIsWindows) {
       hostName = System.getenv("COMPUTERNAME");
     } else {
       hostName = System.getenv("HOSTNAME");
     }
     if ((hostName == null) || (hostName.isEmpty())) {
       hostName = NetworkUtil.getLocalHostName();
     }
     return hostName;
   }
   
   public static String lineSeparator()
   {
     return LINE_SEPARATOR;
   }
   
   public static int dataModel()
   {
     if ((DATA_MODEL == 32) || (DATA_MODEL == 64)) {
       return DATA_MODEL;
     }
     throw new IllegalStateException(String.format("Java system has an unexpected data model value: %1$d", new Object[] { Integer.valueOf(DATA_MODEL) }));
   }
   
   public static long highResTimer()
   {
     if (osIsLinux) {
       return System.currentTimeMillis();
     }
     return System.nanoTime();
   }
   
   public static long highResTimerResolution()
   {
     return timeUnit.convert(1L, TimeUnit.SECONDS);
   }
   
   public static TimeUnit highResTimeUnit()
   {
     return timeUnit;
   }
   
   public static int highResTimeScaleFactor()
   {
     long value = highResTimerResolution();
     int scale = 0;
     while (value > 1L)
     {
       value /= 10L;
       scale++;
     }
     return scale;
   }
   
   public static long getProcessID()
   {
     String jvmName = ManagementFactory.getRuntimeMXBean().getName();
     
     int index = jvmName.indexOf('@');
     if (index > 0) {
       try
       {
         return Long.parseLong(jvmName.substring(0, index));
       }
       catch (NumberFormatException e) {}
     }
     return 0L;
   }
 }

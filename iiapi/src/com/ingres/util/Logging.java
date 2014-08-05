 package com.ingres.util;
 
 import com.ingres.IIapi;
 import com.ingres.IIapi.Exception;
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.PrintStream;
 import java.text.DateFormat;
 import java.text.FieldPosition;
 import java.text.SimpleDateFormat;
 import java.util.Calendar;
 import java.util.Map;
 import java.util.TimeZone;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public class Logging
 {
   public static final int DEBUG = 5;
   public static final int INFO = 4;
   public static final int WARN = 3;
   public static final int ERROR = 2;
   public static final int FATAL = 1;
   public static final int OFF = 0;
   private static final String LEVEL_DEBUG = "DEBUG";
   private static final String LEVEL_INFO = " INFO";
   private static final String LEVEL_WARN = " WARN";
   private static final String LEVEL_ERROR = "ERROR";
   private static final String LEVEL_FATAL = "FATAL";
   private static final String LEVEL_OFF = "  OFF";
   private static int loggingLevel = 1;
   private static boolean logToConsole = false;
   private static final String DATE_TOKEN = "{DATE}";
   private static TimeZone gmt = null;
   private static Calendar currentCal = null;
   private static boolean dailyRollingLog = false;
   private static DateFormat fmt = null;
   private static DateFormat dayfmt = null;
   private static String logFileDir = null;
   private static String logFileTemplate = null;
   private static PrintStream logStrm = null;
   private static String className = Logging.class.getName() + "";
   private static final String LOGGING_LEVEL = "LoggingLevel";
   private static final String LOG_DIRECTORY = "LogDirectory";
   private static final String LOG_FILE = "LogFile";
   private static final String LOG_TO_CONSOLE = "LogToConsole";
   private static Pattern comments = Pattern.compile("^\\s*[#!].*");
   private static Pattern parser = Pattern.compile("^\\s*(\\S+)\\s*=\\s*(.*)$");
   private String prefix = null;
   
   static
   {
     gmt = TimeZone.getTimeZone("Etc/GMT");
     fmt = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss.SSS z");
     dayfmt = new SimpleDateFormat("yyyy-MM-dd");
     currentCal = Calendar.getInstance(gmt);
     fmt.setCalendar(currentCal);
     dayfmt.setCalendar(currentCal);
   }
   
   public Logging(Class clazz)
   {
     this(clazz.getSimpleName());
   }
   
   public Logging(String ident)
   {
     this.prefix = ident;
   }
   
   public void log(int level, String fmt, Object... args)
   {
     if (loggingLevel >= level) {
       LogStream(System.out, level, this.prefix, String.format(fmt, args), logToConsole);
     }
   }
   
   public void logError(int level, String fmt, Object... args)
   {
     if (loggingLevel >= level) {
       LogStream(System.err, level, this.prefix, String.format(fmt, args), true);
     }
   }
   
   public void debug(String fmt, Object... args)
   {
     log(5, fmt, args);
   }
   
   public void info(String fmt, Object... args)
   {
     log(4, fmt, args);
   }
   
   public void warn(String fmt, Object... args)
   {
     log(3, fmt, args);
   }
   
   public void error(String fmt, Object... args)
   {
     logError(2, fmt, args);
   }
   
   public void fatal(String fmt, Object... args)
   {
     logError(1, fmt, args);
   }
   
   public void except(Throwable e)
   {
     String exceptClassName = e.getClass().getName();
     String exceptMsg = e.getMessage();
     if (exceptMsg == null) {
       logError(2, exceptClassName, new Object[0]);
     } else {
       logError(2, "%1$s: %2$s", new Object[] { exceptClassName, exceptMsg });
     }
     if ((e instanceof IIapi.Exception))
     {
       IIapi.Exception next = ((IIapi.Exception)e).getNextException();
       while (next != null)
       {
         logError(2, next.toString(), new Object[0]);
         next = next.getNextException();
       }
     }
     if (logStrm != null) {
       synchronized (logStrm)
       {
         e.printStackTrace(logStrm);
       }
     }
   }
   
   public void except(String expl, Throwable e)
   {
     String exceptClassName = e.getClass().getName();
     String exceptMsg = e.getMessage();
     logError(2, "%1$s: %2$s", new Object[] { expl, exceptMsg == null ? exceptClassName : exceptMsg });
     if ((e instanceof IIapi.Exception))
     {
       IIapi.Exception next = ((IIapi.Exception)e).getNextException();
       while (next != null)
       {
         logError(2, next.toString(), new Object[0]);
         next = next.getNextException();
       }
     }
     if (logStrm != null) {
       synchronized (logStrm)
       {
         e.printStackTrace(logStrm);
       }
     }
   }
   
   public static boolean setLoggingLevel(String level)
   {
     try
     {
       int lev = Integer.parseInt(level);
       if ((lev >= 0) && (lev <= 5))
       {
         loggingLevel = lev;
       }
       else
       {
         System.err.println("Invalid format for logging level value: " + level);
         return false;
       }
     }
     catch (NumberFormatException nfe)
     {
       if (level.equalsIgnoreCase("DEBUG"))
       {
         loggingLevel = 5;
       }
       else if (level.equalsIgnoreCase(" INFO".trim()))
       {
         loggingLevel = 4;
       }
       else if (level.equalsIgnoreCase(" WARN".trim()))
       {
         loggingLevel = 3;
       }
       else if (level.equalsIgnoreCase("ERROR"))
       {
         loggingLevel = 2;
       }
       else if (level.equalsIgnoreCase("FATAL"))
       {
         loggingLevel = 1;
       }
       else if (level.equalsIgnoreCase("  OFF".trim()))
       {
         loggingLevel = 0;
       }
       else
       {
         System.err.println("Invalid format for logging level value: " + level);
         return false;
       }
     }
     return true;
   }
   
   public static void setConsoleLogging(boolean logConsole)
   {
     logToConsole = logConsole;
   }
   
   public static String getLoggingLevel(int level)
   {
     switch (level)
     {
     case 0: 
       return "  OFF";
     case 1: 
       return "FATAL";
     case 2: 
       return "ERROR";
     case 3: 
       return " WARN";
     case 4: 
       return " INFO";
     case 5: 
       return "DEBUG";
     }
     return "";
   }
   
   public static String getLoggingLevel()
   {
     return getLoggingLevel(loggingLevel);
   }
   
   public static boolean isLevelEnabled(int level)
   {
     if ((level >= 0) && (level <= 5)) {
       return loggingLevel >= level;
     }
     return false;
   }
   
   public static boolean isLoggingEnabled()
   {
     return isLevelEnabled(1);
   }
   
   public static boolean isWarnEnabled()
   {
     return isLevelEnabled(3);
   }
   
   public static boolean isInfoEnabled()
   {
     return isLevelEnabled(4);
   }
   
   public static boolean isDebugEnabled()
   {
     return isLevelEnabled(5);
   }
   
   public static boolean isErrorEnabled()
   {
     return isLevelEnabled(2);
   }
   
   public static Calendar formatTime(StringBuffer buf)
   {
     Calendar now = Calendar.getInstance(gmt);
     synchronized (fmt)
     {
       fmt.format(now.getTime(), buf, new FieldPosition(3));
     }
     buf.delete(buf.length() - 6, buf.length());
     return now;
   }
   
   public static void LogStream(PrintStream ps, int level, String prefix, String stmt, boolean logConsole)
   {
     StringBuffer buf = new StringBuffer();
     Calendar now = formatTime(buf);
     if ((dailyRollingLog) && 
       (now.get(6) != currentCal.get(6)))
     {
       currentCal = now;
       setLogFile(logFileDir, logFileTemplate);
     }
     Thread curThread = Thread.currentThread();
     buf.append(" T[").append(curThread.getName()).append(']');
     buf.append('(').append(curThread.getId()).append(')');
     if ((level >= 0) && (level <= 5)) {
       buf.append(' ').append(getLoggingLevel(level));
     }
     if ((prefix != null) && (!prefix.isEmpty())) {
       buf.append(' ').append(prefix);
     }
     buf.append(": ").append(stmt);
     String output = buf.toString();
     if (logConsole) {
       synchronized (ps)
       {
         ps.println(output);
       }
     }
     if (logStrm != null) {
       synchronized (logStrm)
       {
         logStrm.println(output);
       }
     }
     output = null;
     buf = null;
     now = null;
   }
   
   public static void Log(int level, String fmt, Object... args)
   {
     if (loggingLevel >= level) {
       LogStream(System.out, level, null, String.format(fmt, args), logToConsole);
     }
   }
   
   public static void LogError(int level, String fmt, Object... args)
   {
     if (loggingLevel >= level) {
       LogStream(System.err, level, null, String.format(fmt, args), true);
     }
   }
   
   public static void Debug(String fmt, Object... args)
   {
     Log(5, fmt, args);
   }
   
   public static void Info(String fmt, Object... args)
   {
     Log(4, fmt, args);
   }
   
   public static void Warn(String fmt, Object... args)
   {
     Log(3, fmt, args);
   }
   
   public static void Error(String fmt, Object... args)
   {
     LogError(2, fmt, args);
   }
   
   public static void Fatal(String fmt, Object... args)
   {
     LogError(1, fmt, args);
   }
   
   public static void Except(Throwable e)
   {
     String exceptClassName = e.getClass().getName();
     String exceptMsg = e.getMessage();
     if (exceptMsg == null) {
       LogError(2, exceptClassName, new Object[0]);
     } else {
       LogError(2, "%1$s: %2$s", new Object[] { exceptClassName, exceptMsg });
     }
     if ((e instanceof IIapi.Exception))
     {
       IIapi.Exception next = ((IIapi.Exception)e).getNextException();
       while (next != null)
       {
         LogError(2, next.toString(), new Object[0]);
         next = next.getNextException();
       }
     }
     if (logStrm != null) {
       synchronized (logStrm)
       {
         e.printStackTrace(logStrm);
       }
     }
   }
   
   public static void Except(String expl, Throwable e)
   {
     String exceptClassName = e.getClass().getName();
     String exceptMsg = e.getMessage();
     LogError(2, "%1$s: %2$s", new Object[] { expl, exceptMsg == null ? exceptClassName : exceptMsg });
     if ((e instanceof IIapi.Exception))
     {
       IIapi.Exception next = ((IIapi.Exception)e).getNextException();
       while (next != null)
       {
         LogError(2, next.toString(), new Object[0]);
         next = next.getNextException();
       }
     }
     if (logStrm != null) {
       synchronized (logStrm)
       {
         e.printStackTrace(logStrm);
       }
     }
   }
   
   public static void setLogFile(String logDirectory, String logFile)
   {
     logFileDir = logDirectory;
     logFileTemplate = logFile;
     
 
     File logDir = new File(logDirectory);
     if ((logDir.exists()) || (logDir.mkdirs()))
     {
       StringBuilder buf = new StringBuilder(logFile);
       int ix = buf.indexOf("{DATE}");
       if (ix >= 0)
       {
         dailyRollingLog = true;
         synchronized (dayfmt)
         {
           buf.replace(ix, ix + "{DATE}".length(), dayfmt.format(currentCal.getTime()));
         }
         logFile = buf.toString();
       }
       File logF = new File(logDir, logFile);
       try
       {
         if (!logF.exists()) {
           logF.createNewFile();
         }
         logStrm = new PrintStream(new FileOutputStream(logF, true), true);
       }
       catch (IOException ioe)
       {
         System.err.println("IO Exception trying to create log file! " + ioe.toString());
       }
     }
   }
   
   public static void readConfiguration(InputStream is, Map<String, String> symbols)
   {
     String logDir = null;
     String logFile = null;
     BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
     String line = null;
     try
     {
       while ((line = rdr.readLine()) != null)
       {
         Matcher m = comments.matcher(line);
         if (!m.matches())
         {
           m = parser.matcher(line);
           if (m.matches())
           {
             String name = m.group(1);
             if (name.startsWith(className))
             {
               name = name.substring(className.length());
               String value = CharUtil.substituteEnvValue(m.group(2), symbols);
               if (name.equalsIgnoreCase("LoggingLevel")) {
                 setLoggingLevel(value);
               } else if (name.equalsIgnoreCase("LogDirectory")) {
                 logDir = value;
               } else if (name.equalsIgnoreCase("LogFile")) {
                 logFile = value;
               } else if (name.equalsIgnoreCase("LogToConsole")) {
                 setConsoleLogging(Boolean.parseBoolean(value));
               }
             }
           }
         }
       }
     }
     catch (IOException ioe)
     {
       System.err.format("I/O Exception reading configuration: %1$s!%n", new Object[] { ioe.getMessage() });
     }
     if ((logDir != null) && (logFile != null)) {
       setLogFile(logDir, logFile);
     }
   }
   
   public static void readConfiguration(Object obj, Map<String, String> symbols)
   {
     try
     {
       FileInputStream fis = null;
       if ((obj instanceof File)) {
         fis = new FileInputStream((File)obj);
       } else if ((obj instanceof String)) {
         fis = new FileInputStream((String)obj);
       } else {
         fis = new FileInputStream(obj.toString());
       }
       readConfiguration(fis, symbols);
       fis.close();
     }
     catch (IOException ioe)
     {
       System.err.format("Unable to read configuration from file: %1$s!%n", new Object[] { ioe.getMessage() });
     }
   }
   
   public static void stopLogging()
   {
     if (logStrm != null)
     {
       logStrm.flush();
       logStrm.close();
     }
   }
 }

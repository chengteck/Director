 package com.ingres.util;
 
 //import java.io.PrintStream;
 import java.util.HashMap;
 import java.util.Locale;
 import java.util.Map;
 import java.util.MissingResourceException;
 import java.util.ResourceBundle;
 
 public class Intl
 {
   private static Map<String, Provider> providerMap = new HashMap<String, Provider>();
   
   public static abstract interface Provider
   {
     public abstract Object getSource();
     
     public abstract Locale getLocale();
     
     public abstract String getString(String paramString);
   }
   
   public static class ResourceBundleProvider
     implements Intl.Provider
   {
     protected ResourceBundle resources;
     protected Locale locale;
     protected String name;
     
     protected ResourceBundleProvider() {}
     
     private ResourceBundleProvider(String name, Locale locale, ClassLoader classLoader)
     {
       this.locale = locale;
       this.name = name;
       Intl.providerMap.put(name, this);
       updateResources(name, locale, classLoader);
     }
     
     protected void updateResources(String name, Locale locale, ClassLoader classLoader)
     {
       Intl.log.debug("Loading resource bundle for '%1$s', locale %2$s...", new Object[] { name, locale.getDisplayName() });
       this.resources = ResourceBundle.getBundle(String.format("%1$s.resources", new Object[] { name }), locale, classLoader);
     }
     
     protected static void install(String name, Locale locale, ClassLoader classLoader)
     {
       Intl.log.info("Installing resource bundle for '%1$s', locale %2$s...", new Object[] { name, locale.getDisplayName() });
       ResourceBundleProvider provider = (ResourceBundleProvider)Intl.providerMap.get(name);
       if (provider == null) {
         new ResourceBundleProvider(name, locale, classLoader);
       } else if (!provider.getLocale().equals(locale)) {
         provider.updateResources(name, locale, classLoader);
       }
     }
     
     public Object getSource()
     {
       return this.resources;
     }
     
     public Locale getLocale()
     {
       return this.locale;
     }
     
     public String getString(String resourceName)
     {
       try
       {
         Object obj = this.resources.getObject(resourceName);
         if ((obj instanceof String))
         {
           String string = (String)obj;
           //int len; //todo
			int len = 0;
           if (!string.isEmpty())
           {
             len = string.length();
             if ((string.charAt(0) != '"') || (string.charAt(len - 1) != '"')) {}
           }
           return string.substring(1, len - 1);
         }
         if (obj == null)
         {
           Intl.log.error("getString: Unknown string resource: '%1$s'", new Object[] { resourceName });
           return resourceName;
         }
         return obj.toString();
       }
       catch (MissingResourceException mre)
       {
         Intl.log.except(resourceName, mre);
       }
       return resourceName;
     }
   }
   
   public static class PackageResourceProvider
     extends Intl.ResourceBundleProvider
   {
     public static void install(Package pkg, Locale locale)
     {
       install(pkg.getName(), locale, ClassLoader.getSystemClassLoader());
     }
   }
   
   private static Provider defaultProvider = null;
   private static Logging log = new Logging(Intl.class);
   
   public static Provider getProvider(Package pkg)
   {
     return (Provider)providerMap.get(pkg.getName());
   }
   
   public static Object getSource()
   {
     return defaultProvider.getSource();
   }
   
   public static Object getSource(Package pkg)
   {
     return getProvider(pkg).getSource();
   }
   
   public static Locale getLocale()
   {
     return defaultProvider.getLocale();
   }
   
   public static Locale getLocale(Package pkg)
   {
     return getProvider(pkg).getLocale();
   }
   
   public static void initResources(Provider provider)
   {
     defaultProvider = provider;
   }
   
   public static void initResources(Package pkg, Locale locale)
   {
     PackageResourceProvider.install(pkg, locale);
   }
   
   public static String getString(String resourceName)
   {
     return defaultProvider.getString(resourceName);
   }
   
   public static String getString(String object, String key)
   {
     return defaultProvider.getString(String.format("%1$s.%2$s", new Object[] { object, key }));
   }
   
   public static String formatString(String formatKey, Object... args)
   {
     String format = defaultProvider.getString(formatKey);
     return String.format(format, args);
   }
   
   public static String getKeyString(String key)
   {
     if ((key != null) && (key.length() > 1) && (key.charAt(0) == '%')) {
       return defaultProvider.getString(key.substring(1));
     }
     return key;
   }
   
   public static String getString(Package pkg, String resourceName)
   {
     return getProvider(pkg).getString(resourceName);
   }
   
   public static String getString(Package pkg, String object, String key)
   {
     return getProvider(pkg).getString(String.format("%1$s.%2$s", new Object[] { object, key }));
   }
   
   public static String formatString(Package pkg, String formatKey, Object... args)
   {
     String format = getProvider(pkg).getString(formatKey);
     return String.format(format, args);
   }
   
   public static String getKeyString(Package pkg, String key)
   {
     if ((key != null) && (key.length() > 1) && (key.charAt(0) == '%')) {
       return getProvider(pkg).getString(key.substring(1));
     }
     return key;
   }
   
   public static void outPrintln(Package pkg, String key)
   {
     System.out.println(getString(pkg, key));
   }
   
   public static void errPrintln(Package pkg, String key)
   {
     System.err.println(getString(pkg, key));
   }
   
   public static void outFormat(Package pkg, String formatKey, Object... args)
   {
     System.out.println(formatString(pkg, formatKey, args));
   }
   
   public static void errFormat(Package pkg, String formatKey, Object... args)
   {
     System.err.println(formatString(pkg, formatKey, args));
   }
   
   public static void printHelp(Package pkg, String prefix)
   {
     int numLines = Integer.valueOf(getString(pkg, prefix, "helpNumberLines")).intValue();
     for (int line = 1; line <= numLines; line++) {
       System.out.println(getString(pkg, prefix, String.format("help%1$d", new Object[] { Integer.valueOf(line) })));
     }
   }
 }
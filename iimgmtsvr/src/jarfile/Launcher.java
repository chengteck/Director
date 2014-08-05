 package jarfile;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.URL;
 import java.security.MessageDigest;
 import java.security.NoSuchAlgorithmException;
 import java.util.jar.Attributes;
 import java.util.jar.Attributes.Name;
 import java.util.jar.JarFile;
 import java.util.jar.Manifest;
 
 /**
  * 该类main()方法会被remote manager service的execute_java_cpp调用执行
  * @author zheng
  *
  */
 public class Launcher
 {
   private static void throwException(Throwable cause, String msg, Object... args)
   {
     String fullMessage = String.format(msg, args);
     throw new RuntimeException(fullMessage, cause);
   }
   
   private static File getOurJarFile()
   {
     try
     {
       Class c = Launcher.class;
       URL classURL = c.getResource(c.getSimpleName() + ".class");
       String classURLString = classURL.toString();
       if (!classURLString.startsWith("file:"))
       {
         String f = classURL.getFile();
         
         int ix = f.indexOf(".jar!");
         if (ix >= 0) {
           classURLString = f.substring(0, ix + 4);
         } else {
           classURLString = f;
         }
       }
       return new File(new URI(classURLString));
     }
     catch (URISyntaxException use) {}
     return null;
   }
   
   private static void checkChecksum(File jarFile, String checksum)
     throws IllegalAccessException, IOException, NoSuchAlgorithmException
   {
     MessageDigest __md = MessageDigest.getInstance("MD5");
     __md.reset();
     FileInputStream fis = null;
     try
     {
       fis = new FileInputStream(jarFile);
       byte[] bytes = new byte[4096];
       int len;
       while ((len = fis.read(bytes)) != -1) {
         __md.update(bytes, 0, len);
       }
     }
     finally
     {
       if (fis != null) {
         fis.close();
       }
     }
     byte[] result = __md.digest();
     
 
     String[] checkBytes = checksum.split("!");
     if (result.length != checkBytes.length) {
       throw new RuntimeException("Checksum length is incorrect.");
     }
     for (int i = 0; i < result.length; i++) {
       if (result[i] != (byte)Integer.parseInt(checkBytes[i], 16)) {
         throw new RuntimeException(String.format("Checksum does not match: byte %1$d, %2$02x != %3$s", new Object[] { Integer.valueOf(i), Byte.valueOf(result[i]), checkBytes[i] }));
       }
     }
   }
   
   public static void main(String checksum, String mainClassName, String[] args)
   {
     File ourJarFile = getOurJarFile();
     try
     {
       checkChecksum(ourJarFile, checksum);
       if ((mainClassName == null) || (mainClassName.isEmpty()))
       {
         JarFile jarFile = new JarFile(ourJarFile);
         Manifest manifest = jarFile.getManifest();
         Attributes attributes = manifest.getMainAttributes();
         mainClassName = attributes.getValue(Attributes.Name.MAIN_CLASS);
       }
       Class<?> c = Class.forName(mainClassName);
       Method main = c.getMethod("main", new Class[] { new String[0].getClass() });
       Class<?>[] types = main.getParameterTypes();
       main.invoke(null, new Object[] { types[0].cast(args) });
     }
     catch (NoSuchAlgorithmException nsae)
     {
       throwException(nsae, "Unable to verify the integrity of '%1$s' file", new Object[] { ourJarFile.getPath() });
     }
     catch (IllegalAccessException iae)
     {
       throwException(iae, "Unable to access 'main' method in class '%1$s'", new Object[] { mainClassName });
     }
     catch (InvocationTargetException ite)
     {
       throwException(ite, "Exception occurred in 'main' method in class '%1$s'", new Object[] { mainClassName });
     }
     catch (NoSuchMethodException nsme)
     {
       throwException(nsme, "Unable to find 'main' method in class '%1$s'", new Object[] { mainClassName });
     }
     catch (ClassNotFoundException cnfe)
     {
       throwException(cnfe, "Unable to find main class '%1$s'", new Object[] { mainClassName });
     }
     catch (IOException ioe)
     {
       throwException(ioe, "Unable to open jar file '%1$s'", new Object[] { ourJarFile.getPath() });
     }
   }
 }

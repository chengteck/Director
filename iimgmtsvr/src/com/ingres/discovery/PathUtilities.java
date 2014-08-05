 package com.ingres.discovery;
 
 import com.ingres.util.Environment;
 import java.io.File;
 import java.io.IOException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.URL;
 
 /**
  * 路径工具
  * @author Ingres
  *　用于获取程序命令等的路径
  */
 public class PathUtilities
 {
	
	/**
	 * 获取程序路径
	 * @return
	 */
	public static File getProgramPath() {
		try {
			Class c = PathUtilities.class;
			URL classURL = c.getResource(c.getSimpleName() + ".class");
			String classURLString = classURL.toString();
			if (!classURLString.startsWith("file:")) {
				String f = classURL.getFile();

				int ix = f.indexOf(".jar!");
				if (ix >= 0) {
					classURLString = f.substring(0, ix + 4);
				} else {
					classURLString = f;
				}
			}
			File classFile = new File(new URI(classURLString));
			return classFile.getParentFile();
		} catch (URISyntaxException use) {
		}
		return null;
	}

	/**
	 * 获取IISystem路径 .../ingres/ 
	 * @return
	 */
   	public static File getIISystem()
   	{
   		File II_SYSTEM = null;
   		File ourPath = getProgramPath();
   		if (ourPath != null)
   		{
   			String lastName = ourPath.getName();
   			if ((lastName.equalsIgnoreCase("bin")) || (lastName.equalsIgnoreCase("lib")) || (lastName.equalsIgnoreCase("utility")))
   			{
   				File ourParent = ourPath.getParentFile();
   				lastName = ourParent.getName();
   				if (lastName.equalsIgnoreCase("ingres")) {
   					II_SYSTEM = ourParent.getParentFile();
   				}
   			}
   		}
   		return II_SYSTEM;
   	}
   
   private static File findUtility(File basePath, String commandName)
   {
     File fullPath = new File(basePath, commandName);
     if ((fullPath.exists()) && (fullPath.canExecute())) {
       return fullPath;
     }
     if (Environment.isWindows())
     {
       String exts = System.getenv("PATHEXT");
       if (exts != null)
       {
         String[] extensions = exts.split(";");
         for (String ext : extensions)
         {
           fullPath = new File(basePath, String.format("%1$s%2$s", new Object[] { commandName, ext }));
           if ((fullPath.exists()) && (fullPath.canExecute())) {
             try
             {
               return fullPath.getCanonicalFile();
             }
             catch (IOException ioe)
             {
               return null;
             }
           }
         }
       }
     }
     return null;
   }
   
   public static File getUtilityPath(File iiSystem, String commandName)
   {
     File fullPath = null;
     if (iiSystem != null)
     {
       File basePath = new File(iiSystem, "ingres");
       File testPath = new File(basePath, "bin");
       if ((fullPath = findUtility(testPath, commandName)) == null)
       {
         testPath = new File(basePath, "utility");
         if ((fullPath = findUtility(testPath, commandName)) == null)
         {
           testPath = new File(basePath, "files");
           fullPath = findUtility(testPath, commandName);
         }
       }
     }
     return fullPath;
   }
 }
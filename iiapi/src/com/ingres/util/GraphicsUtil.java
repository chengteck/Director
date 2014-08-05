 package com.ingres.util;
 
 import java.awt.Font;
 import java.awt.FontMetrics;
 import java.awt.Graphics2D;
 import java.awt.GraphicsConfiguration;
 import java.awt.GraphicsDevice;
 import java.awt.GraphicsEnvironment;
 import java.awt.image.BufferedImage;
 import java.util.LinkedHashSet;
 import java.util.Set;
 import java.util.concurrent.Semaphore;
 
 public class GraphicsUtil
 {
   private static GraphicsEnvironment ge = null;
   private static GraphicsDevice gd = null;
   private static GraphicsConfiguration gc = null;
   private static BufferedImage fontImage = null;
   private static Graphics2D fontGraphics = null;
   public static final String DEFAULT_FONT_FAMILY_NAME = "Monospaced";
   private static Semaphore fontCacheSemaphore = null;
   private static Set<String> disallowedFontNames = null;
   public static Set<String> monospacedFontFamilyNames = null;
   public static Set<String> fontFamilyNames = null;
   private static final char[] codePoints = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
   
   static
   {
     ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
     gd = ge.getDefaultScreenDevice();
     gc = gd.getDefaultConfiguration();
     
     fontImage = gc.createCompatibleImage(10, 10);
     fontGraphics = fontImage.createGraphics();
     
     disallowedFontNames = new LinkedHashSet<String>(5);
     disallowedFontNames.add("Webdings");
     disallowedFontNames.add("Wingdings");
     disallowedFontNames.add("Monospaced");
     disallowedFontNames.add("DialogInput");
     disallowedFontNames.add("Symbol");
   }
   
   public static BufferedImage createOverlayImage(BufferedImage baseImage, BufferedImage overlayImage)
   {
     BufferedImage resultImage = gc.createCompatibleImage(baseImage.getWidth(), baseImage.getHeight(), 3);
     Graphics2D graphics = resultImage.createGraphics();
     graphics.drawImage(baseImage, 0, 0, null);
     graphics.drawImage(overlayImage, 0, 0, null);
     return resultImage;
   }
   
   public static boolean isFontValid(String name)
   {
     if (name.equalsIgnoreCase("Monospaced")) {
       return true;
     }
     Font font = Font.decode(name);
     return (font != null) && (font.getFamily().equalsIgnoreCase(name));
   }
   
   private static boolean isFontMonospaced(Font font)
   {
     boolean isMonospaced = true;
     
     FontMetrics fontMetrics = fontGraphics.getFontMetrics(font);
     int firstCharacterWidth = fontMetrics.charWidth(codePoints[0]);
     for (char ch : codePoints)
     {
       int characterWidth = fontMetrics.charWidth(ch);
       if (characterWidth != firstCharacterWidth)
       {
         isMonospaced = false;
         break;
       }
     }
     return isMonospaced;
   }
   
   public static boolean isFontMonospaced(String name)
   {
     if ((monospacedFontFamilyNames != null) && (!monospacedFontFamilyNames.isEmpty())) {
       return monospacedFontFamilyNames.contains(name);
     }
     return isFontMonospaced(Font.decode(name));
   }
   
   public static void cacheFontNames()
   {
     if (fontCacheSemaphore == null)
     {
       fontCacheSemaphore = new Semaphore(0);
       Thread thread = new Thread(new Runnable()
       {
         public void run()
         {
           String[] fontFamilies = GraphicsUtil.ge.getAvailableFontFamilyNames();
           GraphicsUtil.fontFamilyNames = new LinkedHashSet<String>(fontFamilies.length);
           GraphicsUtil.monospacedFontFamilyNames = new LinkedHashSet<String>();
           for (String fontFamilyName : fontFamilies) {
             if (!GraphicsUtil.disallowedFontNames.contains(fontFamilyName))
             {
               GraphicsUtil.fontFamilyNames.add(fontFamilyName);
               Font font = Font.decode(fontFamilyName);
               if (GraphicsUtil.isFontMonospaced(font)) {
                 GraphicsUtil.monospacedFontFamilyNames.add(fontFamilyName);
               }
             }
           }
           GraphicsUtil.fontCacheSemaphore.release();
         }
       });
       thread.setDaemon(true);
       thread.start();
     }
     else
     {
       fontCacheSemaphore.acquireUninterruptibly();
       fontCacheSemaphore.release();
     }
   }
 }

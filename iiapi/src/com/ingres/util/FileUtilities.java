/*  1:   */ package com.ingres.util;
/*  2:   */ 
/*  3:   */ import java.io.File;
/*  4:   */ import java.io.FileInputStream;
/*  5:   */ import java.io.FileNotFoundException;
/*  6:   */ import java.io.FileOutputStream;
/*  7:   */ import java.io.IOException;
/*  8:   */ import java.nio.channels.FileChannel;
/*  9:   */ 
/* 10:   */ public class FileUtilities
/* 11:   */ {
/* 12:   */   public static void copyFile(File in, File out)
/* 13:   */     throws IOException, FileNotFoundException
/* 14:   */   {
/* 15:27 */     FileChannel inChannel = null;
/* 16:28 */     FileChannel outChannel = null;
/* 17:   */     try
     {
       inChannel = new FileInputStream(in).getChannel();
       outChannel = new FileOutputStream(out).getChannel();
       
       int maxSize = 67076096;
       long size = inChannel.size();
       long pos = 0L;
       while (pos < size) {
         pos += inChannel.transferTo(pos, maxSize, outChannel);
       }
     }
     finally
     {
       if (inChannel != null) {
         inChannel.close();
       }
       if (outChannel != null)
       {
         outChannel.force(true);
         outChannel.close();
       }
     }
   }
   
   public static void main(String[] args)
     throws IOException, FileNotFoundException
   {
     copyFile(new File(args[0]), new File(args[1]));
   }
 }
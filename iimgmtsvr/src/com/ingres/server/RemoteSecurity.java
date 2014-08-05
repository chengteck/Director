 package com.ingres.server;
 
 import com.ingres.IIapi;
import com.ingres.IIapi.Exception;
import com.ingres.util.Environment;
import com.ingres.util.Intl;
import com.ingres.util.Logging;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
 
 public class RemoteSecurity
 {
   private static Logging logger = new Logging(RemoteSecurity.class);
   private static Charset unicodeCharset = Charset.forName("UTF-8");
   private static String DEFAULT_KEY_ALGORITHM = "RSA";
   private static String DEFAULT_CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
   private static String USER_DEFAULT_AUTH_VERSION_0 = "USER:DFLT:0000:";
   private static byte[] userDfltVers0 = convertToBinary(USER_DEFAULT_AUTH_VERSION_0);
   private PublicKey key = null;
   private Cipher cipher;
   
   public static byte[] convertToBinary(String str)
   {
     byte[] bytes;
     if ((str == null) || (str.length() == 0)) {
       bytes = new byte[0];
     } else {
       bytes = str.getBytes(unicodeCharset);
     }
     return bytes;
   }
   
   public static String convertFromBinary(byte[] data)
   {
     String str;
     if ((data == null) || (data.length == 0)) {
       str = new String();
     } else {
       str = new String(data, unicodeCharset);
     }
     return str;
   }
   
   public RemoteSecurity()
     throws IIapi.Exception, NoSuchAlgorithmException
   {
     try
     {
       this.cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
       KeyPairGenerator kpg = KeyPairGenerator.getInstance(DEFAULT_KEY_ALGORITHM);
       SecureRandom random = new SecureRandom();
       kpg.initialize(1024, random);
       KeyPair kp = kpg.generateKeyPair();
       this.cipher.init(2, kp.getPrivate());
       this.key = kp.getPublic();
     }
     catch (Exception ex)
     {
       logger.except("RemoteSecurity constructor", ex);
       throw new IIapi.Exception(Intl.formatString(RemoteCommand.pkg, "security.initError", new Object[] { ex.getMessage() }));
     } catch (NoSuchPaddingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (InvalidKeyException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
   }
   
   public RemoteSecurity(byte[] encodedKey)
     throws IIapi.Exception, NoSuchAlgorithmException
   {
     try
     {
       this.cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
       KeyFactory kf = KeyFactory.getInstance(DEFAULT_KEY_ALGORITHM);
       X509EncodedKeySpec eks = new X509EncodedKeySpec(encodedKey);
       this.cipher.init(1, kf.generatePublic(eks));
     }
     catch (Exception ex)
     {
       logger.except("RemoteSecurity constructor from key", ex);
       throw new IIapi.Exception(Intl.formatString(RemoteCommand.pkg, "security.initError", new Object[] { ex.getMessage() }));
     } catch (NoSuchPaddingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (InvalidKeyException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (InvalidKeySpecException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
   }
   
   public byte[] getEncodedKey()
   {
     return this.key == null ? null : this.key.getEncoded();
   }
   
   public byte[] encode(byte[] data) 
   {
     if (this.key != null) {
       data = null;
     } else {
       try
       {
         data = this.cipher.doFinal(data);
       }
       catch (Exception ex)
       {
         data = null;
       } catch (IllegalBlockSizeException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (BadPaddingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
     }
     return data;
   }
   
   public byte[] decode(byte[] data) 
   {
     if (this.key == null) {
       data = null;
     } else {
       try
       {
         data = this.cipher.doFinal(data);
       }
       catch (Exception ex)
       {
         data = null;
       } catch (IllegalBlockSizeException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (BadPaddingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
     }
     return data;
   }
   
   public byte[] getUserAuth()
   {
     if (this.key != null) {
       return null;
     }
     byte[] uid = convertToBinary(Environment.currentUser());
     int len = this.cipher.getOutputSize(uid.length);
     
     len &= 0xFFFFFF80;
     byte[] cert = new byte[userDfltVers0.length + len];
     System.arraycopy(userDfltVers0, 0, cert, 0, userDfltVers0.length);
     try
     {
       this.cipher.doFinal(uid, 0, uid.length, cert, userDfltVers0.length);
     }
     catch (Exception ex)
     {
       return null;
     } catch (ShortBufferException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IllegalBlockSizeException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (BadPaddingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
     return cert;
   }
   
   public boolean validateUserAuth(String uid, byte[] auth) 
   {
     if (this.key == null) 
     {
       return false;
     }
     if (beginsWith(auth, userDfltVers0))
     {
       byte[] data;
       try
       {
         data = this.cipher.doFinal(auth, userDfltVers0.length, auth.length - userDfltVers0.length);
       }
       catch (Exception ex)
       {
         logger.except("validateUserAuth: cipher exception", ex);
         return false;
       } 
       catch (IllegalBlockSizeException e) 
       {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return false;
	   } 
       catch (BadPaddingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return false;
	   }
       
       String usr = convertFromBinary(data);
       return uid.equals(usr);
     }
     logger.warn("Unknown authorization certificate format: not equal '%1$s'", new Object[] { USER_DEFAULT_AUTH_VERSION_0 });
     
 
     return false;
   }
   
   private static boolean beginsWith(byte[] value, byte[] prefix)
   {
     if ((value == null) || (prefix == null) || (value.length < prefix.length)) {
       return false;
     }
     for (int i = 0; i < prefix.length; i++) {
       if (value[i] != prefix[i]) {
         return false;
       }
     }
     return true;
   }
 }
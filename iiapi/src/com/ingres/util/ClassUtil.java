 package com.ingres.util;
 
 //import com.ingres.annotations.Scriptable;
 import java.lang.reflect.Field;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.util.List;
 
 public class ClassUtil
 {
//   public static boolean findScriptable(Class<?> clazz, String name)
//   {
//     Scriptable annotation = null;
//     try
//     {
//       Field field = clazz.getDeclaredField(name);
//       annotation = (Scriptable)field.getAnnotation(Scriptable.class);
//     }
//     catch (NoSuchFieldException nsfe)
//     {
//       try
//       {
//         String getMethodName = String.format("get%1$c%2$s", new Object[] { Character.valueOf(Character.toUpperCase(name.charAt(0))), name.substring(1) });
//         
// 
//         Method method = clazz.getDeclaredMethod(getMethodName, new Class[0]);
//         annotation = (Scriptable)method.getAnnotation(Scriptable.class);
//       }
//       catch (NoSuchMethodException nsme) {}
//     }
//     return annotation != null;
//   }
   
   public static Object createAndSetValues(Class<?> clazz, List<String> keys, List<Object> values)
     throws Throwable
   {
     Object obj = null;
     try
     {
       obj = clazz.newInstance();
       if (obj != null) {
         for (int i = 0; i < keys.size(); i++)
         {
           String key = (String)keys.get(i);
           Object value = values.get(i);
           try
           {
             Field field = clazz.getDeclaredField(key);
             Class<?> fieldClass = field.getType();
             Class<?> valueClass = value.getClass();
             if (fieldClass == String.class) {
               field.set(obj, value == null ? null : value.toString());
             } else if (fieldClass == Boolean.TYPE)
             {
               if ((value instanceof Boolean)) {
                 field.setBoolean(obj, ((Boolean)value).booleanValue());
               } else if ((value instanceof Number)) {
                 field.setBoolean(obj, ((Number)value).intValue() != 0);
               } else if ((value instanceof String)) {
                 field.setBoolean(obj, Boolean.valueOf((String)value).booleanValue());
               } else {
                 field.set(obj, value);
               }
             }
             else if (fieldClass == Integer.TYPE)
             {
               if ((value instanceof Number)) {
                 field.setInt(obj, ((Number)value).intValue());
               } else if ((value instanceof String)) {
                 field.setInt(obj, Integer.valueOf((String)value).intValue());
               } else {
                 field.set(obj, value);
               }
             }
             else if (fieldClass == Double.TYPE)
             {
               if ((value instanceof Number)) {
                 field.setDouble(obj, ((Number)value).doubleValue());
               } else if ((value instanceof String)) {
                 field.setDouble(obj, Double.valueOf((String)value).doubleValue());
               } else {
                 field.set(obj, value);
               }
             }
             else if (fieldClass.isEnum())
             {
               if ((value instanceof String))
               {
                 Method valueMethod = fieldClass.getDeclaredMethod("valueOf", new Class[] { String.class });
                 field.set(obj, valueMethod.invoke(null, new Object[] { ((String)value).toUpperCase() }));
               }
               else
               {
                 field.set(obj, value);
               }
             }
             else if (fieldClass.isArray())
             {
               if (valueClass.isArray())
               {
                 field.set(obj, value);
               }
               else
               {
                 Class<?> elementType = fieldClass.getComponentType();
                 if (elementType == String.class)
                 {
                   String[] array = new String[1];
                   array[0] = (value == null ? null : value.toString());
                   field.set(obj, array);
                 }
               }
             }
             else {
               field.set(obj, value);
             }
           }
           catch (NoSuchFieldException nsfe)
           {
             try
             {
               String setMethodName = String.format("set%1$c%2$s", new Object[] { Character.valueOf(Character.toUpperCase(key.charAt(0))), key.substring(1) });
               
 
               Method method = clazz.getDeclaredMethod(setMethodName, new Class[] { value.getClass() });
               method.invoke(obj, new Object[] { value });
             }
             catch (NoSuchMethodException nsme) {}catch (InvocationTargetException ite)
             {
               throw ite.getTargetException();
             }
           }
         }
       }
     }
     catch (InstantiationException ie) {}catch (IllegalAccessException iae) {}
     return obj;
   }
 }
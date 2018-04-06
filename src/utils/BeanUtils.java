package utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 反射工具类
 *
 * @author itguang
 * @create 2018-04-05 22:20
 **/
public class BeanUtils {


    /**
     * 实例化一个class
     * @param <T>
     * @param clazz Person.class
     * @return
     */
    public static <T> T instanceClass(Class<T> clazz){
        if(!clazz.isInterface()){
            try {
                return clazz.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 通过构造函数实例化
     * @param <T>
     * @param ctor
     * @param args
     * @return
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static <T> T instanceClass(Constructor<T> ctor, Object... args)
            throws IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        makeAccessible(ctor);
        return ctor.newInstance(args);//调用构造方法实例化
    }

    /**
     * 查找某个class的方法
     * @param clazz
     * @param methodName
     * @param paramTypes
     * @return
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public static  Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes){
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            return findDeclaredMethod(clazz, methodName, paramTypes);//返回共有的方法
        }
    }

    public static Method findDeclaredMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes){
        try {
            return clazz.getDeclaredMethod(methodName, paramTypes);
        }
        catch (NoSuchMethodException ex) {
            if (clazz.getSuperclass() != null) {
                return findDeclaredMethod(clazz.getSuperclass(), methodName, paramTypes);
            }
            return null;
        }
    }

    public static Method[] findDeclaredMethods(Class<?> clazz){
        return clazz.getDeclaredMethods();
    }

    public static void makeAccessible(Constructor<?> ctor) {
        if ((!Modifier.isPublic(ctor.getModifiers())
                || !Modifier.isPublic(ctor.getDeclaringClass().getModifiers()))
                && !ctor.isAccessible()) {
            ctor.setAccessible(true);//如果是私有的 设置为true 使其可以访问
        }
    }

    public static Field[] findDeclaredFields(Class<?> clazz){
        return clazz.getDeclaredFields();
    }
}

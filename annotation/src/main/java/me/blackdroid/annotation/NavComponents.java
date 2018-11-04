package me.blackdroid.annotation;

import android.app.Activity;
import android.content.Context;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NavComponents {

    private static String suffix = "NavComponent";
    private NavComponents() {

    }

    public static <T extends Activity> void bind(T activity) {
        instantiateBinder(activity, suffix);
    }

    public static void bind(Object fragment) {
        instantiateBinder(fragment, suffix);
    }

    private static void instantiateBinder(Object target, String suffix) {
        Class<?> targetClass = target.getClass();
        String className = targetClass.getName();
        try {
            Class<?> bindingClass = targetClass
                    .getClassLoader()
                    .loadClass(className + suffix);
            Method method = bindingClass.getMethod("inject", targetClass);
            method.invoke(null, target);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to find Class for " + className + suffix, e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find constructor for " + className + suffix, e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    public static <T> void start(Context context, Class<T> clazz, Object ... objects) {
        String className = clazz.getName();
        try {
            Class<?> bindingClass = clazz
                    .getClassLoader()
                    .loadClass(className + suffix);
            Class<?>[] paraClassTypeArray = new Class<?>[objects.length + 1];
            Object[] paraClassObjectArray = new Object[objects.length + 1];
            paraClassTypeArray[0] = Context.class;
            paraClassObjectArray[0] = context;

            for (int i = 0; i < objects.length; i++) {
                paraClassTypeArray[i + 1] = objects[i].getClass();
                paraClassObjectArray[i + 1] = objects[i];
            }
            Method method = bindingClass.getMethod("start", paraClassTypeArray);
            method.invoke(null, paraClassObjectArray);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to find Class for " + className + suffix, e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find Method for " + className + suffix + "start()" , e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to invoke " + className + suffix, e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to create instance.", e);
        }
    }

    private static <T extends Activity> void instantiateBinder(T target, String suffix) {
        Class<?> targetClass = target.getClass();
        String className = targetClass.getName();
        try {
            Class<?> bindingClass = targetClass
                    .getClassLoader()
                    .loadClass(className + suffix); // dynamically loads Java class into memory...
            Constructor<?> classConstructor = bindingClass.getConstructor(targetClass);
            try {
                classConstructor.newInstance(target);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to invoke " + classConstructor, e);
            } catch (InstantiationException e) {
                throw new RuntimeException("Unable to invoke " + classConstructor, e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                if (cause instanceof Error) {
                    throw (Error) cause;
                }
                throw new RuntimeException("Unable to create instance.", cause);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to find Class for " + className + suffix, e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find constructor for " + className + suffix, e);
        }
    }

}

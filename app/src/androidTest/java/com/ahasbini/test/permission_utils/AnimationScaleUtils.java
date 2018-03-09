package com.ahasbini.test.permission_utils;

import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Utils to disable animations for testing purposes.
 * Created by ahasbini on 04-Jan-18.
 */

public class AnimationScaleUtils {

    private static final String TAG = AnimationScaleUtils.class.getSimpleName();

    public static void disableAnimations() {
        setSystemAnimationsScale(0.0f);
    }

    public static void enableAnimations() {
        setSystemAnimationsScale(1.0f);
    }

    private static void setSystemAnimationsScale(float animationScale) {
        try {
            Class<?> windowManagerStubClazz = Class.forName("android.view.IWindowManager$Stub");
            Method asInterface = windowManagerStubClazz.getDeclaredMethod("asInterface", IBinder.class);
            Class<?> serviceManagerClazz = Class.forName("android.os.ServiceManager");
            Method getService = serviceManagerClazz.getDeclaredMethod("getService", String.class);
            Class<?> windowManagerClazz = Class.forName("android.view.IWindowManager");
            Method setAnimationScales = windowManagerClazz.getDeclaredMethod("setAnimationScales", float[].class);
            Method getAnimationScales = windowManagerClazz.getDeclaredMethod("getAnimationScales");

            IBinder windowManagerBinder = (IBinder) getService.invoke(null, "window");
            Object windowManagerObj = asInterface.invoke(null, windowManagerBinder);
            float[] currentScales = (float[]) getAnimationScales.invoke(windowManagerObj);
            for (int i = 0; i < currentScales.length; i++) {
                currentScales[i] = animationScale;
            }
            setAnimationScales.invoke(windowManagerObj, new Object[]{currentScales});
            Log.i(TAG, "setSystemAnimationsScale: animations changed");
        } catch (Exception e) {
            Log.e(TAG, "Could not change animation scale to " + animationScale + " :'(", e);
        }
    }
}

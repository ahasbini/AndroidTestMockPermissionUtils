package com.ahasbini.test.permission_utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

/**
 * A modified {@link ContextWrapper} which allows the tests to interface and simulate certain
 * scenarios such as Runtime Permissions. Used by {@link CustomBuildBaseActivity} to inject the
 * interface. In case the app is running in normal use (not running tests), the class will fallback
 * to the default implementations.
 * <p></p>
 * Created by ahasbini on 06-Mar-18.
 */

public class CustomBuildContext extends ContextWrapper {

    private static final String TAG = CustomBuildContext.class.getSimpleName();

    public CustomBuildContext(Context base) {
        super(base);
        Log.i(TAG, "CustomBuildContext: base: " + base);
        Log.i(TAG, "CustomBuildContext: called");
    }

    @Override
    public int checkPermission(String permission, int pid, int uid) {
        Log.i(TAG, "checkPermission: called");
        if (CustomBuildBaseActivity.permissionOverrideMap.containsKey(permission)) {
            Log.i(TAG, "checkPermission: found permission: " + permission);
            Log.i(TAG, "checkPermission: result: " +
                    CustomBuildBaseActivity.permissionOverrideMap.get(permission));
            return CustomBuildBaseActivity.permissionOverrideMap.get(permission);
        }
        return super.checkPermission(permission, pid, uid);
    }

    @Override
    public int checkSelfPermission(String permission) {
        Log.i(TAG, "checkSelfPermission: called");
        if (CustomBuildBaseActivity.permissionOverrideMap.containsKey(permission)) {
            Log.i(TAG, "checkSelfPermission: found permission: " + permission);
            Log.i(TAG, "checkSelfPermission: result: " +
                    CustomBuildBaseActivity.permissionOverrideMap.get(permission));
            return CustomBuildBaseActivity.permissionOverrideMap.get(permission);
        }
        return super.checkSelfPermission(permission);
    }
}

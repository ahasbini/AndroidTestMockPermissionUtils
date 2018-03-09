package com.ahasbini.test.permission_utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.HashMap;

/**
 * A modified {@link AppCompatActivity} which is extends by all other activities to allow for tests
 * to interface them and simulate certain scenarios such as Runtime Permissions.
 * <p></p>
 * Created by ahasbini on 07-Mar-18.
 */

@SuppressLint("Registered")
public class CustomBuildBaseActivity extends AppCompatActivity {

    private static final String TAG = CustomBuildBaseActivity.class.getSimpleName();

    public static HashMap<String, Integer> permissionOverrideMap = new HashMap<>();
    public static HashMap<String, Boolean> shouldShowRequestPermissionRationaleMap = new HashMap<>();

    @Override
    protected void attachBaseContext(Context newBase) {
        Log.i(TAG, "attachBaseContext: called");
        super.attachBaseContext(new CustomBuildContext(newBase));
    }

    @Override
    public boolean shouldShowRequestPermissionRationale(@NonNull String permission) {
        Log.i(TAG, "shouldShowRequestPermissionRationaleMap: called");
        if (shouldShowRequestPermissionRationaleMap.containsKey(permission)) {
            Log.i(TAG, "shouldShowRequestPermissionRationaleMap: found permission: " + permission);
            Log.i(TAG, "shouldShowRequestPermissionRationaleMap: result: " +
                    shouldShowRequestPermissionRationaleMap.get(permission));
            return shouldShowRequestPermissionRationaleMap.get(permission);
        }
        return super.shouldShowRequestPermissionRationale(permission);
    }
}

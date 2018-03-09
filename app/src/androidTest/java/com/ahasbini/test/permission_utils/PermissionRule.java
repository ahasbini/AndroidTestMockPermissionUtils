package com.ahasbini.test.permission_utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * Class to manage the permissions necessary for the tests execution. It uses {@link UiDevice} to
 * run pm commands using the shell user. Also replaces the {@link ActivityCompat.PermissionCompatDelegate}
 * with a custom implementation to simulate test cases.
 * <p></p>
 * Created by ahasbini on 05-Mar-18.
 */

// TODO: 08-Mar-18 ahasbini: implement handling for REVOKE_WITH_RATIONALE
// TODO: 08-Mar-18 ahasbini: improve upon semaphore implementation (use acquire instead of tryAcquire) and try to make it non-static
public class PermissionRule implements TestRule {

    private static final String TAG = PermissionRule.class.getSimpleName();

    enum Type {
        GRANT,
        REVOKE,
        REVOKE_WITH_RATIONALE
    }

    private final Type type;
    private final String permission;
    private final boolean force;
    private static final Semaphore requestPermissionSemaphore = new Semaphore(0);

    private PermissionRule(Type type, String permission, boolean force) {
        this.type = type;
        this.permission = permission;
        this.force = force;
        if (ActivityCompat.getPermissionCompatDelegate() != permissionCompatDelegate) {
            if (ActivityCompat.getPermissionCompatDelegate() != null) {
                Log.i(TAG, "PermissionRule: ActivityCompat.getPermissionCompatDelegate() "
                        + ActivityCompat.getPermissionCompatDelegate());
                Log.w(TAG, "PermissionRule: replacing PermissionCompatDelegate");
            }
            ActivityCompat.setPermissionCompatDelegate(permissionCompatDelegate);
        }
    }

    /**
     * Grants the permission if not granted and device Android API is Marshmallow or greater.
     *
     * @param permission one of {@link Manifest.permission}
     * @return {@link TestRule} to check and modify if necessary.
     * @see PermissionRule#grant(String, boolean)
     */
    public static PermissionRule grant(String permission) {
        return grant(permission, false);
    }

    /**
     * Grants the permission if not granted.
     *
     * @param permission one of {@link Manifest.permission}
     * @param force      grants permission regardless of Android API if <code>true</code>,
     *                   otherwise doesn't and continues test execution
     * @return {@link TestRule} to check and modify if necessary.
     */
    public static PermissionRule grant(String permission, boolean force) {
        return new PermissionRule(Type.GRANT, permission, force);
    }

    /**
     * Revokes the permission if not revoked and device Android API is Marshmallow or greater.
     *
     * @param permission one of {@link Manifest.permission}
     * @return {@link TestRule} to check and modify if necessary.
     * @see PermissionRule#revoke(String, boolean)
     */
    public static PermissionRule revoke(String permission) {
        return revoke(permission, false);
    }

    /**
     * Revokes the permission if not revoked.
     *
     * @param permission one of {@link Manifest.permission}
     * @param force      revokes permission regardless of Android API if <code>true</code>,
     *                   otherwise doesn't and continues test execution
     * @return {@link TestRule} to check and modify if necessary.
     */
    public static PermissionRule revoke(String permission, boolean force) {
        return new PermissionRule(Type.REVOKE, permission, force);
    }

    /**
     * Checks if the app has sent a request to {@link PermissionRule#permissionCompatDelegate}
     * within a set timeout. Blocks the thread during the timeout.
     * @param timeout in milliseconds
     * @return <code>true</code> if the app has requested permissions, <code>false</code> otherwise
     */
    public boolean isRequestPermissionCalled(long timeout) {
        try {
            if (requestPermissionSemaphore.tryAcquire()) {
                return true;
            }

            Thread.sleep(timeout);

            return requestPermissionSemaphore.tryAcquire();
        } catch (InterruptedException e) {
            Log.w(TAG, "isRequestPermissionCalled: ", e);
        }

        return false;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                Log.i(TAG, "evaluate: called");

                if (!force && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    Log.w(TAG, "permissions on pre-Marshmallow don't need to be managed");
                    if (type == Type.GRANT) {
                        base.evaluate();
                    } else {
                        Log.w(TAG, "ignoring " + description + " test case with REVOKE permission rule");
                    }
                    return;
                }

                Context context = InstrumentationRegistry.getTargetContext();
                switch (type) {
                    case GRANT:
                        // Permission already meeting rule requirement, continuing test
                        if (ContextCompat.checkSelfPermission(context, permission) ==
                                PackageManager.PERMISSION_GRANTED) {
                            base.evaluate();
                            return;
                        }

                        // Permission needs to be granted
                        grantPermission(context.getPackageName(), permission);

                        // Checking if grant was successful
                        if (ContextCompat.checkSelfPermission(context, permission) ==
                                PackageManager.PERMISSION_GRANTED) {
                            base.evaluate();
                            return;
                        }

                        // Waiting for grant to be appear in check
                        Thread.sleep(500);
                        Assert.assertTrue("Unable to " + type.name() + " permission",
                                ContextCompat.checkSelfPermission(context, permission) ==
                                        PackageManager.PERMISSION_GRANTED);
                        break;
                    case REVOKE:
                        CustomBuildBaseActivity.permissionOverrideMap.put(permission, PackageManager.PERMISSION_DENIED);
                        CustomBuildBaseActivity.shouldShowRequestPermissionRationaleMap.put(permission, true);
                        base.evaluate();
                        CustomBuildBaseActivity.permissionOverrideMap.remove(permission);
                        CustomBuildBaseActivity.shouldShowRequestPermissionRationaleMap.remove(permission);
                        break;
                    case REVOKE_WITH_RATIONALE:
                        CustomBuildBaseActivity.permissionOverrideMap.put(permission, PackageManager.PERMISSION_DENIED);
                        CustomBuildBaseActivity.shouldShowRequestPermissionRationaleMap.put(permission, false);
                        base.evaluate();
                        CustomBuildBaseActivity.permissionOverrideMap.remove(permission);
                        CustomBuildBaseActivity.shouldShowRequestPermissionRationaleMap.remove(permission);
                        break;
                }
            }
        };
    }

    /**
     * Method that gets shell access with the privileges to grant permissions to apps, and runs
     * a command to grant the requested permission to the app.
     * @param packageName package name (application id) of the app
     * @param permission requested permission to granted
     * @throws IOException due to command error
     */
    private static void grantPermission(String packageName, String permission) throws IOException {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("pm grant " + packageName + " " + permission);
    }

    /**
     * This is the mock implementation that is passed to
     * {@link ActivityCompat#setPermissionCompatDelegate(ActivityCompat.PermissionCompatDelegate)}
     * which will get called when the app is requesting for permissions. Based on the release notes,
     * {@link ActivityCompat.PermissionCompatDelegate} was intended for instant apps use, however
     * it has a higher priority to be called over the default implementation if set in
     * {@link ActivityCompat} hence providing the opportunity to control and simulate the scenarios
     * needed for our testing. It will automatically update {@link CustomBuildBaseActivity} to
     * grant the permissions and will also ensure that the permissions are granted by the Android
     * System. Furthermore after granting the permissions, tests could check if request was
     * successful by asserting {@link PermissionRule#isRequestPermissionCalled(long)} true in the
     * test.
     */
    private static final ActivityCompat.PermissionCompatDelegate permissionCompatDelegate
            = new ActivityCompat.PermissionCompatDelegate() {

        private final String TAG = ActivityCompat.PermissionCompatDelegate.class.getSimpleName();

        @Override
        public boolean requestPermissions(@NonNull final Activity activity,
                                          @NonNull final String[] permissions, final int requestCode) {
            Log.i(TAG, "requestPermissions: called");
            Thread permissionsThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    final int[] grantedResults = new int[permissions.length];
                    Context context = InstrumentationRegistry.getTargetContext();
                    for (int i = 0; i < permissions.length; i++) {
                        Log.i(TAG, "run: granting the permission " + permissions[i]);

                        // Removing override if set
                        CustomBuildBaseActivity.permissionOverrideMap.put(permissions[i],
                                PackageManager.PERMISSION_GRANTED);

                        // Checking if permission is already granted
                        if (ActivityCompat.checkSelfPermission(
                                context, permissions[i]) ==
                                PackageManager.PERMISSION_GRANTED) {
                            grantedResults[i] = PackageManager.PERMISSION_GRANTED;
                            continue;
                        }

                        // Permission needs to be granted
                        try {
                            grantPermission(context.getPackageName(), permissions[i]);
                        } catch (IOException e) {
                            Log.w(TAG, "run: couldn't grant permissions", e);
                        }

                        // Checking if grant was successful
                        if (ActivityCompat.checkSelfPermission(
                                context, permissions[i]) ==
                                PackageManager.PERMISSION_GRANTED) {
                            grantedResults[i] = PackageManager.PERMISSION_GRANTED;
                            continue;
                        }

                        // Waiting for grant to be appear in check
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Log.w(TAG, "run: ", e);
                        }

                        Assert.assertTrue("Unable to GRANT permission",
                                ActivityCompat.checkSelfPermission(context, permissions[i])
                                        == PackageManager.PERMISSION_GRANTED);
                        grantedResults[i] = PackageManager.PERMISSION_GRANTED;
                    }

                    new Handler(activity.getMainLooper()).post(new Runnable() {

                        @Override
                        public void run() {
                            // Notifying activity
                            activity.onRequestPermissionsResult(requestCode, permissions, grantedResults);
                            // Notifying test if waiting for result.
                            requestPermissionSemaphore.release();
                        }
                    });
                }
            });
            permissionsThread.start();
            return true;
        }

        @Override
        public boolean onActivityResult(@NonNull Activity activity, int requestCode,
                                        int resultCode, @Nullable Intent data) {
            // Do Nothing
            return false;
        }
    };
}

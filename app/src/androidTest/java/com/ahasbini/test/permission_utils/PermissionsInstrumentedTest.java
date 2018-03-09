package com.ahasbini.test.permission_utils;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * A set of test cases to display how the implementation works and what caveats to lookout for.
 * Created by ahasbini on 08-Mar-18.
 */

public class PermissionsInstrumentedTest extends BaseTest {

    private static final String TAG = PermissionsInstrumentedTest.class.getSimpleName();

    /**
     * The rule chain first ensures that the test running on a device with Marshmallow or above,
     * grants the permission (using package manager), then mocks the revoke of
     * the permission and finally requests the activity ({@link MainActivity}) to start. This
     * simulates a typical scenario where an app might have already received a grant for the
     * permissions (during normal use, development or during testing) and a test needs to revoke
     * that permission before starting the test. With the implementation of mocking the Runtime
     * Permissions, we'll be able to run the tests in a modular and independent way such that tough
     * restrictions like having to kill an app (which causes the tests to crash) are no longer faced.
     */
    @Rule
    public RuleChain ruleChain = RuleChain
            .outerRule(new AndroidApiRule(Build.VERSION_CODES.M))
            .around(PermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE))
            .around(revokedPermissionRule =
                    PermissionRule.revoke(Manifest.permission.READ_EXTERNAL_STORAGE))
            .around(activityTestRule = new ActivityTestRule<>(MainActivity.class));

    private PermissionRule revokedPermissionRule;
    private ActivityTestRule<MainActivity> activityTestRule;

    /**
     * Testing what the activity is seeing the permission as. The result should be permission denied.
     */
    @Test
    public void activityPermissionDeniedTest() {
        Log.i(TAG, "activityPermissionDeniedTest: starting");
        Log.i(TAG, "activityPermissionDeniedTest: getting activity and testing");
        Assert.assertTrue(activityTestRule.getActivity()
                .checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED);
        Log.i(TAG, "activityPermissionDeniedTest: finished");
    }

    /**
     * Testing what the testing framework sees the permission as. The result should be granted.
     * Meaning even if we implemented an interface that mocks handling the permissions, the Android
     * Support Test libraries do not get affected by the mock implementation.
     */
    @Test
    public void targetContextPermissionGrantedTest() {
        Log.i(TAG, "targetContextPermissionGrantedTest: started");
        Log.i(TAG, "targetContextPermissionGrantedTest: getting target context from instrumentation");
        Assert.assertTrue(InstrumentationRegistry.getTargetContext()
                .checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED);
        Log.i(TAG, "targetContextPermissionGrantedTest: finished");
    }

    /**
     * This test gets the app to request for permission. Although the permission has been already
     * granted, the mock implementation will return a result denied when the permission is checked
     * by the app. Furthermore, the app will send a request to the mock implementation for the
     * permissions to be granted and eventually will call the
     * {@link android.app.Activity#onRequestPermissionsResult(int, String[], int[])} or the
     * {@link android.app.Fragment#onRequestPermissionsResult(int, String[], int[])} to signal the
     * app that the permissions have been granted.
     * <p></p>
     * It seems funny that the app had the permission all along but the mock implementation was
     * lying to it :).
     */
    @Test
    public void permissionCompatDelegateTest() {
        Log.i(TAG, "permissionCompatDelegateTest: starting");
        Log.i(TAG, "permissionCompatDelegateTest: clicking show sd card button");
        Espresso.onView(ViewMatchers.withText(R.string.show_sd_card))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click());
        Assert.assertTrue(revokedPermissionRule.isRequestPermissionCalled(1000));
        Log.i(TAG, "permissionCompatDelegateTest: requestPermissions asserted true");
        Log.i(TAG, "permissionCompatDelegateTest: clicking ok button");
        TestUtils.findTextInDialog(activityTestRule.getActivity(), android.R.string.ok)
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click());
        Log.i(TAG, "permissionCompatDelegateTest: finished");
    }
}

package com.ahasbini.test.permission_utils;

import android.Manifest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

/**
 * A base class for all tests. It takes care of disabling the animations before the tests start.
 * Created by ahasbini on 02-Jan-18.
 */

@RunWith(AndroidJUnit4.class)
public class BaseTest {

    @ClassRule
    public static PermissionRule grantPermissionRule =
            PermissionRule.grant(Manifest.permission.SET_ANIMATION_SCALE, true);

    @BeforeClass
    public static void disableAnimation() {
        AnimationScaleUtils.disableAnimations();
    }

    @AfterClass
    public static void enableAnimation() {
        AnimationScaleUtils.enableAnimations();
    }
}

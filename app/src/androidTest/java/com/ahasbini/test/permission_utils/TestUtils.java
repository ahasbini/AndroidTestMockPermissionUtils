package com.ahasbini.test.permission_utils;

import android.app.Activity;
import android.support.annotation.StringRes;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.RootMatchers;
import android.support.test.espresso.matcher.ViewMatchers;

import org.hamcrest.Matchers;

/**
 * Utility class used within the testing.
 * <p></p>
 * Created by ahasbini on 13-Feb-18.
 */

public class TestUtils {

    public static ViewInteraction findTextInDialog(Activity activity, @StringRes int stringId) {
        return  Espresso.onView(ViewMatchers.withText(stringId))
                .inRoot(RootMatchers.withDecorView(Matchers.not(Matchers.is(
                        activity.getWindow().getDecorView()))));
    }
}

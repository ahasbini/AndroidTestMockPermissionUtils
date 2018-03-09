package com.ahasbini.test.permission_utils;

import android.os.Build;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Created by ahasbini on 08-Mar-18.
 */

public class AndroidApiRule implements TestRule {

    private static final String TAG = AndroidApiRule.class.getSimpleName();
    
    private final int api;

    public AndroidApiRule(int api) {
        this.api = api;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                if (Build.VERSION.SDK_INT >= api) {
                    base.evaluate();
                }
            }
        };
    }
}

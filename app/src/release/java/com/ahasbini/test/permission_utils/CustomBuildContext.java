package test.ahasbini.com.test;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;

/**
 * An unmodified {@link ContextWrapper} used in release mode with all implementations working as
 * normal.
 * <p></p>
 * Created by ahasbini on 06-Mar-18.
 */

public class CustomBuildContext extends ContextWrapper {

    public CustomBuildContext(Context base) {
        super(base);
    }
}

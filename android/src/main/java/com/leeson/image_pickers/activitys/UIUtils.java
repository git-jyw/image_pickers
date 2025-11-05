package com.leeson.image_pickers.activitys;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.luck.picture.lib.basic.PictureSelectorSupporterActivity;
import com.luck.picture.lib.basic.PictureSelectorTransparentActivity;

/**
 * Because on Android 15 and later, edge-to-edge UI is enabled by default.
 * Which causes UI issues in some Activities in 3rd party libraries, this is for adapting
 * the navigation bar UI style when running on Android OSes above Android 15.
 */

public class UIUtils {
    private static final boolean isEdgeToEdgeSupported =
            Build.VERSION.SDK_INT >= 35;

    public static void registerEdgeToEdgeAdaptation(Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if (activity instanceof PictureSelectorSupporterActivity
                        || activity instanceof PictureSelectorTransparentActivity) {
                    if (!isEdgeToEdgeSupported) return;
                    View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
                    if (rootView != null) {
                        applySystemInsetsToView(rootView);
                    }
                }
            }

            @Override public void onActivityStarted(@NonNull Activity activity) {}
            @Override public void onActivityResumed(@NonNull Activity activity) {}
            @Override public void onActivityPaused(@NonNull Activity activity) {}
            @Override public void onActivityStopped(@NonNull Activity activity) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }

    public static void applySystemInsetsToView(final View view) {
        final int initialPaddingBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int systemBarsType = WindowInsetsCompat.Type.systemBars();
            android.graphics.Insets systemBars = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                systemBars = insets.getInsets(systemBarsType).toPlatformInsets();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        initialPaddingBottom + systemBars.bottom
                );
            }
            return insets;
        });
    }
}
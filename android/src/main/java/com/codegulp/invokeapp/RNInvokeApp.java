
package com.codegulp.invokeapp;

import android.app.ActivityManager;

import android.content.Context;
import android.content.Intent;

import android.os.Bundle;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import android.os.Handler;
import java.util.List;

public class RNInvokeApp extends ReactContextBaseJavaModule {

    private static ReactApplicationContext reactContext;
    public static final String LOG_TAG = "RNInvokeApp";
    private static Bundle bundle = null;

    public RNInvokeApp(ReactApplicationContext context) {
        super(context);
        reactContext = context;
    }

    @Override
    public String getName() {
        return "ReactNativeInvokeApp";
    }

    @ReactMethod
    public void invokeApp(ReadableMap params) {
        ReadableMap data = params.hasKey("data") ? params.getMap("data") : null;

        if (data != null) {
            bundle = Arguments.toBundle(data);
        }

        String packageName = reactContext.getPackageName();
        Intent launchIntent = reactContext.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();

        try {
            Class<?> activityClass = Class.forName(className);
            Intent activityIntent = new Intent(reactContext, activityClass);

            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            reactContext.startActivity(activityIntent);
        } catch(Exception e) {
            Log.e(LOG_TAG, "Class not found", e);
            return;
        }

        // Because somehow at the time we start activity, the app is not in foreground yet
        // So to make sure the event is always called after, we need to do a condition checking here
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isAppOnForeground(reactContext))
                    handler.postDelayed(this, 100);
                else {
                    sendEvent();
                }
            }
        }, 100);
    }

    public static void sendEvent() {
        if (bundle != null) {
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("appInvoked", Arguments.fromBundle(bundle));
            bundle = null;
        }
    }

    private boolean isAppOnForeground(ReactApplicationContext context) {
        /**
         * We need to check if app is in foreground otherwise the app will crash.
         * http://stackoverflow.com/questions/8489993/check-android-application-is-in-foreground-or-not
         **/
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}

package com.example.pp.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScreenUtils {
    public static Set<String> getLauncherPackages(Context context) {
        Set<String> launcherPackages = new HashSet<>();
        launcherPackages.add("com.android.systemui");
        launcherPackages.add("com.google.android.apps.nexuslauncher");
        launcherPackages.add("com.nothing.launcher");
        
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : resolveInfos) {
            if (info.activityInfo != null) {
                launcherPackages.add(info.activityInfo.packageName);
            }
        }
        return launcherPackages;
    }
}

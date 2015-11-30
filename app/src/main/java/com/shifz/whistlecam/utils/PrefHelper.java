package com.shifz.whistlecam.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Shifar Shifz on 11/23/2015.
 */
public final class PrefHelper {


    private static final String PREF_NAME = "whistle_cam";
    private final SharedPreferences pref;

    private PrefHelper(final Context mContext) {
        this.pref = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static PrefHelper instance;

    public static PrefHelper getInstance(final Context context) {
        if (instance == null) {
            instance = new PrefHelper(context);
        }
        return instance;
    }

    public String getStringPref(String key, String defaultValue) {
        return pref.getString(key, defaultValue);
    }

    public boolean getBooleanPref(String key, boolean defaultValue) {
        return this.pref.getBoolean(key, defaultValue);
    }

    public int getIntPref(String key, int defValue) {
        return this.pref.getInt(key, defValue);
    }

    public void savePref(String key, int value) {
        this.pref.edit().putInt(key, value).commit();
    }
}

package com.example.pp.core.preferences;

import android.content.Context;
import com.google.gson.Gson;

public abstract class BasePreferenceHelper {
    protected final PreferenceManager preferenceManager;
    protected final Gson gson;

    protected BasePreferenceHelper(Context context) {
        this.preferenceManager = PreferenceManager.getInstance(context);
        this.gson = new Gson();
    }

    protected <T> void putObject(String key, T object) {
        if (object == null) {
            preferenceManager.remove(key);
        } else {
            preferenceManager.putString(key, gson.toJson(object));
        }
    }

    protected <T> T getObject(String key, Class<T> clazz) {
        String json = preferenceManager.getString(key, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    protected void putString(String key, String value) {
        preferenceManager.putString(key, value);
    }

    protected String getString(String key, String defaultValue) {
        return preferenceManager.getString(key, defaultValue);
    }

    protected void clear() {
        preferenceManager.clear();
    }
}

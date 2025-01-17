package com.example.adaptanklebrace.utils

import android.content.SharedPreferences

class SharedPreferencesUtil {
    companion object {
        // Get boolean preferences from app storage
        fun getPreference(sharedPreferences: SharedPreferences, key: String, value: Boolean): Boolean {
            return sharedPreferences.getBoolean(key, value)
        }

        // Get string preferences from app storage
        fun getPreference(sharedPreferences: SharedPreferences, key: String, value: String): String? {
            return sharedPreferences.getString(key, value)
        }

        // Save boolean preferences to app storage
        fun savePreference(sharedPreferences: SharedPreferences, key: String, value: Boolean) {
            sharedPreferences.edit().putBoolean(key, value).apply()
        }

        // Save string preferences to app storage
        fun savePreference(sharedPreferences: SharedPreferences, key: String, value: String) {
            sharedPreferences.edit().putString(key, value).apply()
        }
    }
}

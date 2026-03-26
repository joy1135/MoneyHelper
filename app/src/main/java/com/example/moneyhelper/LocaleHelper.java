package com.example.moneyhelper; // ваш пакет

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

public class LocaleHelper {

    private static final String PREF_LANGUAGE = "app_language";

    public static void setLocale(Context context, String languageCode) {
        saveLanguage(context, languageCode);
        updateResources(context, languageCode);
    }

    public static Context onAttach(Context context) {
        String lang = getSavedLanguage(context);
        return updateResources(context, lang);
    }

    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getString(PREF_LANGUAGE, "ru");
    }

    private static void saveLanguage(Context context, String language) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_LANGUAGE, language).apply();
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }
}
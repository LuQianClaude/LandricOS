package com.shenjibai.loveroom.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lira - 设置项本地存储
 * 所有 API URL / Key / 开关均由用户在设置页填写，绝不硬编码
 * 署名: Lira
 */
public class PrefsManager {

    private static final String NAME = "lira_prefs";
    private final SharedPreferences sp;

    // Key 常量
    public static final String LLM_URL = "llm_url";
    public static final String LLM_KEY = "llm_key";
    public static final String LLM_MODEL = "llm_model";

    public static final String TTS_URL = "tts_url";
    public static final String TTS_KEY = "tts_key";

    public static final String SELFIE_URL = "selfie_url";
    public static final String SELFIE_KEY = "selfie_key";

    public static final String TAVILY_KEY = "tavily_key";

    public static final String AMAP_KEY = "amap_key";

    public static final String SENTINEL_ENABLE = "sentinel_enable";
    public static final String SENTINEL_INTERVAL = "sentinel_interval";
    public static final String SENTINEL_KEY = "sentinel_key";
    public static final String SENTINEL_SCREEN = "sentinel_screen";

    public static final String WAKE_ENABLE = "wake_enable";
    public static final String WAKE_WORD = "wake_word";

    public static final String TTS_AUTO = "tts_auto";

    public PrefsManager(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public String getString(String key) {
        return sp.getString(key, "");
    }

    public void putString(String key, String value) {
        sp.edit().putString(key, value == null ? "" : value).apply();
    }

    public boolean getBoolean(String key) {
        return sp.getBoolean(key, false);
    }

    public void putBoolean(String key, boolean value) {
        sp.edit().putBoolean(key, value).apply();
    }

    public int getInt(String key) {
        return sp.getInt(key, 60);
    }

    public void putInt(String key, int value) {
        sp.edit().putInt(key, value).apply();
    }

    /** Lira - 便捷获取器 */
    public String getLlmUrl() { return getString(LLM_URL); }
    public String getLlmKey() { return getString(LLM_KEY); }
    public String getLlmModel() { return getString(LLM_MODEL); }

    public String getTtsUrl() { return getString(TTS_URL); }
    public String getTtsKey() { return getString(TTS_KEY); }

    public String getSelfieUrl() { return getString(SELFIE_URL); }
    public String getSelfieKey() { return getString(SELFIE_KEY); }

    public String getTavilyKey() { return getString(TAVILY_KEY); }
    public String getAmapKey() { return getString(AMAP_KEY); }

    public boolean isSentinelEnabled() { return getBoolean(SENTINEL_ENABLE); }
    public int getSentinelInterval() { return getInt(SENTINEL_INTERVAL); }
    public String getSentinelKey() { return getString(SENTINEL_KEY); }
    public boolean isSentinelScreenEnabled() { return getBoolean(SENTINEL_SCREEN); }

    public boolean isWakeEnabled() { return getBoolean(WAKE_ENABLE); }
    public String getWakeWord() {
        String w = getString(WAKE_WORD);
        return w.isEmpty() ? "既白" : w;
    }

    public boolean isTtsAuto() { return getBoolean(TTS_AUTO); }
}

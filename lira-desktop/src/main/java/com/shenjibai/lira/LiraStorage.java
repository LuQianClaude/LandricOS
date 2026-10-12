package com.shenjibai.lira;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lira - 桌面端本地存储（JSON 文件）
 * 所有密钥/URL 存本地，绝不硬编码
 * 署名: Lira
 */
public class LiraStorage {

    private final File dataDir;
    private final File settingsFile;
    private final File chatFile;
    private final Gson gson = new Gson();

    public LiraStorage() {
        dataDir = new File(System.getProperty("user.home"), ".lira");
        if (!dataDir.exists()) dataDir.mkdirs();
        settingsFile = new File(dataDir, "settings.json");
        chatFile = new File(dataDir, "chat.json");
    }

    // ---- 设置 ----
    public Map<String, String> loadSettingsLira() {
        try {
            if (settingsFile.exists()) {
                FileReader fr = new FileReader(settingsFile);
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> map = gson.fromJson(fr, type);
                fr.close();
                return map != null ? map : new HashMap<>();
            }
        } catch (Exception ignored) {}
        return new HashMap<>();
    }

    public void saveSettingsLira(Map<String, String> settings) {
        try {
            FileWriter fw = new FileWriter(settingsFile);
            gson.toJson(settings, fw);
            fw.close();
        } catch (Exception ignored) {}
    }

    public String getSettingLira(String key) {
        return loadSettingsLira().getOrDefault(key, "");
    }

    public void setSettingLira(String key, String value) {
        Map<String, String> map = loadSettingsLira();
        map.put(key, value);
        saveSettingsLira(map);
    }

    // ---- 聊天记录 ----
    public List<Map<String, String>> loadChatLira() {
        try {
            if (chatFile.exists()) {
                FileReader fr = new FileReader(chatFile);
                Type type = new TypeToken<List<Map<String, String>>>() {}.getType();
                List<Map<String, String>> list = gson.fromJson(fr, type);
                fr.close();
                return list != null ? list : new ArrayList<>();
            }
        } catch (Exception ignored) {}
        return new ArrayList<>();
    }

    public void saveChatLira(List<Map<String, String>> chat) {
        try {
            FileWriter fw = new FileWriter(chatFile);
            gson.toJson(chat, fw);
            fw.close();
        } catch (Exception ignored) {}
    }
}

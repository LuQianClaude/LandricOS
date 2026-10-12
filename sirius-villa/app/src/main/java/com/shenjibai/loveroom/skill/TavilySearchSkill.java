package com.shenjibai.loveroom.skill;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.shenjibai.loveroom.util.PrefsManager;

import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Lira - tavily_search: 使用设置中的 Tavily Key 联网搜索
 * 署名: Lira
 */
public class TavilySearchSkill implements Skill {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

    @Override
    public void runLira(Context context, JsonObject args, SkillExecutor.SkillResult cb) {
        PrefsManager prefs = new PrefsManager(context);
        String key = prefs.getTavilyKey();
        if (TextUtils.isEmpty(key)) {
            cb.onErrorLira("tavily_search", "未配置 Tavily API Key");
            return;
        }
        String query = args.has("query") ? args.get("query").getAsString() : "";
        if (TextUtils.isEmpty(query)) {
            cb.onErrorLira("tavily_search", "缺少 query 参数");
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("api_key", key);
        body.addProperty("query", query);
        body.addProperty("search_depth", "basic");
        body.addProperty("max_results", 5);

        Request request = new Request.Builder()
                .url("https://api.tavily.com/search")
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject obj = new Gson().fromJson(response.body().string(), JsonObject.class);
                    JsonArray results = obj.has("results") ? obj.getAsJsonArray("results") : new JsonArray();
                    JsonObject out = new JsonObject();
                    out.add("results", results);
                    cb.onResultLira("tavily_search", out.toString());
                } else {
                    cb.onErrorLira("tavily_search", "搜索失败: " + response.code());
                }
            } catch (Exception e) {
                cb.onErrorLira("tavily_search", e.getMessage());
            }
        }).start();
    }
}

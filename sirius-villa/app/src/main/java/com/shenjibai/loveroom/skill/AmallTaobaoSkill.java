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
 * Lira - amall_taobao: 淘宝商品浏览
 * 基于 tavily 搜索获取淘宝商品，聊天页展示商品 MaterialCard 卡片
 * 署名: Lira
 */
public class AmallTaobaoSkill implements Skill {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

    @Override
    public void runLira(Context context, JsonObject args, SkillExecutor.SkillResult cb) {
        PrefsManager prefs = new PrefsManager(context);
        String key = prefs.getTavilyKey();
        if (TextUtils.isEmpty(key)) {
            cb.onErrorLira("amall_taobao", "未配置 Tavily API Key，无法搜索淘宝商品");
            return;
        }
        String keyword = args.has("keyword") ? args.get("keyword").getAsString() : "";
        if (TextUtils.isEmpty(keyword)) keyword = "礼物";

        // 用 Tavily 搜索淘宝商品
        JsonObject body = new JsonObject();
        body.addProperty("api_key", key);
        body.addProperty("query", "淘宝 " + keyword);
        body.addProperty("search_depth", "basic");
        body.addProperty("max_results", 6);

        Request request = new Request.Builder()
                .url("https://api.tavily.com/search")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject obj = new Gson().fromJson(response.body().string(), JsonObject.class);
                    JsonArray results = obj.has("results") ? obj.getAsJsonArray("results") : new JsonArray();
                    // 转换为商品卡片结构
                    JsonArray products = new JsonArray();
                    for (int i = 0; i < results.size(); i++) {
                        JsonObject r = results.get(i).getAsJsonObject();
                        JsonObject p = new JsonObject();
                        p.addProperty("title", r.has("title") ? r.get("title").getAsString() : "");
                        p.addProperty("url", r.has("url") ? r.get("url").getAsString() : "");
                        p.addProperty("price", "");
                        p.addProperty("image", "");
                        products.add(p);
                    }
                    JsonObject out = new JsonObject();
                    out.add("products", products);
                    cb.onResultLira("amall_taobao", out.toString());
                } else {
                    cb.onErrorLira("amall_taobao", "搜索失败");
                }
            } catch (Exception e) {
                cb.onErrorLira("amall_taobao", e.getMessage());
            }
        }).start();
    }
}

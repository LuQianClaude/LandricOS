package com.shenjibai.loveroom.net;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.shenjibai.loveroom.util.PrefsManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Lira - 大模型流式客户端（SSE 逐字输出）
 * 所有 URL/Key 来自 PrefsManager，不硬编码
 * 署名: Lira
 */
public class LlmClient {

    public interface StreamCallback {
        void onTokenLira(String token);
        void onDoneLira(String fullText);
        void onErrorLira(String error);
        // 工具调用：模型返回 JSON 指令
        void onToolCallLira(String toolName, String toolArgs);
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final Gson gson = new Gson();

    public LlmClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 流式聊天请求
     * @param systemPrompt 人设 System Prompt
     * @param messages 历史消息（role/content）
     */
    public void chatStreamLira(PrefsManager prefs, String systemPrompt,
                               List<String[]> messages, StreamCallback cb) {
        String url = prefs.getLlmUrl();
        String key = prefs.getLlmKey();
        String model = prefs.getLlmModel();

        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(key)) {
            cb.onErrorLira("请先在设置页填写对话大模型的 API URL 与 Key");
            return;
        }

        // 构建消息体
        JsonArray msgArray = new JsonArray();
        if (!TextUtils.isEmpty(systemPrompt)) {
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", systemPrompt);
            msgArray.add(sys);
        }
        for (String[] m : messages) {
            JsonObject o = new JsonObject();
            o.addProperty("role", m[0]);      // user / assistant
            o.addProperty("content", m[1]);
            msgArray.add(o);
        }

        JsonObject body = new JsonObject();
        if (!TextUtils.isEmpty(model)) body.addProperty("model", model);
        body.add("messages", msgArray);
        body.addProperty("stream", true);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        new Thread(() -> {
            StringBuilder full = new StringBuilder();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    cb.onErrorLira("请求失败: " + response.code());
                    return;
                }
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    // SSE 格式: data: {...}
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) break;
                        if (data.isEmpty()) continue;
                        try {
                            JsonObject obj = gson.fromJson(data, JsonObject.class);
                            // 兼容 OpenAI 风格 choices[0].delta.content
                            if (obj.has("choices")) {
                                JsonObject delta = obj.getAsJsonArray("choices")
                                        .get(0).getAsJsonObject()
                                        .getAsJsonObject("delta");
                                if (delta.has("content") && !delta.get("content").isJsonNull()) {
                                    String token = delta.get("content").getAsString();
                                    full.append(token);
                                    cb.onTokenLira(token);
                                }
                                // 工具调用：tool_calls
                                if (delta.has("tool_calls")) {
                                    JsonObject tc = delta.getAsJsonArray("tool_calls")
                                            .get(0).getAsJsonObject()
                                            .getAsJsonObject("function");
                                    String name = tc.get("name").getAsString();
                                    String args = tc.has("arguments")
                                            ? tc.get("arguments").getAsString() : "{}";
                                    cb.onToolCallLira(name, args);
                                }
                            }
                        } catch (Exception ignored) {
                            // 单条解析失败继续
                        }
                    }
                }
                cb.onDoneLira(full.toString());
            } catch (Exception e) {
                cb.onErrorLira(e.getMessage());
            }
        }).start();
    }

    /** 非流式请求（用于哨兵、主动消息等） */
    public String chatOnceLira(PrefsManager prefs, String systemPrompt,
                               List<String[]> messages) {
        String url = prefs.getLlmUrl();
        String key = prefs.getLlmKey();
        String model = prefs.getLlmModel();
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(key)) return "";

        JsonArray msgArray = new JsonArray();
        if (!TextUtils.isEmpty(systemPrompt)) {
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", systemPrompt);
            msgArray.add(sys);
        }
        for (String[] m : messages) {
            JsonObject o = new JsonObject();
            o.addProperty("role", m[0]);
            o.addProperty("content", m[1]);
            msgArray.add(o);
        }
        JsonObject body = new JsonObject();
        if (!TextUtils.isEmpty(model)) body.addProperty("model", model);
        body.add("messages", msgArray);
        body.addProperty("stream", false);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + key)
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject obj = gson.fromJson(response.body().string(), JsonObject.class);
                if (obj.has("choices")) {
                    return obj.getAsJsonArray("choices").get(0).getAsJsonObject()
                            .getAsJsonObject("message").get("content").getAsString();
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    /**
     * Lira - 非流式请求（显式指定凭证，供哨兵使用独立模型 Key）
     */
    public String chatOnceWithKeyLira(String url, String key, String model,
                                      String systemPrompt, List<String[]> messages) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(key)) return "";

        JsonArray msgArray = new JsonArray();
        if (!TextUtils.isEmpty(systemPrompt)) {
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", systemPrompt);
            msgArray.add(sys);
        }
        for (String[] m : messages) {
            JsonObject o = new JsonObject();
            o.addProperty("role", m[0]);
            o.addProperty("content", m[1]);
            msgArray.add(o);
        }
        JsonObject body = new JsonObject();
        if (!TextUtils.isEmpty(model)) body.addProperty("model", model);
        body.add("messages", msgArray);
        body.addProperty("stream", false);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + key)
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonObject obj = gson.fromJson(response.body().string(), JsonObject.class);
                if (obj.has("choices")) {
                    return obj.getAsJsonArray("choices").get(0).getAsJsonObject()
                            .getAsJsonObject("message").get("content").getAsString();
                }
            }
        } catch (Exception ignored) {}
        return "";
    }
}

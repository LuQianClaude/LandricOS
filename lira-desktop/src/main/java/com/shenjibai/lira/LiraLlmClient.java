package com.shenjibai.lira;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Lira - 桌面端大模型流式客户端
 * 复用安卓端逻辑，URL/Key 来自 LiraStorage
 * 署名: Lira
 */
public class LiraLlmClient {

    public interface StreamCallback {
        void onTokenLira(String token);
        void onDoneLira(String full);
        void onErrorLira(String error);
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build();
    private final Gson gson = new Gson();
    private final LiraStorage storage;

    public LiraLlmClient(LiraStorage storage) {
        this.storage = storage;
    }

    public void chatStreamLira(String systemPrompt, List<String[]> messages, StreamCallback cb) {
        String url = storage.getSettingLira("llm_url");
        String key = storage.getSettingLira("llm_key");
        String model = storage.getSettingLira("llm_model");

        if (url.isEmpty() || key.isEmpty()) {
            cb.onErrorLira("请先在设置页填写对话大模型 API URL 与 Key");
            return;
        }

        JsonArray msgArray = new JsonArray();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
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
        if (!model.isEmpty()) body.addProperty("model", model);
        body.add("messages", msgArray);
        body.addProperty("stream", true);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + key)
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
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) break;
                        if (data.isEmpty()) continue;
                        try {
                            JsonObject obj = gson.fromJson(data, JsonObject.class);
                            if (obj.has("choices")) {
                                JsonObject delta = obj.getAsJsonArray("choices")
                                        .get(0).getAsJsonObject().getAsJsonObject("delta");
                                if (delta.has("content") && !delta.get("content").isJsonNull()) {
                                    String token = delta.get("content").getAsString();
                                    full.append(token);
                                    cb.onTokenLira(token);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
                cb.onDoneLira(full.toString());
            } catch (Exception e) {
                cb.onErrorLira(e.getMessage());
            }
        }).start();
    }
}

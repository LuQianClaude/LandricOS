package com.shenjibai.loveroom.skill;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.shenjibai.loveroom.util.PrefsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Lira - generate_selfie【发自拍】
 * 读取 assets/selfie_ref 下参考图作为形象锚点，调用生图 API 生成图片
 * 下载到私有缓存，以图片消息插入聊天
 * 署名: Lira
 */
public class GenerateSelfieSkill implements Skill {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build();

    @Override
    public void runLira(Context context, JsonObject args, SkillExecutor.SkillResult cb) {
        PrefsManager prefs = new PrefsManager(context);
        String url = prefs.getSelfieUrl();
        String key = prefs.getSelfieKey();
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(key)) {
            cb.onErrorLira("generate_selfie", "未配置生图 API URL 与 Key");
            return;
        }

        String prompt = args.has("prompt") ? args.get("prompt").getAsString() : "自拍";

        // 读取 assets/selfie_ref 参考图作为锚点
        JsonArray refImages = new JsonArray();
        try {
            String[] files = context.getAssets().list("selfie_ref");
            if (files != null && files.length > 0) {
                // 取第一张作为锚点参考
                InputStream is = context.getAssets().open("selfie_ref/" + files[0]);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp != null) {
                    String b64 = bitmapToBase64Lira(bmp);
                    refImages.add(b64);
                    bmp.recycle();
                }
                is.close();
            }
        } catch (Exception ignored) {}

        JsonObject body = new JsonObject();
        body.addProperty("prompt", prompt);
        if (refImages.size() > 0) body.add("reference_images", refImages);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject obj = new Gson().fromJson(response.body().string(), JsonObject.class);
                    String imgBase64 = "";
                    // 兼容多种返回结构
                    if (obj.has("data") && obj.getAsJsonArray("data").size() > 0) {
                        JsonObject d = obj.getAsJsonArray("data").get(0).getAsJsonObject();
                        if (d.has("b64_json")) imgBase64 = d.get("b64_json").getAsString();
                        else if (d.has("url")) {
                            // 下载 URL
                            imgBase64 = downloadUrlAsBase64Lira(d.get("url").getAsString());
                        }
                    } else if (obj.has("images")) {
                        imgBase64 = obj.getAsJsonArray("images").get(0).getAsString();
                    }

                    if (!TextUtils.isEmpty(imgBase64)) {
                        File cacheFile = saveToCacheLira(context, imgBase64);
                        if (cacheFile != null) {
                            JsonObject out = new JsonObject();
                            out.addProperty("image_path", cacheFile.getAbsolutePath());
                            cb.onResultLira("generate_selfie", out.toString());
                            return;
                        }
                    }
                    cb.onErrorLira("generate_selfie", "生成图片失败");
                } else {
                    cb.onErrorLira("generate_selfie", "生图请求失败: " + response.code());
                }
            } catch (Exception e) {
                cb.onErrorLira("generate_selfie", e.getMessage());
            }
        }).start();
    }

    private String bitmapToBase64Lira(Bitmap bmp) {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    private String downloadUrlAsBase64Lira(String url) {
        try (Response response = client.newCall(new Request.Builder().url(url).build()).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                byte[] bytes = response.body().bytes();
                return Base64.encodeToString(bytes, Base64.NO_WRAP);
            }
        } catch (Exception ignored) {}
        return "";
    }

    private File saveToCacheLira(Context context, String base64) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            File dir = new File(context.getCacheDir(), "selfies");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "selfie_" + System.currentTimeMillis() + ".png");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();
            return file;
        } catch (Exception e) {
            return null;
        }
    }
}

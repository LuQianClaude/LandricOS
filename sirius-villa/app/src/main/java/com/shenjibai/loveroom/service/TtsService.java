package com.shenjibai.loveroom.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shenjibai.loveroom.util.PrefsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Lira - TTS 语音朗读 Service
 * 只朗读沈既白回复；读取设置页 TTS-API-URL / TTS-API-Key
 * 署名: Lira
 */
public class TtsService extends Service {

    public static final String ACTION_SPEAK = "com.shenjibai.loveroom.SPEAK";
    public static final String EXTRA_TEXT = "extra_text";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build();
    private MediaPlayer mediaPlayer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_SPEAK.equals(intent.getAction())) {
            String text = intent.getStringExtra(EXTRA_TEXT);
            if (!TextUtils.isEmpty(text)) {
                speakLira(text);
            }
        }
        return START_NOT_STICKY;
    }

    /** Lira - 调用 TTS API 并播放 */
    private void speakLira(String text) {
        PrefsManager prefs = new PrefsManager(this);
        String url = prefs.getTtsUrl();
        String key = prefs.getTtsKey();
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(key)) {
            stopSelf();
            return;
        }

        new Thread(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("input", text);
                body.addProperty("voice", "shen");
                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + key)
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(body.toString(),
                                MediaType.parse("application/json; charset=utf-8")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        byte[] audioBytes = response.body().bytes();
                        File tmp = new File(getCacheDir(), "tts_tmp.mp3");
                        FileOutputStream fos = new FileOutputStream(tmp);
                        fos.write(audioBytes);
                        fos.close();
                        playAudioLira(tmp.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                // 静默失败
            }
        }).start();
    }

    private void playAudioLira(String path) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
                stopSelf();
            });
            mediaPlayer.start();
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}

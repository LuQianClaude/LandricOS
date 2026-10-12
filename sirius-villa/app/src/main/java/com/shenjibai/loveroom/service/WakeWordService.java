package com.shenjibai.loveroom.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.shenjibai.loveroom.MainActivity;
import com.shenjibai.loveroom.R;
import com.shenjibai.loveroom.util.PrefsManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Lira - 语音唤醒 Service
 * 后台持续麦克风监听唤醒词，触发后激活聊天会话
 * 需要录音权限；可在设置开关
 * 署名: Lira
 */
public class WakeWordService extends Service {

    private static final String CHANNEL_ID = "lira_wake_channel";
    private static final int SAMPLE_RATE = 16000;

    private AudioRecord audioRecord;
    private volatile boolean listening = false;
    private Thread listenThread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannelLira();
        startForeground(1001, buildNotificationLira());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopListeningLira();
            stopSelf();
            return START_NOT_STICKY;
        }
        startListeningLira();
        return START_STICKY;
    }

    private void createNotificationChannelLira() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "语音唤醒", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotificationLira() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Lira 语音唤醒中")
                .setContentText("正在聆听唤醒词…")
                .setSmallIcon(R.drawable.ic_mic)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void startListeningLira() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return;
        }
        listening = true;
        listenThread = new Thread(this::listenLoopLira);
        listenThread.start();
    }

    /** Lira - 简单唤醒词检测：基于能量阈值 + 关键词识别占位 */
    private void listenLoopLira() {
        PrefsManager prefs = new PrefsManager(this);
        String wakeWord = prefs.getWakeWord();
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            audioRecord.startRecording();

            short[] buffer = new short[bufferSize / 2];
            while (listening) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read <= 0) continue;

                // 计算音量
                double sum = 0;
                for (int i = 0; i < read; i++) sum += buffer[i] * buffer[i];
                double rms = Math.sqrt(sum / read);

                // 超过阈值认为有声音（简化版唤醒，实际可接入 Vosk/Porcupine 等）
                if (rms > 2000) {
                    onWakeDetectedLira();
                    // 冷却
                    Thread.sleep(3000);
                }
            }
        } catch (Exception e) {
            // 录音失败
        }
    }

    private void onWakeDetectedLira() {
        // 唤醒后激活聊天会话并发送系统通知
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("wake_up", true);
        startActivity(intent);

        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("沈既白")
                .setContentText("我在，昭昭。")
                .setSmallIcon(R.drawable.ic_chat)
                .setAutoCancel(true);
        nm.notify(2001, b.build());
    }

    private void stopListeningLira() {
        listening = false;
        if (audioRecord != null) {
            try { audioRecord.stop(); audioRecord.release(); } catch (Exception ignored) {}
            audioRecord = null;
        }
    }

    @Override
    public void onDestroy() {
        stopListeningLira();
        super.onDestroy();
    }
}

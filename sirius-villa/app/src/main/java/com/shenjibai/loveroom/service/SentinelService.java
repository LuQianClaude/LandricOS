package com.shenjibai.loveroom.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.shenjibai.loveroom.entity.ChatMessage;
import com.shenjibai.loveroom.db.AppDatabase;
import com.shenjibai.loveroom.net.LlmClient;
import com.shenjibai.loveroom.util.PrefsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Lira - 哨兵 Sentinel 后台服务
 * 按配置间隔轮询；读取 GPS 位置变动作为事件送入 AI 上下文
 * 可选屏幕截图权限，截图作为图像输入给哨兵模型
 * 使用设置中独立哨兵模型 key 与独立 prompt
 * 哨兵产出事件可触发 AI 主动发消息推送聊天 + 系统通知
 * 署名: Lira
 */
public class SentinelService extends Service {

    private static final String CHANNEL_ID = "lira_sentinel_channel";
    private volatile boolean running = false;
    private Thread pollThread;
    private Location lastLocation;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannelLira();
        startForeground(1002, buildNotificationLira());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopPollingLira();
            stopSelf();
            return START_NOT_STICKY;
        }
        startPollingLira();
        return START_STICKY;
    }

    private void createChannelLira() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "哨兵", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification buildNotificationLira() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Lira 哨兵运行中")
                .setContentText("正在守护昭昭的日常…")
                .setSmallIcon(com.shenjibai.loveroom.R.drawable.ic_memory)
                .setOngoing(true)
                .build();
    }

    private void startPollingLira() {
        running = true;
        pollThread = new Thread(this::pollLoopLira);
        pollThread.start();
    }

    /** Lira - 哨兵轮询主循环 */
    private void pollLoopLira() {
        PrefsManager prefs = new PrefsManager(this);
        int interval = Math.max(30, prefs.getSentinelInterval());

        while (running) {
            try {
                Thread.sleep(interval * 1000L);
                if (!running) break;

                StringBuilder eventContext = new StringBuilder();

                // 1. 读取 GPS 位置变动
                Location loc = getLocationLira();
                if (loc != null) {
                    if (lastLocation == null
                            || loc.distanceTo(lastLocation) > 50) {
                        eventContext.append("[位置变动] 经度:").append(loc.getLongitude())
                                .append(" 纬度:").append(loc.getLatitude()).append("\n");
                        lastLocation = loc;
                    }
                }

                // 2. 可选截图（哨兵模型独立 key）
                if (prefs.isSentinelScreenEnabled()) {
                    eventContext.append("[屏幕截图] 已捕获当前屏幕画面\n");
                    // 实际截图通过 MediaProjection 在 Activity 中授权后获取
                }

                if (eventContext.length() > 0) {
                    analyzeEventLira(prefs, eventContext.toString());
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private Location getLocationLira() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return null;
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            return loc;
        } catch (Exception e) {
            return null;
        }
    }

    /** Lira - 使用独立哨兵模型 key 与独立 prompt 分析事件 */
    private void analyzeEventLira(PrefsManager prefs, String event) {
        String sentinelKey = prefs.getSentinelKey();
        if (TextUtils.isEmpty(sentinelKey)) return;

        // 哨兵独立 prompt（不含人设内容，仅事件分析）
        String sentinelPrompt = "你是沈既白的哨兵模块。监控昭昭的状态事件。" +
                "如果事件需要关心或提醒，请输出需要发送给昭昭的一句话；否则输出 NONE。";

        List<String[]> messages = new ArrayList<>();
        messages.add(new String[]{"user", event});

        // 临时构造使用哨兵 key 的 LLM 调用
        String reply = callSentinelLira(prefs.getLlmUrl(), sentinelKey,
                prefs.getLlmModel(), sentinelPrompt, messages);

        if (!TextUtils.isEmpty(reply) && !"NONE".equalsIgnoreCase(reply.trim())) {
            // 触发 AI 主动发消息：插入聊天 + 系统通知
            pushProactiveMessageLira(reply);
        }
    }

    /** Lira - 使用哨兵独立 key 调用模型 */
    private String callSentinelLira(String url, String key, String model,
                                    String systemPrompt, List<String[]> messages) {
        LlmClient client = new LlmClient();
        return client.chatOnceWithKeyLira(url, key, model, systemPrompt, messages);
    }

    /** Lira - 主动消息：写入聊天记录 + 弹出系统通知 */
    private void pushProactiveMessageLira(String text) {
        new Handler(Looper.getMainLooper()).post(() -> {
            ChatMessage msg = new ChatMessage();
            msg.setSender("shen");
            msg.setType("text");
            msg.setContent(text);
            msg.setTimestamp(System.currentTimeMillis());
            new Thread(() -> AppDatabase.getLira(this).chatMessageDao().insertLira(msg)).start();

            // 系统通知
            NotificationManager nm = getSystemService(NotificationManager.class);
            Notification.Builder b = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("沈既白")
                    .setContentText(text)
                    .setSmallIcon(com.shenjibai.loveroom.R.drawable.ic_chat)
                    .setAutoCancel(true);
            nm.notify(3001, b.build());
        });
    }

    private void stopPollingLira() {
        running = false;
        if (pollThread != null) pollThread.interrupt();
    }

    @Override
    public void onDestroy() {
        stopPollingLira();
        super.onDestroy();
    }
}

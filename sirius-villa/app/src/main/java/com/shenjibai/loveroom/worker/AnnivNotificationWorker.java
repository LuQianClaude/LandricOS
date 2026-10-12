package com.shenjibai.loveroom.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.shenjibai.loveroom.MainActivity;
import com.shenjibai.loveroom.R;
import com.shenjibai.loveroom.db.AppDatabase;
import com.shenjibai.loveroom.entity.Anniversary;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Lira - 纪念日系统通知提醒（WorkManager）
 * 署名: Lira
 */
public class AnnivNotificationWorker extends Worker {

    private static final String CHANNEL_ID = "lira_anniv_channel";

    public AnnivNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            createChannelLira();
            List<Anniversary> list = AppDatabase.getLira(getApplicationContext())
                    .anniversaryDao().getAllLira();

            Calendar today = Calendar.getInstance();
            SimpleDateFormat fmt = new SimpleDateFormat("MM-dd", Locale.getDefault());
            String todayStr = fmt.format(today.getTime());

            for (Anniversary a : list) {
                if (a.getDate() != null && a.getDate().endsWith(todayStr)) {
                    sendNotificationLira(a.getName());
                }
            }
        } catch (Exception e) {
            return Result.retry();
        }
        return Result.success();
    }

    private void createChannelLira() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "纪念日提醒", NotificationManager.IMPORTANCE_HIGH);
            getApplicationContext().getSystemService(NotificationManager.class)
                    .createNotificationChannel(ch);
        }
    }

    private void sendNotificationLira(String name) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),
                0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(
                getApplicationContext(), CHANNEL_ID)
                .setContentTitle("纪念日提醒")
                .setContentText("今天是「" + name + "」")
                .setSmallIcon(R.drawable.ic_anniv)
                .setContentIntent(pi)
                .setAutoCancel(true);

        NotificationManager nm = getApplicationContext()
                .getSystemService(NotificationManager.class);
        nm.notify((int) System.currentTimeMillis(), b.build());
    }
}

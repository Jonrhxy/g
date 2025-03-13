package com.example.cverdetotoo;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class NotificationForegroundService extends Service {

    private static final String CHANNEL_ID = "foreground_channel";
    private static final int FOREGROUND_NOTIFICATION_ID = 1000;
    // The interval for checking whether to post a notification (2 minutes).
    private static final long INTERVAL_MS = TimeUnit.HOURS.toMillis(4);

    private Handler handler;
    private Runnable notificationRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        handler = new Handler(Looper.getMainLooper());

        // This runnable will check every 2 minutes:
        // If the app is not in the foreground AND 2 minutes have passed since the user left the app,
        // it will post a motivational notification.
        notificationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!MainActivity.isForeground &&
                        (System.currentTimeMillis() - MainActivity.lastBackgroundTime) >= INTERVAL_MS) {

                    // Choose a random motivational message.
                    String[] messages = {
                            "Every ton of CO₂ reduced makes a difference for our planet.",
                            "Small actions can collectively cut carbon emissions.",
                            "Sustainable choices today ensure a healthier Earth for tomorrow.",
                            "Reducing your carbon footprint is an investment in a brighter future.",
                            "Switching to renewable energy powers a cleaner, greener world.",
                            "Every tree planted absorbs CO₂ and brings us closer to a sustainable future.",
                            "Recycling helps reduce waste and conserve energy.",
                            "Don't forget to take a challenge to earn points!",
                            "Energy conservation is a simple way to protect our climate.",
                            "Mindful consumption preserves natural resources for future generations.",
                            "Every sustainable choice helps combat climate change."
                    };
                    String message = messages[new Random().nextInt(messages.length)];

                    // Create an intent to launch MainActivity when the notification is tapped.
                    Intent intent = new Intent(NotificationForegroundService.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    PendingIntent pendingIntent = PendingIntent.getActivity(
                            NotificationForegroundService.this,
                            0,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE
                    );

                    // Build the motivational notification.
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(NotificationForegroundService.this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle("C-Verde Facts")
                            .setContentText(message)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);

                    // Post the notification if POST_NOTIFICATIONS permission is granted.
                    if (ActivityCompat.checkSelfPermission(NotificationForegroundService.this,
                            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        int notificationId = new Random().nextInt(10000);
                        NotificationManagerCompat.from(NotificationForegroundService.this)
                                .notify(notificationId, builder.build());
                    }
                }
                // Re-schedule this check for another 2 minutes.
                handler.postDelayed(this, INTERVAL_MS);
            }
        };

        // Start the first check after 2 minutes.
        handler.postDelayed(notificationRunnable, INTERVAL_MS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create a minimal persistent notification for the foreground service.
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("C-Verde")
                .setContentText("Let's reduce our carbon emissions!")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        startForeground(FOREGROUND_NOTIFICATION_ID, builder.build());
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Foreground Service Channel";
            String description = "Channel for foreground service notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (handler != null && notificationRunnable != null) {
            handler.removeCallbacks(notificationRunnable);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not used since this is a started service.
    }
}
package com.worthlessapps.www.dailyduckface;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    public AlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("", "Alarm received");

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(context.getApplicationContext());

        Intent newIntent = new Intent(context.getApplicationContext(), MainActivity.class);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent
                .getActivity(context.getApplicationContext(), 0, newIntent, PendingIntent.FLAG_ONE_SHOT);
        builder.setContentText("Get your duck face on, it's time for a selfie!");
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle("Daily Duck Face");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setAutoCancel(true);
        builder.setSound(Uri.parse(
                "android.resource://com.worthlessapps.www.dailyduckface/" + R.raw.quack));
        //notificationManager.notify(7, builder.build());
    }
}

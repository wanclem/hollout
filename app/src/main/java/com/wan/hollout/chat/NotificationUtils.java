package com.wan.hollout.chat;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.text.Spanned;

import com.parse.ParseUser;
import com.wan.hollout.R;
import com.wan.hollout.components.ApplicationLoader;
import com.wan.hollout.ui.activities.UserProfileActivity;
import com.wan.hollout.utils.AppConstants;
import com.wan.hollout.utils.HolloutUtils;
import com.wan.hollout.utils.UiUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * @author Wan Clem
 */

public class NotificationUtils {

    public static final String TAG = NotificationUtils.class.getSimpleName();

    @SuppressLint("StaticFieldLeak")
    private static Context context = ApplicationLoader.getInstance();

    private static NotificationManager getNotificationManager() {
        return (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    }

    public static void displayIndividualChatRequestNotification(final ParseUser requester) {

        HolloutCommunicationsManager.getInstance().execute(new Runnable() {

            @Override
            public void run() {

                Intent userProfileIntent = new Intent(context, UserProfileActivity.class);
                userProfileIntent.putExtra(AppConstants.USER_PROPERTIES, requester);

                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, userProfileIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                String senderName = requester.getString(AppConstants.APP_USER_DISPLAY_NAME);
                String requesterPhoto = requester.getString(AppConstants.APP_USER_PROFILE_PHOTO_URL);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                builder.setContentTitle(context.getString(R.string.app_name));
                Spanned message = UiUtils.fromHtml("<b>" + senderName + "</b> wants to chat with you");
                builder.setContentText(message);
                builder.setTicker(message);
                builder.setSmallIcon(R.mipmap.ic_launcher);
                builder.setLights(Color.parseColor("blue"), 500, 1000);
                Bitmap notifInitiatorBitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
                if (StringUtils.isNotEmpty(requesterPhoto)) {
                    notifInitiatorBitmap = getBitmapFromURL(requesterPhoto);
                }
                builder.setLargeIcon(notifInitiatorBitmap);
                builder.setAutoCancel(true);
                builder.setContentIntent(pendingIntent);
                builder.setContentText(message);
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message).setBigContentTitle(context.getString(R.string.app_name)));
                Notification notification = builder.build();
                notification.defaults |= Notification.DEFAULT_LIGHTS;
                if (AppConstants.UNACKNOWLEDGED_CHAT_REQUESTS_COUNT == 0) {
                    notification.defaults |= Notification.DEFAULT_VIBRATE;
                }
                notification.defaults |= Notification.DEFAULT_SOUND;
                if (pendingIntent != null) {
                    getNotificationManager().notify(AppConstants.CHAT_REQUEST_NOTIFICATION_ID, notification);
                    AppConstants.UNACKNOWLEDGED_CHAT_REQUESTS_COUNT = AppConstants.UNACKNOWLEDGED_CHAT_REQUESTS_COUNT + 1;
                }

            }

        });

    }

    public static void displayKindIsNearbyNotification(final ParseUser requester) {

        HolloutCommunicationsManager.getInstance().execute(new Runnable() {

            @Override
            public void run() {

                ParseUser signedInUser = ParseUser.getCurrentUser();
                List<String> aboutUser = requester.getList(AppConstants.ABOUT_USER);
                List<String> aboutSignedInUser = signedInUser.getList(AppConstants.ABOUT_USER);
                if (aboutUser != null && aboutSignedInUser != null) {
                    try {
                        List<String> common = new ArrayList<>(aboutUser);
                        common.retainAll(aboutSignedInUser);
                        String firstInterest = !common.isEmpty() ? common.get(0) : aboutUser.get(0);
                        if (StringUtils.isNotEmpty(firstInterest)) {
                            String senderName = requester.getString(AppConstants.APP_USER_DISPLAY_NAME);
                            String requesterPhoto = requester.getString(AppConstants.APP_USER_PROFILE_PHOTO_URL);
                            Spanned message = UiUtils.fromHtml("<b>" + senderName + "</b>, " + HolloutUtils.getQualifier(firstInterest) + " " + "<b>" + StringUtils.capitalize(firstInterest) + "</b> like you is nearby. Say hi!");
                            Intent userProfileIntent = new Intent(context, UserProfileActivity.class);
                            userProfileIntent.putExtra(AppConstants.USER_PROPERTIES, requester);
                            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, userProfileIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                            builder.setContentTitle(context.getString(R.string.app_name));
                            builder.setContentText(message);
                            builder.setTicker(message);
                            builder.setSmallIcon(R.mipmap.ic_launcher);
                            builder.setLights(Color.parseColor("blue"), 500, 1000);
                            Bitmap notifInitiatorBitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
                            if (StringUtils.isNotEmpty(requesterPhoto)) {
                                notifInitiatorBitmap = getBitmapFromURL(requesterPhoto);
                            }
                            builder.setLargeIcon(notifInitiatorBitmap);
                            builder.setAutoCancel(true);
                            builder.setContentIntent(pendingIntent);
                            builder.setContentText(message);
                            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
                            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message).setBigContentTitle(context.getString(R.string.app_name)));
                            Notification notification = builder.build();
                            notification.defaults |= Notification.DEFAULT_LIGHTS;
                            if (AppConstants.NEARBY_KIND_NOTIFICATION_COUNT == 0) {
                                notification.defaults |= Notification.DEFAULT_VIBRATE;
                            }
                            notification.defaults |= Notification.DEFAULT_SOUND;
                            if (pendingIntent != null) {
                                getNotificationManager().notify(AppConstants.NEARBY_KIND_NOTIFICATION_ID, notification);
                                AppConstants.NEARBY_KIND_NOTIFICATION_COUNT = AppConstants.NEARBY_KIND_NOTIFICATION_COUNT + 1;
                            }
                        }
                    } catch (NullPointerException ignored) {

                    }
                }
            }
        });
    }

    private static Bitmap getBitmapFromURL(final String strURL) {
        Callable<Bitmap> bitmapCallable = new Callable<Bitmap>() {

            @Override
            public Bitmap call() throws Exception {
                try {
                    URL url = new URL(strURL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    return BitmapFactory.decodeStream(input);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };

        FutureTask<Bitmap> bitmapFutureTask = new FutureTask<>(bitmapCallable);
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(bitmapFutureTask);

        try {
            return bitmapFutureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

}

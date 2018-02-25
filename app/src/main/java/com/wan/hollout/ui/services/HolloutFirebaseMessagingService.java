package com.wan.hollout.ui.services;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.wan.hollout.utils.AppConstants;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author Wan Clem
 */

@SuppressLint("LongLogTag")
public class HolloutFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "HolloutFirebaseMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> notifData = remoteMessage.getData();
        if (notifData != null) {
            String notificationType = notifData.get(AppConstants.NOTIFICATION_TYPE);
            if (notificationType != null) {
                if (notificationType.equals(AppConstants.NOTIFICATION_TYPE_AM_NEARBY)) {
                    String senderId = notifData.get(AppConstants.REAL_OBJECT_ID);
                    if (StringUtils.isNotEmpty(senderId)) {
                        Bundle userInfoExtras = new Bundle();
                        userInfoExtras.putString(AppConstants.EXTRA_USER_ID, senderId);
                        userInfoExtras.putString(AppConstants.NOTIFICATION_TYPE, AppConstants.NOTIFICATION_TYPE_AM_NEARBY);
                        FetchUserInfoService fetchUserInfoService = new FetchUserInfoService();
                        fetchUserInfoService.onHandleWork(userInfoExtras);
                    }
                }
            }
        }
    }

}

package com.wan.hollout.ui.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.wan.hollout.callbacks.DoneCallback;
import com.wan.hollout.components.ApplicationLoader;
import com.wan.hollout.utils.AppConstants;
import com.wan.hollout.utils.AppStateManager;
import com.wan.hollout.utils.AuthUtil;
import com.wan.hollout.utils.HolloutLogger;
import com.wan.hollout.utils.HolloutPreferences;
import com.wan.hollout.utils.HolloutUtils;
import com.wan.hollout.utils.NotificationCenter;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Wan Clem
 */

public class AppInstanceDetectionService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GetLocationTask getLocationTask;
    //Activity's Google Api Client
    private GoogleApiClient googleApiClient;
    //Activity's LocationRequest
    private LocationRequest mLocationRequest;
    private String TAG = AppInstanceDetectionService.class.getSimpleName();

    private ParseObject signedInUser;

    private AppStateManager appStateManager;

    private AppStateManager.Listener myListener = new AppStateManager.Listener() {

        public void onBecameForeground() {
            signedInUser.put(AppConstants.APP_USER_ONLINE_STATUS, AppConstants.ONLINE);
            signedInUser.put(AppConstants.APP_USER_LAST_SEEN, System.currentTimeMillis());
            updateSignedInUserProps(false);
        }

        public void onBecameBackground() {
            //This is also a good place to blow new message notifications for a foregrounded app
            signedInUser.put(AppConstants.APP_USER_ONLINE_STATUS, AppConstants.OFFLINE);
            signedInUser.put(AppConstants.APP_USER_LAST_SEEN, System.currentTimeMillis());
            updateSignedInUserProps(false);
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();
        attemptToConnectGoogleApiClient();
        signedInUser = AuthUtil.getCurrentUser();
        appStateManager = AppStateManager.init(ApplicationLoader.getInstance());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (signedInUser == null) {
            signedInUser = AuthUtil.getCurrentUser();
        }
        if (appStateManager == null) {
            appStateManager = AppStateManager.init(ApplicationLoader.getInstance());
        }
        if (myListener != null) {
            try {
                AppStateManager.get(this).addListener(myListener);
            } catch (IllegalStateException ignored) {

            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (appStateManager != null && myListener != null) {
                AppStateManager.get(this).removeListener(myListener);
            }
        } catch (IllegalStateException | NullPointerException ignored) {

        }
    }

    private class GetLocationTask extends AsyncTask<Location, Void, Void> {

        private Context context;
        // Create a list to contain the result address
        private List<Address> addresses;

        GetLocationTask(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(final Location... params) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            // Get the current location from the input parameter list
            final Location loc = params[0];
            if (signedInUser != null) {
                ParseGeoPoint userGeoPoint = new ParseGeoPoint();
                userGeoPoint.setLatitude(loc.getLatitude());
                userGeoPoint.setLongitude(loc.getLongitude());
                signedInUser.put(AppConstants.APP_USER_GEO_POINT, userGeoPoint);
            }
            try {
                addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            } catch (IOException | IllegalArgumentException ignored) {

            }
            if (addresses != null && addresses.size() > 0 && signedInUser != null) {
                // Get the first address
                Address address = addresses.get(0);
                // COUNTRY
                final String countryName = address.getCountryName();
                //COUNTRY
                if (StringUtils.isNotEmpty(countryName)) {
                    signedInUser.put(AppConstants.APP_USER_COUNTRY, HolloutUtils.stripDollar(countryName));
                }
                //STREET
                final String streetAddress = address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "";
                if (StringUtils.isNotEmpty(streetAddress)) {
                    signedInUser.put(AppConstants.APP_USER_STREET, HolloutUtils.stripDollar(streetAddress));
                }
                //LOCALITY
                String locality = address.getLocality();
                if (StringUtils.isNotEmpty(locality)) {
                    signedInUser.put(AppConstants.APP_USER_LOCALITY, HolloutUtils.stripDollar(locality));
                }
                //Admin
                String adminAddress = address.getAdminArea();
                if (StringUtils.isNotEmpty(adminAddress)) {
                    signedInUser.put(AppConstants.APP_USER_ADMIN_AREA, HolloutUtils.stripDollar(adminAddress));
                }
                updateSignedInUserProps(true);
            }
            return null;
        }
    }

    private void cancelRunningLocationTaskBeforeRun(Location location) {
        if (location != null) {
            if (getLocationTask != null && !getLocationTask.isCancelled()) {
                getLocationTask.cancel(true);
                getLocationTask = null;
            }
            getLocationTask = new GetLocationTask(this);
            getLocationTask.execute(location);
        }
    }

    private void attemptToConnectGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(60000);
        mLocationRequest.setFastestInterval(60000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (HolloutPreferences.canAccessLocation()) {
            processLocation(null);
        } else {
            EventBus.getDefault().post(AppConstants.PLEASE_REQUEST_LOCATION_ACCESSS);
        }
    }

    @SuppressWarnings({"MissingPermission"})
    public void processLocation(Location location) {
        if (location != null) {
            cancelRunningLocationTaskBeforeRun(location);
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        googleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        HolloutLogger.d(TAG, "Changed Location = " + location.toString());
        if (HolloutPreferences.canAccessLocation()) {
            processLocation(location);
        } else {
            EventBus.getDefault().post(AppConstants.PLEASE_REQUEST_LOCATION_ACCESSS);
        }
    }

    private void updateSignedInUserProps(final boolean sendPushNotification) {
        if (signedInUser != null) {
            AuthUtil.updateCurrentLocalUser(signedInUser, new DoneCallback<Boolean>() {
                @Override
                public void done(Boolean result, Exception e) {
                    if (e == null) {
                        ParseInstallation parseInstallation = ParseInstallation.getCurrentInstallation();
                        if (parseInstallation != null) {
                            try {
                                ParseGeoPoint parseGeoPoint = AuthUtil.getCurrentUser().getParseGeoPoint(AppConstants.APP_USER_GEO_POINT);
                                if (parseGeoPoint != null) {
                                    parseInstallation.put(AppConstants.APP_USER_GEO_POINT, parseGeoPoint);
                                }
                                parseInstallation.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (sendPushNotification) {
                                            sendAmNearbyPushNotification();
                                        }
                                    }
                                });
                            } catch (NullPointerException ignored) {
                            }
                        }
                    }
                }
            });
        }
    }

    private void sendAmNearbyPushNotification() {
        String signedInUserId = signedInUser.getString(AppConstants.REAL_OBJECT_ID);
        List<String> savedUserChats = signedInUser.getList(AppConstants.APP_USER_CHATS);
        List<String> aboutUser = signedInUser.getList(AppConstants.ABOUT_USER);
        ArrayList<String> newUserChats = new ArrayList<>();
        ParseQuery<ParseObject> peopleQuery = ParseQuery.getQuery(AppConstants.PEOPLE_GROUPS_AND_ROOMS);
        ParseGeoPoint signedInUserGeoPoint = signedInUser.getParseGeoPoint(AppConstants.APP_USER_GEO_POINT);
        if (signedInUserGeoPoint != null && aboutUser != null) {
            if (savedUserChats != null) {
                if (!savedUserChats.contains(signedInUserId.toLowerCase())) {
                    savedUserChats.add(signedInUserId.toLowerCase());
                }
                peopleQuery.whereNotContainedIn(AppConstants.REAL_OBJECT_ID, savedUserChats);
            } else {
                if (!newUserChats.contains(signedInUserId)) {
                    newUserChats.add(signedInUserId);
                }
                peopleQuery.whereNotContainedIn(AppConstants.REAL_OBJECT_ID, newUserChats);
            }
            peopleQuery.whereEqualTo(AppConstants.OBJECT_TYPE,AppConstants.OBJECT_TYPE_INDIVIDUAL);
            peopleQuery.whereContainedIn(AppConstants.ABOUT_USER, aboutUser);
            peopleQuery.whereWithinKilometers(AppConstants.APP_USER_GEO_POINT, signedInUserGeoPoint, 10.0);
            peopleQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> parseUsers, ParseException e) {
                    if (e==null && parseUsers!=null && !parseUsers.isEmpty()){
                        List<String>appUserIds = new ArrayList<>();
                        for (ParseObject parseUser:parseUsers){
                            String appUserId = parseUser.getString(AppConstants.REAL_OBJECT_ID);
                            appUserIds.add(appUserId);
                        }
                        if (!appUserIds.isEmpty()){
                            final ParseQuery<ParseInstallation>parseInstallationParseQuery = ParseInstallation.getQuery();
                            parseInstallationParseQuery.whereContainedIn(AppConstants.REAL_OBJECT_ID,appUserIds);
                            parseInstallationParseQuery.findInBackground(new FindCallback<ParseInstallation>() {
                                @Override
                                public void done(List<ParseInstallation> objects, ParseException e) {
                                    if (e==null && objects!=null){
                                        NotificationCenter.sendAmNearbyNotification(signedInUser.getString(AppConstants.REAL_OBJECT_ID),parseInstallationParseQuery);
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
    }
}

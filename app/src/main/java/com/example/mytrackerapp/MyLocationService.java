package com.example.mytrackerapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;


public class MyLocationService extends Service implements LocationListener
{
    private static final int TWO_MINUTES = 1000 * 60 * 1;
    public static final String BROADCAST_ACTION = "LOCATION_UPDATE";

    private Context context;
    private Intent intent;
    private String restAPIEndPoint ;
    private String userId;
    private String sessionId;
    private PostData postData = null;

    private LocationManager locationManager = null;
    private Location previousBestLocation = null;

    private FusedLocationProviderClient mFusedLocationClient = null;

    private Queue<String> locationDataStack = new LinkedList<String>();

    private Timer timer;
    private MyTimerClass timerTask;

    @TargetApi(26)
    private Notification getNotification() {
        NotificationChannel channel = new NotificationChannel( "channel_01", "Location Data", NotificationManager.IMPORTANCE_DEFAULT );

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        Notification.Builder builder = new Notification.Builder(getApplicationContext(), "channel_01");

        return builder.build();
    }

    class MyTimerClass extends TimerTask {
        final Handler handler = new Handler();
        public MyLocationService delegate = null;
        public void run() {
            handler.post(new Runnable() {
                public void run() {
                    try {
                        int size = locationDataStack.size();
                        if( size > 0 ) {
                            String s = null;
                            StringBuilder sb = new StringBuilder();
                            sb.append("{\"Items\":[");
                            s = locationDataStack.peek();
                            while (s != null) {
                                s = locationDataStack.remove();
                                sb.append(s);
                                s = locationDataStack.peek();
                                if (s != null) {
                                    sb.append(",");
                                }
                            }
                            sb.append("]}");
                            Log.i("MyLocationService", sb.toString() );
                            // Log.i("MyLocationService", "Sending " + size + " Location Data Points" );

                            if (postData != null)
                                postData.cancel(true);
                            postData = new PostData();
                            postData.delegate = delegate;
                            postData.execute(restAPIEndPoint, sb.toString());
                        }
                    } catch (SecurityException e) {
                    }  finally {
                    }
                }
            });
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        intent = new Intent(BROADCAST_ACTION);
        context=this;

        startForeground(123123, getNotification() );

        useLocationListener();
        useTimer();
    }

    private void useLocationListener() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission( this.getApplicationContext(), Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
        } else if (ContextCompat.checkSelfPermission( this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        else {
            //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, this);

            //if(mFusedLocationClient == null) {
            //    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
            //    mFusedLocationClient.requestLocationUpdates(mLocationRequest, this, null);
            //}
        }
    }

    private void useTimer() {
        timerTask = new MyTimerClass();
        timerTask.delegate = this;
        timer = new Timer();
        timer.schedule(timerTask, 15000, 20000);
    }

    public void onPostComplete(Long result, String resp) {
        if( result == 200L ) {
            // TODO: Successfully sent data
            // for( int i=0; i< resp.cnt; i++ )
            //  locationDataStack.remove();
        } else {
            // TODO: Post data failed, or partial failure
        }
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();
        restAPIEndPoint = extras.get("restAPIEndPoint").toString();
        userId = extras.get("userId").toString();
        sessionId = extras.get("sessionId").toString();
        return START_NOT_STICKY ;
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v("STOP_SERVICE", "DONE");

        // handler.removeCallbacks(sendUpdatesToUI); // timer service
        locationManager.removeUpdates(this);
    }

    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }

    @Override
    public void onLocationChanged(final Location loc)
    {
        Log.i("MyLocationService", "Location changed");
        if(isBetterLocation(loc, previousBestLocation)) {
            previousBestLocation = loc;
            loc.getLatitude();
            loc.getLongitude();

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            String json = String.format("{\"user\":\"%s\", \"session\":\"%s\", \"dt\":\"%s\", \"lat\":%.8f, \"lng\":%.8f}",
                    userId,
                    sessionId,
                    formatter.format(date),
                    loc.getLatitude(),
                    loc.getLongitude() );

            intent.putExtra("dt", date);
            intent.putExtra("Latitude", loc.getLatitude());
            intent.putExtra("Longitude", loc.getLongitude());
            intent.putExtra("Provider", loc.getProvider());
            intent.putExtra("Location", loc);
            // loc.getSpeed();
            // loc.getTime();

            // Tell Activity location has changed
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            locationDataStack.add(json);
        }
    }

    public void onProviderDisabled(String provider)
    {
        Toast.makeText( getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT ).show();
    }


    public void onProviderEnabled(String provider)
    {
        Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
    }


    public void onStatusChanged(String provider, int status, Bundle extras)
    {

    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {

        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        Log.i("location updates", String.format("timeDelta=%d",timeDelta));

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            Log.i("location updates", "isSignificantlyNewer");
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            Log.i("location updates", "isSignificantlyOlder");
            return false;
        }

        /*
            smaller accurecy is beter.
            68% chance of actual location being in a radius of the given accuracy.
         */

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 20;

        Log.i("location updates", String.format("accuracyDelta=%d",accuracyDelta));

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        if( currentBestLocation.equals("gps") && !location.getProvider().equals("gps") ) {
            Log.i("location updates", "gps -> non gps, nope");
            return false;
        }

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            Log.i("location updates", "isMoreAccurate");
            return true;
        } else if (isNewer && !isLessAccurate) {
            Log.i("location updates", "isNewer && !isLessAccurate");
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            Log.i("location updates", "isNewer && !isSignificantlyLessAccurate && isFromSameProvider");
            return true;
        }
        Log.i("location updates", "not better, returning false");
        return false;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}
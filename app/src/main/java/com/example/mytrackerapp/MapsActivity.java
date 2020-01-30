package com.example.mytrackerapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.provider.Settings.Secure;
import android.content.BroadcastReceiver;
import android.os.Bundle;

import android.content.Context;
import android.content.Intent;
import android.Manifest.permission;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import android.location.Location;
import android.widget.Toast;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private String restAPIEndPoint = null;
    private boolean isVisible = false;
    private GoogleMap mMap;
    private static Context context;
    private Intent locationIntent;
    private LatLng lastlatlng = null;
    private String sessionId = null;
    private String userId = null;
    private Marker lastMarker = null;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Date dt = (Date)intent.getExtras().get("dt");
            double lat = (double)intent.getExtras().get("Latitude");
            double lng = (double)intent.getExtras().get("Longitude");
            Location loc = (Location)intent.getExtras().get("Location");

            LatLng latlng = new LatLng(lat, lng);
            if( lastlatlng != null) {
                if( lastMarker != null ) {
                    lastMarker.remove();
                }
                lastMarker = mMap.addMarker(new MarkerOptions().position(latlng).title(String.format("%f",loc.getAccuracy())));
                PolylineOptions pp = new PolylineOptions();
                pp.add(lastlatlng);
                pp.add(latlng);
                mMap.addPolyline(pp);
            }
            if( isVisible ) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
            }
            lastlatlng = latlng;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
    }

    public static Context getAppContext() {
        return MapsActivity.context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.restAPIEndPoint = getString(R.string.restAPIEndPoint);

        MapsActivity.context = getApplicationContext();

        sessionId = UUID.randomUUID().toString();
        userId = Secure.getString(getApplicationContext().getContentResolver(),Secure.ANDROID_ID);

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        if (ContextCompat.checkSelfPermission( this.getApplicationContext(), permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                new String[]{permission.INTERNET},
                34);
        }
        else {
            startLocationUpdates();
            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("LOCATION_UPDATE"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        context.stopService(locationIntent);
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission( this.getApplicationContext(), permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{permission.ACCESS_FINE_LOCATION},
                    33);
        } else {
            // this.lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 0.0F, this);

            // use this to start and trigger a service
            locationIntent = new Intent(context, MyLocationService.class); // potentially add data to the intent
            locationIntent.putExtra("userId", userId );
            locationIntent.putExtra("sessionId", sessionId );
            locationIntent.putExtra("restAPIEndPoint", restAPIEndPoint );
            context.startService(locationIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions, int[] grantResults) {
           switch(requestCode) {
               case 33:
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    }
                    return;
               case 34:
                   if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                   }
                   return;
           }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }
}


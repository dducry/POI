package com.poi.poi;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;


public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location currentLocation;
    private Location poiLocation;
    private SharedPreferences preferences;

    private final int ACCESS_FINE_LOCATION_REQUEST = 0;
    private final int LOCATION_UPDATE_FREQUENCY = 3000;
    private final float ZOOM_INIT = 19f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Create an instance of Google API Client
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Get the location of the poi we are tracking
        Intent i = getIntent();
        double latitude = i.getDoubleExtra(MainActivity.LATITUDE, 0);
        double longitude = i.getDoubleExtra(MainActivity.LONGITUDE, 0);
        poiLocation = new Location("poi");
        poiLocation.setLatitude(latitude);
        poiLocation.setLongitude(longitude);


        // Sets the main layout to the activity
        setContentView(R.layout.activity_tracking);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, ACCESS_FINE_LOCATION_REQUEST);
            while (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                ;
        }
        mMap.setMyLocationEnabled(true);
        UiSettings us = mMap.getUiSettings();
        us.setMyLocationButtonEnabled(false);
        us.setAllGesturesEnabled(false);
        us.setMapToolbarEnabled(false);

        switch (preferences.getString("types_carte", "normal")) {
            case "normal": {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            }
            case "hybrid": {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            }
            case "satellite": {
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            }
        }

        mMap.moveCamera(CameraUpdateFactory.zoomTo(ZOOM_INIT));

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, ACCESS_FINE_LOCATION_REQUEST);
            while (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                ;
        }
        Location lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (lastKnownLocation != null) {
            currentLocation = lastKnownLocation;
            final double currentLatitude = currentLocation.getLatitude();
            final double currentLongitude = currentLocation.getLongitude();
            if (mMap != null)
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(currentLatitude, currentLongitude)));
        }

        createLocationRequest();
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_UPDATE_FREQUENCY);
        mLocationRequest.setFastestInterval(LOCATION_UPDATE_FREQUENCY);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude())), LOCATION_UPDATE_FREQUENCY, null);
    }
}

package com.poi.poi;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

/*Ajouté par Damien*/

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/*-----------------*/


public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location currentLocation;
    private Location poiLocation;
    private Marker currentMarker;
    private Marker poiMarker;
    private SharedPreferences preferences;
    private Polyline currentPolyline;
    private boolean orientedMode = false;

    private final int ACCESS_FINE_LOCATION_REQUEST = 0;
    private final int LOCATION_UPDATE_FREQUENCY = 5000;
    private final float ZOOM_INIT = 20f;
    private final float TILT_INIT = 60;

    /*Ajouté par Damien*/

    private CompassView compassView;
    private SensorManager sensorManager;
    private Sensor sensor;

    //Notre listener sur le capteur de la boussole numérique
    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            //currentLocation.getBearing();
            if(currentLocation == null){
                updateOrientation(event.values[SensorManager.DATA_X]);
            }
            else{
                updateOrientation(currentLocation.bearingTo(poiLocation) - event.values[SensorManager.DATA_X]);
                //Log.i("pop", Float.toString(currentLocation.bearingTo(poiLocation)));
            }
            //updateOrientation(currentLocation.getBearing());
            //currentLocation.bearingTo(poiLocation)
            //Log.i("pop", Float.toString(currentLocation.bearingTo(poiLocation)));

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    /*-----------------*/



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

        /*Ajouté par Damien*/

        compassView = (CompassView)findViewById(R.id.compassView);
        //Récupération du gestionnaire de capteurs
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        //Demander au gestionnaire de capteur de nous retourner les capteurs de type boussole
        List<Sensor> sensors =sensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
        //s’il y a plusieurs capteurs de ce type on garde uniquement le premier
        if (sensors.size() > 0) {
            sensor = sensors.get(0);
        }

        /*-----------------*/

    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        currentMarker.remove();
        currentPolyline.remove();
        poiMarker.remove();

        /*Ajouté par Damien*/
        sensorManager.unregisterListener(sensorListener);
        /*-----------------*/

        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        UiSettings us = mMap.getUiSettings();
        us.setAllGesturesEnabled(false);
        us.setZoomGesturesEnabled(true);
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
            LatLng currentPosition = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            LatLng poiPosition = new LatLng(poiLocation.getLatitude(), poiLocation.getLongitude());

            // Draw markers
            currentMarker = mMap.addMarker(new MarkerOptions()
                    .position(currentPosition)
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_compass)));
            poiMarker = mMap.addMarker(new MarkerOptions()
                    .position(poiPosition)
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromResource(android.R.drawable.arrow_down_float)));

            // Draw the line
            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(currentPosition, poiPosition)
                    .color(0xFFFF0000)
                    .width(60f);
            currentPolyline = mMap.addPolyline(polylineOptions);

            if (mMap != null) {
                if (!orientedMode)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, ZOOM_INIT));
                else {
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(currentPosition)
                            .zoom(ZOOM_INIT)
                            .bearing(currentLocation.bearingTo(poiLocation))
                            .tilt(TILT_INIT)
                            .build();
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            }
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
        LatLng startPosition = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        LatLng poiPosition = new LatLng(poiLocation.getLatitude(), poiLocation.getLongitude());
        currentLocation = location;
        LatLng finalPosition = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        if (!orientedMode)
            mMap.animateCamera(CameraUpdateFactory.newLatLng(finalPosition), LOCATION_UPDATE_FREQUENCY, null);
        else {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(finalPosition)
                    .zoom(ZOOM_INIT)
                    .bearing(currentLocation.bearingTo(poiLocation))
                    .tilt(TILT_INIT)
                    .build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        // Update markers and polyline
        animate(startPosition, finalPosition, poiPosition);
    }


    /*Ajouté par Damien*/

    //Mettre à jour l'orientation
    protected void updateOrientation(float rotation)
    {
        compassView.setNorthOrientation(rotation);
    }


    @Override
    protected void onResume(){
        super.onResume();
        //Lier les évènements de la boussole numérique au listener
        sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);

    }
    /*------------------*/


    void animate(final LatLng startPosition, final LatLng finalPosition, final LatLng poiPosition) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
        final float durationInMs = LOCATION_UPDATE_FREQUENCY;

        handler.post(new Runnable() {
            long elapsed;
            float t;
            float v;

            @Override
            public void run() {

                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start;
                t = elapsed / durationInMs;
                v = interpolator.getInterpolation(t);

                double clatitude = (finalPosition.latitude - startPosition.latitude) * v + startPosition.latitude;
                double clongitude = (finalPosition.longitude - startPosition.longitude) * v + startPosition.longitude;
                LatLng newPosition = new LatLng(clatitude, clongitude);
                currentPolyline.remove();
                PolylineOptions polylineOptions = new PolylineOptions()
                        .add(newPosition, poiPosition)
                        .color(0xFFFF0000)
                        .width(60f);
                currentPolyline = mMap.addPolyline(polylineOptions);
                currentMarker.setPosition(newPosition);

                // Repeat till progress is complete.
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 32);
                }
            }
        });
    }
}

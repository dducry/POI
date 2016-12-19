package com.poi.poi;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

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


public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, SensorEventListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location currentLocation;
    private Location poiLocation;
    private Marker currentMarker;
    private Marker poiMarker;
    private SharedPreferences preferences;
    private Polyline currentPolyline;
    private boolean isCompassMode = true;

    private final int ACCESS_FINE_LOCATION_REQUEST = 0;
    private final int LOCATION_UPDATE_FREQUENCY = 5000;
    private final float ZOOM_INIT = 20f;
    private final float TILT_INIT = 60;

    /*Ajouté par Damien*/

    Float azimut;  // View to draw a compass
    private SensorManager mSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;
    float orientation[] = new float[3];
    private CompassView compassView;

    private View.OnClickListener clickListenerModeButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            LinearLayout parent = (LinearLayout) findViewById(R.id.compass_or_ra);
            switch (v.getId()) {
                case R.id.compass_button:
                    if (!isCompassMode)
                        parent.addView(compassView, 0);
                    isCompassMode = true;
                    break;
                case R.id.reality_button:
                    if (isCompassMode)
                        parent.removeViewAt(0);
                    isCompassMode = false;
                    break;
                default:
            }
        }
    };
    /*------------------*/

    /* Added by Florian */
    private Camera mCamera = null;
    private CameraView cameraView;
    private HUDView hudView;
    Sensor rotationSensor;

    public TrackingActivity() {
    }
    /* ---------------- */

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

        // Sets the main layout to the activity
        setContentView(R.layout.activity_tracking);

        RadioButton compassRadioButton = (RadioButton) findViewById(R.id.compass_button);
        RadioButton realityRadioButton = (RadioButton) findViewById(R.id.reality_button);
        compassRadioButton.setOnClickListener(clickListenerModeButton);
        realityRadioButton.setOnClickListener((clickListenerModeButton));

        // Get the location of the poi we are tracking
        Intent i = getIntent();
        double latitude = i.getDoubleExtra(MainActivity.LATITUDE, 0);
        double longitude = i.getDoubleExtra(MainActivity.LONGITUDE, 0);
        poiLocation = new Location("poi");
        poiLocation.setLatitude(latitude);
        poiLocation.setLongitude(longitude);

        // Sets the title
        String poi_name = i.getStringExtra(MainActivity.POI_NAME);
        String poi_dist = i.getStringExtra(MainActivity.POI_DIST);
        ((TextView) findViewById(R.id.tracking_title)).setText(poi_name);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /*Ajouté par Damien*/
        compassView = (CompassView) findViewById(R.id.compassView);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        /*-----------------*/

        /* Added by Florian */
        try{
            mCamera = Camera.open();//you can use open(int) to use different cameras
        } catch (Exception e){
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }

        if(mCamera != null) {
            cameraView = new CameraView(this, mCamera);//create a SurfaceView to show camera data
            //FrameLayout camera_view = (FrameLayout)findViewById(R.id.camera_view);
            //camera_view.addView(mCameraView);//add the SurfaceView to the layout
        }


        hudView = new HUDView(this, mCamera.getParameters().getHorizontalViewAngle(), mCamera.getParameters().getVerticalViewAngle());
        //FrameLayout hud_view = (FrameLayout)findViewById(R.id.hud_view);
        //hud_view.addView(mCustomDrawableView);//add the SurfaceView to the layout
        rotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        /* ---------------- */

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

        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        UiSettings us = mMap.getUiSettings();
        us.setAllGesturesEnabled(true);
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

            mMap.clear();
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
                    .width(30f);
            currentPolyline = mMap.addPolyline(polylineOptions);

            if (mMap != null) {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(currentPosition)
                        .zoom(ZOOM_INIT)
                        .tilt(TILT_INIT)
                        .build();
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
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
        mMap.animateCamera(CameraUpdateFactory.newLatLng(finalPosition), LOCATION_UPDATE_FREQUENCY, null);

        // Update markers and polyline
        animate(startPosition, finalPosition, poiPosition);
    }


    /*Ajouté par Damien*/

    //Mettre à jour l'orientation
    protected void updateOrientation(float rotation) {
        compassView.setNorthOrientation(rotation);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    float[] mGravity;
    float[] mGeomagnetic;

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                //float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimut = orientation[0]; // orientation contains: azimut, pitch and roll
            }
        }
        ;

        float angle = (180 * orientation[0]) / (float) Math.PI;

        if (currentLocation == null) {
            updateOrientation(angle);
        } else {
            updateOrientation(angle - currentLocation.bearingTo(poiLocation));
            //Log.i("angle", Float.toString(angle));
        }

        /* Added by Florian */
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] RIN = new float[16];
            float[] roationV = new float[16];
            float[] rotationVector = event.values.clone();
            //Get Rotation Matrix
            SensorManager.getRotationMatrixFromVector(RIN, rotationVector);
            //Rotate Coordinate Ststem to the relative phone system
            SensorManager.remapCoordinateSystem(RIN, SensorManager.AXIS_X, SensorManager.AXIS_Z, roationV);
            float orientation[] = new float[3];
            //Retrive the orientation vector
            orientation = SensorManager.getOrientation(roationV, orientation);
            hudView.az.add(orientation[0]);
            while(hudView.az.size() > 10)
                hudView.az.poll();
            hudView.pt.add(orientation[1]);
            while(hudView.pt.size() > 10)
                hudView.pt.poll();
            hudView.rl.add(orientation[2]);
            while(hudView.rl.size() > 10)
                hudView.rl.poll();

            hudView.targetAzimuth = currentLocation.bearingTo(poiLocation);
            hudView.targetPitch = (float) (-1*Math.atan2(poiLocation.getAltitude() - currentLocation.getAltitude(), currentLocation.distanceTo(poiLocation)));
        }
        hudView.invalidate();
        /* ---------------  */
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
                        .width(30f);
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

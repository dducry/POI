package com.poi.poi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, LocationListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private GoogleMap mMap;
    private Location currentLocation;
    private LocationManager locationManager;
    private Map<Marker, JSONObject> placesMap;
    private Marker currentMarker = null;
    private String locationProvider;
    private SharedPreferences preferences;
    private final int ACCESS_FINE_LOCATION_REQUEST = 0;


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            // if the rayon preference has changed, we run the searching thread
            case "Rayon": {
                new Thread(new SearchPoiThread()).start();
            }
        }
    }

    // A thread to do http requests for the radar search
    private class SearchPoiThread implements Runnable {
        @Override
        public void run() {
            final String apiKey = getString(R.string.google_maps_key);
            final double currentLatitude = currentLocation.getLatitude();
            final double currentLongitude = currentLocation.getLongitude();
            String rayon = preferences.getString("Rayon", "2000");
            String urlMainRequest = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + currentLatitude + "," + currentLongitude + "&radius=" + rayon + "&types=food&key=" + apiKey;
            try {
                final JSONObject jsonAllPlaces = getJSONObjectFromURL(urlMainRequest);
                    /*
                    String specificPlaceId = jsonAllPlaces.getJSONArray("results").getJSONObject(0).getString("place_id");
                    String urlSpecificRequest = "https://maps.googleapis.com/maps/api/place/details/json?placeid=" + specificPlaceId + "&key=" + apiKey;
                    JSONObject jsonSpecificPlace = getJSONObjectFromURL(urlSpecificRequest);
                    */

                while (mMap == null) ;
                // Update UI in the right thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Remove every elements on the map
                            mMap.clear();

                            // Move camera to the current location
                            LatLng pos = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, mMap.getCameraPosition().zoom));

                            // Draw the circle
                            String rayon = preferences.getString("Rayon", "2000");
                            CircleOptions co = new CircleOptions()
                                    .center(pos)
                                    .radius(Integer.parseInt(rayon));
                            mMap.addCircle(co);

                            placesMap = new HashMap<Marker, JSONObject>();
                            JSONArray jsonPlacesArray = jsonAllPlaces.getJSONArray("results");
                            if (jsonPlacesArray.length() <= 0)
                                return;

                            // Add markers on map for all POI found and keep the closest
                            JSONObject closest = null;
                            Marker closestMarker = null;
                            float minDist = Float.MAX_VALUE;
                            for (int i = 0; i < jsonPlacesArray.length(); i++) {
                                JSONObject currentPlace = jsonPlacesArray.getJSONObject(i);
                                double latitude = currentPlace.getJSONObject("geometry").getJSONObject("location").getDouble("lat");
                                double longitude = currentPlace.getJSONObject("geometry").getJSONObject("location").getDouble("lng");

                                float distance[] = new float[1];
                                Location.distanceBetween(latitude, longitude, currentLatitude, currentLongitude, distance);
                                LatLng currentPos = new LatLng(latitude, longitude);
                                Marker cMarker = mMap.addMarker(new MarkerOptions()
                                        //.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher))
                                        .position(currentPos));
                                if (distance[0] < minDist) {
                                    minDist = distance[0];
                                    closest = currentPlace;
                                    closestMarker = cMarker;
                                }
                                placesMap.put(cMarker, currentPlace);

                            }
                            if (closest != null) {
                                currentMarker = closestMarker;
                                closestMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                                showPoiInfo(closest);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

                /*
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        LatLng pos = new LatLng(-33.8670522, 151.1957362);
                        MarkerOptions mo = new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher))
                                .position(pos)
                                .draggable(false);
                        mMap.addMarker(mo);
                    }
                });
                */
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);

        // Obtain the current location
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        searchLocationProvider();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Launch searching thread
        new Thread(new SearchPoiThread()).start();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        UiSettings us = mMap.getUiSettings();
        us.setAllGesturesEnabled(false);
        us.setZoomGesturesEnabled(true);
        us.setMapToolbarEnabled(false);

        mMap.setOnMarkerClickListener(this);

        LatLng posInit = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        float zoomInit = 14f;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posInit, zoomInit));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        showPoiInfo(placesMap.get(marker));
        if (currentMarker != null)
            currentMarker.setIcon(BitmapDescriptorFactory.defaultMarker());
        currentMarker = marker;
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        return false;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.preferences) {
            startActivity(new Intent(MainActivity.this, PreferenceActivityExample.class));
        }

        return super.onOptionsItemSelected(item);
    }

    // Method to show informations about a place
    protected void showPoiInfo(JSONObject currentPlace) {
        try {
            String name = currentPlace.getString("name");

            double currentLatitude = currentLocation.getLatitude();
            double currentLongitude = currentLocation.getLongitude();
            double latitude = currentPlace.getJSONObject("geometry").getJSONObject("location").getDouble("lat");
            double longitude = currentPlace.getJSONObject("geometry").getJSONObject("location").getDouble("lng");

            float distance[] = new float[1];
            Location.distanceBetween(latitude, longitude, currentLatitude, currentLongitude, distance);
            ((TextView) findViewById(R.id.name_POI)).setText(name);
            ((TextView) findViewById(R.id.dist_POI)).setText(Integer.toString(Math.round(distance[0])) + "m");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        new Thread(new SearchPoiThread()).start();

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // if we loose the current location provider or if there is no locationProvider yet, then we look for another.
        if ((provider.equals(locationProvider) && status == LocationProvider.OUT_OF_SERVICE) || (locationProvider == null && status == LocationProvider.AVAILABLE))
            searchLocationProvider();
    }


    @Override
    public void onProviderEnabled(String provider) {
        // if there is no locationProvider yet and the new one respects the criteria, so we take it
        if (locationProvider == null) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            List<String> locationProviders = locationManager.getProviders(criteria, true);
            if (locationProviders.contains(provider))
                locationProvider = provider;
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        // if user disables the current location provider, then we look for another.
        if (provider.equals(locationProvider))
            searchLocationProvider();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int grantResults[]) {
        /*switch (requestCode) {
            case ACCESS_FINE_LOCATION_REQUEST: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
            }
        }
        */
    }


    protected void searchLocationProvider() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, ACCESS_FINE_LOCATION_REQUEST);
            while (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                ;
        }
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationProvider = locationManager.getBestProvider(criteria, true);
        // if no provider exists, then create a fictive location
        if (locationProvider == null) {
            currentLocation = new Location("");
            currentLocation.setLatitude(0.0d);
            currentLocation.setLongitude(0.0d);
        } else {
            locationManager.requestLocationUpdates(locationProvider, 120000, 50, this);
            currentLocation = locationManager.getLastKnownLocation(locationProvider);
        }
    }


    // Static method to manage connection to google places server returning JSON data
    public static JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {

        HttpURLConnection urlConnection = null;

        URL url = new URL(urlString);

        urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000 /* milliseconds */);
        urlConnection.setConnectTimeout(15000 /* milliseconds */);

        urlConnection.setDoOutput(true);

        urlConnection.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

        char[] buffer = new char[1024];

        String jsonString = new String();

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();

        jsonString = sb.toString();

        System.out.println("JSON: " + jsonString);

        return new JSONObject(jsonString);
    }
}

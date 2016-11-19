package com.poi.poi;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // Launch a thread to do http requests for the radar search
        new Thread(new Runnable() {
            public void run() {
                String urlMainRequest = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=-33.8670522,151.1957362&radius=1000&types=food&name=cruise&key=AIzaSyDMUy-sME63T9m-2Uos0LfwtsVH1d1UG48";
                try{
                    JSONObject jsonAllPlaces = getJSONObjectFromURL(urlMainRequest);
                    /*
                    String specificPlaceId = jsonAllPlaces.getJSONArray("results").getJSONObject(0).getString("place_id");
                    String urlSpecificRequest = "https://maps.googleapis.com/maps/api/place/details/json?placeid=" + specificPlaceId + "&key=AIzaSyDMUy-sME63T9m-2Uos0LfwtsVH1d1UG48";
                    JSONObject jsonSpecificPlace = getJSONObjectFromURL(urlSpecificRequest);
                    */


                    final Map<Marker, JSONObject> placesMap = new HashMap<Marker, JSONObject>();
                    final JSONArray jsonPlacesArray = jsonAllPlaces.getJSONArray("results");

                    while (mMap == null);
                    // Update UI in the right thread
                    runOnUiThread(new Runnable(){
                        @Override
                        public void run() {
                            try{
                                for (int i=0; i<jsonPlacesArray.length(); i++){
                                    JSONObject currentPlace = jsonPlacesArray.getJSONObject(i);
                                    double latitude = currentPlace.getJSONObject("geometry").getJSONObject("location").getDouble("lat");
                                    double longitude = currentPlace.getJSONObject("geometry").getJSONObject("location").getDouble("lng");

                                    LatLng currentPos = new LatLng(latitude, longitude);

                                    Marker currentMarker = mMap.addMarker(new MarkerOptions()
                                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher))
                                            .position(currentPos));
                                    placesMap.put(currentMarker, currentPlace);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
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
        }).start();
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
        us.setMapToolbarEnabled(false);

        mMap.setPadding(10, 10, 10, 10);

        LatLng posInit = new LatLng(-33.8670522, 151.1957362);
        float zoomInit = 14f;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posInit, zoomInit));

        // Draw a the circle
        CircleOptions co = new CircleOptions()
                .center(posInit)
                .radius(1000);
        mMap.addCircle(co);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

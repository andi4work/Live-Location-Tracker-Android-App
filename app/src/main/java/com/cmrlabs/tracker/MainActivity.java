package com.cmrlabs.tracker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cmrlabs.tracker.FetchLocation.Login;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, OnMapReadyCallback {
    //VEHICLE LOCATION
    Circle circleOrigin, circleDestination;
    private LatLng mOrigin, mDestination, mVehicleLiveLocation;
    private Location location;
    private TextView distanceFromVehicle, durationFromVehicle, vehicleLocation;
    private GoogleApiClient googleApiClient;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private LocationRequest locationRequest;
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000; // = 5 seconds
    // lists for permissions
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    // integer for permissions results request
    private static final int ALL_PERMISSIONS_RESULT = 1011;
    private Polyline mPolyline;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private String TAG = "cmrlabs", mDistance, mDuration;
    private GoogleMap mMap;
    ArrayList<LatLng> mMarkerPoints;
    LatLngBounds latLngBounds;
    FirebaseRemoteConfig mFirebaseRemoteConfig;
    Double origin_lat, origin_lon, vehicle_latitude, vehicle_longitude, destination_lat, destination_lon;
    String serviceNumber;
    TextView title, busStops;
    EditText etServiceNumber;
    Button bTrackVehicle;
    ProgressBar progressBar;
    RelativeLayout main;
    ArrayList<LatLng> points = null;
    Marker marker;
    AdapterBusStops adapter;
    RecyclerView recyclerView;
    ArrayList<String> busStopNames;
    Button bSwitch;
    int switchToMap = 0;
    String busStopName;
    String city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            this.getSupportActionBar().hide();
        } catch (NullPointerException e) {
        }

        setContentView(R.layout.activity_main);

        // data to populate the RecyclerView with
   /*     busStopNames.add("vinjamur");
        busStopNames.add("Chandrapadiya");
        busStopNames.add("Rajollu");
        busStopNames.add("Nellore Palem");
        busStopNames.add("Atmakur");*/
        recyclerView = findViewById(R.id.rvBusStops);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterBusStops(this, busStopNames);


        main = findViewById(R.id.rlMain);
        //busStops = findViewById(R.id.bus_stops);

        progressBar = new ProgressBar(getApplicationContext(), null, android.R.attr.progressBarStyleLarge);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        main.addView(progressBar, params);
        recyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(),
                DividerItemDecoration.VERTICAL));

        Bundle bundle = getIntent().getExtras();
        serviceNumber = bundle.getString("SERVICE_NUMBER");
        vehicleLocation = findViewById(R.id.city);
        // mDestination = new LatLng(13.0061, 77.6594);

        // we add permissions we need to request location of the users
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        permissionsToRequest = permissionsToRequest(permissions);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        bSwitch = findViewById(R.id.bSwitch);
        bSwitch.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(new Intent(getApplicationContext(), Login.class));
                return false;
            }
        });
        bSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchToMap == 0) {
                    bSwitch.setText("MAP VIEW");
                    recyclerView.setVisibility(View.VISIBLE);
                    mapFragment.getView().setVisibility(View.GONE);
                    switchToMap = 1;
                } else {
                    bSwitch.setText("LIST VIEW");
                    recyclerView.setVisibility(View.GONE);
                    mapFragment.getView().setVisibility(View.VISIBLE);
                    switchToMap = 0;
                }
            }
        });

        mapFragment.getMapAsync(this);
        mMarkerPoints = new ArrayList<>();
        distanceFromVehicle = findViewById(R.id.distance);
        durationFromVehicle = findViewById(R.id.duration);
        if (mDistance != null && mDuration != null) {
            distanceFromVehicle.setText(mDistance);
            durationFromVehicle.setText(mDuration);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }

        // we build google api client
        googleApiClient = new GoogleApiClient.Builder(this).
                addApi(LocationServices.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).build();
    }

    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!checkPlayServices()) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop location updates
        if (googleApiClient != null && googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {
                finish();
            }

            return false;
        }

        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Permissions ok, we get last location
        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (location != null) {
        }

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            //mDestination = new LatLng(location.getLatitude(), location.getLongitude());

            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("bus_tracking").child("service_no");

            databaseReference.child(serviceNumber).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot mainSnapshot) {

                    vehicle_latitude = new Double(mainSnapshot.child("live_lat").getValue().toString());
                    vehicle_longitude = new Double(mainSnapshot.child("live_lon").getValue().toString());
                    mVehicleLiveLocation = new LatLng(vehicle_latitude, vehicle_longitude);

                    origin_lat = new Double(mainSnapshot.child("origin_lat").getValue().toString());
                    origin_lon = new Double(mainSnapshot.child("origin_lon").getValue().toString());
                    mOrigin = new LatLng(origin_lat, origin_lon);
                    System.out.println("ORIGIN_LAT_LON : " + origin_lat + origin_lon);

                    destination_lat = new Double(mainSnapshot.child("destination_lat").getValue().toString());
                    destination_lon = new Double(mainSnapshot.child("destination_lon").getValue().toString());
                    mDestination = new LatLng(destination_lat, destination_lon);
                    System.out.println("DESTINATION_LAT_LON : " + destination_lat + destination_lon);
                    busStopNames = new ArrayList<>();
                    for (DataSnapshot snapshot : mainSnapshot.child("bus_stops").getChildren()) {
                        busStopName = snapshot.child("name").getValue().toString();
                        String busStopLat = snapshot.child("lat").getValue().toString();
                        String busStopLon = snapshot.child("lon").getValue().toString();
                        LatLng busStop = new LatLng(Double.valueOf(busStopLat), Double.valueOf(busStopLon));

                        MarkerOptions options = new MarkerOptions();
                        // Setting the position of the marker
                        options.position(busStop);
                        options.icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_bus_stop_icon));
                        mMap.addMarker(options);


                        // Add new marker to the Google Map Android API V2

                        System.out.println(busStopName + "\n" + busStopLat + "\n" + busStopLon + "\n");

                        Location busStopp = new Location(LocationManager.GPS_PROVIDER);
                        Location busLocation = new Location(LocationManager.GPS_PROVIDER);
                        busStopp.setLatitude(Double.valueOf(busStopLat));
                        busStopp.setLongitude(Double.valueOf(busStopLon));
                        busLocation.setLatitude(mVehicleLiveLocation.latitude);
                        busLocation.setLongitude(mVehicleLiveLocation.longitude);
                        float distance = busLocation.distanceTo(busStopp) / 1000;
                        String distanceToBusStop = String.valueOf(distance);
                        String finalDistance = distanceToBusStop.substring(0, Math.min(distanceToBusStop.length(), 4));
                        busStopNames.add("--- " + finalDistance + "kms ---> " + busStopName);
                    }
                    adapter = new AdapterBusStops(getApplicationContext(), busStopNames);
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });

            databaseReference.child(serviceNumber).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot mainSnapshot) {

                    vehicle_latitude = mainSnapshot.child("live_lat").getValue(Double.class);
                    vehicle_longitude = mainSnapshot.child("live_lon").getValue(Double.class);
                    //mOrigin = new LatLng(vehicle_latitude, vehicle_longitude);
                    System.out.println("VEHILCE_LAT_LON : " + vehicle_longitude + vehicle_longitude);
                    mVehicleLiveLocation = new LatLng(vehicle_latitude, vehicle_longitude);
                    MarkerOptions live = new MarkerOptions();
                    // Setting the position of the marker
                    live.position(mVehicleLiveLocation);
                    live.icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_vehicle_location));

                    if (marker == null) {
                        marker = mMap.addMarker(live);
                    } else {
                        marker.setPosition(mVehicleLiveLocation);
                    }


                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
      /*        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
            FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(10)
                    .build();
            mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

            mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);

            mFirebaseRemoteConfig.fetchAndActivate();

          vehicle_latitude = mFirebaseRemoteConfig.getString("tracker_vehicle_latitude");
            vehicle_longitude = mFirebaseRemoteConfig.getString("tracker_vehicle_longitude");
*/

            //GET VEHICLE LOCATION HERE
            //mDestination = new LatLng(location.getLatitude(), location.getLongitude());
            if (mDestination != null & mOrigin != null) {
                System.out.println("Origin+++++++++++++LOC : " + mOrigin.latitude + " _ " + mOrigin.longitude);
                System.out.println("Destination++++++++LOC : " + mDestination.latitude + " _ " + mDestination.longitude);
                System.out.println("REMOTE CONFIG LOCATION : " + vehicle_latitude + " _ " + vehicle_longitude);


                Geocoder geocoder;
                List<Address> addresses;
                geocoder = new Geocoder(this, Locale.getDefault());

                try {
                    addresses = geocoder.getFromLocation(vehicle_latitude, vehicle_longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                    String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                    city = addresses.get(0).getLocality();
                    String state = addresses.get(0).getAdminArea();
                    String country = addresses.get(0).getCountryName();
                    String postalCode = addresses.get(0).getPostalCode();
                    String knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL
                    Log.i("Full Address : ", address + city + state + country + postalCode + knownName);
                    if (address != null) {
                        vehicleLocation.setText("Bus Location : " + city);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //   drawRoute();

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }

                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(MainActivity.this).
                                    setMessage("These permissions are mandatory to get your location. You need to allow them.").
                                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.
                                                        toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    }).setNegativeButton("Cancel", null).create().show();

                            return;
                        }
                    }
                } else {
                    if (googleApiClient != null) {
                        googleApiClient.connect();

                    }
                }

                break;
        }
    }


    private String getDirectionsUrl(LatLng origin, LatLng dest, String via) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Key
        String key = "key=" + getString(R.string.google_maps_key);

        //String extra = "&waypoints=via:kaligiri|via:vinjamur";
        // String extra = "&waypoints=via:vinjamur|via:kaligiri";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&waypoints=via:" + via + "&" + key;
        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        return url;
    }

    private void drawRoute(String via) {

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(mDestination).include(mOrigin);
                latLngBounds = builder.build();
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(builder.build(), 100);
                mMap.moveCamera(cameraUpdate);
            }
        });
//Animate to the bounds


//Animate to the bounds
       /* CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(builder.build(), 100);
        mMap.moveCamera(cameraUpdate);
*/
        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(mOrigin, mDestination, via);
        System.out.println("URL : " + url);
        String url1 = makeURL(mOrigin.latitude, mOrigin.longitude, mDestination.latitude, mDestination.longitude);
        System.out.println("URL1 : " + url1);

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);

        // Instantiates a new CircleOptions object and defines the center and radius
        CircleOptions circleOptionsOrigin = new CircleOptions()
                .center(new LatLng(37.4, -122.1))
                .radius(2); // In meters
        CircleOptions circleOptionsDestination = new CircleOptions()
                .center(new LatLng(37.4, -122.1))
                .radius(2); // In meters
        circleOptionsOrigin.center(mOrigin);
        circleOptionsDestination.center(mDestination);


        circleOrigin = mMap.addCircle(circleOptionsOrigin);
        circleOrigin.setFillColor(getResources().getColor(R.color.mapRouteColor));
        circleOrigin.setStrokeColor(getResources().getColor(R.color.mapRouteBorder));

        circleDestination = mMap.addCircle(circleOptionsDestination);
        circleDestination.setFillColor(getResources().getColor(R.color.mapRouteColor));
        circleDestination.setStrokeColor(getResources().getColor(R.color.mapRouteBorder));


    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap) {

        if (checkSelfPermission(ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission();
            return;
        } else {

            mMap = googleMap;
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().isMyLocationButtonEnabled();
            mMap.setMyLocationEnabled(true);
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("bus_tracking").child("service_no");

            databaseReference.child(serviceNumber).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot mainSnapshot) {

                    vehicle_latitude = new Double(mainSnapshot.child("live_lat").getValue().toString());
                    vehicle_longitude = new Double(mainSnapshot.child("live_lon").getValue().toString());
                    mVehicleLiveLocation = new LatLng(vehicle_latitude, vehicle_longitude);

                    origin_lat = new Double(mainSnapshot.child("origin_lat").getValue().toString());
                    origin_lon = new Double(mainSnapshot.child("origin_lon").getValue().toString());
                    mOrigin = new LatLng(origin_lat, origin_lon);
                    System.out.println("ORIGIN_LAT_LON : " + origin_lat + origin_lon);

                    destination_lat = new Double(mainSnapshot.child("destination_lat").getValue().toString());
                    destination_lon = new Double(mainSnapshot.child("destination_lon").getValue().toString());
                    String via = mainSnapshot.child("via").getValue().toString();
                    mDestination = new LatLng(destination_lat, destination_lon);
                    System.out.println("DESTINATION_LAT_LON : " + destination_lat + destination_lon);
                    busStopNames = new ArrayList<>();
                    for (DataSnapshot snapshot : mainSnapshot.child("bus_stops").getChildren()) {
                        busStopName = snapshot.child("name").getValue().toString();
                        String busStopLat = snapshot.child("lat").getValue().toString();
                        String busStopLon = snapshot.child("lon").getValue().toString();
                        LatLng busStop = new LatLng(Double.valueOf(busStopLat), Double.valueOf(busStopLon));

                        MarkerOptions options = new MarkerOptions();
                        // Setting the position of the marker
                        options.position(busStop);
                        options.icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.ic_bus_stop_icon));
                        mMap.addMarker(options);

                        // Add new marker to the Google Map Android API V2

                        System.out.println(busStopName + "\n" + busStopLat + "\n" + busStopLon + "\n");

                        Location busStopp = new Location(LocationManager.GPS_PROVIDER);
                        Location busLocation = new Location(LocationManager.GPS_PROVIDER);
                        busStopp.setLatitude(Double.valueOf(busStopLat));
                        busStopp.setLongitude(Double.valueOf(busStopLon));
                        busLocation.setLatitude(mVehicleLiveLocation.latitude);
                        busLocation.setLongitude(mVehicleLiveLocation.longitude);
                        float distance = busLocation.distanceTo(busStopp) / 1000;
                        String distanceToBusStop = String.valueOf(distance);

                        String finalDistance = distanceToBusStop.substring(0, Math.min(distanceToBusStop.length(), 4));

                        busStopNames.add("--- " + finalDistance + "kms ---> " + busStopName);

                    }
                    adapter = new AdapterBusStops(getApplicationContext(), busStopNames);
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    drawRoute(via);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });

        }

       /* LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (location != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                    .zoom(17)                   // Sets the zoom
                    .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }*/

    }


    /**
     * A class to download data from Google Directions URL
     */
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                //  Log.d("DownloadTask", "DownloadTask : " + data);

                GeoApiContext context = new GeoApiContext.Builder()
                        .apiKey("AIzaSyCQu5V8WAnzUeP-wtGgfxsVrIWIdImBI4Y")
                        .build();

                GeocodingResult[] results = GeocodingApi.geocode(context,
                        "1600 Amphitheatre Parkway Mountain View, CA 94043").await();
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                System.out.println("CHECKERRR : " + gson.toJson(results[0].addressComponents));

            } catch (Exception e) {
                //  Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception on download", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    /**
     * A class to parse the Google Directions in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                JSONArray results = jObject.getJSONArray("routes");
                JSONObject first = results.getJSONObject(0);
                JSONArray legs = first.getJSONArray("legs");
                JSONObject firs = legs.getJSONObject(0);
                JSONObject distance = firs.getJSONObject("distance");
                JSONObject duration = firs.getJSONObject("duration");
                mDistance = distance.getString("text");
                mDuration = duration.getString("text");
                System.out.println("DISTANCEEEE : " + mDistance + " DURATIONNNN : " + mDuration);
                DirectionJSONParser parser = new DirectionJSONParser();
                // Starts parsing data
                routes = parser.parse(jObject);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

            PolylineOptions lineOptions = null;
            if (mDistance != null && mDuration != null) {
                distanceFromVehicle.setText("Distance : " + mDistance);
                durationFromVehicle.setText("Duration : " + mDuration);
            } // Traversing through all the routes

            if (result == null) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "No route is found", Toast.LENGTH_LONG).show();

            } else {
                for (int i = 0; i < result.size(); i++) {
                    points = new ArrayList<LatLng>();
                    lineOptions = new PolylineOptions();

                    // Fetching i-th route
                    List<HashMap<String, String>> path = result.get(i);

                    // Fetching all the points in i-th route
                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);

                        double lat = Double.parseDouble(point.get("live_lat"));
                        double lng = Double.parseDouble(point.get("live_lng"));
                        LatLng position = new LatLng(lat, lng);
                        points.add(position);
                    }

                    // Adding all the points in the route to LineOptions
                    lineOptions.addAll(points);
                    lineOptions.width(14);
                    lineOptions.color(getResources().getColor(R.color.mapRouteColor));
                }

                // Drawing polyline in the Google Map for the i-th route
                if (lineOptions != null) {
                    if (mPolyline != null) {
                        mPolyline.remove();
                    }
                    mPolyline = mMap.addPolyline(lineOptions);
                    //mMap.setTrafficEnabled(true);
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), "No route is found", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle(R.string.location_permission_title)
                        .setMessage(R.string.location_permission_message)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }


    public static String recurseKeys(JSONObject jObj, String findKey) throws JSONException {
        String finalValue = "";
        if (jObj == null) {
            return "";
        }

        Iterator<String> keyItr = jObj.keys();
        Map<String, String> map = new HashMap<>();

        while (keyItr.hasNext()) {
            String key = keyItr.next();
            map.put(key, jObj.getString(key));
        }

        for (Map.Entry<String, String> e : (map).entrySet()) {
            String key = e.getKey();
            if (key.equalsIgnoreCase(findKey)) {
                return jObj.getString(key);
            }

            // read value
            Object value = jObj.get(key);

            if (value instanceof JSONObject) {
                finalValue = recurseKeys((JSONObject) value, findKey);
            }
        }

        // key is not found
        return finalValue;
    }


    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public String makeURL(double sourcelat, double sourcelog, double destlat, double destlog) {
        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString.append(Double.toString(sourcelog));
        urlString.append("&destination=");// to
        urlString.append(Double.toString(destlat));
        urlString.append(",");
        urlString.append(Double.toString(destlog));
        urlString.append("&sensor=false&mode=driving&alternatives=true");
        urlString.append("&key=YOUR_API_KEY");
        return urlString.toString();

    }


}
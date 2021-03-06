package com.example.aqi.travelapp;

import android.app.Fragment;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MapFragment connects to the Google Api client
 * and creates the MapView that shows the map
 * Also contains methods for handling locations and addresses
 */
public class MapFragment extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    public View root;
    private MapView mMapView;
    private GoogleMap googleMap;

    private GoogleApiClient mGoogleApiClient;
    public static final String TAG = "MapFragment";
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private LocationRequest mLocationRequest;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate and return the layout
        root = inflater.inflate(R.layout.fragment_map, container,
                false);
        mMapView = (MapView) root.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume();// needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        googleMap = mMapView.getMap();

        if (googleMap != null){
            googleMap.setMyLocationEnabled(true);
        }

        //Get map switch
        Switch mapTypeSwitch = (Switch) root.findViewById(R.id.mapTypeSwitch);

        //attach a listener to check for changes in state
        mapTypeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                } else {
                    googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }

            }
        });

        return root;
    }



    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {

            Log.d(TAG, "location services");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else
        {
            //Move camera to current location
            handleNewLocation(location);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(getActivity(),"Location services suspended. Please reconnect to use maps",
                Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
         /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(getActivity(), CONNECTION_FAILURE_RESOLUTION_REQUEST);
                 /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "location changed, calling handler");
        handleNewLocation(location);
    }

    @Override
    public void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        mMapView.onLowMemory();
    }

    @Override
    public void onStop() {
        //Make root layout gone
         if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        super.onStop();


    }


    /**
     * Given arraylists of latitudes and longitudes, computes and
     * returns the maximum and minimum (lat,lon) in a LatLngBounds
     * @param lats arraylist of latitudes
     * @param lons arraylist of longitudes
     * @return LatLngBounds from the southwestern to northeastern most
     */
    private LatLngBounds computeLatLngBounds(ArrayList<Double> lats, ArrayList<Double> lons){
        //Get the max and min lats and lons
        Collections.sort(lats);
        Collections.sort(lons);

        LatLng southwest = new LatLng(lats.get(0),lons.get(0));
        LatLng northeast = new LatLng(lats.get(lats.size()-1), lons.get(lons.size()-1));
        return  new LatLngBounds(southwest, northeast);
    }

    /**
     * Draws a polyline between two markers, with a color
     * corresponding to the transportMode
     * @param fromMarker
     * @param toMarker
     * @param transportmode
     */
    private void drawLine(Marker fromMarker, Marker toMarker, int transportmode){
        int color;
        if (transportmode == 0){
            color = ContextCompat.getColor(getActivity(),R.color.colorWalk);//walking
        } else if (transportmode == 1){
            color = ContextCompat.getColor(getActivity(),R.color.colorBus);//public transport
        } else {
            color = ContextCompat.getColor(getActivity(),R.color.colorTaxi);//taxi
        }
        PolylineOptions options = new PolylineOptions()
                .add(fromMarker.getPosition())
                .add(toMarker.getPosition())
                .color(color);

        googleMap.addPolyline(options);
    }

    /**
     * Finds users location and displays it on the map
     * when the fragment is first created
     * @param location
     */
    private void handleNewLocation(Location location) {
            googleMap.clear();
            double currentLatitude = location.getLatitude();
            double currentLongitude = location.getLongitude();
            LatLng latLng = new LatLng(currentLatitude, currentLongitude);
            MarkerOptions options = new MarkerOptions()
                    .position(latLng);
                    //.title(foundFromLocation);
            googleMap.addMarker(options);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
    }


    /**
     * Gets String address from a lat and lon
     * @param lat latitude
     * @param lon longitude
     * @return address
     */
    public String getAddressfromLocation(double lat, double lon)
    {
        String address = "";
        try {
            Geocoder myGcdr = new Geocoder(getActivity());
            List<Address> matchedList = myGcdr.getFromLocation(lat, lon, 1);
            Address add = matchedList.get(0);
            for(int i =0; i<add.getMaxAddressLineIndex();i++ )
                address += add.getAddressLine(i);
            if(add.getPostalCode()!=null)
                address += " " + add.getPostalCode();
            //include the postal code if there is one

        }
        catch(Exception e)
        {
            Toast.makeText(getActivity(), "A problem occurred in retrieving the address",
                    Toast.LENGTH_SHORT).show();
        }
        return address;

    }

    /**
     * Gets the latitude and longitude of an address
     * @param address address String
     * @return array contianing lat and lon
     */
    public double[] getLocationfromAddress(String address)
    {
        double[] latlon = new double[2];
        try {
            Geocoder myGcdr = new Geocoder(getActivity());
            List<Address> matchedList = myGcdr.getFromLocationName(
                    address, 1, 1.251484, 103.618240, 1.464026, 104.110222);
            latlon[0] = matchedList.get(0).getLatitude();
            latlon[1] = matchedList.get(0).getLongitude();
        }
        catch(Exception e)
        {
            Toast.makeText(getActivity(), "A problem occurred in retrieving the location",
                    Toast.LENGTH_SHORT).show();
        }

        return latlon;

    }

    /**
     * Updates the map to display a single destination
     * and draw a marker. Returns the address so
     * that the address text view can be updated as well
     * @param destination destination to display
     * @return
     */
    public String update(String destination)
    {
        googleMap.clear();
        double[] latlonarr = getLocationfromAddress(destination);

        if(latlonarr == null)
            return null;

        //Get the placename to put into the text view
        String placeName = getAddressfromLocation(latlonarr[0],latlonarr[1]);

        //Add marker and update the camera
        LatLng latlon = new LatLng(latlonarr[0],latlonarr[1]);
        MarkerOptions marker = new MarkerOptions()
                .position(latlon)
                .title(placeName);
        googleMap.addMarker(marker);
        try {
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latlon));
        } catch (Exception ex){
            ex.printStackTrace();
        }

        return placeName;
    }

    /**
     * Updates the map to show a list of nodes,
     * drawing markers and polylines to show the route
     * @param destinations list of destination nodes
     * @param transportmodes transport modes between nodes
     */
    public void update(ArrayList<String> destinations, int[] transportmodes){

        googleMap.clear();
        ArrayList<Double> lats = new ArrayList<>();
        ArrayList<Double> lons = new ArrayList<>();
        ArrayList<Marker> markers = new ArrayList<Marker>();
        //Add the markers to each node and draw polylines
        for (int i = 0;i<destinations.size();i++) {
            double[] latlon = getLocationfromAddress(destinations.get(i));

            if(latlon==null)
                return;

            lats.add(latlon[0]);
            lons.add(latlon[1]);

            markers.add(googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latlon[0], latlon[1]))
                    .title(destinations.get(i))));
            if(i!=0)
            {
                //Draw the polylines between markers
                drawLine(markers.get(i-1),markers.get(i),transportmodes[i-1]);
            }
        }
        //Draw final polyline (end to start)
        drawLine(markers.get(destinations.size()-1),markers.get(0),transportmodes[0]);

        LatLngBounds latLngBounds = computeLatLngBounds(lats, lons);

        try {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 200));
        } catch (Exception ex){
            ex.printStackTrace();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 500, 500, 200));
        }
    }

}

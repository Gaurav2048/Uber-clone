package com.example.saurav.uber;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends AppCompatActivity implements OnMapReadyCallback
        , GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener
        , GoogleApiClient.ConnectionCallbacks {

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest locationRequest;
    Button logout, request;

    LatLng pickupLocation;
    boolean requestBol = false;
    private GoogleMap mMap;
    Button History,settings;
    private LatLng destinationLatlng;
    RadioGroup radio_group;

    String requestService= "";
    String destination = "";
    private LinearLayout mDriverInfoLayout;
    ImageView DriverrImage;
    TextView DriverName, Drivernumber,DriverCar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        logout = (Button) findViewById(R.id.logout);
        request = (Button) findViewById(R.id.request);
        radio_group=(RadioGroup) findViewById(R.id.radio_group); radio_group.check(R.id.uberX);

        settings = (Button) findViewById(R.id.Setting);
        History = (Button) findViewById(R.id.History);
        DriverrImage = (ImageView) findViewById(R.id.Driver_profile_image);
        mDriverInfoLayout = (LinearLayout) findViewById(R.id.driver_info);
        DriverName = (TextView) findViewById(R.id.DriverName);
        Drivernumber = (TextView) findViewById(R.id.DriverPhone);
        DriverCar = (TextView) findViewById(R.id.DriverCar);


        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(CustomerMapActivity.this, MainActivity.class));
            }
        });

        request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestBol) {
                   endRide();
                }else {
                    requestBol =true;

                    int selectId = radio_group.getCheckedRadioButtonId();
                    RadioButton radioButton = (RadioButton) findViewById(selectId);
                    if(radioButton.getText()==null){
                        return;
                    }
                    requestService = radioButton.getText().toString();

                    String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("request_customer");
                    if(user_id!=null)
                    {
                        GeoFire geoFire = new GeoFire(reference);
                        geoFire.setLocation(user_id, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                                mMap.addMarker(new MarkerOptions().position(pickupLocation).title("pick up here"));
                                request.setText("Getting your driver..");
                                getclosestDriver();
                            }
                        });
                    }

                }

            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class));
return;
            }
        });
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
destinationLatlng= place.getLatLng();
                destination = place.getName().toString();
             }

            @Override
            public void onError(Status status) {
                Log.e( "onError: ",status.getStatus()+" "+status.getStatusMessage()+" "+status.getResolution() );
                // TODO: Handle the error.
             }
        });
History.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(getApplicationContext(), HistoryActivity.class);
        intent.putExtra("identify", "Customers");
        startActivity(intent);
    }
});
    }
int radius = 1;
    boolean driverFound = false;
    GeoQuery geoQuery;
    String driverId = "driver_id";
    private void getclosestDriver() {
        DatabaseReference reference_driver_location = FirebaseDatabase.getInstance().getReference().child("driverAvailable");
        GeoFire geoFire = new GeoFire(reference_driver_location);

         geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude,pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();
geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
    @Override
    public void onKeyEntered(String key, GeoLocation location) {

        if(!driverFound && requestBol){
            Log.e( "onKeyEntered: ",key+" " );

            DatabaseReference mCustomerDatabseReference =  FirebaseDatabase.getInstance().getReference().child("Users")
                                                           .child("Drivers").child(key);
            mCustomerDatabseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                    if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0)
                    {
                        Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();

                      if(driverFound){
                          return;
                      }
                        if(driverMap.get("service").equals(requestService)){

                            driverFound = true;
                            driverId = dataSnapshot.getKey();
                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
                            String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            HashMap map = new HashMap();
                            map.put("customerRideId", customerId);
                            map.put("destination", destination);
                            map.put("destinationLatitude", destinationLatlng.latitude);
                            map.put("destinationLongitude", destinationLatlng.longitude);
                            databaseReference.updateChildren(map);
                            getDriverLocation();
                            getDriverInfo();
                            gethasRideEnded();
                            request.setText("Looking for driver");
                      }
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


        }

    }

    @Override
    public void onKeyExited(String key) {

    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {

    }

    @Override
    public void onGeoQueryReady() {
        if(driverFound==false)
        {
            radius++;
            getclosestDriver();
        }
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {

    }
});
    }

    DatabaseReference  driveHasEndedReference;
    private ValueEventListener driveHasEndedRefListner;
    private void gethasRideEnded() {

        driveHasEndedReference= FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId");
        driveHasEndedRefListner=  driveHasEndedReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                }else {
                    endRide();


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void endRide() {

        requestBol = false;
        geoQuery.removeAllListeners();
        databaseReference.removeEventListener(drValueEventListener);
        driveHasEndedReference.removeEventListener(driveHasEndedRefListner);
        if(driverId!= null) {
            DatabaseReference driverReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
            driverReference.removeValue();
            driverId = null;
        }
        driverFound = false;
        radius =1;
        if(mDrivermarker!= null)
        {
            mDrivermarker.remove();
        }
        String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference refer = FirebaseDatabase.getInstance().getReference("request_customer");

        GeoFire geoFire = new GeoFire(refer);
        geoFire.removeLocation(user_id, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                request.setText("Go Uber");
                mDriverInfoLayout.setVisibility(View.GONE);
                DriverName.setText("");
                DriverCar.setText("");
                Drivernumber.setText("");
            }
        });

    }


    private void getDriverInfo() {

        mDriverInfoLayout.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId);

        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){

                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!= null){

                        DriverName.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!= null){
                        Drivernumber.setText(map.get("phone").toString());
                    }
                    if(map.get("car")!= null){
                        DriverCar.setText(map.get("car").toString());
                    }
                    if(map.get("profileImageUri")!= null){
                        Glide.with(getApplication())
                                .load(map.get("profileImageUri").toString()).into(DriverrImage);



                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



    }

    private Marker mDrivermarker;
    private  DatabaseReference databaseReference;
    private ValueEventListener drValueEventListener;
    private void getDriverLocation() {

          databaseReference = FirebaseDatabase.getInstance().getReference().child("driverWorking").child(driverId).child("l");
       drValueEventListener= databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locallat =0;
                    double locallang=0;
                    request.setText("Driver found");
                    if(map.get(0)!= null){
                        locallat = Double.parseDouble(map.get(0).toString());

                    }
                    if(map.get(1)!= null){
                        locallang = Double.parseDouble(map.get(1).toString());

                    }
                    LatLng driverLatlang = new LatLng(locallat, locallang);
                    if(mDrivermarker != null){
                        mDrivermarker.remove();
                    }

                    Location location = new Location("");
                    location.setLatitude(pickupLocation.latitude);
                    location.setLongitude(pickupLocation.longitude);


                    Location location_driver = new Location("");
                    location_driver.setLatitude(driverLatlang.latitude);
                    location_driver.setLongitude(driverLatlang.longitude);

                    float distance = location.distanceTo(location_driver);

                    request.setText(distance +" kms away");


                    mDrivermarker = mMap.addMarker(new MarkerOptions().position(driverLatlang).title("your driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



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


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
destinationLatlng = new LatLng(0.0,0.0);



    }

    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();



    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        Log.e( "onLocationChanged: ",location.getLatitude()+" "+ location.getLongitude() +" ");
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));



    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}

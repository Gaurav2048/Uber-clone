package com.example.saurav.uber;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback
        , GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener
        , GoogleApiClient.ConnectionCallbacks, RoutingListener {

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest locationRequest;
    boolean islogginOut = false;
    Button logout,Settings;
private String CustomerId= "", destination ;
private LatLng destinationLatlng;
private int status = 0;
private LinearLayout mCustoemrInfoLayout;
Button rideStatus;
ImageView customerImage;
TextView customerName, customernumber,customerDestination;
    private GoogleMap mMap;
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.colorPrimary,R.color.colorPrimaryDark,R.color.colorAccent,R.color.colorPrimaryDark,R.color.primary_dark_material_light};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        logout = (Button) findViewById(R.id.logout);
        polylines = new ArrayList<>();
        mCustoemrInfoLayout = (LinearLayout) findViewById(R.id.customer_info);
        rideStatus= (Button) findViewById(R.id.rideStatus);
        rideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

               switch (status){
                   case 1:
                       status=2;
                       erasePolylines();
                       if(destinationLatlng.latitude!=0.0 && destinationLatlng.longitude!=0.0)
                       {
                           getRouteToMarker(destinationLatlng);
                       }
                       rideStatus.setText("Ride completed");
                       break;
                   case 2:
                       recordRide();
                       endRide();
                       break;

               }

            }
        });
        customerImage = (ImageView) findViewById(R.id.customer_profile_image);
        customerName = (TextView) findViewById(R.id.customerName);
        customernumber= (TextView) findViewById(R.id.customerPhone);
        Settings = (Button) findViewById(R.id.Settings);
        customerDestination=(TextView) findViewById(R.id.customerDestination);
        Settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(), DriverSettingsActivity.class);
                intent.putExtra("identify", "Drivers");
                startActivity(intent);
            }
        });



        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                islogginOut = true;
                disconnectDriver();
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(DriverMapActivity.this, MainActivity.class));
                finish();
            }
        });
    getAssignedCustomer();

    }

    private void recordRide() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("history");
        DatabaseReference customerReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(CustomerId).child("history");
        DatabaseReference historyReference = FirebaseDatabase.getInstance().getReference().child("history");

        String requestId = historyReference.push().getKey();

        driverReference.child(requestId).setValue(true);
        customerReference.child(requestId).setValue(true);
        HashMap map = new HashMap();
        map.put("driver", userId);
        map.put("customer", CustomerId);
        map.put("rating", 0);
        historyReference.child(requestId).updateChildren(map);




    }

    private void endRide() {


       rideStatus.setText("Pick customer");
       erasePolylines();
       String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
             DatabaseReference driverReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
            driverReference.removeValue();

        if(mDrivermarker!= null)
        {
            mDrivermarker.remove();
        }

         DatabaseReference refer = FirebaseDatabase.getInstance().getReference("request_customer");

        GeoFire geoFire = new GeoFire(refer);
        geoFire.removeLocation(CustomerId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

                CustomerId="";
                CustomerId = "";
                if(mDrivermarker!=null){
                    mDrivermarker.remove();
                }
                if(assigendCustomerPickUprefListner!=null){
                    assignedCustomerPickupLocation.removeEventListener(assigendCustomerPickUprefListner);
                }
                mCustoemrInfoLayout.setVisibility(View.GONE);
                customerName.setText("");
                customernumber.setText("");


            }
        });

    }
    private void getAssignedCustomer() {
String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference  driverReference= FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId");
        driverReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                       status =1;
                         CustomerId = dataSnapshot.getValue().toString();
                        getAssignedCustomerPickupLocation();
                    getAssignedCustomerDestination();
                    getAssignedCustomerinfo();
                }else {
                 endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerDestination() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference  driverReference= FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
        driverReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    Map<String, Object> map = (Map<String, Object>)dataSnapshot.getValue();


                    if(map.get("destination")!=null)
                    {
                        destination = map.get("destination").toString();
                        customerDestination.setText(destination);

                    }else {

                        customerDestination.setText("Destination-");
                    }

Double destinationLatitude =0.0;
Double destinationLongitude =0.0;
if(map.get("destinationLatitude")!= null){
destinationLatitude = Double.valueOf(map.get("destinationLatitude").toString());
}
if(map.get("destinationLongitude")!= null){
    destinationLongitude = Double.valueOf(map.get("destinationLongitude").toString());
destinationLatlng = new LatLng(destinationLatitude, destinationLongitude);

   }
                }else {
                    customerDestination.setText("destination-");
                    erasePolylines();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerinfo() {
       mCustoemrInfoLayout.setVisibility(View.VISIBLE);
       DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(CustomerId);

        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){

                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!= null){

                        customerName.setText(map.get("name").toString());
                    }

                    if(map.get("phone")!= null){
                         customernumber.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUri")!= null){
                         Glide.with(getApplication())
                                .load(map.get("profileImageUri").toString()).into(customerImage);



                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    ValueEventListener assigendCustomerPickUprefListner;
    DatabaseReference assignedCustomerPickupLocation;
    Marker mDrivermarker;
    private void getAssignedCustomerPickupLocation() {
           assignedCustomerPickupLocation= FirebaseDatabase.getInstance().getReference().child("request_customer").child(CustomerId).child("l");
       assigendCustomerPickUprefListner= assignedCustomerPickupLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locallat =0;
                    double locallang=0;

                    if(map.get(0)!= null){
                        locallat = Double.parseDouble(map.get(0).toString());
                        Log.e( "onDataChange: ",locallat+" " );

                    }
                    if(map.get(1)!= null){
                        locallang = Double.parseDouble(map.get(1).toString());
                        Log.e( "onDataChange: ",locallang+" " );

                    }
                    LatLng driverLatlang = new LatLng(locallat, locallang);
                    if(mDrivermarker != null){
                        mDrivermarker.remove();
                    }
                    mDrivermarker = mMap.addMarker(new MarkerOptions().position(driverLatlang).title("Pick up location"));
                    getRouteToMarker(driverLatlang);


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getRouteToMarker(LatLng pickuplatlang) {
        Routing routing = new Routing.Builder()
                          .travelMode(AbstractRouting.TravelMode.DRIVING)
                          .withListener(this)
                          .alternativeRoutes(false)
                          .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()),pickuplatlang)

                .build();
                 routing.execute();
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


    }

    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();



    }

    @Override
    public void onLocationChanged(final Location location) {
        mLastLocation = location;
        Log.e( "onLocationChanged: ",location.getLatitude()+" "+ location.getLongitude() +" ");
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

        final String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driverAvailable");
        DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driverWorking");
        final GeoFire geoFire = new GeoFire(refAvailable);
        final GeoFire geoworkingFire = new GeoFire(refWorking);


        switch (CustomerId){
            case "":
                geoworkingFire.removeLocation(user_id, new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        geoFire.setLocation(user_id, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {

                            }
                        });
                    }
                });

                break;

                default:
                    geoFire.removeLocation(user_id, new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            geoworkingFire.setLocation(user_id, new GeoLocation(location.getLatitude(), location.getLongitude()),
                                    new GeoFire.CompletionListener() {
                                        @Override
                                        public void onComplete(String key, DatabaseError error) {

                                        }
                                    }
                            );
                        }
                    });

                    break;
        }




    }

    @Override
    protected void onPause() {
        super.onPause();

    }
private void disconnectDriver(){
    String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driverAvailable");
    if(user_id!=null) {
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(user_id, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });

    }
}
    @Override
    protected void onStop() {
        super.onStop();
        if(!islogginOut){
disconnectDriver();
        }
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

    @Override
    public void onRoutingFailure(RouteException e) {
if(e!=null){
    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
}else {
    Toast.makeText(this, "some thing went wrong ", Toast.LENGTH_SHORT).show();

}
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }

  private void erasePolylines(){
    for(Polyline line : polylines)
    {
        line.remove();
    }

    polylines.clear();
  }
}

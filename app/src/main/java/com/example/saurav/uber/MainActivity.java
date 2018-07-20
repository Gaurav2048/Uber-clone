package com.example.saurav.uber;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {
Button rider, customer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Dexter.withActivity(this)
                .withPermissions(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ).withListener(new MultiplePermissionsListener() {
            @Override public void onPermissionsChecked(MultiplePermissionsReport report) {/* ... */}
            @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {/* ... */}
        }).check();


     rider = (Button) findViewById(R.id.driver);
     customer = (Button) findViewById(R.id.customer);

     rider.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {

             startActivity(new Intent(MainActivity.this, DriverloginActivity.class));
             finish();
         }
     });

     customer.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
startActivity(new Intent(MainActivity.this, CustomerLoginActivity.class));
         }
     });


    }
}

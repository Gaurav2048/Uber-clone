package com.example.saurav.uber;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerSettingsActivity extends AppCompatActivity {
EditText name;
EditText phone_no;
Button confirm, back;

private FirebaseAuth mAuth;
ImageView profile_image;
private DatabaseReference mCustomerDatabase;
String nName, nPhone, profileImageUri;
 private  Uri resultUri;
private String user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);
        name = (EditText) findViewById(R.id.name);
        phone_no = (EditText) findViewById(R.id.phone_no);
        confirm = (Button) findViewById(R.id.confirm);
        back = (Button) findViewById(R.id.back);
        profile_image = (ImageView) findViewById(R.id.profile_image);

        Dexter.withActivity(CustomerSettingsActivity.this)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE

                )
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                    }
                }).check();
        mAuth = FirebaseAuth.getInstance();
        user_id = mAuth.getCurrentUser().getUid();
        mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(user_id);
        getUserInfo();

        profile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 300);
            }
        });
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                saveUserInformation();

            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode ==300 && resultCode == Activity.RESULT_OK){
           resultUri = data.getData();
            profile_image.setImageURI(resultUri);

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void  getUserInfo (){
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){

                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name")!= null){
                        nName = map.get("name").toString();
                        name.setText(nName);
                    }
                    if(map.get("phone")!= null){
                        nPhone = map.get("phone").toString();
                        phone_no.setText(nPhone);
                    }
                    if(map.get("profileImageUri")!= null){
                        profileImageUri = map.get("profileImageUri").toString();
                        Glide.with(getApplication())
                                .load(profileImageUri).into(profile_image);



                     }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void saveUserInformation() {

        Map userinfo = new HashMap();
        nName = name.getText().toString();
        nPhone = phone_no.getText().toString();
         userinfo.put("name", nName);
         userinfo.put("phone",nPhone);
         mCustomerDatabase.updateChildren(userinfo, new DatabaseReference.CompletionListener() {
             @Override
             public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

             }
         });
if(resultUri!= null){
    StorageReference filepath = FirebaseStorage.getInstance().getReference().child("image_profile").child(user_id);
    Bitmap bitmap = null;
    try {
        bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver()
                , resultUri);
    } catch (IOException e) {
        e.printStackTrace();
    }
    ByteArrayOutputStream boas = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, boas);
    byte[] data = boas.toByteArray();
    UploadTask uploadTask = filepath.putBytes(data);
    uploadTask.addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
       finish();
       return;
        }
    });
    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
        @Override
        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
            Task<Uri> downloadUri = taskSnapshot.getMetadata().getReference().getDownloadUrl();

            downloadUri.addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    Map newImage = new HashMap();
                    Log.e( "onComplete: ",task.getResult().toString() );
                    Uri imri = task.getResult();
                    newImage.put("profileImageUri", imri.toString());
                    mCustomerDatabase.updateChildren(newImage);
                    finish();
                    return;
                }
            });
        }
    });
}

    }
}

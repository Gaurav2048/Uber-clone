package com.example.saurav.uber;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerLoginActivity extends AppCompatActivity {

    EditText email, password;
    Button login, register;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    startActivity(new Intent(CustomerLoginActivity.this, CustomerMapActivity.class));
                    finish();
                }

            }
        };

        setContentView(R.layout.activity_customer_login);
        email = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.passsword);
        login = (Button) findViewById(R.id.login);
        register = (Button) findViewById(R.id.registration);



        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email_data = email.getText().toString();
                final  String password_data = password.getText().toString();
                mAuth.createUserWithEmailAndPassword(email_data, password_data).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            String user_id = mAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(user_id);
                            current_user_db.setValue(true);
                        }else {
                            Toast.makeText(CustomerLoginActivity.this, "error registration", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email_data = email.getText().toString();
                final  String password_data = password.getText().toString();

                mAuth.signInWithEmailAndPassword(email_data,password_data).addOnCompleteListener(CustomerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){

                        }else{
                            Toast.makeText(CustomerLoginActivity.this, "error login", Toast.LENGTH_SHORT).show();

                        }
                    }
                });
            }
        });



    }

    @Override
    protected void onStart() {
        mAuth.addAuthStateListener(authStateListener);
        super.onStart();
    }

    @Override
    protected void onStop() {
        mAuth.removeAuthStateListener(authStateListener);
        super.onStop();
    }
}

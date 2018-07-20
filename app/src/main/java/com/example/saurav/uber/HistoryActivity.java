package com.example.saurav.uber;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.saurav.uber.Adapter.HistoryAdapter;
import com.example.saurav.uber.Model.history;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
RecyclerView historyRecyclerView;
String DriverOrCustomer , userId;
HistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        DriverOrCustomer = getIntent().getStringExtra("identify");
        historyRecyclerView = (RecyclerView) findViewById(R.id.historyRecyclerView);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        historyAdapter = new HistoryAdapter(getApplicationContext(), getHistoryList());
        historyRecyclerView.setAdapter(historyAdapter);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getHistoryId();
    }

    private void getHistoryId() {


        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(DriverOrCustomer).child(userId).child("history");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    for (DataSnapshot history : dataSnapshot.getChildren())
                    {
                        FetchRideInformation(history.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void FetchRideInformation(String key) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("history").child(key);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    String rideId = dataSnapshot.getKey();
                    history history = new history(rideId);
                    historyArrayList.add(history);
                    historyAdapter.notifyDataSetChanged();


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private ArrayList <history> historyArrayList= new ArrayList<>();
    private List<history> getHistoryList() {
             return historyArrayList;
    }
}

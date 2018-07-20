package com.example.saurav.uber.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.saurav.uber.Model.history;
import com.example.saurav.uber.R;

import java.util.List;

/**
 * Created by saurav on 7/11/2018.
 */

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.holderview> {


    List<history> historyList;
    Context context;



    public HistoryAdapter(Context context, List<history> historyList) {

        this.historyList= historyList;
        this.context = context;



    }

    @NonNull
    @Override
    public holderview onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new holderview(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull holderview holder, int position) {
holder.rideId.setText(historyList.get(position).getReideId());
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class  holderview extends RecyclerView.ViewHolder
    {public TextView rideId;
        public holderview(View itemView) {
            super(itemView);
            rideId = (TextView) itemView.findViewById(R.id.riderId);
        }
    }}

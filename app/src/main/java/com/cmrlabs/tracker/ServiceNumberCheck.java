package com.cmrlabs.tracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cmrlabs.tracker.FetchLocation.Login;

public class ServiceNumberCheck extends AppCompatActivity {
    EditText etServiceNumber;
    Button bTrack, bFetch;
    String serviceNumber;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_number_check);

        etServiceNumber = findViewById(R.id.etServiceNumber);
        bTrack = findViewById(R.id.bTrackVehicle);
        bFetch = findViewById(R.id.bFetch);
        bFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), Login.class));
            }
        });
        bTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serviceNumber = etServiceNumber.getText().toString();
                if (serviceNumber.length() > 0) {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.putExtra("SERVICE_NUMBER", serviceNumber);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "Enter service number", Toast.LENGTH_LONG).show();
                }

            }
        });

    }
}

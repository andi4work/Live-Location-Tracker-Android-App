package com.cmrlabs.tracker.FetchLocation;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cmrlabs.tracker.R;
import com.google.firebase.database.FirebaseDatabase;

public class Login extends AppCompatActivity {
    EditText etVehicleId;
    String busId;
    Button bFetch;
    private FirebaseDatabase mFirebaseInstance;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        new Utils(this);
        bFetch = findViewById(R.id.bFetch);
        mFirebaseInstance = FirebaseDatabase.getInstance();

        etVehicleId = findViewById(R.id.etBusId);
        busId = etVehicleId.getText().toString();


        checkLocationPermission();
        bFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkLocationPermission()) {
                    SharedPreferences.Editor edt = getSharedPreferences("A", MODE_PRIVATE).edit();
                    edt.putString("busID", etVehicleId.getText().toString());
                    edt.commit();
                    busId = etVehicleId.getText().toString();

                    if (busId.length() > 0) {
                        Intent intent = new Intent(getApplicationContext(), FetchLiveLocation.class);
                        intent.putExtra("busID", busId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getApplicationContext(), "Enter ID to Login", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("LOCATION PERMISSION REQUIRED")
                        .setMessage("Must provide location to run this app")
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(Login.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        SharedPreferences.Editor edt = getSharedPreferences("A", MODE_PRIVATE).edit();
                        edt.putString("busID", etVehicleId.getText().toString());
                        edt.commit();
                        busId = etVehicleId.getText().toString();

                        if (busId.length() > 0) {
                            Intent intent = new Intent(getApplicationContext(), FetchLiveLocation.class);
                            intent.putExtra("busID", busId);
                            startActivity(intent);
                        } else {
                            Toast.makeText(getApplicationContext(), "Enter ID to Login", Toast.LENGTH_SHORT).show();
                        }
                    }

                } else {
                    Toast.makeText(this, "Please allow location permission to run this app", Toast.LENGTH_LONG).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

        }
    }
}
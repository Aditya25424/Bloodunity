package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity2 extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    EditText etLocation;
    FusedLocationProviderClient fusedLocationClient;

    // Set your center location (1km radius around this point)
    private final double centerLat = 28.6139;
    private final double centerLng = 77.2090;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        etLocation = findViewById(R.id.et_location);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ImageView backbutton = findViewById(R.id.backbuttomnn);
        backbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity2.this, MainActivity.class);
                startActivity(intent);
            }
        });
        Button searchblood = findViewById(R.id.btn_submit);
        searchblood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =new Intent(MainActivity2.this,searchblood.class);
                startActivity(intent);
            }

        });

        etLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askLocationPermission();
            }
        });
    }

    private void askLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getUserLocation();
        }
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location userLocation) {
                            if (userLocation != null) {
                                double userLat = userLocation.getLatitude();
                                double userLng = userLocation.getLongitude();

                                // Create Location object for center point
                                Location centerLocation = new Location("");
                                centerLocation.setLatitude(centerLat);
                                centerLocation.setLongitude(centerLng);

                                // Calculate distance in meters
                                float distance = userLocation.distanceTo(centerLocation);

                                if (distance <= 1000) {
                                    // Within 1km radius
                                    Geocoder geocoder = new Geocoder(MainActivity2.this, Locale.getDefault());
                                    try {
                                        List<Address> addresses = geocoder.getFromLocation(userLat, userLng, 1);
                                        if (!addresses.isEmpty()) {
                                            Address address = addresses.get(0);
                                            String fullAddress = address.getAddressLine(0);
                                            etLocation.setText(fullAddress);
                                        } else {
                                            etLocation.setText("Lat: " + userLat + ", Lng: " + userLng);
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        etLocation.setText("Lat: " + userLat + ", Lng: " + userLng);
                                    }
                                } else {
                                    etLocation.setText("");
                                    Toast.makeText(MainActivity2.this,
                                            "You're outside the allowed 1km range.",
                                            Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(MainActivity2.this,
                                        "Unable to get location",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

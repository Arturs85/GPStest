package com.example.gpstest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssMeasurementsEvent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.renderscript.Double2;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {
    TextView textView, textViewSatsCount, textViewNmeaCnt;
    int nmeaMsgCount = 0;
    OutputStreamWriter outputStreamWriter;
    LocationManager locationManager;
    ImuReader imuReader;
    PointsMapView pmv;


    LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        imuReader = new ImuReader(this);

        {
            locationManager = (LocationManager)
                    getSystemService(Context.LOCATION_SERVICE);
        }
        try {
            File mediaStorageDir = new File(Environment.getExternalStorageDirectory().getPath(), "nmeaData");

            //if this "JCGCamera folder does not exist
            if (!mediaStorageDir.exists())
                mediaStorageDir.mkdirs();

            File mediaFile;
            //and make a media file:
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "nmeaMsg.txt");

            //  File path = this.getFilesDir();
            //  File file = new File(path, "mynmeaMsgs.txt");
            outputStreamWriter = new OutputStreamWriter(new FileOutputStream(mediaFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        textViewSatsCount = findViewById(R.id.textViewSatsCount);
        textViewNmeaCnt = findViewById(R.id.textViewNmeaMsgCount);
        LinearLayout ll = findViewById(R.id.linearLayout);
        pmv = new PointsMapView(this, imuReader);

        ll.addView(pmv);

        GpsStatus.Listener gl = new GpsStatus.Listener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onGpsStatusChanged(int event) {
                if (event != GpsStatus.GPS_EVENT_SATELLITE_STATUS) return;
                GpsStatus mGpsStatus = null;
                mGpsStatus = locationManager.getGpsStatus(mGpsStatus);
                String satINview = "";
                if (mGpsStatus != null) {
                    Iterable<GpsSatellite> satellites = mGpsStatus.getSatellites();
                    int iTempCountInView = 0;
                    int iTempCountInUse = 0;

                    for (GpsSatellite gpsSatellite : satellites) {
                        iTempCountInView++;
                        if (gpsSatellite.usedInFix())
                            iTempCountInUse++;
                    }
                    satINview = "" + iTempCountInView;
                    String s = "sats in view: " + satINview + ", in use: " + iTempCountInUse + "\n";
                    textViewSatsCount.setText(s);
                }

            }
        };


        locationManager.addGpsStatusListener(gl);

        GpsStatus.NmeaListener nl = new GpsStatus.NmeaListener() {
            @Override
            public void onNmeaReceived(long timestamp, String nmea) {
                Log.v("Main", nmea);
                if (nmea.contains("$GPGGA")) {
                    nmeaMsgCount++;
                    textViewNmeaCnt.setText("nmea cnt : " + nmeaMsgCount);
                    try {
                        outputStreamWriter.write(nmea);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        try {
            //noinspection JavaReflectionMemberAccess
            Method addNmeaListener =
                    LocationManager.class.getMethod("addNmeaListener", GpsStatus.NmeaListener.class);
            addNmeaListener.invoke(locationManager, nl);
        } catch (Exception exception) {
            // TODO
        }


        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location loc) {

//                Toast.makeText(
//                        getBaseContext(),
//                        "Location changed: Lat: " + loc.getLatitude() + " Lng: "
                //                    + loc.getLongitude(), Toast.LENGTH_SHORT).show();
                String longitude = "Longitude: " + loc.getLongitude();
                Log.v("Main", longitude);
                String latitude = "Latitude: " + loc.getLatitude();
                Log.v("Main", latitude);

                PointsMapView.path.add(new Double2(loc.getLatitude(), loc.getLongitude()));
                pmv.invalidate();
                String s = longitude + "\n" + latitude + "\n";
                textView.setText(s);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(
                    getBaseContext(),
                    "Location -no permisions", Toast.LENGTH_SHORT).show();
            return;

        }
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000, 1, locationListener);


    }

    @Override
    protected void onStop() {
        super.onStop();
        imuReader.unregister();
        try {
            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        locationManager.removeUpdates(locationListener);
        Toast.makeText(
                getBaseContext(),
                "closed nmea file", Toast.LENGTH_SHORT).show();
    }
}

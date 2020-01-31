package com.example.geofencingexample;

import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.List;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GeoQueryEventListener {

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    public FusedLocationProviderClient fusedLocationProviderClient;
    private Marker currentUser;
    public  LocationServices locationServices;
    private DatabaseReference myLocationRef;
    public GeoFire geoFire;
    private List<LatLng> DangerousArea;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {

                        buildLocationRequest();
                        buildLocationCallback();
                        fusedLocationProviderClient=locationServices.getFusedLocationProviderClient(MapsActivity.this);

                        SupportMapFragment mapFragment=(SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map);
                        mapFragment.getMapAsync(MapsActivity.this);
                         initArea();

                        settingGeoFire();
                    }



                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MapsActivity.this,"debes otrogar permisos",Toast.LENGTH_LONG);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();


    }

    private void initArea() {
       DangerousArea.add(new LatLng(37.442,-122.044));
        DangerousArea.add(new LatLng(37.442,-122.144));
        DangerousArea.add(new LatLng(37.442,-122.244));
    }

    private void settingGeoFire() {
        myLocationRef= FirebaseDatabase.getInstance().getReference("MyLocation");
        geoFire=new GeoFire(myLocationRef);

    }
    private void buildLocationCallback() {
        locationCallback=new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (mMap!=null){
                    if (currentUser!=null) currentUser.remove();
                    currentUser=mMap.addMarker((new MarkerOptions())
                            .position(new LatLng(locationResult.getLastLocation().getLatitude(),locationResult.getLastLocation().getLongitude()))
                            .title("Tu"));
                    //despues agg marcador, mueve la camara
                    mMap.animateCamera(CameraUpdateFactory
                            .newLatLngZoom(currentUser.getPosition(),12.0f));
                }
            }
        };
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(locationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

       mMap.getUiSettings().setZoomControlsEnabled(true);

    if (fusedLocationProviderClient!= null)
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                return;
            }
        }
    fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,Looper.myLooper());
    for (LatLng latLng:DangerousArea){
        mMap.addCircle(new CircleOptions().center(latLng)
        .radius(500)
                .fillColor(0x220000FF)
                .strokeWidth(5.0f)
        );

        //crear la geoconsulta
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latLng.latitude,latLng.longitude),0.5f);
        geoQuery.addGeoQueryEventListener(MapsActivity.this);



    }

    }
    @Override
    protected void onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        sendNotification("EDMTDev0",String.format("@s entered the dangerous area",key));
    }

    @Override
    public void onKeyExited(String key) {
        sendNotification("EDMTDev0",String.format("@s entered the dangerous area",key));

    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        sendNotification("EDMTDev0",String.format("@s entered the dangerous area",key));

    }



    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Toast.makeText(this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String title, String content) {
        String NOTIFIVATION_CHANEL_ID="multiples_localizaciones";
        NotificationManager notificationManager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel notificationChannel=new NotificationChannel(NOTIFIVATION_CHANEL_ID,"mi notificacion",
                    NotificationManager.IMPORTANCE_DEFAULT);
            //configuracion
            notificationChannel.setDescription("Canal de Descripcion ");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder=new NotificationCompat.Builder(this,NOTIFIVATION_CHANEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));

        Notification notification=builder.build();
        notificationManager.notify(new Random().nextInt(),notification);
    }
}

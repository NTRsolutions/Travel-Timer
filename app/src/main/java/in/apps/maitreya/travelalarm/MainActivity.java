package in.apps.maitreya.travelalarm;

import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 0;
    static final int MAP_SOURCE_REQ = 0;  // The request code for source
    static final int MAP_DESTINATION_REQ = 1;  // The request code for source
    boolean destinationYN,serviceStarted;
    //
    TextView v1, v2, v3, v4;
    Intent global;
    //
    Vibrator v;
    float alarm_dis, actual_dis;
    static MediaPlayer mMediaPlayer;
    LatLng source, destination, currentLocation;
    LocationManager locationManager;
    MyReceiver myReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        v1 = (TextView) findViewById(R.id.source_tv);
        v2 = (TextView) findViewById(R.id.destination_tv);
        v3 = (TextView) findViewById(R.id.calDis_tv);
        v4 = (TextView) findViewById(R.id.AlarmDis_tv);
        mMediaPlayer = new MediaPlayer();
}

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Open Location Settings",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void mapSD(View v) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
        } else {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Intent intent = new Intent(this, MapsActivity.class);
                if (destinationYN)
                    startActivityForResult(intent, MAP_DESTINATION_REQ);
                else
                    startActivityForResult(intent, MAP_SOURCE_REQ);
            } else {
                showGPSDisabledAlertToUser();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void mapD(View v) {
        destinationYN = true;
        mapSD(v);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void mapS(View v) {
        destinationYN = false;
        mapSD(v);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull  String permissions[],@NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        Intent intent = new Intent(this, MapsActivity.class);
                        startActivityForResult(intent, MAP_SOURCE_REQ);
                    } else {
                        showGPSDisabledAlertToUser();
                    }
                } else {
                    Toast.makeText(this, "Sorry! Location Access Permission needed!", Toast.LENGTH_SHORT).show();
                    //nothing can be done
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        Geocoder geocoder;
        List<Address> addressList=null;
        geocoder=new Geocoder(this, Locale.getDefault());
        if (requestCode == MAP_SOURCE_REQ) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Bundle b = data.getExtras();
                source = (LatLng) b.get("marker_latlng");
                //
                if(source!=null)
                try {
                    addressList=geocoder.getFromLocation(source.latitude,source.longitude,1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //
                if(addressList!=null)
                v1.setText(getAddressAsString(addressList.get(0)));
                //
                if(destination!=null)
                    calculateDistance(source,destination);
            }
        } else if (requestCode == MAP_DESTINATION_REQ) {
            if (resultCode == RESULT_OK) {
                Bundle b = data.getExtras();
                destination = (LatLng) b.get("marker_latlng");
                //
                if(destination!=null)
                try {
                    addressList=geocoder.getFromLocation(destination.latitude,destination.longitude,1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //
                if(addressList!=null)
                v2.setText(getAddressAsString(addressList.get(0)));
                //
                if(source!=null)
                    calculateDistance(source,destination);
            }
        }
    }

    public void calculateDistance(LatLng s,LatLng d) {
        if (source != null && destination != null) {
            Location s1 = new Location("S");
            Location d1 = new Location("D");
            s1.setLatitude(s.latitude);
            s1.setLongitude(s.longitude);
            d1.setLongitude(d.longitude);
            d1.setLatitude(d.latitude);
            float disf = actual_dis = s1.distanceTo(d1);
            int dis = (int) disf;
            v3.setText("Distance = " + dis / 1000.0 + " kms");
        } else {
            if (source == null)
                Toast.makeText(this, "Source not selected!", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Destination not selected!", Toast.LENGTH_SHORT).show();
        }
    }

    public void ring() {
        //
        // Get instance of Vibrator from current Context
        sendNotification();
        //
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Start without a delay
        // Vibrate for 1000 milliseconds
        // Sleep for 1000 milliseconds
        long[] pattern = {0, 1000, 1000};

        // The '0' here means to repeat indefinitely
        // '0' is actually the index at which the pattern keeps repeating from (the start)
        // To repeat the pattern from any other point, you could increase the index, e.g. '1'
        //v.vibrate(pattern, 0);
        //
        try {
            Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (!mMediaPlayer.isPlaying()) {
                mMediaPlayer = new MediaPlayer();
                v.vibrate(pattern, 0);
            }
            mMediaPlayer.setDataSource(this, alert);
            final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Turn off Alarm?")
                .setCancelable(false)
                .setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mMediaPlayer.stop();
                                v.cancel();
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }
    public void setAlarmDistance(View v) {
        if(source!=null){
            if(destination!=null) {
                final Dialog dialog = new Dialog(this);
                LayoutInflater li = getLayoutInflater();
                View v1 = li.inflate(R.layout.distance_list, (ViewGroup) v, false);
                dialog.setContentView(v1);
                dialog.setTitle(R.string.select_distance);
                dialog.show();
                RadioGroup rg = (RadioGroup) v1.findViewById(R.id.radioGroup1);
                rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        switch (checkedId) {
                            case R.id.radioButton500m:
                                alarm_dis = 500;
                                v4.setText(R.string.meter500);
                                break;
                            case R.id.radioButton1km:
                                alarm_dis = 1000;
                                v4.setText(R.string.km1);
                                break;
                            case R.id.radioButton2km:
                                alarm_dis = 2000;
                                v4.setText(R.string.km2);
                                break;
                            case R.id.radioButton5km:
                                alarm_dis = 5000;
                                v4.setText(R.string.km5);
                                break;
                        }
                        dialog.cancel();
                        if (actual_dis <= alarm_dis)
                            ring();
                    }
                });
            }else
                Toast.makeText(this,"Destination not selected!",Toast.LENGTH_SHORT).show();
        }else
            Toast.makeText(this,"Source not selected!",Toast.LENGTH_SHORT).show();

    }

    public void startBLS(View v) {
        //
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if(source!=null) {
                if(destination!=null) {
                    if(alarm_dis>0) {
                        myReceiver = new MyReceiver();
                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(BackgroundLocationService.MY_ACTION);
                        registerReceiver(myReceiver, intentFilter);
                        Intent intent = new Intent(this, BackgroundLocationService.class);
                        //
                        intent.putExtra("act_dist", actual_dis);
                        intent.putExtra("alarm_dist", alarm_dis);
                        //
                        global = intent;
                        serviceStarted = true;
                        startService(intent);
                    }
                    else
                        Toast.makeText(this,"Alarm Distance is not set",Toast.LENGTH_SHORT).show();
                }
                else
                    Toast.makeText(this,"Destination not selected!",Toast.LENGTH_SHORT).show();
            }
            else
                Toast.makeText(this,"Source not selected!",Toast.LENGTH_SHORT).show();
        }
        else
            showGPSDisabledAlertToUser();
    }
    public void stopBLS(View v){
        stopBackgroundLocationService();
    }
    public void stopBackgroundLocationService(){
        if(serviceStarted) {
            unregisterReceiver(myReceiver);
            stopService(global);
            Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
            serviceStarted=false;
        }
        else
            Toast.makeText(this,"Service has not started yet!",Toast.LENGTH_SHORT).show();
    }
    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {

            Bundle b=arg1.getBundleExtra("Location");
            Location l= b.getParcelable("Location");
            if(l!=null)
            currentLocation=new LatLng(l.getLatitude(),l.getLongitude());
            //Toast.makeText(ctx,"Difference: " + (actual_dis-alarm_dis)+" m",Toast.LENGTH_SHORT).show();
            calculateDistance(currentLocation,destination);
            if (actual_dis <= alarm_dis) {
                ring();
                stopBackgroundLocationService();
            }
        }

    }
    public String getAddressAsString(Address address){

        String display_address = "";
        display_address += address.getAddressLine(0) + "\n";

        for(int i = 1; i < address.getMaxAddressLineIndex(); i++)
        {
            display_address += address.getAddressLine(i) + ", ";
        }

        display_address = display_address.substring(0, display_address.length() - 2);

        return display_address;
    }
    //notification function
    public void sendNotification(){
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_ic_travel_alarm)
                        .setContentTitle("Your are almost there")
                        .setAutoCancel(true)
                        .setContentText("Click to turn off alarm!");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }
}

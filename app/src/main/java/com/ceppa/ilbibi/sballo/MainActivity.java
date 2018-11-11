package com.ceppa.ilbibi.sballo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.hardware.SensorEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private TextView textView;
    private TextView textView2;
    private File myFile;
    private String myFilename;
    private Date myDate;
    float[] refVals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        refVals = new float[0];

        textView = (TextView) findViewById(R.id.textView);
        textView2 = (TextView) findViewById(R.id.textView2);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }


        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (senSensorManager != null) {
            senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (senAccelerometer != null) {
                textView.setText(senAccelerometer.toString());
            }
        }
    }

    public void appendData(String str) {
        try {
            FileOutputStream fOut = new FileOutputStream(myFile, true);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(str+"\n");
            myOutWriter.flush();
            myOutWriter.close();
            fOut.close();
        } catch (Exception e) {
            textView.setText("cannot write");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Date now = new Date();
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            if (refVals.length != 3) {
                Log.d("refvals", String.valueOf(sensorEvent.values.length));
                refVals = sensorEvent.values.clone();
            } else {
                long millisec = (now.getTime()-myDate.getTime());
                String str = String.format("%010d %+02.3f %+02.3f %+02.3f", millisec, x-refVals[0], y-refVals[1], z-refVals[2]);
                textView.setText(str);
                appendData(str);
                textView2.setText(myFilename+String.format(" %d", myFile.length() / str.length()));
            }

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    protected void onPause() {
        Log.d("pause", myFile.toString());
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();

        File myDir = new File(android.os.Environment.getExternalStorageDirectory().toString()+"/sballo");
        boolean mkdirs = myDir.mkdirs();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss") ;
        myDate = new Date();
        myFilename = dateFormat.format(myDate) + ".txt";

        myFile = new File(myDir, myFilename);
        Log.d("create", myFile.toString());

        textView.setText(myFile.toString());
        try {
            myFile.createNewFile();
        } catch (Exception e) {
            Log.e("ERR", "Could not create file", e);
        }
        refVals = new float[0];

        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void createNewFile(View target) throws IOException {
        onPause();
        onResume();
    }

    public void shareFile(View target) throws IOException {
        String thisfile=myFile.getCanonicalPath();
        createNewFile(target);
        Log.d("share", thisfile.toString());
    }

    public void removeOthers(View target) throws IOException {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        File[] toRemove;

        builder.setTitle("Remove old files");
        builder.setMessage("Are you sure?");

        String str1=myFile.getCanonicalPath();

        final List<File> allFiles = new ArrayList<File>(Arrays.asList(new File(myFile.getParent()).listFiles()));

        Iterator<File> iterator = allFiles.iterator();
        while(iterator.hasNext()){
            File thisFile = iterator.next();
            String str2=thisFile.getCanonicalPath();
            if (str1.equals(str2)) {
                iterator.remove();
                Log.d("share", str1);
            }
        }

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                for (File thisFile : allFiles) {
                    Log.d("remove", thisFile.getName());
                    thisFile.delete();
                }
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();


    }

}

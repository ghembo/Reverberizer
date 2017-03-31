package com.example.agambini.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG  = "MainActivity";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 101;

    private static String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};

    private boolean permissionToRecordAccepted = false;

    private Thread audioThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = false;

                for (int i = 0; i < grantResults.length; i++) {
                    if(permissions[i].equals(Manifest.permission.RECORD_AUDIO)){
                        permissionToRecordAccepted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    }
                }
                break;
        }
    }

    public void startPlayback(View view){
        askForRecordAudioPermission();

        if (!permissionToRecordAccepted){
            Log.w(TAG, "Permission to record audio denied");

            // TODO: show message, if don't accept the app doesn't do anything...
            return;
        }

        Object audioManager = getSystemService(Context.AUDIO_SERVICE);

        audioThread = new Thread(new AudioProcessingRunnable(audioManager));

        audioThread.start();
    }

    public void stopPlayback(View view){
        audioThread.interrupt();

        audioThread = null;
    }

    private void askForRecordAudioPermission() {
        permissionToRecordAccepted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        if (!permissionToRecordAccepted){
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }
}

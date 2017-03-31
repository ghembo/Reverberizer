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
import android.widget.Button;

//TODO gestire fallimenti
//TODO loop su diversi settings (frame, sampling)
//TODO abilitare/disabilitare o parametrizzare riverbero da UI
//TODO provare riverbero + complesso
//TODO testare qualità e latenza
//TODO provare producer/consumer

public class MainActivity extends AppCompatActivity {
    private static final String TAG  = "MainActivity";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 101;

    private static final String[] PERMISSIONS = new String[]{Manifest.permission.RECORD_AUDIO};

    private boolean mPermissionToRecordAccepted = false;

    private Thread mAudioThread;
    private boolean mPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPlaying = false;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                mPermissionToRecordAccepted = false;

                for (int i = 0; i < grantResults.length; i++) {
                    if(permissions[i].equals(Manifest.permission.RECORD_AUDIO)){
                        mPermissionToRecordAccepted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    }
                }
                break;
        }
    }

    public void playButtonClicked(View view){
        Button button = (Button) view;

        if (!mPlaying){
            boolean started = startPlayback();

            if (started){
                button.setText(R.string.button_stop);
                mPlaying = true;
            }
        } else {
            stopPlayback();

            button.setText(R.string.button_play);
            mPlaying = false;
        }
    }

    private boolean startPlayback(){
        askForRecordAudioPermission();

        if (!mPermissionToRecordAccepted){
            Log.w(TAG, "Permission to record audio denied");

            // TODO: show message, if don't accept the app doesn't do anything...
            return false;
        }

        Object audioManager = getSystemService(Context.AUDIO_SERVICE);

        mAudioThread = new Thread(new AudioProcessingRunnable(audioManager));

        mAudioThread.start();

        return true;
    }

    private void stopPlayback(){
        mAudioThread.interrupt();

        mAudioThread = null;
    }

    private void askForRecordAudioPermission() {
        mPermissionToRecordAccepted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        if (!mPermissionToRecordAccepted){
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }
}

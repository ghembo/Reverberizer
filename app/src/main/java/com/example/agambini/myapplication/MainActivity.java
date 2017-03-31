package com.example.agambini.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ToggleButton;

//TODO gestire fallimenti
//TODO loop su diversi settings (frame, sampling)
//TODO abilitare/disabilitare o parametrizzare riverbero da UI
//TODO provare riverbero + complesso
//TODO testare qualità e latenza
//TODO provare producer/consumer

public class MainActivity extends AppCompatActivity {
    private static final String TAG  = "MainActivity";

    private static final String PERMISSION_TO_RECORD_ACCEPTED_EXTRA  = "PERMISSION_TO_RECORD_ACCEPTED_EXTRA";
    private static final String PLAYING_EXTRA  = "PLAYING_EXTRA";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 101;

    private static final String[] PERMISSIONS = new String[]{Manifest.permission.RECORD_AUDIO};

    private boolean mPermissionToRecordAccepted = false;

    private Thread mAudioThread;
    private boolean mPlaying;

    private ToggleButton mBtnReverbEnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPlaying = false;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnReverbEnable = (ToggleButton)findViewById(R.id.btn_reverb_on_off);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState != null){
            mPermissionToRecordAccepted = savedInstanceState.getBoolean(PERMISSION_TO_RECORD_ACCEPTED_EXTRA);
            mPlaying = !savedInstanceState.getBoolean(PLAYING_EXTRA);

            Button button = (Button)findViewById(R.id.btn_start_stop);

            playButtonClicked(button);
        }
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(PERMISSION_TO_RECORD_ACCEPTED_EXTRA, mPermissionToRecordAccepted);
        outState.putBoolean(PLAYING_EXTRA, mPlaying);
    }

    public void playButtonClicked(View view){
        if (mPlaying){
            stopPlayback();
            mPlaying = false;
        } else {
            mPlaying = startPlayback(mBtnReverbEnable.isChecked());
        }

        Button button = (Button) view;

        if (mPlaying){
            button.setText(R.string.button_stop);
        } else {
            button.setText(R.string.button_play);
        }
    }

    public void reverbButtonClicked(View view) {
        if (mPlaying){
            stopPlayback();
            startPlayback(mBtnReverbEnable.isChecked());
        }
    }

    private boolean startPlayback(boolean reverbEnable){
        askForRecordAudioPermission();

        if (!mPermissionToRecordAccepted){
            Log.w(TAG, "Permission to record audio denied");

            // TODO: use DialogFragment
            new AlertDialog.Builder(this).
                    setTitle(R.string.permission_denied).
                    setMessage(R.string.app_name + getString(R.string.permission_denied_error)).
                    setPositiveButton("OK", null).
                    show();

            return false;
        }

        Object audioManager = getSystemService(Context.AUDIO_SERVICE);

        mAudioThread = new Thread(new AudioProcessingRunnable(audioManager, reverbEnable));

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

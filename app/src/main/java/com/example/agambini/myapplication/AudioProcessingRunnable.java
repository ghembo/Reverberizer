package com.example.agambini.myapplication;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.PresetReverb;
import android.os.Build;
import android.util.Log;

class AudioProcessingRunnable implements Runnable {
    private static final String TAG  = "AudioProcessingRunnable";

    private final int mAudioRecordBufferSize;
    private int mOutputBufferSize;
    private int mSampleRate;
    private boolean mReverbEnable;

    AudioProcessingRunnable(Object audioManagerObject, boolean reverbEnable){
        mReverbEnable = reverbEnable;

        mSampleRate = 44100;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mSampleRate = android.media.AudioFormat.SAMPLE_RATE_UNSPECIFIED;
        }

        mOutputBufferSize = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            AudioManager audioManager = (AudioManager) audioManagerObject;
            int rate = Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
            mOutputBufferSize = Integer.parseInt(audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));

            if (rate != 0){
                mSampleRate = rate;
            }
        }

        mAudioRecordBufferSize = AudioRecord.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        if (mOutputBufferSize == 0){
            mOutputBufferSize = mAudioRecordBufferSize;
        }
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        AudioRecord recorder = null;
        AudioTrack track = null;

        try
        {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, mAudioRecordBufferSize);

            if (recorder.getState() != AudioRecord.STATE_INITIALIZED){
                Log.e(TAG, "Cannot initialize AudioRecord");

                return;
            }

            int audioTrackBufferSize = AudioTrack.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

            track = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, audioTrackBufferSize, AudioTrack.MODE_STREAM);

            if (track.getState() != AudioTrack.STATE_INITIALIZED){
                Log.e(TAG, "Cannot initialize AudioTrack");

                return;
            }

            if (mReverbEnable){
                PresetReverb reverb = new PresetReverb(1, track.getAudioSessionId());
                reverb.setPreset(PresetReverb.PRESET_LARGEHALL);
                reverb.setEnabled(true);

                track.attachAuxEffect(reverb.getId());
                track.setAuxEffectSendLevel(1.0f);
            }

            short[] buffer = new short[mOutputBufferSize / 2];

            recorder.startRecording();
            track.play();

            while(!Thread.interrupted())
            {
                int bufferSize = recorder.read(buffer, 0, buffer.length);
                track.write(buffer, 0, bufferSize);
            }
        }
        catch(Throwable ex)
        {
            Log.e(TAG, "Error processing audio", ex);
        }
        finally
        {
            if (recorder != null){
                recorder.release();
            }

            if (track != null){
                track.release();
            }
        }
    }
}

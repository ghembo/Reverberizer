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

    private int sampleRate;
    private int bufferSize;
    private int outputBufferSize;

    AudioProcessingRunnable(Object audioManager){
        sampleRate = 44100;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sampleRate = android.media.AudioFormat.SAMPLE_RATE_UNSPECIFIED;
        }

        outputBufferSize = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            AudioManager audioManager2 = (AudioManager) audioManager;
            int rate = Integer.parseInt(audioManager2.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
            outputBufferSize = Integer.parseInt(audioManager2.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));

            if (rate != 0){
                sampleRate = rate;
            }
        }

        bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        if (outputBufferSize == 0){
            outputBufferSize = bufferSize;
        }
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        AudioRecord recorder = null;
        AudioTrack track = null;

        try
        {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            if (recorder.getState() != AudioRecord.STATE_INITIALIZED){
                // invalid
            }

            bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

            track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

            PresetReverb reverb = new PresetReverb(1, track.getAudioSessionId());
            reverb.setPreset(PresetReverb.PRESET_LARGEHALL);
            reverb.setEnabled(true);

            track.attachAuxEffect(reverb.getId());
            track.setAuxEffectSendLevel(1.0f);

            short[] buffer = new short[outputBufferSize / 2];

            recorder.startRecording();
            track.play();

            while(!Thread.interrupted())
            {
                bufferSize = recorder.read(buffer, 0, buffer.length);
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
                recorder.stop();
                recorder.release();
            }

            if (track != null){
                track.stop();
                track.release();
            }
        }
    }
}

package com.example.homework1;

import java.io.IOException;

import android.media.MediaRecorder;
import android.util.Log;

public class SoundRecord {
    public boolean isInit = false;
    private MediaRecorder mRecorder = null;

    public void start() {
            if (mRecorder == null) {
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mRecorder.setOutputFile("/dev/null"); 
                try {
					mRecorder.prepare();
				} catch (IllegalStateException e) {
					Log.e("Shit's going down, son", e.toString());
					e.printStackTrace();
				} catch (IOException e) {
					Log.e("Shit's going down, son", e.toString());
					e.printStackTrace();
				}
                mRecorder.start();
                isInit = true;
            }
    }
    
    public void stop() {
            if (mRecorder != null) {
            		
                    mRecorder.stop();       
                    mRecorder.release();
                    mRecorder = null;
                    isInit = false;
            }
    }
    
    public double getAmplitude() {
            if (mRecorder != null)
                    return  (mRecorder.getMaxAmplitude());
            else
                    return 0;

    }
}

//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE.md file in the project root for full license information.
//
// <code>
package com.microsoft.cognitiveservices.speech.samples.quickstart;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;

import static android.Manifest.permission.*;
import static com.microsoft.cognitiveservices.speech.samples.quickstart.Util.Constants.TXT_INSTRUCTIONS;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    // Replace below with your own subscription key
    private static String speechSubscriptionKey = "7614e5a304ac49c8b56930bd680da8e1";
    // Replace below with your own service region (e.g., "westus").
    private static String serviceRegion = "eastus";
    private SpeechConfig speechConfig;
    private SpeechSynthesizer synthesizer;
    //Record audio
    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private static final String LOG_TAG = "AudioRecording";
    private static String mFileName = null;
    private Timer timer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Note: we need to request the permissions
//        int requestCode = 5; // unique code for the permission request
//        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, INTERNET}, requestCode);
        RequestPermissions();
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/AudioRecording.3gp";

    }

    public void onSpeechButtonClicked(View v) {
        TextView txt = (TextView) this.findViewById(R.id.hello); // 'hello' is the ID of your text view
        TimerTask timerTaskStopRecord = new TimerTask() {
            @Override
            public void run() {
                stopRecord();
            }
        };
        try {

            speechConfig = SpeechConfig.fromSubscription(speechSubscriptionKey, serviceRegion);
            speechConfig.setSpeechRecognitionLanguage("es-CO");
            speechConfig.setSpeechSynthesisLanguage("es-CO");
            assert (speechConfig != null);

            synthesizer = new SpeechSynthesizer(speechConfig);
            assert (synthesizer != null);

            SpeechRecognizer reco = new SpeechRecognizer(speechConfig);
            assert (reco != null);

            Future<SpeechRecognitionResult> task = reco.recognizeOnceAsync();
            assert (task != null);

            // Note: this will block the UI thread, so eventually, you want to
            //        register for the event (see full samples)
            SpeechRecognitionResult result = task.get();
            assert (result != null);

            if (result.getReason() == ResultReason.RecognizedSpeech) {
                txt.setText(result.getText());
                if (result.getText().contains("foto")) {
                    dispatchTakePictureIntent();
                    txt.setText("Se ha tomado una foto");
                }
                if (result.getText().contains("Instrucciones")) {
                    speachIntructions(TXT_INSTRUCTIONS);
                    txt.setText("Se han leido las isntrucciones");
                }
                if (result.getText().contains("Grabar audio")) {
                    txt.setText("Se ha grabado el audio");
                    startRecord();
                    timer.schedule(timerTaskStopRecord, 10000);
                }

                if (result.getText().contains("Reproducir audio")) {
                    playAudio();
                }

                Log.e("SpeechSDKDemo", "Speach " + result.getText());
            } else {
                txt.setText("Error reconociendo voz" + System.lineSeparator() + result.toString());
            }

            reco.close();
        } catch (Exception ex) {
            Log.e("SpeechSDKDemo", "unexpected " + ex.getMessage());
            assert (false);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            // display error state to the user
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ImageView imageView = this.findViewById(R.id.imageView);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        synthesizer.close();
        speechConfig.close();
    }

    private void speachIntructions(String textToSay) {

        try {
            // Note: this will block the UI thread, so eventually, you want to register for the event
            SpeechSynthesisResult result = synthesizer.SpeakText(textToSay);
            assert (result != null);

            if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
//                outputMessage.setText("Speech synthesis succeeded.");
            } else if (result.getReason() == ResultReason.Canceled) {
                String cancellationDetails =
                        SpeechSynthesisCancellationDetails.fromResult(result).toString();
            }

            result.close();
        } catch (Exception ex) {
            Log.e("SpeechSDKDemo", "unexpected " + ex.getMessage());
            assert (false);
        }
    }

    private void startRecord() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(mFileName);
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        mRecorder.start();
        Toast.makeText(getApplicationContext(), "Se empezo a grabar el audio", Toast.LENGTH_LONG).show();
    }

    private void stopRecord() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        Looper.prepare();
        Toast.makeText(getApplicationContext(), "Se termino de grabar el audio", Toast.LENGTH_LONG).show();
        Looper.loop();
    }

    private void playAudio() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
            Toast.makeText(getApplicationContext(), "Se empezo a reproducir la grabacion", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void RequestPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, INTERNET}, REQUEST_AUDIO_PERMISSION_CODE);
    }
}



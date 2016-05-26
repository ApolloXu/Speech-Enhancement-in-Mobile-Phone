package xu.fyp.project;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class RecordActivity extends Activity {

    private ImageButton record_button;
    private boolean isRecording = false;


    private Item item;
  // 檔案名稱
    private String fileName;
    private String recFileName;


    private AudioManager am = null;
    private static AudioRecord record = null;
    private AudioTrack track = null;
    private static int minBufSize;
    private Thread recordPlayThread,recordProcessThread;
    private boolean stop = true;
    private boolean isStart = false;
    private static final String AUDIO_FOLDER = "project";
    private static String AUDIO_TEMPFILE = "record_temp";
    private static final int SAMPLE_RATE = 16000;
    //private static final int SAMPLE_RATE = 44100;
    private static String fullTmpFilename;
    private static String originalFullTmpFilename;
    private static FileOutputStream os = null;
    private static FileOutputStream os1 = null;
    private static TextView recordTimeTV;
    private static WaveformView waveformView;

    private double recordTime = 0.0;
    private double realTime = 0.0;
    private final double maxRecordTime = 5*60*60;		// 5 hours maximum
    private boolean isPaused = false;
    private boolean isOne = false;
    private boolean isTwo = false;
    private boolean isThree = false;
    private static short[]buffer1,buffer2,buffer3,buffer4,spectralSbutraction;
    private static double[] noise,signal;
    private static short shortArr[];
    private static short output_frame[];
    private static Date curDate;
    private static long beginTime;
    private static ImageButton playBtn;
    private static ImageButton stopBtn;
    private boolean lastIsOne = false;
    private boolean lastIsTwo = false;
    private boolean lastIsThree = false;
    private static SpectralSubtraction ss;
    private int loop=0;
    private double noiseAverage;
    private webrtcNS webRtcNS;
    private static int webRtcHandle=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        processViews();

        processControllers();

       // int x = webrtcNS.createNSInstance();
        //Log.e("webrtc",String.valueOf(x));

        // 讀取檔案名稱
        //Intent intent = getIntent();

    }


    public void onSubmit(View view) {
        if (!stop) {
            // 停止錄音
            stopRecordPlay();
        }

        // 確定
        if (view.getId() == R.id.record_ok) {

            item.setRecFileName(recFileName);
            item.setFileName(fileName);

            item.setTitle(String.format("%.1f", recordTime));
            item.setContent(String.format("%.1f", realTime));
            Intent result = getIntent();
            result.putExtra("xu.fyp.project.Item", item);
            setResult(Activity.RESULT_OK, result);


        }

        finish();
    }

    private void processViews() {
        //record_button = (ImageButton) findViewById(R.id.record_button);
        //record_volumn = (ProgressBar) findViewById(R.id.record_volumn);
        // 隱藏狀態列ProgressBar

        playBtn = (ImageButton) findViewById(R.id.record_button);

        stopBtn = (ImageButton) findViewById(R.id.test_stop);

        recordTimeTV = (TextView)findViewById(R.id.timer);
        recordTimeTV.setText("00:00.0");

        //waveformView = (WaveformView) findViewById(R.id.waveform_view);



        isPaused = false;
        prepareTempFile();


        fullTmpFilename=getTempFilename();
        originalFullTmpFilename = getOriginalTempFilename();
        //Log.e("ss",fullTmpFilename);
        final File recordFile = configRecFileName(".wav");
        final File originalFile = configRecFileName("_original.wav");
        recFileName = recordFile.getAbsolutePath();
        fileName = originalFile.getAbsolutePath();
        //Log.e("save", recFileName);

        init();

        item = new Item();


        item.setDatetime(new Date().getTime());



    }


    private void processControllers(){

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (stop) {
                    stop = false;                                    // Avoid user pressing the Play key multiple times very quickly
                    try {
                        os = new FileOutputStream(fullTmpFilename);
                        os1 = new FileOutputStream(originalFullTmpFilename);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    //am.startBluetoothSco();
                    //am.setBluetoothScoOn(true);


                    curDate = new Date(System.currentTimeMillis());
                    beginTime = curDate.getTime();
                    recordTime = 0.0;

                    //thread for recording
                    recordPlayThread = new Thread() {
                        @Override
                        public void run() {
                            recordAndPlay();
                        }
                    };
                    recordPlayThread.start();

                    //thread for processing
                    recordProcessThread = new Thread() {
                        @Override
                        public void run() {
                            recordAndProcess();
                        }

                    };
                    recordProcessThread.start();

                }
            }
        });

		/*
		 * Kill the recorder thread and copy the temp file to a WAV file. Ignore button click if
		 * the recorder has already been stopped.
		 */
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!stop) {
                    stopRecordPlay();
                }
            }
        });


    }
    public void stopRecordPlay() {
        stop = true;
        playBtn.setImageResource(R.drawable.button_end);
        isStart = false;

        record.stop();
        //am.stopBluetoothSco();
        //am.setBluetoothScoOn(false);
        recordPlayThread = null;
        recordProcessThread = null;
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        os = null;
        os1=null;
        String fullname = getFullname();
        String originalFullname = getOriginalFullname();
        //Log.e("save",fullname);
        //System.out.println("Debugging: " + fullTmpFilename + " to " + fullname + ".wav");
        Wave.copyTmpfileToWavfile(fullTmpFilename, fullname, (long) SAMPLE_RATE, minBufSize);
        Wave.copyTmpfileToWavfile(originalFullTmpFilename, originalFullname, (long) SAMPLE_RATE, minBufSize);
        //Log.e("save", fullTmpFilename);
        Wave.deleteTempFile(fullTmpFilename);
        Wave.deleteTempFile(originalFullTmpFilename);// Delete the temp file
    }
    private static class MyHandler extends android.os.Handler {
        private final WeakReference<RecordActivity> mActivity;

        public MyHandler(RecordActivity activity) {
            mActivity = new WeakReference<RecordActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            RecordActivity activity = mActivity.get();
            if (activity != null) {
                recordTimeTV.setText((String) msg.obj);
            }
        }
    }
    private final MyHandler myHandler = new MyHandler(this);


    private void recordAndPlay() {
        int bufSize =minBufSize/2;
        //Log.e("ss",String.valueOf(bufSize));// minBufSize is the size of buf in bytes
        buffer1 = new short[bufSize];
        buffer2 = new short[bufSize];
        buffer3 = new short[bufSize];
        int num = 0;

        record.startRecording();
        isOne = true;
        isTwo = false;
        isStart = true;
        while (stop == false) {
            if(isOne) {

                //read data into buffer1 whose size is bufSize
                num = record.read(buffer1,0,bufSize);

                if (os != null) {
                    //Wave.writeAudioBufToFile(fullTmpFilename, buffer1, os);
                    //isStart= false;
                }
                isOne = false;
                isTwo = true;
                //isStart = true;
            }   else if(isTwo){
                //Log.e("snak","read2");
                num = record.read(buffer2,0,bufSize);
                if (os != null) {
                    //Wave.writeAudioBufToFile(fullTmpFilename, buffer2, os);

                }
                isTwo = false;
                isOne = true;
                //isStart = true;
            } /*else if(isThree){
                //Log.e("snak","read3");
                num = record.read(buffer3,0,bufSize);
                if (os != null) {
                    //Wave.writeAudioBufToFile(fullTmpFilename, buffer2, os);

                }
                isThree = false;
                isOne = true;

            }*/

            //num = record.read(buffer1,0,bufSize);
            if (isPaused == false) {
                //ss = new SpectralSubtraction(lin);
                //double n =ss.noiseAverage();
                //Log.d("ss",String.valueOf(n));


                								// To reduce computation, we display waveform
            }																	// only if the app is running in the foreground


            if (recordTime >= maxRecordTime) {
                stopRecordPlay();
                stop = true;
            }
        }
    }
    private File configRecFileName( String extension) {

            String path;
            path = FileUtil.getUniqueFileName();


        return new File(FileUtil.getExternalStorageDir(FileUtil.APP_DIR),
                 path + extension);
    }
    private String getFullname() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, FileUtil.APP_DIR);
        String eventID=recFileName;
        if (!file.exists()) {
            file.mkdirs();
        }


        return eventID;
    }
    private String getOriginalFullname() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, FileUtil.APP_DIR);
        String eventID=fileName;
        if (!file.exists()) {
            file.mkdirs();
        }


        return eventID;
    }

    private void recordAndProcess(){
        int bufSize = minBufSize/2;
        shortArr = new short[minBufSize];
        output_frame = new short[160];
        buffer3 = new short[bufSize/2];
        buffer4 = new short[bufSize/2];
        noise = new double[bufSize*8];
        spectralSbutraction = new short[bufSize*8];
        signal = new double[bufSize];
        lastIsOne = false;
        lastIsTwo = true;
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");


        while(stop ==false){


            while(isStart) {
                //Log.e("ss", "Looping");
                if (isTwo&&lastIsOne) {
                                   /*webRtcNS = new webrtcNS();
                   webRtcHandle = webRtcNS.initiate(16000);
                    while(webRtcHandle == -1){
                        webRtcHandle = webRtcNS.initiate(16000);
                    }

                    //Log.e("webrtc",String.valueOf(webRtcHandle));
                    for (int i = 0; i <= bufSize/160; i++) {
                        if (i < bufSize / 160) {
                            for (int j = 0; j <= 159; j++) {
                                if ((i * 160 + j) < bufSize) {
                                    shortArr[j] = buffer1[i * 160 + j];
                                }
                            }
                            //Log.e("webrtc",String.valueOf(shortArr.length));
                            output_frame = (webRtcNS.process(webRtcHandle, shortArr)).clone();
                            //Log.e("webrtc","222");
                            while (output_frame == null) {
                                output_frame = (webRtcNS.process(webRtcHandle, shortArr)).clone();
                            }
                            //Log.e("ss", "end1");

                            System.arraycopy(output_frame, 0, buffer4, 160 * i, 160);


                        }else{
                            int remain = bufSize-i*160;
                            for (int j = 0; j < remain; j++) {
                                if ((i * 160 + j) < bufSize) {
                                    shortArr[j] = buffer1[i * 160 + j];
                                }
                            }
                            output_frame = (webRtcNS.process(webRtcHandle, shortArr)).clone();
                            //Log.e("webrtc","222");
                            while (output_frame == null) {
                                output_frame = (webRtcNS.process(webRtcHandle, shortArr)).clone();
                            }
                            System.arraycopy(output_frame, 0, buffer4, 160 * i, remain);
                        }
                        int flag =  webRtcNS.free(webRtcHandle);
                        while(flag == -1){
                            flag = webRtcNS.free(webRtcHandle);
                        }
                        Log.e("ss","free1");

                    }*/

                    //Log.e("webrtc", "333");

                    Wave.writeAudioBufToFile(originalFullTmpFilename, buffer1, os1);

                    if(loop<7){
                        for(int i=0;i<bufSize;i++){
                            signal[i]=(double) buffer1[i];
                            signal[i]= signal[i]/32768.0;
                        }
                        System.arraycopy(signal,0,noise,loop*bufSize,bufSize);
                        System.arraycopy(buffer1,0,spectralSbutraction,loop*bufSize,bufSize);
                        buffer1 = new short[bufSize];
                    }else if(loop==7){
                        System.arraycopy(signal,0,noise,loop*bufSize,bufSize);
                        System.arraycopy(buffer1,0,spectralSbutraction,loop*bufSize,bufSize);
                        ss = new SpectralSubtraction(spectralSbutraction,bufSize);
                        noiseAverage = ss.noiseAverage(noise);
                        buffer1 = new short[bufSize];
                    }
                    else{
                    //Log.e("ss","start to modify");
                        System.arraycopy(buffer3,0,shortArr,0,bufSize/2);
                        System.arraycopy(buffer1,0,shortArr,bufSize/2,bufSize);
                        System.arraycopy(buffer3, 0, shortArr, bufSize/2+bufSize, bufSize/2);
                        ss.setSignal(shortArr);
                        System.arraycopy(ss.noiseSubtraction(noiseAverage),bufSize/2,buffer1,0,bufSize);
                        //buffer1 = ss.noiseSubtraction(noiseAverage);
                        //Log.e("ss",String.valueOf(loop));
                    }
                    Wave.writeAudioBufToFile(fullTmpFilename, buffer1, os);
                    //Log.e("ss", "write1");
                    //waveformView.updateAudioData(buffer1);
                    lastIsOne= false;
                    lastIsTwo = true;
                    realTime +=(double)(bufSize)/(double)SAMPLE_RATE;
                    //Log.e("ss", "finish1");
                    //recordTime += (double)(bufSize)/(double)SAMPLE_RATE;
                    //Log.e("webrtc", "444");
                    loop++;
                } else if(isOne&&lastIsTwo){
                   /* webRtcNS = new webrtcNS();
                    webRtcHandle = webRtcNS.initiate(16000);
                    while(webRtcHandle == -1){
                        webRtcHandle = webRtcNS.initiate(16000);
                    }

                    //Log.e("webrtc",String.valueOf(webRtcHandle));
                    for (int i = 0; i <= bufSize/160; i++) {
                        if (i < bufSize / 160) {
                            for (int j = 0; j <= 159; j++) {
                                if ((i * 160 + j) < bufSize) {
                                    shortArr[j] = buffer2[i * 160 + j];
                                }
                            }
                            //Log.e("webrtc",String.valueOf(shortArr.length));
                            output_frame = (webRtcNS.process(webRtcHandle, shortArr)).clone();
                            //Log.e("webrtc","222");
                            while (output_frame == null) {
                                output_frame = (webRtcNS.process(webRtcHandle, shortArr)).clone();
                            }
                            //Log.e("ss", "end1");

                            System.arraycopy(output_frame, 0, buffer4, 160 * i, 160);


                        }else{
                            int remain = bufSize-i*160;
                            for (int j = 0; j < remain; j++) {
                                if ((i * 160 + j) < bufSize) {
                                    shortArr[j] = buffer2[i * 160 + j];
                                }
                            }
                            output_frame = (webRtcNS.process(webRtcHandle, shortArr)).clone();
                            //Log.e("webrtc","222");
                            while (output_frame == null) {
                                output_frame = (webRtcNS.process(webRtcHandle, shortArr)).clone();
                            }
                            System.arraycopy(output_frame, 0, buffer4, 160 * i, remain);
                        }
                        int flag =  webRtcNS.free(webRtcHandle);
                        while(flag == -1){
                            flag = webRtcNS.free(webRtcHandle);
                        }
                        Log.e("ss","free1");

                    }*/
                    Wave.writeAudioBufToFile(originalFullTmpFilename, buffer2, os1);


                    if(loop<7){
                        for(int i=0;i<bufSize;i++){
                            signal[i]=(double) buffer2[i];
                            signal[i]= signal[i]/32768.0;
                        }
                        System.arraycopy(signal,0,noise,loop*bufSize,bufSize);
                        System.arraycopy(buffer2,0,spectralSbutraction,loop*bufSize,bufSize);
                        buffer2 = new short[bufSize];
                    }else if(loop==7){
                        System.arraycopy(signal,0,noise,loop*bufSize,bufSize);
                        System.arraycopy(buffer2,0,spectralSbutraction,loop*bufSize,bufSize);
                        ss = new SpectralSubtraction(spectralSbutraction,bufSize);
                        noiseAverage = ss.noiseAverage(noise);
                        buffer2 = new short[bufSize];
                    }
                    else{
                        //Log.e("ss","start to modify");
                        System.arraycopy(buffer3,0,shortArr,0,bufSize/2);
                        System.arraycopy(buffer2,0,shortArr,bufSize/2,bufSize);
                        System.arraycopy(buffer3, 0, shortArr, bufSize/2+bufSize, bufSize/2);
                        ss.setSignal(shortArr);
                        System.arraycopy(ss.noiseSubtraction(noiseAverage),bufSize/2,buffer2,0,bufSize);
                    }

                    //transform data from short to byte and save to temp file
                    Wave.writeAudioBufToFile(fullTmpFilename, buffer2, os);


                    //Log.e("ss", "write2");
                    lastIsTwo=false;
                    lastIsOne = true;

                    realTime +=(double)(bufSize)/(double)SAMPLE_RATE;

                    //waveformView.updateAudioData(buffer2);
                    loop++;
                    //Log.e("ss", "finish2");
                    //recordTime += (double)(bufSize)/(double)SAMPLE_RATE;
                }/*else if(isOne&&lastIsThree){
                    webRtcNS = new webrtcNS();
                    webRtcHandle = webRtcNS.initiate(16000);
                    while(webRtcHandle == -1){
                        webRtcHandle = webRtcNS.initiate(16000);
                    }


                    for (int i = 0; i <=bufSize/160; i++) {
                        Log.e("ss","Start3");
                        Log.e("ss",String.valueOf(i));
                        for (int j = 0; j <= 159; j++) {
                            if((i*160+j) < bufSize) {
                                shortArr[j] = buffer3[i * 160 + j];
                            }
                        }
                        output_frame = (webRtcNS.process(webRtcHandle, shortArr)).clone();
                        while(output_frame == null){
                            output_frame = (webRtcNS.process(webRtcHandle, shortArr)).clone();
                        }
                        Log.e("ss","end3");
                        if(i<bufSize/160){
                            System.arraycopy(output_frame, 0, buffer4, 160 * i, 160);
                        }else{
                            System.arraycopy(output_frame,0,buffer4,160*i,bufSize-160*i);
                        }
                        Log.e("ss","close3");
                    }
                    int flag =  webRtcNS.free(webRtcHandle);
                    while(flag == -1){
                        flag = webRtcNS.free(webRtcHandle);
                    }
                    Log.e("ss", "free3");

                    if(loop<3){
                        for(int i=0;i<bufSize;i++){
                            signal[i]=(double) buffer3[i];
                            signal[i]= signal[i]/32768.0;
                        }
                        System.arraycopy(signal,0,noise,loop*bufSize,bufSize);
                        System.arraycopy(buffer3,0,spectralSbutraction,loop*bufSize,bufSize);
                    }else if(loop==3){
                        System.arraycopy(signal,0,noise,loop*bufSize,bufSize);
                        System.arraycopy(buffer3,0,spectralSbutraction,loop*bufSize,bufSize);
                        ss = new SpectralSubtraction(spectralSbutraction);
                        noiseAverage = ss.noiseAverage(noise);
                    }
                    else{
                        //Log.e("ss","start to modify");
                        ss.setSignal(buffer3);
                        buffer3 = ss.noiseSubtraction(noiseAverage);

                    }

                    Wave.writeAudioBufToFile(fullTmpFilename, buffer3, os);
                    //Log.e("ss", "write3");
                    lastIsThree=false;
                    lastIsOne = true;
                    realTime +=(double)(bufSize)/(double)SAMPLE_RATE;
                    //waveformView.updateAudioData(buffer2);
                    loop++;
                    //Log.e("snn", String.valueOf(noiseAverage));
                    //recordTime += (double)(bufSize)/(double)SAMPLE_RATE;
                }*/
                //Log.e("ss","loopingmid");
                					// Assuming 16bit per sample


                Date preDate = new Date(System.currentTimeMillis());
                long preTime = preDate.getTime();
                long timer = preTime - beginTime;
                recordTime = (double) timer/1000.0;


                Message msg = myHandler.obtainMessage();
                msg.obj = String.format("%.1f seconds", recordTime);
                myHandler.sendMessage(msg);


                //Log.e("ss","Loopend");

               /* try{
                    recordProcessThread.sleep(50);
                }catch (Exception e){
                    e.printStackTrace();
                }*/

            }

        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("Call: onDestroy");
        stop = true;											// Cause the while loop in recordAndPlay() to finish
        if (recordPlayThread != null) {
            try {
                recordPlayThread.join();						// Wait until the thread to finish
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        recordPlayThread = null;
         if (record != null) {
            record.stop();									// stop recording
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;

    }



    private void init() {

        am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.setMode(AudioManager.STREAM_VOICE_CALL);
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);


        //Log.e("ss", String.valueOf(minBufSize));
        //minBufSize=2048;


        record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize);



        //int maxJitter = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        //track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                //maxJitter, AudioTrack.MODE_STREAM);

    }
    private void prepareTempFile() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, FileUtil.APP_DIR);
        if (!file.exists()) {
            file.mkdirs();
        }
        File tempFile = new File(filepath, AUDIO_TEMPFILE);
        if (tempFile.exists()) {
            tempFile.delete();
        }
        File tempFile1 = new File(filepath, AUDIO_TEMPFILE+"1");
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }





    private String getTempFilename() {

        String filepath = Environment.getExternalStorageDirectory().getPath();

        File file = new File(filepath, FileUtil.APP_DIR);
        String path = file.getAbsolutePath() + "/" + AUDIO_TEMPFILE;

        return (file.getAbsolutePath() + "/" + AUDIO_TEMPFILE);

    }
    private String getOriginalTempFilename() {

        String filepath = Environment.getExternalStorageDirectory().getPath();

        File file = new File(filepath, FileUtil.APP_DIR);
        String path = file.getAbsolutePath() + "/" + AUDIO_TEMPFILE+"1";

        return (file.getAbsolutePath() + "/" + AUDIO_TEMPFILE+"1");

    }


}
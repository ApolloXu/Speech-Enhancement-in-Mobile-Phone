package xu.fyp.project;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PlayActivity extends Activity {

    private MediaPlayer mediaPlayer;
    private Item item;
    private static final int SAMPLE_RATE = 16000;
    private String fileName;
    private String recFileName;
    private static AudioTrack track = null;
    private Thread recordPlayThread;
    private static WaveformAudio waveformAudio;
    private static WaveformCompare waveformCompare;
    private static short shortArr[];
    private static short history[];
    private static double doubArry[];

    private static int blockSize;
    private static SpectralSubtraction ss;

    public static final float pi= (float) 3.1415926;
    private static double[] abs;
    private static webrtcNS webRtcNS;
    private static int webRtcHandle=0;
    private short temp[];
    private static short enhancement[];



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        waveformAudio = (WaveformAudio) findViewById(R.id.waveform_audio);
        waveformCompare = (WaveformCompare)findViewById(R.id.waveform_compare);
        Intent intent = getIntent();
        fileName = intent.getStringExtra("fileName");
        recFileName = intent.getStringExtra("recFileName");
        //Log.e("ss",fileName);
       // Log.e("ss",recFileName);
        try {

            processViews(fileName,recFileName);
        }catch(IOException e){
            e.printStackTrace();
        }
        item = new Item();
        item = (Item)intent.getExtras().getSerializable("xu.fyp.project.Item");




    }

    private void processViews(String filePath, String recFilePath)throws IOException{
        //Log.e("ss","start");
        byte[] byteData = null;
        File file = null;
        file = new File(recFilePath); // for ex. path= "/sdcard/samplesound.pcm" or// "/sdcard/samplesound.wav"
        byteData = new byte[(int) file.length()];
        shortArr = new short[byteData.length/2-22];
        history = new short[shortArr.length];
        doubArry = new double[shortArr.length];
        //Log.d("Test",String.valueOf(shortArr.length));
        //webRtcNS = new webrtcNS();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            in.read(byteData);

            for (int i = 22; i <byteData.length/2 ; i++)
            {
                shortArr[i-22] = ( (short)( ( byteData[i*2] & 0xff )|( byteData[i*2 + 1] << 8 ) ) );
                history[i-22] = shortArr[i-22];
            }

            in.close();

        } catch (FileNotFoundException e) {

            e.printStackTrace();
        }


        //waveformAudio.printAudioData(shortArr);
        //waveformAudio.printAudioData(shortArr);
       // Log.e("ss","end");


    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    public void onSubmit(View view) {

        if (track !=null) {
            track.stop();
            track.release();
        }

        // 結束Activity元件
        Intent result = getIntent();
        result.putExtra("xu.fyp.project.Item", item);
        setResult(Activity.RESULT_OK,result);
        finish();
    }

    public void clickPlay(View view) {
        // start
        //Log.e("ss","start");
        recordPlayThread = new Thread() {
            @Override
            public void run() {
                try{
                    PlayAudioTrack(fileName, recFileName);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        };
        recordPlayThread.start();

    }

    public void clickPause(View view) {
        // pause
        if(track!=null)
            track.pause();
    }

    public void clickStop(View view) {
        // stop
        if (track !=null) {
            track.stop();
            track.release();
        }

        // go back to start

        recordPlayThread = null;
        Intent result = getIntent();
        result.putExtra("xu.fyp.project.Item", item);
        setResult(Activity.RESULT_OK, result);
        finish();
    }
    public static void PlayAudioTrack(String filePath, String recFilePath)
            throws IOException {
        // We keep temporarily filePath globally as we have only two sample
        // sounds now..Ω
        //Log.e("ss","start1");
        if (filePath == null)
            return;
        //Log.e("ss","start2");
        // Reading the file..



        byte[] byteData = null;

        File originalFile = null;
        originalFile = new File(filePath); // for ex. path= "/sdcard/samplesound.pcm" or// "/sdcard/samplesound.wav"
        byteData = new byte[(int) originalFile.length()];
        //Log.e("ss",originalFile.getAbsolutePath());
        history = new short[byteData.length/2-22];
        FileInputStream in1 = null;
        try {
            in1 = new FileInputStream(originalFile);
            in1.read(byteData);

            for (int i = 22; i <byteData.length/2 ; i++)
            {
                history[i-22] = ( (short)( ( byteData[i*2] & 0xff )|( byteData[i*2 + 1] << 8 ) ) );
                //history[i-22] = shortArr[i-22];
            }

            //Log.e("ss",String.valueOf(shortArr.length));
            in1.close();




        } catch (FileNotFoundException e) {

            e.printStackTrace();
        }

        File file = null;
        file = new File(recFilePath); // for ex. path= "/sdcard/samplesound.pcm" or// "/sdcard/samplesound.wav"
        byteData = new byte[(int) file.length()];
        shortArr = new short[byteData.length/2-22];
        //history = new short[shortArr.length];
        //Log.e("ss",file.getAbsolutePath());
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            in.read(byteData);

            for (int i = 22; i <byteData.length/2 ; i++)
            {
                shortArr[i-22] = ( (short)( ( byteData[i*2] & 0xff )|( byteData[i*2 + 1] << 8 ) ) );
                //history[i-22] = shortArr[i-22];
            }

            //Log.e("ss",String.valueOf(shortArr.length));
            in.close();




        } catch (FileNotFoundException e) {

            e.printStackTrace();
        }






       /* Log.e("ss","all the ss");
        ss = new SpectralSubtraction(shortArr);
        webRtcNS = new webrtcNS();

        boolean flag = true;
        int length = shortArr.length;
        int loop = length/160;
        short[] temp = new short[160];
        //short[] output = new short[160];
        enhancement = new short[length];

        for(int i=0;i<loop;i++){
            webRtcHandle = webRtcNS.initiate(16000);

            while(webRtcHandle==-1){
                webRtcHandle = webRtcNS.initiate(16000);
            }
            Log.e("ss","initial");
            for(int j=0;j<160;j++){
                if(i*160+j>=length-1) {
                    flag=false;
                    Log.e("ss", String.valueOf(i * 160 + j));
                    break;
                }
                temp[j]= shortArr[i*160+j];

            }
            Log.e("ss","process");
            if(flag) {
                short[] output = (webRtcNS.process(webRtcHandle, temp)).clone();

                while (output == null || output.length != 160) {
                    output = (webRtcNS.process(webRtcHandle, temp)).clone();
                }
                Log.e("ss","process1");
                System.arraycopy(output, 0, enhancement, i * 160, 160);
                webRtcNS.free(webRtcHandle);
                Log.e("ss", "free");
                while(webRtcHandle==-1){
                    webRtcNS.free(webRtcHandle);
                }
            }

        }
        Log.e("ss","start to modify");



       */

        //shortArr = ss.noiseSubtraction();
        //Log.e("ss","start to modify");
        //FileOutputStream os = null;
        //File newfile = new File("/storage/emulated/0/androidtutorial/record_temp1");
        //os = new FileOutputStream("/storage/emulated/0/project/record_temp1");
        //Wave.writeAudioBufToFile("/storage/emulated/0/project/record_temp1", enhancement, os);

       // Wave.copyTmpfileToWavfile("/storage/emulated/0/project/record_temp1", "/storage/emulated/0/project/2017.wav", (long) SAMPLE_RATE, 1536);
        //byte[] soundData = short2byte(enhancement);
        //Log.e("ss","end");

        int maxJitter = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);


        track = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxJitter, AudioTrack.MODE_STREAM);


        if (track != null) {
            //Log.e("ss","update the wave");
            waveformAudio.printAudioData(shortArr);
            waveformCompare.printAudioData(history);
            //Log.e("ss","play the wave");
            track.play();

            // Write the byte array to the track
            track.write(byteData, 0, byteData.length);
            track.stop();
            track.release();
            track = null;
        } else
            Log.e("ss", "audio track is not initialised ");

    }
    private static byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
        }
        return bytes;
    }


}
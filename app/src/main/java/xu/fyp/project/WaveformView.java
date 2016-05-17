package xu.fyp.project;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * A view that displays audio data on the screen as a waveform.
 *
 * Acknowledgment: This waveform display class is dervied from the WaveformView.java
 * developed by Tony Allevato (https://github.com/allevato)
 * https://github.com/googleglass/gdk-waveform-sample/blob/master/src/com/google/android/glass/sample/waveform/WaveformView.java
 */

public class WaveformView extends SurfaceView {

    // The number of buffer frames to keep around (for a nice fade-out visualization).
    private static final int HISTORY_SIZE = 2;
    private static int angle = 7200;

    // To make quieter sounds still show up well on the display, we use +/- 8192
    // as the amplitude that reaches the top/bottom of the view instead of +/- 32767. Any samples
    // that have magnitude higher than this limit will simply be clipped during drawing.
    private static final float MAX_AMP_TO_DRAW = 8192.0f;

    // The queue that will hold historical audio data.
    private final LinkedList<short[]> mAudioData;

    private final Paint mPaint;
    //private final Paint mPaint1;
    //private final Paint mPaint2;
    //private final Paint mPaint3;
    private final Paint mPaint4;


    public WaveformView(Context context) {
        this(context, null, 0);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyle) {

        super(context, attrs, defStyle);
        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mAudioData = new LinkedList<short[]>();
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(0);
        mPaint.setAntiAlias(true);
        /*mPaint1 = new Paint();
        mPaint1.setStyle(Paint.Style.STROKE);
        mPaint1.setColor(Color.WHITE);
        mPaint1.setStrokeWidth(0);
        mPaint1.setAntiAlias(true);
        mPaint2 = new Paint();
        mPaint2.setStyle(Paint.Style.STROKE);
        mPaint2.setColor(Color.WHITE);
        mPaint2.setStrokeWidth(0);
        mPaint2.setAntiAlias(true);
        mPaint3 = new Paint();
        mPaint3.setStyle(Paint.Style.STROKE);
        mPaint3.setColor(Color.WHITE);
        mPaint3.setStrokeWidth(0);
        mPaint3.setAntiAlias(true);*/
        mPaint4 = new Paint();
        mPaint4.setStyle(Paint.Style.STROKE);
        mPaint4.setColor(Color.WHITE);
        mPaint4.setStrokeWidth(0);
        mPaint4.setAntiAlias(true);
    }

    /**
     * Updates the waveform view with a new "frame" of samples and renders it.
     * The new frame gets added to the front of the rendering queue, pushing the
     * previous frames back, causing them to be faded out visually.
     *
     * @param buffer
     *            the most recent buffer of audio samples
     */
    public synchronized void updateAudioData(short[] buffer) {
        short[] newBuffer;

        // We want to keep a small amount of history in the view to provide a nice fading effect.
        // We use a linked list that we treat as a queue for this.
        if (mAudioData.size() == HISTORY_SIZE) {
            newBuffer = mAudioData.removeFirst();
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
        } else {
            newBuffer = buffer.clone();
        }
        mAudioData.addLast(newBuffer);

        // Update the display.
        Canvas canvas = getHolder().lockCanvas();
        if (canvas != null) {
            drawWaveform(canvas);
            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    /**
     * Repaints the view's surface.
     *
     * @param canvas
     *            the {@link Canvas} object on which to draw
     */
    private void drawWaveform(Canvas canvas) {
        // Clear the screen each time because SurfaceView won't do this for us.
        //canvas.drawColor(0X444444);
        canvas.drawColor(Color.BLACK);



        float width = getWidth();
        float height = getHeight();
        float centerY = height / 2;


        // We draw the history from oldest to newest so that the older audio data is further back
        // and darker than the most recent data.
        int colorDelta = 255 / (HISTORY_SIZE + 1);
        int brightness = colorDelta;

        mPaint.setStrokeWidth(4);
        //mPaint1.setStrokeWidth(4);
        //mPaint2.setStrokeWidth(4);
        //mPaint3.setStrokeWidth(4);
        mPaint4.setStrokeWidth(4);



        // Rescale the amplitude of buffer so that it max is <= MAX_AMP_TO_DRAW
        rescaleAudioData(mAudioData, MAX_AMP_TO_DRAW);

        //for (short[] buffer : mAudioData) {
        int xinc = 1;
        float bufferlength = 0;
        double sum = 0;
        for(short[] buffer:mAudioData){
            for(int i=0; i<buffer.length;i++){
                sum=sum+buffer[i]*buffer[i];
                if (buffer.length <= 640) {
                    xinc = 4;
                }

            }
            bufferlength =+buffer.length;
        }
        //short[] buffer = mAudioData.getLast();
        mPaint.setColor(Color.argb(brightness, 255, 255, 255));
        //mPaint1.setColor(Color.argb(brightness/2, 255, 255, 255));
        //mPaint2.setColor(Color.argb(brightness/3, 255, 255, 255));
        //mPaint3.setColor(Color.argb(brightness/4, 255, 255, 255));
        mPaint4.setColor(Color.argb(brightness/5, 255, 255, 255));
        float lastX = -1;
        float lastY = -1;

            // For efficiency, we don't draw all of the samples in the buffer, but only the ones
            // that align with pixel boundaries. To further reduce computation overhead, only draw
            // once every 4 pixels. This solves the jittering problem in Samsung G4 where the minBufSize is
            // very small (40ms)
        final double amplitude = sum / bufferlength;
        for (int x = 0; x < width; x+=xinc) {
            int index = (int) ((x / width) * bufferlength);

            angle=angle-1;
            if(angle == 0){
                angle = 7200;
            }

            float y = (float)(Math.sqrt(amplitude)*Math.sin((index+angle/20)*Math.PI/180)/centerY*10+centerY);
            //float y1 = (float)(Math.sqrt(amplitude)*Math.sin((index+angle/20)*Math.PI/180)/centerY*8+centerY);
            //float y2 = (float)(Math.sqrt(amplitude)*Math.sin((index+angle/20)*Math.PI/180)/centerY*6+centerY);
            //float y3 = (float)(Math.sqrt(amplitude)*Math.sin((index+angle/20)*Math.PI/180)/centerY*4+centerY);
            float y4 = (float)(Math.sqrt(amplitude)*Math.sin((index+angle/20)*Math.PI/180)/centerY*2+centerY);
            //Log.d("test",String.valueOf(angle));
            //float y = (float) ((sample / MAX_AMP_TO_DRAW) * Math.sin((index)*Math.PI/180)*centerY+centerY);
            if (lastX != -1) {
                canvas.drawLine(lastX, lastY, x, y, mPaint);
                //canvas.drawLine(lastX, lastY, x, y1, mPaint1);
                //canvas.drawLine(lastX, lastY, x, y2, mPaint2);
                //canvas.drawLine(lastX, lastY, x, y3, mPaint3);
                canvas.drawLine(lastX, lastY, x, y4, mPaint4);
            }
            lastX = x;
            lastY = y;
        }
        brightness += colorDelta;
    }


    /*
     * Rescale the audio data so that the maximum amplitude is less than or equal to input maxAmp
     */
    private void rescaleAudioData(LinkedList<short[]> audioData, final float maxAmp) {
        float newMaxAmp = maxAmp;
        Iterator<short[]> it = audioData.iterator();
        while (it.hasNext()) {
            short buf[] = it.next();
            for (short s : buf) {
                float as = (float)Math.abs(s);
                if (as > newMaxAmp) {
                    newMaxAmp = as;
                }
            }
        }
        if (newMaxAmp > maxAmp) {
            it = audioData.iterator();
            while (it.hasNext()) {
                short buf[] = it.next();
                for (int i=0; i<buf.length; i++) {
                    buf[i] = (short)((maxAmp/newMaxAmp)*buf[i]);
                }
            }
        }
    }
}
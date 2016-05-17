package xu.fyp.project;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

public class WaveformAudio extends SurfaceView {

    // The number of buffer frames to keep around (for a nice fade-out visualization).

    // To make quieter sounds still show up well on the display, we use +/- 8192
    // as the amplitude that reaches the top/bottom of the view instead of +/- 32767. Any samples
    // that have magnitude higher than this limit will simply be clipped during drawing.

    private final float MAX_AMP_TO_DRAW = 500.0f;

    // The queue that will hold historical audio data.
    private static LinkedList<short[]> mAudioData;

    private final Paint mPaint;

    public WaveformAudio(Context context) {
        this(context, null, 0);
    }

    public WaveformAudio(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveformAudio(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mAudioData = new LinkedList<short[]>();
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(0);
        mPaint.setAntiAlias(true);
    }

    /**
     * Updates the waveform view with a new "frame" of samples and renders it.
     * The new frame gets added to the front of the rendering queue, pushing the
     * previous frames back, causing them to be faded out visually.
     *
     * @param buffer
     *            the most recent buffer of audio samples
     */
    public synchronized void printAudioData(short[] buffer) {

        mAudioData = new LinkedList<short[]>();

        double[] newBuffer;

        // We want to keep a small amount of history in the view to provide a nice fading effect.
        // We use a linked list that we treat as a queue for this.
        //newBuffer = buffer.clone();

        if(mAudioData.size() !=0){
            mAudioData.removeFirst();
        }
        mAudioData.addLast(buffer);

        // Update the display.

        Canvas canvas = getHolder().lockCanvas();

        if (canvas != null) {
            //Log.e("ss","!=null");
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
        canvas.drawColor(Color.BLACK);
        //Log.d("Test", "before draw0");
        float width = getWidth();
        float height = getHeight();
        float centerY = height / 2;

        // We draw the history from oldest to newest so that the older audio data is further back
        // and darker than the most recent data.
        int colorDelta = 255;
        int brightness = colorDelta;

        mPaint.setStrokeWidth(1);
        //Log.d("Test", "before draw0");
        // Rescale the amplitude of buffer so that it max is <= MAX_AMP_TO_DRAW
        float MAX_AMP_TO_DRAW =rescaleAudioData(mAudioData);

        for (short[] buffer : mAudioData) {
            mPaint.setColor(Color.argb(brightness, 255, 255, 255));
            float lastX = -1;
            //float lastY = -1;

            // For efficiency, we don't draw all of the samples in the buffer, but only the ones
            // that align with pixel boundaries. To further reduce computation overhead, only draw
            // once every 4 pixels. This solves the jittering problem in Samsung G4 where the minBufSize is
            // very small (40ms)
            int xinc = 1;

            for (int x = 0; x < width; x+=xinc) {


                int index = (int) ((x/width)*buffer.length);
                //Log.d("value", String.valueOf(index));
                double sample = buffer[index];
                //Log.d("value",String.valueOf(sample));
                //Log.d("Test", "getSample");

                float y = (float) ((sample / MAX_AMP_TO_DRAW) * centerY + centerY);


                if (lastX != -1) {

                    canvas.drawLine(x, centerY, x, y, mPaint);
                }
                lastX = x;
                //lastY = y;
            }
            //canvas.drawLine(0,0, width, height,mPaint);

            //brightness += colorDelta;
        }
    }

    /*
     * Rescale the audio data so that the maximum amplitude is less than or equal to input maxAmp
     */
    private float rescaleAudioData(LinkedList<short[]> audioData) {
        float newMaxAmp=0;
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
       /* if (newMaxAmp > maxAmp) {

            /*it = audioData.iterator();
            while (it.hasNext()) {
                short buf[] = it.next();
                for (int i=0; i<buf.length; i++) {
                    buf[i] = (short)((maxAmp/newMaxAmp)*buf[i]);
                }
            }
        }*/
        return newMaxAmp;
    }
}
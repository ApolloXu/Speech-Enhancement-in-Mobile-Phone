package xu.fyp.project;

import android.util.Log;

import java.io.FileOutputStream;


/**
 * Created by Apollo on 5/1/16.
 */
public class SpectralSubtraction {

    private final static double framePeriod = 0.064;
    private static int frameSize;
    private final static double alphaMax = 4.0;
    private final static double alphaMin = 1.0;
    private final static double betaMax = 0.1;
    private final static double betaMin = 0.04;
    private static int frameAdv;
    private final static int po = 1;
    private static FFT FFT;



    protected static double[] signal;
    private static short[] result;
    private static double[] specNoise;
    private static double[] timeY, specY, phaseY;
    private static Complex[] freqY, freqX;
    private static double[] timeX, specX;
    private static double noiseEnergy =0;
    private static DoubleFFT_1D FFT1D;

    public SpectralSubtraction(short[] buffer, int frameSize){
        this.frameSize = frameSize;
        frameAdv = frameSize/2;
        signal = new double[buffer.length];
        for(int i=0;i<buffer.length;i++){
            signal[i]=(double) buffer[i];
            signal[i]= signal[i]/32768.0;
            //Log.e("ss",String.valueOf(signal[i]));
        }
        result = new short[signal.length];
        //Log.e("ss",String.valueOf(signal.length));
        noiseAverage(signal);

    }

    public void setSignal(short[] input){
        this.signal = new double[input.length];
        for(int i=0; i<input.length;i++){
            this.signal[i] = (double)input[i];
            this.signal[i] = this.signal[i]/32768.0;
        }
        this.result = new short[input.length];
    }



    public double noiseAverage (double[] recordNoise){
        double[] noise = new double[frameSize];
        int start,end;
        specNoise = new double[frameSize];
        Complex[] complex = new Complex[frameSize];
        Complex[] complexes = new Complex[frameSize];
        noiseEnergy = 0;
        /*for(int i=0;i<500;i++) {
            Log.e("ss", String.valueOf(signal[i]));
        }*/

        for(int i =0; i<20;i++){
            start = i*frameSize/4;
            end = start + frameSize;
            for(int j= start; j<end; j++){
                noise[j-start]= recordNoise[j];              //noise[] equal to a frame of signal;
            }
            // create a new fft object
            FFT1D = new DoubleFFT_1D(frameSize);
            double[] fft = new double[frameSize*2];
            System.arraycopy(noise,0,fft,0,noise.length);

            // do the real forward fft
            FFT1D.realForwardFull(fft);

            //record the spectrum of noise signal
            for( int b=0; b<frameSize;b++){
                specNoise[b]= specNoise[b]+Math.hypot(fft[b*2], fft[b*2+1]);
            }
        }
        //normalize the spectrum of noise signal and compute the average noise
        for(int i=0;i<frameSize;i++){
            specNoise[i] = specNoise[i]/20.0;
            noiseEnergy = noiseEnergy+specNoise[i];
        }
        return noiseEnergy;
    }

    public short[] noiseSubtraction(){
        //Log.e("ss","noiseSubtraction");
        int numFrames = signal.length/frameAdv;
        //Log.e("sss",String.valueOf(numFrames));
        int start, end;
        Complex[] complex = new Complex[frameSize];
        freqY = new Complex[frameSize];
        double snr,sumY=0;
        double beta,alpha;
        int pointer =0;
        double noise = noiseEnergy;
        specY = new double[frameSize];
        timeY = new double[frameSize];
        phaseY = new double[frameSize];
        specX = new double[frameSize];
        freqX = new Complex[frameSize];

        double test = 0;
        double test1 = 0;
        for(int i=0;i<numFrames;i++){

            start = i*frameAdv;
            end = start+frameSize-1;
            sumY =0;
            // break if length is less than frameAdv
            if(end >= signal.length){
                break;
            }

            //initial a frame of data
            for(int j=0;j<frameSize;j++){
                timeY[j] = signal[start+j];
            }
           //apply hamming window
            double[] hamming = HammingWindow();
            timeY = applyHammingWindow(timeY, hamming);

           // create a new fft object and do real forward fft
            FFT1D = new DoubleFFT_1D(timeY.length);
            double[] fft = new double[timeY.length*2];
            System.arraycopy(timeY,0,fft,0,timeY.length);
            FFT1D.realForwardFull(fft);

            // get the real part and img part
            for(int h=0;h<frameSize;h++){
                specY[h] = Math.hypot(fft[h * 2], fft[h * 2 + 1]);
                phaseY[h] = Math.atan2(fft[h*2+1],fft[h*2]);
                sumY +=specY[h];
            }

            // calculate the SNR
            snr = sumY/noise;
            if(snr<1.0){
                beta = betaMin;
            }else{
                beta = betaMax;
            }
            alpha = -0.5*snr+2.5;
            if(alpha>alphaMax){
                alpha = alphaMax;
            }else if(alpha<alphaMin){
                alpha = alphaMin;
            }


            for(int b=0;b<frameSize;b++){
                if(specY[b]<=0){
                    specX[b] = 0;
                }else
                if(specY[b] > (alpha+beta)*specNoise[b]){
                    specX[b] = specY[b]-alpha*specNoise[b];
                }else{
                    specX[b] = beta*specNoise[b];
                }
            }

            FFT1D = new DoubleFFT_1D(freqY.length);
            double[] ifft = new double[frameSize*2];
            for(int c=0;c<frameSize;c++){
                ifft[c*2] = specX[c]*(Math.cos(phaseY[c]));
                ifft[c*2+1]=specX[c]*(Math.sin(phaseY[c]));
            }


            FFT1D.complexInverse(ifft, true);
            for(int d=0;d<frameSize;d++){
                specX[d] = ifft[d*2];
            }
         specX = removeHammingWindow(specX,hamming);

            for( int e=255;e<767; e++){
                double temp = specX[e]*32768.0;
                result[e+start] = (short)temp;
            }


        }
        //Log.e("ss","end of for loop");
        return result;
    }

    public short[] noiseSubtraction(double thisnoiseAverage){
        //Log.e("ss","noiseSubtraction");
        int numFrames = signal.length/frameAdv;
        int start, end;
        Complex[] complex = new Complex[frameSize];
        freqY = new Complex[frameSize];
        double snr,sumY=0;
        double beta,alpha;
        int pointer =0;
        double noise = thisnoiseAverage;
        specY = new double[frameSize];
        timeY = new double[frameSize];
        phaseY = new double[frameSize];
        specX = new double[frameSize];
        freqX = new Complex[frameSize];

        double test = 0;
        double test1 = 0;
        for(int i=0;i<numFrames;i++){
            //Log.e("ss",String.valueOf(numFrames));
            start = i*frameAdv;
            end = start+frameSize-1;
            sumY =0;
            //Log.e("ss",String.valueOf(end));
            //Log.e("ss",String.valueOf(numFrames));
            if(end >= signal.length){
                //Log.e("ss","noiseSubtraction after break");
                break;

            }
            //
            //initial timeY
            for(int j=0;j<frameSize;j++){
                //Log.e("ss",String.valueOf(signal[j]));
                timeY[j] = signal[start+j];
            }
            //Log.e("ss","get Hammingwindow");
            double[] hamming = HammingWindow();
            //Log.e("ss","get Hammingwindow");
            timeY = applyHammingWindow(timeY, hamming);
            //Log.e("ss","apply Hammingwindow");
            FFT1D = new DoubleFFT_1D(timeY.length);
            double[] fft = new double[timeY.length*2];
            System.arraycopy(timeY,0,fft,0,timeY.length);
            FFT1D.realForwardFull(fft);
            for(int h=0;h<frameSize;h++){
                specY[h] = Math.hypot(fft[h*2],fft[h*2+1]);
                phaseY[h] = Math.atan2(fft[h * 2 + 1], fft[h * 2]);
                phaseY[h] = (phaseY[h] > 0.0 ? phaseY[h] : phaseY[h] +Math.PI*2);
                sumY +=specY[h];
                //test = test+phaseY[h];
            }
            //Log.e("ss","noiseSubtraction get yfft");
            /*freqY = FFT.fft(complex);

            for(int a=0;a<frameSize;a++){

                specY[a] = freqY[a].abs();
                phaseY[a] = freqY[a].phase();
                sumY = sumY +specY[a];
                //test1 = test1+phaseY[a];
            }*/
            snr = sumY/noise;
            //Log.e("ss",String.valueOf(sumY));
            test1 = 0;
            //Log.e("ss", String.valueOf(snr));
            if(snr<1.0){
                beta = betaMin;
            }else{
                beta = betaMax;
            }
            alpha = -0.5*snr+2.5;
            if(alpha>alphaMax){
                alpha = alphaMax;
            }else if(alpha<alphaMin){
                alpha = alphaMin;
            }
            //Log.e("ss","get alpha");

            for(int b=0;b<frameSize;b++){
                if(specY[b]<=0){
                    specX[b] = 0;
                }else
                if(specY[b] > (alpha+beta)*specNoise[b]){
                    specX[b] = specY[b]-alpha*specNoise[b];
                }else{
                    specX[b] = beta*specNoise[b];
                }


                //
            }
            //Log.e("ss",String.valueOf(test));
            test = 0;
            FFT1D = new DoubleFFT_1D(freqY.length);
            double[] ifft = new double[frameSize*2];
            for(int c=0;c<frameSize;c++){
                //freqX[c] = new Complex((specX[c]*Math.cos(phaseY[c])),(specX[c]*Math.sin(phaseY[c])));
                ifft[c*2] = specX[c]*(Math.cos(phaseY[c]));
                ifft[c*2+1]=specX[c]*(Math.sin(phaseY[c]));
                //test1 = test1+ifft[c*2];
            }

            //Log.e("ss",String.valueOf(test));
            //Log.e("ss",String.valueOf(test1));
            //test = 0;
            //test1 =0;
            //Log.e("ss","get alpha1");
            /*FFT1D = new DoubleFFT_1D(freqY.length);
            //Log.e("ss","get alpha2");
            double[] ifft = new double[freqY.length*2];
            //Log.e("ss","get alpha3");
            for(int a=0;a<frameSize;a++){
                ifft[a*2]= freqX[a].re();
                ifft[a*2+1]= freqX[a].im();
            }*/
            //Log.e("ss","get alpha");
            FFT1D.complexInverse(ifft, true);
                //freqX = FFT.ifft(freqX);

            for(int d=0;d<frameSize;d++){
                specX[d] = ifft[d*2];

            }
            //Log.e("ss",String.valueOf(test));

            specX = removeHammingWindow(specX,hamming);
            //Log.e("ss","removehamming");
            for( int e=frameAdv/2-1;e<frameAdv*3/2-1; e++){
                double temp = specX[e]*32768.0;
                //Log.e("ss",String.valueOf(specX[e]));
                result[e+start] = (short)temp;
            }
            //pointer+=frameAdv;
            //Log.e("ss","pointer");


        }
        //Log.e("ss","end of for loop");
        return result;
    }





    public static double[] HammingWindow(){
        double [] buffer = new double[frameSize];
        for(int i=0;i<frameSize;i++){
            buffer[i] = 0.54-0.46*(Math.cos(2.0*Math.PI*i/(frameSize-1)));
            //Log.e("ss",String.valueOf(buffer[i]));
        }
        return buffer;
    }

    public double[] applyHammingWindow(double[] buffer, double[] Hamming){
        for(int i=0;i<frameSize;i++){
            buffer[i]*=Hamming[i];
        }
        return buffer;
    }

    public double[] removeHammingWindow(double[] buffer, double[] Hamming){
        double tester=0;
        for(int i=0; i<frameSize;i++){
            buffer[i]/=Hamming[i];
             tester += buffer[i];
        }
        //Log.e("ss",String.valueOf(tester));
        return buffer;
    }
}

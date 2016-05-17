package xu.fyp.project;


public class webrtcNS {
    static{
        System.loadLibrary("webrtc_ns");
    }
    public native int initiate(int sample_rate);
    public native short[] process(int handle ,short[] in_data );
    public native int free(int handle);
}


package com.xsooy.icc;

public class IccUtils {

    static {
        System.loadLibrary("icc");
    }

    public native int loadProfile(String path);

    public native int loadProfile2(String path,String path2);

    public native int apply(int color);

    public native void applyCmyk(float[] in,float[] out);
}

package com.xsooy.icc;

public class IccUtils {

    static {
        System.loadLibrary("icc");
    }

    public static String iccProfileDir = "/";

    public native int loadProfile(String path);

    public native int loadProfileByData(byte[] data);

//    public native int loadProfile2(String path,String path2);

    public native float apply(float color);

    //gray to xyz
    public native void applyGray(float[] in,float[] out);

    //cmyk to lab
    public native void applyCmyk(float[] in,float[] out);
}

package com.xsooy.icc;

public class JpegUtils {
    static {
        System.loadLibrary("jpegUse");
    }

    public native byte[] converData(byte[] data);

}

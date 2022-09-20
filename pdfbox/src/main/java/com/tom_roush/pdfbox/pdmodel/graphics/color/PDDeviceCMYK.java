package com.tom_roush.pdfbox.pdmodel.graphics.color;

import android.graphics.Bitmap;
import android.opengl.Matrix;
import android.util.Log;

import com.tom_roush.pdfbox.cos.COSName;
import com.xsooy.icc.IccUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class PDDeviceCMYK extends PDDeviceColorSpace
{

    private IccUtils iccUtils;
    /**  The single instance of this class. */
    public static PDDeviceCMYK INSTANCE;
    static
    {
        INSTANCE = new PDDeviceCMYK();
    }

    private final PDColor initialColor = new PDColor(new float[] { 0, 0, 0, 1 }, this);
//    private ICC_ColorSpace awtColorSpace;
    private volatile boolean initDone = false;
    private boolean usePureJavaCMYKConversion = false;

    protected PDDeviceCMYK()
    {
    }

    /**
     * Lazy load the ICC profile, because it's slow.
     */
    protected void init() throws IOException
    {
        // no need to synchronize this check as it is atomic
        if (initDone)
        {
            return;
        }
        synchronized (this)
        {
            // we might have been waiting for another thread, so check again
            if (initDone)
            {
                return;
            }
            iccUtils = new IccUtils();
            iccUtils.loadProfile("/storage/emulated/0/Android/data/com.example.test/files/ISOcoated_v2_300_bas.icc");
            // loads the ICC color profile for CMYK
//            ICC_Profile iccProfile = getICCProfile();
//            if (iccProfile == null)
//            {
//                throw new IOException("Default CMYK color profile could not be loaded");
//            }
//            awtColorSpace = new ICC_ColorSpace(iccProfile);

            // there is a JVM bug which results in a CMMException which appears to be a race
            // condition caused by lazy initialization of the color transform, so we perform
            // an initial color conversion while we're still in a static context, see PDFBOX-2184
//            awtColorSpace.toRGB(new float[] { 0, 0, 0, 0 });
//            usePureJavaCMYKConversion = System
//                    .getProperty("org.apache.pdfbox.rendering.UsePureJavaCMYKConversion") != null;

            // Assignment to volatile must be the LAST statement in this block!
            initDone = true;
        }
    }

//    protected ICC_Profile getICCProfile() throws IOException
//    {
//        // Adobe Acrobat uses "U.S. Web Coated (SWOP) v2" as the default
//        // CMYK profile, however it is not available under an open license.
//        // Instead, the "ISO Coated v2 300% (basICColor)" is used, which
//        // is an open alternative to the "ISO Coated v2 300% (ECI)" profile.
//
//        String resourceName = "/org/apache/pdfbox/resources/icc/ISOcoated_v2_300_bas.icc";
//        InputStream resourceAsStream = PDDeviceCMYK.class.getResourceAsStream(resourceName);
//        if (resourceAsStream == null)
//        {
//            throw new IOException("resource '" + resourceName + "' not found");
//        }
//        try (InputStream is = new BufferedInputStream(resourceAsStream))
//        {
//            return ICC_Profile.getInstance(is);
//        }
//    }

    @Override
    public String getName()
    {
        return COSName.DEVICECMYK.getName();
    }

    @Override
    public int getNumberOfComponents()
    {
        return 4;
    }

    @Override
    public float[] getDefaultDecode(int bitsPerComponent)
    {
        return new float[] { 0, 1, 0, 1, 0, 1, 0, 1 };
    }

    @Override
    public PDColor getInitialColor()
    {
        return initialColor;
    }

    @Override
    public float[] toRGB(float[] value) throws IOException
    {
        init();
        //cmyk to lab
        float[] data = new float[3];
        iccUtils.applyCmyk(value,data);
        float[] lab = toLab(data);
        float[] xyz = labToXyz(lab);
        float[] rgb = new float[3];
        Log.w("ceshi","l:"+lab[0]+",a:"+lab[1]+",b:"+lab[2]);
        Log.w("ceshi","x:"+xyz[0]+",y:"+xyz[1]+",z:"+xyz[2]);

        rgb[2] = (xyz[0] * 3.240479f) + (xyz[1] * -1.537150f) + (xyz[2] * -.498535f);
        rgb[1] = (xyz[0] * -.969256f) + (xyz[1] *  1.875992f) + (xyz[2] * .041556f);
        rgb[0] = (xyz[0] * .055648f) +  (xyz[1] * -.204043f) + (xyz[2] * 1.057311f);

        for (int i = 0; i < 3; i++)
        {
            if (rgb[i] > .0031308f)
            {
                rgb[i] = (1.055f * (float)Math.pow(rgb[i], (1.0f / 2.4f))) - .055f;
            }
            else
            {
                rgb[i] = rgb[i] * 12.92f;
            }
        }
        Log.w("ceshi","r:"+rgb[2]+",g:"+rgb[1]+"b:,"+rgb[0]);

//        rgb[0] = rgb[0] * 255.0f;
//        rgb[1] = rgb[1] * 255.0f;
//        rgb[2] = rgb[2] * 255.0f;

        return rgb;
//        return new float[]{1.f-Math.min(1.f,value[0]+value[3]),1.f-Math.min(1.f,value[1]+value[3]),1.f-Math.min(1.f,value[2]+value[3])};
//        return awtColorSpace.toRGB(value);
    }

//    @Override
    public float[] toRGB2(float[] value) {
        return new float[]{(1-value[2])*(1-value[3]),(1-value[1])*(1-value[3]),(1-value[0])*(1-value[3])};
    }

    private float adj(float c) {
        return c;
//        if (Math.abs(c)<0.0031308) {
//            return 12.92f*c;
//        }
//        return 1.055f*(float)Math.pow(c,0.41666)-0.055f;
    }

    @Override
    public Bitmap toRGBImage(Bitmap raster) throws IOException {
        return null;
    }

    public float[] toLab(float[] value) {
        float[] lab = new float[3];
        lab[0] = value[0] * 100;
        float min = -128.0f;
        float max = 127.0f;
        lab[1] = (max-min)*value[1]+min;
        lab[2] = (max-min)*value[2]+min;
        return lab;
    }

    private float[] labToXyz(float[] value) {
        float[] xyz = new float[3];
        xyz[1] = (value[0]+16.f)/116.f;
        xyz[0] = value[1]/500.f+xyz[1];
        xyz[2] = xyz[1]-value[2]/200.f;

        for (int i = 0; i < 3; i++)
        {
            float pow = xyz[i] * xyz[i] * xyz[i];
            float ratio = (6.0f / 29.0f);
            if (xyz[i] > ratio)
            {
                xyz[i] = pow;
            }
            else
            {
                xyz[i] = (3.0f * (6.0f / 29.0f) * (6.0f / 29.0f) * (xyz[i] - (4.0f / 29.0f)));
            }
        }

//        for (int i = 0; i < 3; i++)
//        {
//            float pow = xyz[i] * xyz[i] * xyz[i];
//            if (pow > .008856f)
//            {
//                xyz[i] = pow;
//            }
//            else
//            {
//                xyz[i] = (xyz[i]- 16.0f / 116.0f) / 7.787f;
//            }
//        }

        xyz[0] = xyz[0] * (95.047f);
        xyz[1] = xyz[1] * (100.0f);
        xyz[2] = xyz[2] * (108.883f);

        xyz[0] = xyz[0] / 100f;
        xyz[1] = xyz[1] / 100f;
        xyz[2] = xyz[2] / 100f;

        return xyz;
    }

//    @Override
//    public BufferedImage toRawImage(WritableRaster raster) throws IOException
//    {
//        // Device CMYK is not specified, as its the colors of whatever device you use.
//        // The user should fallback to the RGB image
//        return null;
//    }

//    @Override
//    public BufferedImage toRGBImage(WritableRaster raster) throws IOException
//    {
//        init();
//        return toRGBImageAWT(raster, awtColorSpace);
//    }

//    @Override
//    protected BufferedImage toRGBImageAWT(WritableRaster raster, ColorSpace colorSpace)
//    {
//        if (usePureJavaCMYKConversion)
//        {
//            BufferedImage dest = new BufferedImage(raster.getWidth(), raster.getHeight(),
//                    BufferedImage.TYPE_INT_RGB);
//            ColorSpace destCS = dest.getColorModel().getColorSpace();
//            WritableRaster destRaster = dest.getRaster();
//            float[] srcValues = new float[4];
//            float[] lastValues = new float[] { -1.0f, -1.0f, -1.0f, -1.0f };
//            float[] destValues = new float[3];
//            int startX = raster.getMinX();
//            int startY = raster.getMinY();
//            int endX = raster.getWidth() + startX;
//            int endY = raster.getHeight() + startY;
//            for (int x = startX; x < endX; x++)
//            {
//                for (int y = startY; y < endY; y++)
//                {
//                    raster.getPixel(x, y, srcValues);
//                    // check if the last value can be reused
//                    if (!Arrays.equals(lastValues, srcValues))
//                    {
//                        lastValues[0] = srcValues[0];
//                        srcValues[0] = srcValues[0] / 255f;
//
//                        lastValues[1] = srcValues[1];
//                        srcValues[1] = srcValues[1] / 255f;
//
//                        lastValues[2] = srcValues[2];
//                        srcValues[2] = srcValues[2] / 255f;
//
//                        lastValues[3] = srcValues[3];
//                        srcValues[3] = srcValues[3] / 255f;
//
//                        // use CIEXYZ as intermediate format to optimize the color conversion
//                        destValues = destCS.fromCIEXYZ(colorSpace.toCIEXYZ(srcValues));
//                        for (int k = 0; k < destValues.length; k++)
//                        {
//                            destValues[k] = destValues[k] * 255f;
//                        }
//                    }
//                    destRaster.setPixel(x, y, destValues);
//                }
//            }
//            return dest;
//        }
//        else
//        {
//            return super.toRGBImageAWT(raster, colorSpace);
//        }
//    }
}

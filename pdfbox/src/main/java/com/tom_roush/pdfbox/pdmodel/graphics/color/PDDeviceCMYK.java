package com.tom_roush.pdfbox.pdmodel.graphics.color;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.Matrix;
import android.util.Log;

import com.tom_roush.pdfbox.cos.COSName;
import com.xsooy.icc.IccUtils;

import java.io.BufferedInputStream;
import java.io.File;
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
            if (new File(IccUtils.iccProfileDir+"/ISOcoated_v2_300_bas.icc").exists()) {
                iccUtils = new IccUtils();
                iccUtils.loadProfile(IccUtils.iccProfileDir+"/ISOcoated_v2_300_bas.icc");
            }
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
        if (iccUtils!=null) {
            float[] data = new float[3];
            iccUtils.applyCmyk(value,data);
            float[] lab = toLab(data);
            float[] xyz = labToXyz(lab);

            return NormalColorSpace.xyzToRgb(xyz);
        }
        return new float[]{(1-value[0])*(1-value[3]),(1-value[1])*(1-value[3]),(1-value[2])*(1-value[3])};
    }

    @Override
    public Bitmap toRGBImage(Bitmap raster) throws IOException {
        init();
        int width = raster.getWidth();
        int height = raster.getHeight();

        Bitmap rgbImage = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        int[] src = new int[width];
        int test = 0;
        for (int y = 0; y < height; y++)
        {
            raster.getPixels(src,0,width,0,y,width,1);
            for (int x = 0; x < width; x++)
            {
                test++;
                if (test<100) {
                    Log.w("color_test","src[x]:"+(src[x]&0xff));
                }
                float[] value = new float[] {(src[x]&0xff)/255.f,(src[x]>>8&0xff)/255.f,(src[x]>>16&0xff)/255.f,(src[x]>>24&0xff)/255.f};
                float[] rgb = toRGB(value);
                int color = Color.argb(255,(int)(rgb[0]*255),(int)(rgb[0]*255),(int)(rgb[0]*255));
                rgbImage.setPixel(x,y,color);
            }
        }
        return rgbImage;
    }

    public Bitmap toRGBImage(byte[] raster,int width,int height) throws IOException {
        init();
        Bitmap rgbImage = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        int location = 0;
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                float[] value = new float[4];
                for (int i=0;i<4;i++) {
                    value[i] = ((raster[location]&0xff)/255.f);
                    location++;
                }
                float[] rgb = toRGB(value);
                int color = Color.argb(255,(int)(rgb[0]*255),(int)(rgb[1]*255),(int)(rgb[2]*255));
                rgbImage.setPixel(x,y,color);
            }
        }
        return rgbImage;
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

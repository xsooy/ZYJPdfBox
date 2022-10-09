/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tom_roush.pdfbox.pdmodel.graphics.color;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.IOException;

import com.tom_roush.pdfbox.cos.COSArray;
import com.tom_roush.pdfbox.cos.COSBase;
import com.tom_roush.pdfbox.cos.COSDictionary;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.cos.COSObject;
import com.tom_roush.pdfbox.pdmodel.MissingResourceException;
import com.tom_roush.pdfbox.pdmodel.PDResources;
import com.tom_roush.pdfbox.pdmodel.ResourceCache;
import com.tom_roush.pdfbox.pdmodel.common.COSObjectable;

/**
 * A color space specifies how the colours of graphics objects will be painted on the page.
 *
 * @author John Hewson
 * @author Ben Litchfield
 */
public abstract class PDColorSpace implements COSObjectable
{

    public static final int TYPE_XYZ = 0;

    public static final int TYPE_Lab = 1;

    public static final int TYPE_RGB = 5;

    public static final int TYPE_GRAY = 6;

    public static final int TYPE_CMYK = 9;

    /**
     * Creates a color space given a name or array.
     * @param colorSpace the color space COS object
     * @return a new color space
     * @throws IOException if the color space is unknown or cannot be created
     */
    public static PDColorSpace create(COSBase colorSpace) throws IOException
    {
        return create(colorSpace, null);
    }

    /**
     * Creates a color space given a name or array. Abbreviated device color names are not supported
     * here, please replace them first.
     *
     * @param colorSpace the color space COS object
     * @param resources the current resources.
     * @return a new color space
     * @throws MissingResourceException if the color space is missing in the resources dictionary
     * @throws IOException if the color space is unknown or cannot be created
     */
    public static PDColorSpace create(COSBase colorSpace,
                                      PDResources resources)
            throws IOException
    {
        return create(colorSpace, resources, false);
    }

    /**
     * Creates a color space given a name or array. Abbreviated device color names are not supported
     * here, please replace them first. This method is for PDFBox internal use only, others should
     * use {@link #create(COSBase, PDResources)}.
     *
     * @param colorSpace the color space COS object
     * @param resources the current resources.
     * @param wasDefault if current color space was used by a default color space.
     * @return a new color space.
     * @throws MissingResourceException if the color space is missing in the resources dictionary
     * @throws IOException if the color space is unknown or cannot be created.
     */
    public static PDColorSpace create(COSBase colorSpace,
                                      PDResources resources,
                                      boolean wasDefault)
            throws IOException
    {
        if (colorSpace instanceof COSObject)
        {
            return createFromCOSObject((COSObject) colorSpace, resources);
        }
        else if (colorSpace instanceof COSName)
        {
            COSName name = (COSName)colorSpace;
//            Log.w("ceshi","name==="+name);
            // default color spaces
            if (resources != null)
            {
                COSName defaultName = null;
                if (name.equals(COSName.DEVICECMYK) &&
                        resources.hasColorSpace(COSName.DEFAULT_CMYK))
                {
                    defaultName = COSName.DEFAULT_CMYK;
                }
                else if (name.equals(COSName.DEVICERGB) &&
                        resources.hasColorSpace(COSName.DEFAULT_RGB))
                {
                    defaultName = COSName.DEFAULT_RGB;
                }
                else if (name.equals(COSName.DEVICEGRAY) &&
                        resources.hasColorSpace(COSName.DEFAULT_GRAY))
                {
                    defaultName = COSName.DEFAULT_GRAY;
                }

                if (resources.hasColorSpace(defaultName) && !wasDefault)
                {
                    return resources.getColorSpace(defaultName, true);
                }
            }

            // built-in color spaces
            if (name == COSName.DEVICECMYK)
            {
//                PDDeviceCMYK.INSTANCE.initDone = false;
                return PDDeviceCMYK.INSTANCE;
            }
            else if (name == COSName.DEVICERGB)
            {
                return PDDeviceRGB.INSTANCE;
            }
            else if (name == COSName.DEVICEGRAY)
            {
                return PDDeviceGray.INSTANCE;
            }
            else if (name == COSName.PATTERN)
            {
                return new PDPattern(resources);
            }
            else if (resources != null)
            {
                if (!resources.hasColorSpace(name))
                {
                    throw new MissingResourceException("Missing color space: " + name.getName());
                }
                return resources.getColorSpace(name);
            }
            else
            {
                throw new MissingResourceException("Unknown color space: " + name.getName());
            }
        }
        else if (colorSpace instanceof COSArray)
        {
            COSArray array = (COSArray)colorSpace;
            if (array.size() == 0)
            {
                throw new IOException("Colorspace array is empty");
            }
            COSBase base = array.getObject(0);
            if (!(base instanceof COSName))
            {
                throw new IOException("First element in colorspace array must be a name");
            }
            COSName name = (COSName) base;

            // TODO cache these returned color spaces?

            if (name == COSName.CALGRAY)
            {
                return new PDCalGray(array); //TODO: PdfBox-Android
            }
            else if (name == COSName.CALRGB)
            {
                return new PDCalRGB(array);
            }
            else if (name == COSName.DEVICEN)
            {
//                return new PDDeviceN(array);
                Log.e("PdfBox-Android", "Unsupported color space kind: " + name + ". Will try DeviceRGB instead");
                return PDDeviceRGB.INSTANCE;
            }
            else if (name == COSName.INDEXED)
            {
                return new PDIndexed(array);
            }
            else if (name == COSName.SEPARATION)
            {
                return new PDSeparation(array);
//                Log.e("PdfBox-Android", "Unsupported color space kind: " + name + ". Will try DeviceRGB instead");
//                return PDDeviceRGB.INSTANCE;
            }
            else if (name == COSName.ICCBASED)
            {
                return PDICCBased.create(array, resources);
            }
            else if (name == COSName.LAB)
            {
                return new PDLab(array);
//                Log.e("PdfBox-Android", "Unsupported color space kind: " + name + ". Will try DeviceRGB instead");
//                return PDDeviceRGB.INSTANCE;
            }
            else if (name == COSName.PATTERN)
            {
                if (array.size() == 1)
                {
                    return new PDPattern(resources);
                }
                else
                {
//                    return new PDPattern(resources, PDColorSpace.create(array.get(1)));
                }
                Log.e("PdfBox-Android", "Unsupported color space kind: " + name + ". Will try DeviceRGB instead");
                return PDDeviceRGB.INSTANCE;
            }
            else if (name == COSName.DEVICECMYK ||
                    name == COSName.DEVICERGB ||
                    name == COSName.DEVICEGRAY)
            {
                // not allowed in an array, but we sometimes encounter these regardless
                return create(name, resources, wasDefault);
            }
            else
            {
                throw new IOException("Invalid color space kind: " + name);
            }
        }
        else if (colorSpace instanceof COSDictionary &&
                ((COSDictionary) colorSpace).containsKey(COSName.COLORSPACE))
        {
            // PDFBOX-4833: dictionary with /ColorSpace entry
            COSBase base = ((COSDictionary) colorSpace).getDictionaryObject(COSName.COLORSPACE);
            if (base == colorSpace)
            {
                // PDFBOX-5315
                throw new IOException("Recursion in colorspace: " +
                        ((COSDictionary) colorSpace).getItem(COSName.COLORSPACE) + " points to itself");
            }
            return create(base, resources, wasDefault);
        }
        else
        {
            throw new IOException("Expected a name or array but got: " + colorSpace);
        }
    }

    private static PDColorSpace createFromCOSObject(COSObject colorSpace, PDResources resources)
            throws IOException
    {
        PDColorSpace cs;
        if (resources != null && resources.getResourceCache() != null)
        {
            ResourceCache resourceCache = resources.getResourceCache();
            cs = resourceCache.getColorSpace(colorSpace);
            if (cs != null)
            {
                return cs;
            }
        }
        cs = create(colorSpace.getObject(), resources);
        if (resources != null && resources.getResourceCache() != null && cs != null)
        {
            ResourceCache resourceCache = resources.getResourceCache();
            resourceCache.put(colorSpace, cs);
        }
        return cs;
    }

    // array for the given parameters
    protected COSArray array;

    /**
     * Returns the name of the color space.
     * @return the name of the color space
     */
    public abstract String getName();

    /**
     * Returns the number of components in this color space
     * @return the number of components in this color space
     */
    public abstract int getNumberOfComponents();

    /**
     * Returns the default decode array for this color space.
     * @param bitsPerComponent the number of bits per component.
     * @return the default decode array
     */
    public abstract float[] getDefaultDecode(int bitsPerComponent);

    /**
     * Returns the initial color value for this color space.
     * @return the initial color value for this color space
     */
    public abstract PDColor getInitialColor();

    /**
     * Returns the RGB equivalent of the given color value.
     * @param value a color value with component values between 0 and 1
     * @return an array of R,G,B value between 0 and 255
     * @throws IOException if the color conversion fails
     */
    public abstract float[] toRGB(float[] value) throws IOException;

    /**
     * Returns the (A)RGB equivalent of the given raster.
     * @param raster the source raster
     * @return an (A)RGB buffered image
     * @throws IOException if the color conversion fails
     */
    public abstract Bitmap toRGBImage(Bitmap raster) throws IOException;

//    public abstract BufferedImage toRawImage(WritableRaster raster) throws IOException; TODO: PdfBox-Android

//    protected final BufferedImage toRawImage(WritableRaster raster, ColorSpace awtColorSpace) TODO: PdfBox-Android

//    protected BufferedImage toRGBImageAWT(WritableRaster raster, ColorSpace colorSpace) TODO: PdfBox-Android

    @Override
    public COSBase getCOSObject()
    {
        return array;
    }


    public float getMinValue(int colorType,int index) {
        switch (colorType) {
            case TYPE_XYZ:
                return 0.0f; // X, Y, Z
//                result[1] = 1.0f + (32767.0f / 32768.0f);
//                break;
            case TYPE_Lab:
                switch (index) {
                    case 0:
                        return 0.0f;
                    case 1:
                    case 2:
                        return -128.0f;
                }
            default:
                return 0.0f;
        }
    }

    public float getMaxValue(int colorType,int index) {
        switch (colorType) {
            case TYPE_XYZ:
//                return 0.0f; // X, Y, Z
                return 1.0f + (32767.0f / 32768.0f);
            case TYPE_Lab:
                switch (index) {
                    case 0:
                        return 100.0f;
                    case 1:
                    case 2:
                        return 127.0f;
                }
            default:
                return 1.0f;
        }
    }

    //x,y,z To r=[0],g=[1],b=[2]
    public float[] xyzToRgb(float[] xyz) {
        float[] rgb = new float[3];
        rgb[0] = (xyz[0] * 3.240479f) + (xyz[1] * -1.537150f) + (xyz[2] * -.498535f);
        rgb[1] = (xyz[0] * -.969256f) + (xyz[1] *  1.875992f) + (xyz[2] * .041556f);
        rgb[2] = (xyz[0] * .055648f) +  (xyz[1] * -.204043f) + (xyz[2] * 1.057311f);

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
        return rgb;
    }

    public float[] toLab(float[] value) {
        float[] lab = new float[3];
        for (int i=0;i<3;i++) {
            lab[i] = (getMaxValue(TYPE_Lab,i)-getMinValue(TYPE_Lab,i))*value[i]+getMinValue(TYPE_Lab,i);
        }
//        lab[0] = value[0] * 100;
//        float min = -128.0f;
//        float max = 127.0f;
//        lab[1] = (max-min)*value[1]+min;
//        lab[2] = (max-min)*value[2]+min;
        return lab;
    }

    public float[] labToXyz(float[] value) {
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

        xyz[0] = xyz[0] * (95.047f);
        xyz[1] = xyz[1] * (100.0f);
        xyz[2] = xyz[2] * (108.883f);

        xyz[0] = xyz[0] / 100f;
        xyz[1] = xyz[1] / 100f;
        xyz[2] = xyz[2] / 100f;

        return xyz;
    }


}

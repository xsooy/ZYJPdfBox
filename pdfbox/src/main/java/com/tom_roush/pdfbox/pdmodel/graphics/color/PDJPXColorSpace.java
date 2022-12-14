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
import android.graphics.Canvas;
import android.graphics.ColorSpace;
import android.os.Build;

import com.tom_roush.pdfbox.cos.COSBase;

import java.io.IOException;

/**
 * A color space embedded in a JPX file.
 * This wraps the AWT ColorSpace which is obtained after JAI Image I/O reads a JPX stream.
 *
 * @author John Hewson
 */
public final class PDJPXColorSpace extends PDColorSpace
{
    private final ColorSpace colorSpace;

    /**
     * Creates a new JPX color space from the given AWT color space.
     *
     * @param colorSpace color space from a JPX image
     */
    public PDJPXColorSpace(ColorSpace colorSpace)
    {
        this.colorSpace = colorSpace;
    }

    @Override
    public String getName()
    {
        return "JPX";
    }

    @Override
    public int getNumberOfComponents()
    {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
        {
            return colorSpace.getComponentCount();
        }
        return 0;
    }

    @Override
    public float[] getDefaultDecode(int bitsPerComponent)
    {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
        {
            int n = getNumberOfComponents();
            float[] decode = new float[n * 2];
            for (int i = 0; i < n; i++)
            {
                decode[i * 2] = colorSpace.getMinValue(i);
                decode[i * 2 + 1] = colorSpace.getMaxValue(i);
            }
            return decode;
        }
        return new float[0];
    }

    @Override
    public PDColor getInitialColor()
    {
        throw new UnsupportedOperationException("JPX color spaces don't support drawing");
    }

    @Override
    public float[] toRGB(float[] value)
    {
        throw new UnsupportedOperationException("JPX color spaces don't support drawing");
    }

    @Override
    public Bitmap toRGBImage(Bitmap raster) throws IOException
    {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
        {
            Bitmap dest = Bitmap.createBitmap(raster.getWidth(), raster.getHeight(), Bitmap.Config.RGB_565, false, colorSpace);
            Canvas canvas = new Canvas(dest);
            canvas.drawBitmap(raster, 0, 0, null);
            return dest;
        }
        return raster;
    }

    @Override
    public COSBase getCOSObject()
    {
        throw new UnsupportedOperationException("JPX color spaces don't have COS objects");
    }
}

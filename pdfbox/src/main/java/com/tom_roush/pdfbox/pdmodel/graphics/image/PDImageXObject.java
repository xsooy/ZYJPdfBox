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
package com.tom_roush.pdfbox.pdmodel.graphics.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.List;

import com.tom_roush.pdfbox.cos.COSArray;
import com.tom_roush.pdfbox.cos.COSBase;
import com.tom_roush.pdfbox.cos.COSInputStream;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.cos.COSObject;
import com.tom_roush.pdfbox.cos.COSStream;
import com.tom_roush.pdfbox.filter.DecodeOptions;
import com.tom_roush.pdfbox.filter.DecodeResult;
import com.tom_roush.pdfbox.io.IOUtils;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDResources;
import com.tom_roush.pdfbox.pdmodel.common.PDMetadata;
import com.tom_roush.pdfbox.pdmodel.common.PDStream;
import com.tom_roush.pdfbox.pdmodel.graphics.PDXObject;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColorSpace;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import com.tom_roush.pdfbox.util.filetypedetector.FileType;
import com.tom_roush.pdfbox.util.filetypedetector.FileTypeDetector;

/**
 * An Image XObject.
 *
 * @author John Hewson
 * @author Ben Litchfield
 */
public final class PDImageXObject extends PDXObject implements PDImage
{
    private SoftReference<Bitmap> cachedImage;
    private PDColorSpace colorSpace;

    // initialize to MAX_VALUE as we prefer lower subsampling when keeping/replacing cache.
    private int cachedImageSubsampling = Integer.MAX_VALUE;

    /**
     * current resource dictionary (has color spaces)
     */
    private final PDResources resources;

    /**
     * Creates an Image XObject in the given document. This constructor is for internal PDFBox use
     * and is not for PDF generation. Users who want to create images should look at {@link #createFromFileByExtension(File, PDDocument)
     * }.
     *
     * @param document the current document
     * @throws java.io.IOException if there is an error creating the XObject.
     */
    public PDImageXObject(PDDocument document) throws IOException
    {
        this(new PDStream(document), null);
    }

    /**
     * Creates an Image XObject in the given document using the given filtered stream. This
     * constructor is for internal PDFBox use and is not for PDF generation. Users who want to
     * create images should look at {@link #createFromFileByExtension(File, PDDocument) }.
     *
     * @param document the current document
     * @param encodedStream an encoded stream of image data
     * @param cosFilter the filter or a COSArray of filters
     * @param width the image width
     * @param height the image height
     * @param bitsPerComponent the bits per component
     * @param initColorSpace the color space
     * @throws IOException if there is an error creating the XObject.
     */
    public PDImageXObject(PDDocument document, InputStream encodedStream,
        COSBase cosFilter, int width, int height, int bitsPerComponent,
        PDColorSpace initColorSpace) throws IOException
    {
        super(createRawStream(document, encodedStream), COSName.IMAGE);
        getCOSObject().setItem(COSName.FILTER, cosFilter);
        resources = null;
        colorSpace = null;
        setBitsPerComponent(bitsPerComponent);
        setWidth(width);
        setHeight(height);
        setColorSpace(initColorSpace);
    }

    /**
     * Creates an Image XObject with the given stream as its contents and current color spaces. This
     * constructor is for internal PDFBox use and is not for PDF generation. Users who want to
     * create images should look at {@link #createFromFileByExtension(File, PDDocument) }.
     *
     * @param stream the XObject stream to read
     * @param resources the current resources
     * @throws java.io.IOException if there is an error creating the XObject.
     */
    public PDImageXObject(PDStream stream, PDResources resources) throws IOException
    {
        super(stream, COSName.IMAGE);
        this.resources = resources;
        List<COSName> filters = stream.getFilters();
        if (filters != null && !filters.isEmpty() && COSName.JPX_DECODE.equals(filters.get(filters.size()-1)))
        {
            COSInputStream is = null;
            try
            {
                is = stream.createInputStream();
                DecodeResult decodeResult = is.getDecodeResult();
                stream.getCOSObject().addAll(decodeResult.getParameters());
//                this.colorSpace = decodeResult.getJPXColorSpace(); TODO: PdfBox-Android
            }
            finally
            {
                IOUtils.closeQuietly(is);
            }
        }
    }

    /**
     * Creates a thumbnail Image XObject from the given COSBase and name.
     * @param cosStream the COS stream
     * @return an XObject
     * @throws IOException if there is an error creating the XObject.
     */
    public static PDImageXObject createThumbnail(COSStream cosStream) throws IOException
    {
        // thumbnails are special, any non-null subtype is treated as being "Image"
        PDStream pdStream = new PDStream(cosStream);
        return new PDImageXObject(pdStream, null);
    }

    /**
     * Creates a COS stream from raw (encoded) data.
     */
    private static COSStream createRawStream(PDDocument document, InputStream rawInput)
        throws IOException
    {
        COSStream stream = document.getDocument().createCOSStream();
        OutputStream output = null;
        try
        {
            output = stream.createRawOutputStream();
            IOUtils.copy(rawInput, output);
        }
        finally
        {
            if (output != null)
            {
                output.close();
            }
        }
        return stream;
    }

    /**
     * Create a PDImageXObject from an image file, see {@link #createFromFileByExtension(File, PDDocument)} for
     * more details.
     *
     * @param imagePath the image file path.
     * @param doc the document that shall use this PDImageXObject.
     * @return a PDImageXObject.
     * @throws IOException if there is an error when reading the file or creating the
     * PDImageXObject, or if the image type is not supported.
     */
    public static PDImageXObject createFromFile(String imagePath, PDDocument doc) throws IOException
    {
        return createFromFileByExtension(new File(imagePath), doc);
    }

    /**
     * Create a PDImageXObject from an image file. The file format is determined by the file name
     * suffix. The following suffixes are supported: jpg, jpeg, tif, tiff, gif, bmp and png. This is
     * a convenience method that calls {@link JPEGFactory#createFromStream},
     * {@link CCITTFactory#createFromFile} or {@link BitmapFactory#decodeFile} combined with
     * {@link LosslessFactory#createFromImage}. (The later can also be used to create a
     * PDImageXObject from a Bitmap).
     *
     * @param file the image file.
     * @param doc the document that shall use this PDImageXObject.
     * @return a PDImageXObject.
     * @throws IOException if there is an error when reading the file or creating the
     * PDImageXObject.
     * @throws IllegalArgumentException if the image type is not supported.
     */
    public static PDImageXObject createFromFileByExtension(File file, PDDocument doc) throws IOException
    {
        String name = file.getName();
        int dot = file.getName().lastIndexOf('.');
        if (dot == -1)
        {
            throw new IllegalArgumentException("Image type not supported: " + name);
        }
        String ext = name.substring(dot + 1).toLowerCase();
        if ("jpg".equals(ext) || "jpeg".equals(ext))
        {
            FileInputStream fis = new FileInputStream(file);
            PDImageXObject imageXObject = JPEGFactory.createFromStream(doc, fis);
            fis.close();
            return imageXObject;
        }
        if ("tif".equals(ext) || "tiff".equals(ext))
        {
            return CCITTFactory.createFromFile(doc, file);
        }
        if ("gif".equals(ext) || "bmp".equals(ext) || "png".equals(ext))
        {
            Bitmap bim = BitmapFactory.decodeFile(file.getPath());
            return LosslessFactory.createFromImage(doc, bim);
        }
        throw new IllegalArgumentException("Image type not supported: " + name);
    }

    /**
     * Create a PDImageXObject from an image file. The file format is determined by the file
     * content. The following file types are supported: jpg, jpeg, tif, tiff, gif, bmp and png. This
     * is a convenience method that calls {@link JPEGFactory#createFromStream},
     * {@link CCITTFactory#createFromFile} or {@link BitmapFactory#decodeFile} combined with
     * {@link LosslessFactory#createFromImage}. (The later can also be used to create a
     * PDImageXObject from a Bitmap).
     *
     * @param file the image file.
     * @param doc the document that shall use this PDImageXObject.
     * @return a PDImageXObject.
     * @throws IOException if there is an error when reading the file or creating the
     * PDImageXObject.
     * @throws IllegalArgumentException if the image type is not supported.
     */
    public static PDImageXObject createFromFileByContent(File file, PDDocument doc) throws IOException
    {
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        FileType fileType = null;
        try
        {
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            fileType = FileTypeDetector.detectFileType(bufferedInputStream);
        }
        catch (IOException e)
        {
            throw new IOException("Could not determine file type: " + file.getName(), e);
        }
        finally
        {
            IOUtils.closeQuietly(fileInputStream);
            IOUtils.closeQuietly(bufferedInputStream);
        }
        if (fileType == null)
        {
            throw new IllegalArgumentException("Image type not supported: " + file.getName());
        }

        if (fileType.equals(FileType.JPEG))
        {
            FileInputStream fis = new FileInputStream(file);
            PDImageXObject imageXObject = JPEGFactory.createFromStream(doc, fis);
            fis.close();
            return imageXObject;
        }
        if (fileType.equals(FileType.TIFF))
        {
            try
            {
                return CCITTFactory.createFromFile(doc, file);
            }
            catch (IOException ex)
            {
                Log.d("PdfBox-Android", "Reading as TIFF failed, setting fileType to PNG", ex);
                // Plan B: try reading with ImageIO
                // common exception:
                // First image in tiff is not CCITT T4 or T6 compressed
                fileType = FileType.PNG;
            }
        }
        if (fileType.equals(FileType.BMP) || fileType.equals(FileType.GIF) || fileType.equals(FileType.PNG))
        {
            Bitmap bim = BitmapFactory.decodeFile(file.getPath());
            return LosslessFactory.createFromImage(doc, bim);
        }
        throw new IllegalArgumentException("Image type " + fileType + " not supported: " + file.getName());
    }

    /**
     * Create a PDImageXObject from bytes of an image file. The file format is determined by the
     * file content. The following file types are supported: jpg, jpeg, tif, tiff, gif, bmp and png.
     * This is a convenience method that calls {@link JPEGFactory#createFromByteArray},
     * {@link CCITTFactory#createFromFile} or {@link BitmapFactory#decodeFile} combined with
     * {@link LosslessFactory#createFromImage}. (The later can also be used to create a
     * PDImageXObject from a Bitmap).
     *
     * @param byteArray bytes from an image file.
     * @param document the document that shall use this PDImageXObject.
     * @param name name of image file for exception messages, can be null.
     * @return a PDImageXObject.
     * @throws IOException if there is an error when reading the file or creating the
     * PDImageXObject.
     * @throws IllegalArgumentException if the image type is not supported.
     */
    public static PDImageXObject createFromByteArray(PDDocument document, byte[] byteArray, String name) throws IOException
    {
        FileType fileType;
        try
        {
            fileType = FileTypeDetector.detectFileType(byteArray);
        }
        catch (IOException e)
        {
            throw new IOException("Could not determine file type: " + name, e);
        }
        if (fileType == null)
        {
            throw new IllegalArgumentException("Image type not supported: " + name);
        }

        if (fileType.equals(FileType.JPEG))
        {
            return JPEGFactory.createFromByteArray(document, byteArray);
        }
        if (fileType.equals(FileType.TIFF))
        {
            try
            {
                return CCITTFactory.createFromByteArray(document, byteArray);
            }
            catch (IOException ex)
            {
                Log.d("PdfBox-Android", "Reading as TIFF failed, setting fileType to PNG", ex);
                // Plan B: try reading with ImageIO
                // common exception:
                // First image in tiff is not CCITT T4 or T6 compressed
                fileType = FileType.PNG;
            }
        }
        if (fileType.equals(FileType.BMP) || fileType.equals(FileType.GIF) || fileType.equals(FileType.PNG))
        {
            ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
            Bitmap bim = BitmapFactory.decodeStream(bais);
            return LosslessFactory.createFromImage(document, bim);
        }
        throw new IllegalArgumentException("Image type " + fileType + " not supported: " + name);
    }

    /**
     * Returns the metadata associated with this XObject, or null if there is none.
     * @return the metadata associated with this object.
     */
    public PDMetadata getMetadata()
    {
        COSStream cosStream = (COSStream) getCOSObject().getDictionaryObject(COSName.METADATA);
        if (cosStream != null)
        {
            return new PDMetadata(cosStream);
        }
        return null;
    }

    /**
     * Sets the metadata associated with this XObject, or null if there is none.
     * @param meta the metadata associated with this object
     */
    public void setMetadata(PDMetadata meta)
    {
        getCOSObject().setItem(COSName.METADATA, meta);
    }

    /**
     * Returns the key of this XObject in the structural parent tree.
     *
     * @return this object's key the structural parent tree or -1 if there isn't any.
     */
    public int getStructParent()
    {
        return getCOSObject().getInt(COSName.STRUCT_PARENT);
    }

    /**
     * Sets the key of this XObject in the structural parent tree.
     * @param key the new key for this XObject
     */
    public void setStructParent(int key)
    {
        getCOSObject().setInt(COSName.STRUCT_PARENT, key);
    }

    /**
     * {@inheritDoc}
     * The returned images are cached via a SoftReference.
     */
    @Override
    public Bitmap getImage() throws IOException
    {
        return getImage(null, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bitmap getImage(Rect region, int subsampling) throws IOException
    {
        if (region == null && subsampling == cachedImageSubsampling && cachedImage != null)
        {
            Bitmap cached = cachedImage.get();
            if (cached != null)
            {
                return cached;
            }
        }

        // get image as RGB
        Bitmap image = SampledImageReader.getRGBImage(this, region, subsampling, getColorKeyMask());

        // soft mask (overrides explicit mask)
        PDImageXObject softMask = getSoftMask();

        if (softMask != null)
        {
//            return softMask.getImage();
            float[] matte = extractMatte(softMask);
            Log.w("ceshi","applyMask111");

            image = applyMask(image, softMask.getOpaqueImage(),softMask.getInterpolate(), true, matte);
        }
        else
        {
            // explicit mask - to be applied only if /ImageMask true
            PDImageXObject mask = getMask();
            if (mask != null && mask.isStencil())
            {
                Log.w("ceshi","applyMask222");
                image = applyMask(image, mask.getOpaqueImage(),mask.getInterpolate(), false, null);
            }
        }

        if (region == null && subsampling <= cachedImageSubsampling)
        {
            // only cache full-image renders, and prefer lower subsampling frequency, as lower
            // subsampling means higher quality and longer render times.
            cachedImageSubsampling = subsampling;
            cachedImage = new SoftReference<Bitmap>(image);
        }

        return image;
    }

    private float[] extractMatte(PDImageXObject softMask) throws IOException
    {
        COSBase base = softMask.getCOSObject().getItem(COSName.MATTE);
        float[] matte = null;
        if (base instanceof COSArray)
        {
            // PDFBOX-4267: process /Matte
            // see PDF specification 1.7, 11.6.5.3 Soft-Mask Images
            matte = ((COSArray) base).toFloatArray();
            // convert to RGB
            matte = getColorSpace().toRGB(matte);
        }
        return matte;
    }

    /**
     * {@inheritDoc}
     * The returned images are not cached.
     */
    @Override
    public Bitmap getStencilImage(Paint paint) throws IOException
    {
        if (!isStencil())
        {
            throw new IllegalStateException("Image is not a stencil");
        }
        return SampledImageReader.getStencilImage(this, paint);
    }

    /**
     * Returns an RGB buffered image containing the opaque image stream without any masks applied.
     * If this Image XObject is a mask then the buffered image will contain the raw mask.
     * @return the image without any masks applied
     * @throws IOException if the image cannot be read
     */
    public Bitmap getOpaqueImage() throws IOException
    {
        return SampledImageReader.getRGBImage(this, null);
    }

    // explicit mask: RGB + Binary -> ARGB
    // soft mask: RGB + Gray -> ARGB
//    private Bitmap applyMask(Bitmap image, Bitmap mask,
//        boolean isSoft, float[] matte)
//        throws IOException
//    {
//        if (mask == null)
//        {
//            return image;
//        }
//
//        int width = image.getWidth();
//        int height = image.getHeight();
//
//        // scale mask to fit image, or image to fit mask, whichever is larger
//        if (mask.getWidth() < width || mask.getHeight() < height)
//        {
//            mask = scaleImage(mask, width, height);
//        }
//        else if (mask.getWidth() > width || mask.getHeight() > height)
//        {
//            width = mask.getWidth();
//            height = mask.getHeight();
//            image = scaleImage(image, width, height);
//        }
//
//        // compose to ARGB
//        Bitmap masked = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//
//        int rgb;
//        int rgba;
//        int alphaPixel;
//        int alpha;
//        int test=0;
//
//        for (int y = 0; y < height; y++)
//        {
//            for (int x = 0; x < width; x++)
//            {
//                rgb = image.getPixel(x, y);
//
//                alphaPixel = mask.getPixel(x, y);
//
//                if (isSoft)
//                {
//                    alpha = Color.alpha(alphaPixel);
//                    if (matte != null && Float.compare(alphaPixel, 0) != 0)
//                    {
//                        rgb = Color.rgb(
//                            clampColor(((Color.red(rgb) / 255 - matte[0]) / (alphaPixel / 255) + matte[0]) * 255),
//                            clampColor(((Color.green(rgb) / 255 - matte[1]) / (alphaPixel / 255) + matte[1]) * 255),
//                            clampColor(((Color.blue(rgb) / 255 - matte[2]) / (alphaPixel / 255) + matte[2]) * 255)
//                        );
//                    }
//                }
//                else
//                {
//                    alpha = 255 - Color.alpha(alphaPixel);
//                }
//                if (alpha!=0) {
//                    test++;
//                }
//                rgba = Color.argb(alpha, Color.red(rgb), Color.green(rgb),
//                    Color.blue(rgb));
//                masked.setPixel(x, y, rgba);
//            }
//        }
//
////        Log.w("ceshi","masked==="+test);
//        return masked;
//    }

    private Bitmap applyMask(Bitmap image, Bitmap mask, boolean interpolateMask,
                             boolean isSoft, float[] matte)
    {
        if (mask == null)
        {
            return image;
        }

        final int width = Math.max(image.getWidth(), mask.getWidth());
        final int height = Math.max(image.getHeight(), mask.getHeight());

        // scale mask to fit image, or image to fit mask, whichever is larger.
        // also make sure that mask is 8 bit gray and image is ARGB as this
        // is what needs to be returned.
        if (mask.getWidth() < width || mask.getHeight() < height)
        {
            mask = scaleImage(mask, width, height, interpolateMask);
        }
        if (mask.getConfig() != Bitmap.Config.ALPHA_8 || !image.isMutable())
        {
            mask = mask.copy(Bitmap.Config.ALPHA_8, true);
        }

        if (image.getWidth() < width || image.getHeight() < height)
        {
            image = scaleImage(image, width, height, getInterpolate());
        }
        if (image.getConfig() != Bitmap.Config.ARGB_8888 || !image.isMutable())
        {
            image = image.copy(Bitmap.Config.ARGB_8888, true);
        }
        int[] pixels = new int[width];
        int[] maskPixels = new int[width];

        // compose alpha into ARGB image, either:
        // - very fast by direct bit combination if not a soft mask and a 8 bit alpha source.
        // - fast by letting the sample model do a bulk band operation if no matte is set.
        // - slow and complex by matte calculations on individual pixel components.
        if (!isSoft && image.getByteCount() == mask.getByteCount())
        {
            for (int y = 0; y < height; y++)
            {
                image.getPixels(pixels, 0, width, 0, y, width, 1);
                mask.getPixels(maskPixels, 0, width, 0, y, width, 1);
                for (int i = 0, c = width; c > 0; i++, c--)
                {
                    pixels[i] = pixels[i] & 0xffffff | ~maskPixels[i] & 0xff000000;
                }
                image.setPixels(pixels, 0, width, 0, y, width, 1);
            }
        }
        else if (matte == null)
        {
            for (int y = 0; y < height; y++)
            {
                image.getPixels(pixels, 0, width, 0, y, width, 1);
                mask.getPixels(maskPixels, 0, width, 0, y, width, 1);
                for (int x = 0; x < width; x++)
                {
                    if (!isSoft)
                    {
                        maskPixels[x] ^= -1;
                    }
                    pixels[x] = pixels[x] & 0xffffff | maskPixels[x] & 0xff000000;
                }
                image.setPixels(pixels, 0, width, 0, y, width, 1);
            }
        }
        else
        {
            // Original code is to clamp component and alpha to [0f, 1f] as matte is,
            // and later expand to [0; 255] again (with rounding).
            // component = 255f * ((component / 255f - matte) / (alpha / 255f) + matte)
            //           = (255 * component - 255 * 255f * matte) / alpha + 255f * matte
            // There is a clearly visible factor 255 for most components in above formula,
            // i.e. max value is 255 * 255: 16 bits + sign.
            // Let's use faster fixed point integer arithmetics with Q16.15,
            // introducing neglible errors (0.001%).
            // Note: For "correct" rounding we increase the final matte value (m0h, m1h, m2h) by
            // a half an integer.
            final int fraction = 15;
            final int factor = 255 << fraction;
            final int m0 = Math.round(factor * matte[0]) * 255;
            final int m1 = Math.round(factor * matte[1]) * 255;
            final int m2 = Math.round(factor * matte[2]) * 255;
            final int m0h = m0 / 255 + (1 << fraction - 1);
            final int m1h = m1 / 255 + (1 << fraction - 1);
            final int m2h = m2 / 255 + (1 << fraction - 1);
            for (int y = 0; y < height; y++)
            {
                image.getPixels(pixels, 0, width, 0, y, width, 1);
                mask.getPixels(maskPixels, 0, width, 0, y, width, 1);
                for (int x = 0; x < width; x++)
                {
                    int a = Color.alpha(maskPixels[x]);
                    if (a == 0)
                    {
                        pixels[x] = pixels[x] & 0xffffff;
                        continue;
                    }
                    int rgb = pixels[x];
                    int r = Color.red(rgb);
                    int g = Color.green(rgb);
                    int b = Color.blue(rgb);
                    r = clampColor(((r * factor - m0) / a + m0h) >> fraction);
                    g = clampColor(((g * factor - m1) / a + m1h) >> fraction);
                    b = clampColor(((b * factor - m2) / a + m2h) >> fraction);
                    pixels[x] = Color.argb(a, r, g, b);
                }
                image.setPixels(pixels, 0, width, 0, y, width, 1);
            }
        }
        return image;
    }

    private int clampColor(float color)
    {
        return Float.valueOf(color < 0 ? 0 : (color > 255 ? 255 : color)).intValue();
    }

    /**
     * High-quality image scaling.
     */
    private Bitmap scaleImage(Bitmap image, int width, int height, boolean interpolate)
    {
        return Bitmap.createScaledBitmap(image, width, height, !interpolate);
    }

    /**
     * Returns the Mask Image XObject associated with this image, or null if there is none.
     * @return Mask Image XObject
     * @throws java.io.IOException
     */
    public PDImageXObject getMask() throws IOException
    {
        COSBase mask = getCOSObject().getDictionaryObject(COSName.MASK);
        if (mask instanceof COSArray)
        {
            // color key mask, no explicit mask to return
            return null;
        }
        else
        {
            COSStream cosStream = (COSStream) getCOSObject().getDictionaryObject(COSName.MASK);
            if (cosStream != null)
            {
                // always DeviceGray
                return new PDImageXObject(new PDStream(cosStream), null);
            }
            return null;
        }
    }

    /**
     * Returns the color key mask array associated with this image, or null if there is none.
     * @return Mask Image XObject
     */
    public COSArray getColorKeyMask()
    {
        COSBase mask = getCOSObject().getDictionaryObject(COSName.MASK);
        if (mask instanceof COSArray)
        {
            return (COSArray)mask;
        }
        return null;
    }

    /**
     * Returns the Soft Mask Image XObject associated with this image, or null if there is none.
     * @return the SMask Image XObject, or null.
     * @throws java.io.IOException
     */
    public PDImageXObject getSoftMask() throws IOException
    {
        COSStream cosStream = (COSStream) getCOSObject().getDictionaryObject(COSName.SMASK);
        if (cosStream != null)
        {
            // always DeviceGray
            return new PDImageXObject(new PDStream(cosStream), null);
        }
        return null;
    }

    @Override
    public int getBitsPerComponent()
    {
        if (isStencil())
        {
            return 1;
        }
        else
        {
            return getCOSObject().getInt(COSName.BITS_PER_COMPONENT, COSName.BPC);
        }
    }

    @Override
    public void setBitsPerComponent(int bpc)
    {
        getCOSObject().setInt(COSName.BITS_PER_COMPONENT, bpc);
    }

    @Override
    public PDColorSpace getColorSpace() throws IOException
    {
        if (colorSpace == null)
        {
            COSBase cosBase = getCOSObject().getItem(COSName.COLORSPACE, COSName.CS);
            if (cosBase != null)
            {
                COSObject indirect = null;
                if (cosBase instanceof COSObject &&
                    resources != null && resources.getResourceCache() != null)
                {
                    // PDFBOX-4022: use the resource cache because several images
                    // might have the same colorspace indirect object.
                    indirect = (COSObject) cosBase;
                    colorSpace = resources.getResourceCache().getColorSpace(indirect);
                    if (colorSpace != null)
                    {
                        return colorSpace;
                    }
                }
                colorSpace = PDColorSpace.create(cosBase, resources);
                if (indirect != null)
                {
                    resources.getResourceCache().put(indirect, colorSpace);
                }
            }
            else if (isStencil())
            {
                // stencil mask color space must be gray, it is often missing
                return PDDeviceGray.INSTANCE;
            }
            else
            {
                // an image without a color space is always broken
                throw new IOException("could not determine color space");
            }
        }
        return colorSpace;
    }

    @Override
    public InputStream createInputStream() throws IOException
    {
        return getStream().createInputStream();
    }

    @Override
    public InputStream createInputStream(DecodeOptions options) throws IOException
    {
        return getStream().createInputStream(options);
    }

    @Override
    public InputStream createInputStream(List<String> stopFilters) throws IOException
    {
        return getStream().createInputStream(stopFilters);
    }

    @Override
    public boolean isEmpty()
    {
        return getStream().getCOSObject().getLength() == 0;
    }

    @Override
    public void setColorSpace(PDColorSpace cs)
    {
        getCOSObject().setItem(COSName.COLORSPACE, cs != null ? cs.getCOSObject() : null);
    }

    @Override
    public int getHeight()
    {
        return getCOSObject().getInt(COSName.HEIGHT);
    }

    @Override
    public void setHeight(int h)
    {
        getCOSObject().setInt(COSName.HEIGHT, h);
    }

    @Override
    public int getWidth()
    {
        return getCOSObject().getInt(COSName.WIDTH);
    }

    @Override
    public void setWidth(int w)
    {
        getCOSObject().setInt(COSName.WIDTH, w);
    }

    @Override
    public boolean getInterpolate()
    {
        return getCOSObject().getBoolean(COSName.INTERPOLATE, false);
    }

    @Override
    public void setInterpolate(boolean value)
    {
        getCOSObject().setBoolean(COSName.INTERPOLATE, value);
    }

    @Override
    public void setDecode(COSArray decode)
    {
        getCOSObject().setItem(COSName.DECODE, decode);
    }

    @Override
    public COSArray getDecode()
    {
        COSBase decode = getCOSObject().getDictionaryObject(COSName.DECODE);
        if (decode instanceof COSArray)
        {
            return (COSArray) decode;
        }
        return null;
    }

    @Override
    public boolean isStencil()
    {
        return getCOSObject().getBoolean(COSName.IMAGE_MASK, false);
    }

    @Override
    public void setStencil(boolean isStencil)
    {
        getCOSObject().setBoolean(COSName.IMAGE_MASK, isStencil);
    }

    /**
     * This will get the suffix for this image type, e.g. jpg/png.
     * @return The image suffix or null if not available.
     */
    @Override
    public String getSuffix()
    {
        List<COSName> filters = getStream().getFilters();

        if (filters == null)
        {
            return "png";
        }
        else if (filters.contains(COSName.DCT_DECODE))
        {
            return "jpg";
        }
        else if (filters.contains(COSName.JPX_DECODE))
        {
            return "jpx";
        }
        else if (filters.contains(COSName.CCITTFAX_DECODE))
        {
            return "tiff";
        }
        else if (filters.contains(COSName.FLATE_DECODE)
            || filters.contains(COSName.LZW_DECODE)
            || filters.contains(COSName.RUN_LENGTH_DECODE))
        {
            return "png";
        }
        else if (filters.contains(COSName.JBIG2_DECODE))
        {
            return "jb2";
        }
        else
        {
            Log.w("PdfBox-Android", "getSuffix() returns null, filters: " + filters);
            return null;
        }
    }
}

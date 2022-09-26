/*
 * Copyright (C) 2012 Lightbox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.test;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.View;

import com.blankj.utilcode.util.LogUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * BitmapUtils(图片编辑界面相关)
 *
 * @author taolin
 */
public class BitmapUtils {
    /**
     * Used to tag logs
     */
    @SuppressWarnings("unused")
    private static final String TAG = "BitmapUtils";

    public static final long MAX_SZIE = 1024 * 512;// 500KB

    public static Bitmap rotaingImageView(int angle, Bitmap bitmap) {
        Bitmap returnBm = null;
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bitmap;
        }
        if (bitmap != returnBm) {
            bitmap.recycle();
        }
        return returnBm;
    }


    public static int getOrientation(final String imagePath) {
        int rotate = 0;
        try {
            File imageFile = new File(imagePath);
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
    }

    public static Size getBitmapSize(String filePath) {
        Options options = new Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(filePath, options);

        return new Size(options.outWidth, options.outHeight);
    }

    public static Size getScaledSize(int originalWidth,
                                           int originalHeight, int numPixels) {
        float ratio = (float) originalWidth / originalHeight;

        int scaledHeight = (int) Math.sqrt((float) numPixels / ratio);
        int scaledWidth = (int) (ratio * Math.sqrt((float) numPixels
                / ratio));

        return new Size(scaledWidth, scaledHeight);
    }

//    public static class BitmapSize {
//        public int width;
//        public int height;
//
//        public BitmapSize(int width, int height) {
//            this.width = width;
//            this.height = height;
//        }
//    }

    public static byte[] bitmapTobytes(Bitmap bitmap) {
        ByteArrayOutputStream a = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, 30, a);
        return a.toByteArray();
    }

    public static byte[] bitmapTobytesNoCompress(Bitmap bitmap) {
        ByteArrayOutputStream a = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, 100, a);
        return a.toByteArray();
    }

    public static Bitmap genRotateBitmap(byte[] data) {
        Bitmap bMap = BitmapFactory.decodeByteArray(data, 0, data.length);
        // 自定义相机拍照需要旋转90预览支持竖屏
        Matrix matrix = new Matrix();// 矩阵
        matrix.reset();// 设置为单位矩阵
        matrix.postRotate(90);// 旋转90度
        Bitmap bMapRotate = Bitmap.createBitmap(bMap, 0, 0, bMap.getWidth(),
                bMap.getHeight(), matrix, true);
        bMap.recycle();
        bMap = null;
        System.gc();
        return bMapRotate;
    }

    public static Bitmap byteToBitmap(byte[] data) {
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    /**
     * 将view转为bitmap
     *
     * @param view
     * @return
     */
    public static Bitmap getBitmapFromView(View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(),
                view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            bgDrawable.draw(canvas);
        }
        else {
            canvas.drawColor(Color.WHITE);
        }
        view.draw(canvas);
        return returnedBitmap;
    }

    // 按大小缩放
    public static Bitmap getImageCompress(final String srcPath) {
        Options newOpts = new Options();
        // 开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);// 此时返回bm为空

        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        // 现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = 800f;// 这里设置高度为800f
        float ww = 480f;// 这里设置宽度为480f
        // 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;// be=1表示不缩放
        if (w > h && w > ww) {// 如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {// 如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0) {
            be = 1;
        }
        newOpts.inSampleSize = be;// 设置缩放比例
        // 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        return compressImage(bitmap);// 压缩好比例大小后再进行质量压缩
    }

    // 图片按比例大小压缩
    public static Bitmap compress(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(CompressFormat.JPEG, 100, baos);
        if (baos.toByteArray().length / 1024 > 1024) {// 判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
            baos.reset();// 重置baos即清空baos
            image.compress(CompressFormat.JPEG, 50, baos);// 这里压缩50%，把压缩后的数据存放到baos中
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        Options newOpts = new Options();
        // 开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        // 现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = 800f;// 这里设置高度为800f
        float ww = 480f;// 这里设置宽度为480f
        // 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;// be=1表示不缩放
        if (w > h && w > ww) {// 如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {// 如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0) {
            be = 1;
        }
        newOpts.inSampleSize = be;// 设置缩放比例
        // 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        isBm = new ByteArrayInputStream(baos.toByteArray());
        bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
        return compressImage(bitmap);// 压缩好比例大小后再进行质量压缩
    }

    // 图片质量压缩
    private static Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;

        while (baos.toByteArray().length / 1024 > 100) { // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();// 重置baos即清空baos
            image.compress(CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;// 每次都减少10
//			System.out.println("options--->" + options + "    "
//					+ (baos.toByteArray().length / 1024));
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
        return bitmap;
    }

    // 图片质量压缩
    public static Bitmap compressImage30(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(CompressFormat.JPEG, 30, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
        return bitmap;
    }

    public void printscreen_share(View v, Activity context) {
        View view1 = context.getWindow().getDecorView();
        Display display = context.getWindowManager().getDefaultDisplay();
        view1.layout(0, 0, display.getWidth(), display.getHeight());
        view1.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(view1.getDrawingCache());
    }

    // 图片转为文件
    public static boolean saveBitmap2file(Bitmap bmp, String filepath) {
        CompressFormat format = CompressFormat.PNG;
        int quality = 100;
        OutputStream stream = null;
        try {
            // 判断SDcard状态
            if (!Environment.MEDIA_MOUNTED.equals(Environment
                    .getExternalStorageState())) {
                // 错误提示
                return false;
            }

            // 检查SDcard空间
            File SDCardRoot = Environment.getExternalStorageDirectory();
            if (SDCardRoot.getFreeSpace() < 10000) {
                // 弹出对话框提示用户空间不够
                Log.e("Utils", "存储空间不够");
                return false;
            }

            // 在SDcard创建文件夹及文件
            File bitmapFile = new File(SDCardRoot.getPath() + filepath);
            bitmapFile.getParentFile().mkdirs();// 创建文件夹
            stream = new FileOutputStream(SDCardRoot.getPath() + filepath);// "/sdcard/"
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return bmp.compress(format, quality, stream);
    }

    /**
     * 截屏
     *
     * @param activity
     * @return
     */
    public static Bitmap getScreenViewBitmap(Activity activity) {
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        return bitmap;
    }

    /**
     * 一个 View的图像
     *
     * @param view
     * @return
     */
    public static Bitmap getViewBitmap(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        return bitmap;
    }

    /**
     * Resize a bitmap object to fit the passed width and height
     *
     * @param input
     *           The bitmap to be resized
     * @param destWidth
     *           Desired maximum width of the result bitmap
     * @param destHeight
     *           Desired maximum height of the result bitmap
     * @return A new resized bitmap
     * @throws OutOfMemoryError
     *            if the operation exceeds the available vm memory
     */
    public static Bitmap resizeBitmap(final Bitmap input, int destWidth, int destHeight, int rotation ) throws OutOfMemoryError {

        int dstWidth = destWidth;
        int dstHeight = destHeight;
        final int srcWidth = input.getWidth();
        final int srcHeight = input.getHeight();

        if ( rotation == 90 || rotation == 270 ) {
            dstWidth = destHeight;
            dstHeight = destWidth;
        }

        boolean needsResize = false;
        float p;
        if ( ( srcWidth > dstWidth ) || ( srcHeight > dstHeight ) ) {
            needsResize = true;
            if ( ( srcWidth > srcHeight ) && ( srcWidth > dstWidth ) ) {
                p = (float) dstWidth / (float) srcWidth;
                dstHeight = (int) ( srcHeight * p );
            } else {
                p = (float) dstHeight / (float) srcHeight;
                dstWidth = (int) ( srcWidth * p );
            }
        } else {
            dstWidth = srcWidth;
            dstHeight = srcHeight;
        }

        if ( needsResize || rotation != 0 ) {
            Bitmap output;

            if ( rotation == 0 ) {
                output = Bitmap.createScaledBitmap( input, dstWidth, dstHeight, true );
            } else {
                Matrix matrix = new Matrix();
                matrix.postScale( (float) dstWidth / srcWidth, (float) dstHeight / srcHeight );
                matrix.postRotate( rotation );
                output = Bitmap.createBitmap( input, 0, 0, srcWidth, srcHeight, matrix, true );
            }
            return output;
        } else {
            return input;
        }
    }


    /**
     * Resize a bitmap
     *
     * @param input
     * @param destWidth
     * @param destHeight
     * @return
     * @throws OutOfMemoryError
     */
    public static Bitmap resizeBitmap(final Bitmap input, int destWidth, int destHeight ) throws OutOfMemoryError {
        return resizeBitmap( input, destWidth, destHeight, 0 );
    }

//    public static Bitmap getSampledBitmap(String filePath, int reqWidth, int reqHeight) {
//        Options options = new Options();
//        options.inJustDecodeBounds = true;
//        BitmapFactory.decodeFile(filePath, options);
//        int inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
//        options.inSampleSize = inSampleSize;
//        options.inPreferredConfig = Bitmap.Config.RGB_565;
//        options.inJustDecodeBounds = false;
//        return BitmapFactory.decodeFile(filePath, options);
//    }

    public static Bitmap getSampledBitmap(String path,int reqWidth, int reqHeight) {
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);
        options.inSampleSize = calculateInSampleSize(options,reqWidth,reqHeight);
        Log.w("ceshi","options.inSampleSize:"+options.inSampleSize);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path,options);
    }

    public static Bitmap getSampledBitmapByRGB565(String path,int reqWidth, int reqHeight) {
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);
        options.inSampleSize = calculateInSampleSize(options,reqWidth,reqHeight);
        Log.w("ceshi","options.inSampleSize:"+options.inSampleSize);
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(path,options);
    }

    public static int calculateInSampleSize(Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * 保存Bitmap图片到指定文件
     *
     * @param bm
     */
    public static boolean saveBitmap(Bitmap bm, String filePath) {
        File f = new File(filePath);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(CompressFormat.PNG, 80, out);
            out.flush();
            out.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        // System.out.println("保存文件--->" + f.getAbsolutePath());
    }

    public static boolean saveImageToBitmapFile(String outputFilePath,String inputFilePath){
        FileInputStream fis = null;
        Bitmap bitmap = null;
        Log.w("ceshi","saveImageToBitmapFile_start");
        try {
            fis = new FileInputStream(inputFilePath);
            bitmap = BitmapFactory.decodeStream(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (bitmap != null) {
            int w = bitmap.getWidth(), h = bitmap.getHeight();
            // 输出logCat信息
            int[] pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            byte[] rgb = addBMP_RGB_888(pixels, w, h);
            byte[] header = addBMPImageHeader(rgb.length+54, 54);
            byte[] infos = addBMPImageInfosHeader(w, h, rgb.length, 24);


            byte[] buffer = new byte[54 + rgb.length];
            System.arraycopy(header, 0, buffer, 0, header.length);
            System.arraycopy(infos, 0, buffer, 14, infos.length);
            System.arraycopy(rgb, 0, buffer, 54, rgb.length);
            try {
                FileOutputStream fos = new FileOutputStream(outputFilePath);
                fos.write(buffer);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Log.w("ceshi","saveImageToBitmapFile_end");
            return true;
        }
        else{
            return false;
        }
    }
    //BMP文件头
    public static byte[] addBMPImageHeader(int size, int bmpOffset) {
        byte[] buffer = new byte[14];
        buffer[0] = 0x42;
        buffer[1] = 0x4D;
        buffer[2] = (byte) (size >> 0);
        buffer[3] = (byte) (size >> 8);
        buffer[4] = (byte) (size >> 16);
        buffer[5] = (byte) (size >> 24);
        buffer[6] = 0x00;
        buffer[7] = 0x00;
        buffer[8] = 0x00;
        buffer[9] = 0x00;
        buffer[10] = (byte) (bmpOffset >> 0);
        buffer[11] = (byte) (bmpOffset >> 8);
        buffer[12] = (byte) (bmpOffset >> 16);
        buffer[13] = (byte) (bmpOffset >> 24);
        return buffer;
    }


    //BMP文件信息头
    public static byte[] addBMPImageInfosHeader(int w, int h, int size, int bitCount) {
        byte[] buffer = new byte[40];
        buffer[0] = 0x28;
        buffer[1] = 0x00;
        buffer[2] = 0x00;
        buffer[3] = 0x00;
        buffer[4] = (byte) (w >> 0);
        buffer[5] = (byte) (w >> 8);
        buffer[6] = (byte) (w >> 16);
        buffer[7] = (byte) (w >> 24);
        buffer[8] = (byte) (h >> 0);
        buffer[9] = (byte) (h >> 8);
        buffer[10] = (byte) (h >> 16);
        buffer[11] = (byte) (h >> 24);
        buffer[12] = 0x01;
        buffer[13] = 0x00;
        buffer[14] = (byte) (bitCount >> 0);  //彩色32、24、灰度8，黑白1
        buffer[15] = (byte) (bitCount >> 8);
        buffer[16] = 0x00;
        buffer[17] = 0x00;
        buffer[18] = 0x00;
        buffer[19] = 0x00;
        buffer[20] = (byte)(size >> 0);
        buffer[21] = (byte)(size >> 8);
        buffer[22] = (byte)(size >> 16);
        buffer[23] = (byte)(size >> 24);
        buffer[24] = 0x46;//后面四个字节一起表示水平封分辨率，每米像素数，按600dpi写死，正常换算是：dpi/0.0254
        buffer[25] = 0x5c;
        buffer[26] = 0x00;
        buffer[27] = 0x00;
        buffer[28] = 0x46; //后面四个字节一起表示垂直封分辨率，每米像素数，按600dpi写死，正常换算：dpi/0.0254
        buffer[29] = 0x5c;
        buffer[30] = 0x00;
        buffer[31] = 0x00;
        buffer[32] = 0x00;
        buffer[33] = 0x00;
        buffer[34] = 0x00;
        buffer[35] = 0x00;
        buffer[36] = 0x00;
        buffer[37] = 0x00;
        buffer[38] = 0x00;
        buffer[39] = 0x00;
        return buffer;
    }

    //BMP调色板信息，灰度图需要写入，共256个色值，每个值4个字节（黑白bmp图也需要，内容不一样），跟在bmp文件头之后
    public static byte[] addBMPRGBQuadGray() {
        byte[] quad = new byte[256*4];
        int offset = 0;
        for (int i=0; i<256; i++) {
            quad[offset] = quad[offset+1] = quad[offset+2] = (byte)i;
            quad[offset+3] = 0; //保留值，必须为0
            offset += 4;
        }
        return quad;
    }

    public static byte[] addBMP_RGB_888(int[] b,int w, int h) {
        int len = b.length;
        System.out.println(b.length);
        byte[] buffer = new byte[w*h * 3];
        int offset=0;
        for (int i = len-1; i>=w; i-=w) {
//DIB文件格式最后一行为第一行，每行按从左到右顺序
            int end=i,start=i-w+1;
            for(int j=start;j<=end;j++){
                buffer[offset]=(byte)(b[j]>>0);
                buffer[offset+1]=(byte)(b[j]>>8);
                //原为buffer[offset+1]=(byte)(b[j]>>16); 会对原图造成色差
                buffer[offset+2]=(byte)(b[j]>>16);
                offset += 3;
            }
        }
        return buffer;
    }


    public static Bitmap getBitmapFromPath(String imgPath,Bitmap.Config bmpFormat) {
        Options options = new Options();
//        options.inJustDecodeBounds = true;
        options.inPreferredConfig = bmpFormat;
        return BitmapFactory.decodeFile(imgPath,options);
    }

    public static Size getBitmapSizeByExif(String imgPath) throws IOException {
        ExifInterface exifInterface = new ExifInterface(imgPath);
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath,options);
        switch (orientation) {
            case 6:
                return new Size(options.outHeight,options.outWidth);
            default:
                return new Size(options.outWidth,options.outHeight);
        }
    }

    public static Size getBitmapSizeByExif(String imgPath,int rotate) {
        int orientation = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(imgPath);
            orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath,options);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate+=90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate+=180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate+=270;
                break;
        }
        if (rotate%180!=0) {
            return new Size(options.outHeight,options.outWidth);
        } else {
            return new Size(options.outWidth,options.outHeight);
        }
    }

    public static Bitmap getBitmapFromPathByExif(String imgPath) {
        int orientation = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(imgPath);
            orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath,options);

        int rotate = 0;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate+=90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate+=180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate+=270;
                break;
        }

//        double outWidth = options.outWidth;
//        double outHeight = options.outHeight;
//        if (rotate%180!=0) {
//            outWidth = options.outHeight;
//            outHeight = options.outWidth;
//        }
        options.inJustDecodeBounds = false;

        Bitmap baseBitmap = BitmapFactory.decodeFile(imgPath,options);
        double outWidth = baseBitmap.getWidth();
        double outHeight = baseBitmap.getHeight();
        if (rotate%180!=0) {
            outWidth = baseBitmap.getHeight();
            outHeight = baseBitmap.getWidth();
        }

        Bitmap afterBitmap = Bitmap.createBitmap((int)(outWidth),
                (int)(outHeight),
                baseBitmap.getConfig());
        Canvas canvas = new Canvas(afterBitmap);
        Matrix matrix = new Matrix();
//        matrix.postScale(compressRatio,compressRatio);
//        Log.w("crop_ceshi","rotate=="+rotate);
        matrix.postRotate(rotate);
        switch (rotate) {
            case 90:
                matrix.postTranslate((int)(outWidth),0);
                break;
            case 180:
                matrix.postTranslate((int)(outWidth),(int)(outHeight));
                break;
            case 270:
                matrix.postTranslate(0,(int)(outHeight));
                break;
        }
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawBitmap(baseBitmap,matrix,paint);
        return afterBitmap;
    }

    public static Bitmap compressBitmapFormPath(String imgPath,int reqWidth,int reqHeight) throws IOException {
        return compressBitmapFormPathAndRotate(imgPath,reqWidth,reqHeight,0);
    }

    /**
     * 根据传入的 图片文件 以及 Bitmap的目标输出 宽度、高度、以及角度  压缩创建出最合适的Bitmap
     * @param imgPath
     * @param reqWidth
     * @param reqHeight
     * @param rotate
     * @return
     */
    public static Bitmap compressBitmapFormPathAndRotate(String imgPath,int reqWidth,int reqHeight,int rotate) {
        int orientation = 0;
        //从图片文件的Exif信息从获取其当前的方向
        try {
            ExifInterface exifInterface = new ExifInterface(imgPath);
            orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //配置只解析传入图片文件的原始 宽度和高度，输出到 Options里，不输出Bitmap对象（可以不分配内存读取图片的宽高）
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath,options);

        //更新 orientation变量，将角度更新至于 Exif信息读取出的一致
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate+=90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate+=180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate+=270;
                break;
        }

        //获取通过Options 获取到的图片原始 宽度和高度
        double outWidth = options.outWidth;
        double outHeight = options.outHeight;
        //如果当前 传入图片的角度不为 0 或 180度，则交换宽度和高度
        if (rotate%180!=0) {
            outWidth = options.outHeight;
            outHeight = options.outWidth;
        }
        //定义一个临时变量，表示图片的采样率
        int inSampleSize = 1;

        //计算 原始图片的 宽高比
        double ration = outWidth / outHeight;
        //计算 目标宽高图片 的宽高比
        double reqRatio = reqWidth / reqHeight;
        if (reqRatio > ration)
            while (outHeight/inSampleSize>reqHeight) inSampleSize *=2;
        else
            while (outWidth/inSampleSize>reqWidth) inSampleSize *=2;

        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        if (1 != inSampleSize)
//            return BitmapFactory.decodeFile(imgPath,options);
            options.inSampleSize = inSampleSize / 2;

        Bitmap baseBitmap = BitmapFactory.decodeFile(imgPath,options);

        if (rotate ==0) {
            return baseBitmap;
        }

        outWidth = baseBitmap.getWidth();
        outHeight = baseBitmap.getHeight();
        if (rotate%180!=0) {
            outWidth = baseBitmap.getHeight();
            outHeight = baseBitmap.getWidth();
        }
        float compressRatio = 1;
        if (reqRatio > ration)
            compressRatio = (float) (reqHeight * 1.0f / outHeight);
        else
            compressRatio = (float) (reqWidth * 1.0f / outWidth);

        Bitmap afterBitmap = Bitmap.createBitmap((int)(outWidth*compressRatio),
                (int)(outHeight*compressRatio),
                baseBitmap.getConfig());
        Canvas canvas = new Canvas(afterBitmap);
        Matrix matrix = new Matrix();
        matrix.postScale(compressRatio,compressRatio);
        Log.w("crop_ceshi","rotate=="+rotate);
        matrix.postRotate(rotate);
        switch (rotate) {
            case 90:
                matrix.postTranslate((int)(outWidth*compressRatio),0);
                break;
            case 180:
                matrix.postTranslate((int)(outWidth*compressRatio),(int)(outHeight*compressRatio));
                break;
            case 270:
                matrix.postTranslate(0,(int)(outHeight*compressRatio));
                break;
        }
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawBitmap(baseBitmap,matrix,paint);
        baseBitmap.recycle();
        return afterBitmap;
    }

    public static Bitmap rotationBitmap(Bitmap bitmap,int rotation) {
        Matrix matrix = new Matrix();
        matrix.setRotate(rotation);
        Log.w("crop_ceshi","width:"+bitmap.getWidth()+",height:"+bitmap.getHeight());
        return Bitmap.createBitmap(bitmap,0,0,
                bitmap.getWidth(), bitmap.getHeight(),matrix,true);
    }

    // 调整Bitmap方向，如横向／纵向
    public static Bitmap adjustBitmapRotation(Bitmap bm, final int orientationDegree) {

        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        float targetX, targetY;
        if (orientationDegree == 90) {
            targetX = bm.getHeight();
            targetY = 0;
        } else {
            targetX = bm.getHeight();
            targetY = bm.getWidth();
        }

        final float[] values = new float[9];
        m.getValues(values);

        float x1 = values[Matrix.MTRANS_X];
        float y1 = values[Matrix.MTRANS_Y];

        m.postTranslate(targetX - x1, targetY - y1);

        Bitmap bm1 = Bitmap.createBitmap(bm.getHeight(), bm.getWidth(), Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        Canvas canvas = new Canvas(bm1);
        canvas.drawBitmap(bm, m, paint);

        return bm1;
    }

//    public static PageSize getBitmapSzie(String imgPath) {
//        Options options = new Options();
//        options.inJustDecodeBounds = true;
//        options.inPreferredConfig = Bitmap.Config.RGB_565;
//        BitmapFactory.decodeFile(imgPath,options);
//        return new PageSize(options.outWidth,options.outHeight);
//    }

    public static Bitmap clipBitmap(Bitmap bitmap,int x,int y,int width,int height) {
        Bitmap newBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);// 消除龋齿
        canvas.drawBitmap(bitmap, new Rect(x, y, width, height), new Rect(0, 0, width, height), paint);
        return newBitmap;
    }

    public static void saveBmp2(Bitmap bitmap) {
        int nBmpWidth = bitmap.getWidth();
        int nBmpHeight = bitmap.getHeight();
        int bufferSize = nBmpHeight * (nBmpWidth*3 +nBmpHeight%4);
        try {
            String fileName = "/sdcard/test.bmp";
            File file = new File(fileName);
            if (!file.exists()) file.createNewFile();
            FileOutputStream fileos = new FileOutputStream(fileName);
            int bfType = 0x4d42;
            long bfSize = 14+40+bufferSize;
            int bfReserved1= 0;
            int bfReserved2 = 0;
            long bfOffBits = 14 + 40;
//            writeWord(fileos,bfType);

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    //裁剪多余部分
    public static Bitmap clipBitmap(Bitmap bitmap,int width,int height){
        Bitmap newBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(newBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
//        canvas.drawColor(Color.WHITE);
//        paint.setColor(Color.WHITE);

        canvas.drawBitmap(bitmap,-(bitmap.getWidth()-width)/2,-(bitmap.getHeight()-height)/2,paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
//        canvas.drawRect(0,0,width,height,paint);
        bitmap.recycle();
        return newBitmap;
    }

    /**
     * 将输入的bmp图按角度旋转，保存时保存成RGB24彩图或灰度图
     * @param bitmap bitmap图源
     * @param outputFilePath 输出路径
     * @param isToGray 是否转存成灰度图
     * @param rotate 旋转角度：0，90，180，270（-90）
     * @return 是否保存成功
     */
    public static boolean saveImageToBmpByRotate(Bitmap bitmap, String outputFilePath, boolean isToGray, int rotate){
        Log.w("ceshi","saveImageToBmpByRotate_start");
        if (bitmap != null) {

            //关于像素取值的一些变量
            //注释中的是Bitmap.Config.RGB_565格式需要的变量值--->不需要再将像素值换算为对应的RGB888值，bitmap.getPixel()返回的已经是算好的
            short RGB_MASK_RED = 0XFF;          //0x1F;
            short RGB_MASK_GREEN = 0xFF;        //0x3F;
            short RGB_MASK_BLUE = 0xFF;         //0x1F;
            int pixelOffsetRed = 16;            //11;
            int pixelOffsetGreen = 8;           //5;
            int fillOffsetRed = 0;              //3;
            int fillOffsetGreen = 0;            //2;
            int fillOffsetBlue = 0;             //3;

            LogUtils.file("图片大小："+bitmap.getWidth()+","+bitmap.getHeight());
            Log.w("ceshi","图片大小："+bitmap.getWidth()+","+bitmap.getHeight());
            int w = bitmap.getWidth(), h = bitmap.getHeight();
            // 输出logCat信息
//            PrintLogCat.printLogCat(CommonFunction.getClassNameAndMethodNameAndLineNumberInfo() + "bitmap w : "+w+" bitmap h : "+h);
//            int[] pixels = new int[w * h];
//            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
//            byte[] rgb = addBMP_RGB_888(pixels, w, h);

            int count = 0;
            int newWidth = w;
            int newHeight = h;
            if(rotate%180 != 0){
                newWidth = h;
                newHeight = w;
            }
            //默认保存成彩图，且每行字节数四字节对齐
            int bytesPerRow = (newWidth * 3 + 3)/4*4;
            int lineDiffBytes = bytesPerRow - newWidth*3;
            int length = bytesPerRow * newHeight;
            int  headerTotalLen = 54;
            int  bitCount = 24;
            if(isToGray){

                bytesPerRow = (newWidth + 3)/4*4;
                lineDiffBytes = bytesPerRow - newWidth;
                length = bytesPerRow * newHeight;
                //灰度图头部大小：文件头+调色板
                headerTotalLen = 54+ 1024;
                bitCount = 8;
            }
            byte[] header = addBMPImageHeader(length+headerTotalLen, headerTotalLen);
            byte[] infos = addBMPImageInfosHeader(newWidth, newHeight, length, bitCount);
            byte[] rgbQuad = addBMPRGBQuadGray();
            byte[] fillEmpty = new byte[lineDiffBytes];
            for (int m=0; m<lineDiffBytes; m++) {
                fillEmpty[m] = (byte)255;
            }

//            byte[] buffer = new byte[54 + rgb.length];
//            byte[] buffer = new byte[1024];
//            System.arraycopy(header, 0, buffer, 0, header.length);
//            System.arraycopy(infos, 0, buffer, 14, infos.length);
//            System.arraycopy(rgb, 0, buffer, 54, rgb.length);
            try {
                FileOutputStream fos = new FileOutputStream(outputFilePath);

                fos.write(header,0,header.length);
                fos.write(infos,0,infos.length);
                if(isToGray){
                    fos.write(rgbQuad, 0, rgbQuad.length);
                }
                //按行大小分配数组
                byte[] buffer = new byte[newWidth*bitCount/8];
                int offset = 0;
                int pixel = 0;

                switch (rotate) {
                    case 0:
                        int[] pixels0 = new int[w * 1];
                        for (int i= h-1;i>=0;i--) {
                            bitmap.getPixels(pixels0, 0, w, 0, i, w, 1);
                            for (int j=0;j<w;j++) {
                                pixel = pixels0[j];
                                byte pixelB = (byte)((pixel & RGB_MASK_BLUE) << fillOffsetBlue);
                                byte pixelG = (byte)(((pixel >> pixelOffsetGreen) & RGB_MASK_GREEN) << fillOffsetGreen);
                                byte pixelR = (byte)(((pixel >> pixelOffsetRed) & RGB_MASK_RED)<< fillOffsetRed);
                                if(isToGray){
                                    //RGB转成灰度---和底层使用的一致
                                    buffer[offset] = (byte) ((((int)pixelR & 0xff)*38 + ((int)pixelG & 0xff)*75 + ((int)pixelB & 0xff)*15) >> 7);
                                    offset++;
                                }else{
                                    buffer[offset] = pixelB;
                                    buffer[offset+1] = pixelG;
                                    buffer[offset+2] = pixelR;
                                    offset += 3;
                                }
                                if (offset==buffer.length) {
                                    fos.write(buffer);
                                    offset=0;
                                    count+=buffer.length;
                                }
                            }
                            if(lineDiffBytes > 0){
                                fos.write(fillEmpty, 0, lineDiffBytes);
                                count+=lineDiffBytes;
                            }
                        }
                        if (length-count!=0) {
                            Log.w("ceshi","最后写入数据");
                            fos.write(buffer,0,length-count);
                        }
                        break;
                    case 90:
                        int[] pixels90 = new int[1 * h];
                        for (int i= w-1;i>=0;i--) {
                            //fix bug#4353:获取单个Pixel很耗时，按行或列获取
                            bitmap.getPixels(pixels90, 0, 1, i, 0, 1, h);
                            for (int j=h-1;j>=0;j--) {
                                pixel = pixels90[j];
                                byte pixelB = (byte)((pixel & RGB_MASK_BLUE) << fillOffsetBlue);
                                byte pixelG = (byte)(((pixel >> pixelOffsetGreen) & RGB_MASK_GREEN) << fillOffsetGreen);
                                byte pixelR = (byte)(((pixel >> pixelOffsetRed) & RGB_MASK_RED)<< fillOffsetRed);

                                //Log.w("pixel_ceshi",String.format("pixel R,G,B:%d,%d,%d\n", (int)pixelR & 0xff ,(int)pixelG & 0xff , (int)pixelB & 0xff));

                                if(isToGray){
                                    //RGB转成灰度---和底层使用的一致
                                    //pixelR、pixelG、pixelB的值在pixels90数组中是负的，需要转换为正值再去计算
                                    buffer[offset] = (byte) ((((int)pixelR & 0xff)*38 + ((int)pixelG & 0xff)*75 + ((int)pixelB & 0xff)*15) >> 7);
                                    offset++;
                                }else{
                                    buffer[offset] = pixelB;
                                    buffer[offset+1] = pixelG;
                                    buffer[offset+2] = pixelR;
                                    offset += 3;
                                }

                                if (offset==buffer.length) {
                                    fos.write(buffer);
                                    offset=0;
                                    count+=buffer.length;
                                }
                            }
                            if(lineDiffBytes > 0){
                                fos.write(fillEmpty, 0, lineDiffBytes);
                                count+=lineDiffBytes;
                            }
                        }
                        if (length-count!=0) {
                            Log.w("ceshi","最后写入数据");
                            fos.write(buffer,0,length-count);
                        }
                        break;
                    case 180:
                        int[] pixels180 = new int[w*1];
                        for (int i= 0;i<h;i++) {
                            bitmap.getPixels(pixels180, 0, w, 0, i, w, 1);
                            for (int j=w-1;j>=0;j--) {
                                pixel = pixels180[j];
                                byte pixelB = (byte)((pixel & RGB_MASK_BLUE) << fillOffsetBlue);
                                byte pixelG = (byte)(((pixel >> pixelOffsetGreen) & RGB_MASK_GREEN) << fillOffsetGreen);
                                byte pixelR = (byte)(((pixel >> pixelOffsetRed) & RGB_MASK_RED)<< fillOffsetRed);
                                if(isToGray){
                                    //RGB转成灰度---和底层使用的一致
                                    buffer[offset] = (byte) ((((int)pixelR & 0xff)*38 + ((int)pixelG & 0xff)*75 + ((int)pixelB & 0xff)*15) >> 7);
                                    offset++;
                                }else{
                                    buffer[offset] = pixelB;
                                    buffer[offset+1] = pixelG;
                                    buffer[offset+2] = pixelR;
                                    offset += 3;
                                }
                                if (offset==buffer.length) {
                                    fos.write(buffer);
                                    offset=0;
                                    count+=buffer.length;
                                }
                            }
                            if(lineDiffBytes > 0){
                                fos.write(fillEmpty, 0, lineDiffBytes);
                                count+=lineDiffBytes;
                            }

                        }
                        if (length-count!=0) {
                            Log.w("ceshi","最后写入数据");
                            fos.write(buffer,0,length-count);
                        }
                        break;
                    case -90:
                    case 270:
                        int[] pixels270 = new int[1*h];
                        for (int i= 0;i<w;i++) {
                            bitmap.getPixels(pixels270, 0, 1, i, 0, 1, h);
                            for (int j=0;j<h;j++) {
                                pixel = pixels270[j];
                                byte pixelB = (byte)((pixel & RGB_MASK_BLUE) << fillOffsetBlue);
                                byte pixelG = (byte)(((pixel >> pixelOffsetGreen) & RGB_MASK_GREEN) << fillOffsetGreen);
                                byte pixelR = (byte)(((pixel >> pixelOffsetRed) & RGB_MASK_RED)<< fillOffsetRed);
                                if(isToGray){
                                    //RGB转成灰度---和底层使用的一致
                                    buffer[offset] = (byte) ((((int)pixelR & 0xff)*38 + ((int)pixelG & 0xff)*75 + ((int)pixelB & 0xff)*15) >> 7);
                                    offset++;
                                }else{
                                    buffer[offset] = pixelB;
                                    buffer[offset+1] = pixelG;
                                    buffer[offset+2] = pixelR;
                                    offset += 3;
                                }
                                if (offset==buffer.length) {
                                    fos.write(buffer);
                                    offset=0;
                                    count+=buffer.length;
                                }
                            }
                            if(lineDiffBytes > 0){
                                fos.write(fillEmpty, 0, lineDiffBytes);
                                count+=lineDiffBytes;
                            }
                        }
                        if (length-count!=0) {
                            Log.w("ceshi","最后写入数据");
                            fos.write(buffer,0,length-count);
                        }
                        break;
                }
                fos.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                bitmap.recycle();
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                bitmap.recycle();
                e.printStackTrace();
            }
            bitmap.recycle();
            Log.w("ceshi","saveImageToBmpByRotate_end");
            return true;
        }
        else{
            return false;
        }
    }

    public static boolean saveImageToBitmapFile(Bitmap bitmap,String outputFilePath){
        Log.w("ceshi","saveImageToBitmapFile_start");
        if (bitmap != null) {
            LogUtils.file("图片大小："+bitmap.getWidth()+","+bitmap.getHeight());
            Log.w("ceshi","图片大小："+bitmap.getWidth()+","+bitmap.getHeight());
            int w = bitmap.getWidth(), h = bitmap.getHeight();
            // 输出logCat信息
//            PrintLogCat.printLogCat(CommonFunction.getClassNameAndMethodNameAndLineNumberInfo() + "bitmap w : "+w+" bitmap h : "+h);
//            int[] pixels = new int[w * h];
//            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
//            byte[] rgb = addBMP_RGB_888(pixels, w, h);

            int count = 0;
            int bytesPerRow = (w * 3 + 3)/4*4;
            int lineDiffBytes = bytesPerRow - w*3;
            int length = bytesPerRow * h;
            byte[] header = addBMPImageHeader(length+54, 54);
            byte[] infos = addBMPImageInfosHeader(w, h, length, 24);

//            byte[] buffer = new byte[54 + rgb.length];
//            byte[] buffer = new byte[1024];
//            System.arraycopy(header, 0, buffer, 0, header.length);
//            System.arraycopy(infos, 0, buffer, 14, infos.length);
//            System.arraycopy(rgb, 0, buffer, 54, rgb.length);
            try {
                FileOutputStream fos = new FileOutputStream(outputFilePath);

                fos.write(header,0,header.length);
                fos.write(infos,0,infos.length);
                byte[] buffer = new byte[1024*3];
                int offset = 0;
                int pixel = 0;

                for (int i= h-1;i>0;i--) {
                    for (int j=0;j<w;j++) {
                        pixel = bitmap.getPixel(j,i);
                        buffer[offset]=(byte)(pixel & 0xff);
                        buffer[offset+1]=(byte)(pixel>>8 & 0xff);
                        buffer[offset+2]=(byte)(pixel>>16 & 0xff);
                        offset += 3;
                        if (offset==3072) {
                            fos.write(buffer);
                            offset=0;
                            count+=3072;
                        }
                    }
                    if(lineDiffBytes > 0){
                        fos.write(buffer, 3072-lineDiffBytes, lineDiffBytes);
                    }
                }
                if (length-count!=0) {
                    Log.w("ceshi","最后写入数据");
                    fos.write(buffer,0,length-count);
                }
                fos.close();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            bitmap.recycle();
            Log.w("ceshi","saveImageToBitmapFile_end");
            return true;
        }
        else{
            return false;
        }
    }

}

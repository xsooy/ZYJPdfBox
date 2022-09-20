package com.example.test;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SysPdfRender {

    private void _doForPdf(String path,String outPath) {
        try {
            File file = new File(path);
            ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);
            for (int i = 0; i < pdfRenderer.getPageCount(); i++) {
                PdfRenderer.Page currentPage = pdfRenderer.openPage(i);
                float scale = 600/72f;
                int width, height;
                width = (int) (currentPage.getWidth()*scale);
                height = (int) (currentPage.getHeight()*scale);
                Bitmap bitmap;
                try {
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                } catch (OutOfMemoryError E) {
                    currentPage.close();
                    continue;
                }
                bitmap.eraseColor(Color.WHITE);

                currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);

//                String filePath = outPath+"/"+(i+1)+".jpg";

                // 检测当前页宽高值，确定当前面页方向属于横向还是纵向 状态: 宽<高 纵向
//                if (width < height) {
//                FileUtil.saveBitmap(bitmap,filePath);
//                } else {
//                    FileUtil.saveBitmap(BitmapUtils.adjustBitmapRotation(bitmap, 90), filePath);
//                }
//                if (delegate!=null) delegate.renderDidFinishPageForPreview(this,i+1,filePath,_ipsShouldCancel);
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException interruptedException) {
//                    interruptedException.printStackTrace();
//                }
//                if (delegate!=null) delegate.renderDidFinishPageForPrint(this,i+1,filePath,_ipsShouldCancel);
                currentPage.close();
//                if(_ipsShouldCancel.get()) {
//                    PrintLogCat.log(Render.class.getSimpleName(),"pdf解析提前取消");
//                    break;
//                }
            }
            pdfRenderer.close();
            fileDescriptor.canDetectErrors();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
//            if (delegate!=null)delegate.renderDidFailedPageForPreview(this,0, Error.other);
        } catch (IOException ioException) {
            ioException.printStackTrace();
//            if (delegate!=null)delegate.renderDidFailedPageForPreview(this,0, Error.other);
        } catch (SecurityException exception) {
//            PrintLogCat.log(Render.class.getSimpleName(),"pdf has password");
//            if (delegate!=null)delegate.renderDidFailedPageForPreview(this,0, Error.other);
//            PrintLogCat.w(TAG,"pdf has password");
//            if (delegate!=null)delegate.renderDidFailedPageForPreview(this,0,Error.pdfSecurity);
        }
//        Log.w("print_ceshi","endRendering3333");
//        if (delegate!=null)delegate.renderDidEndJob(this);
    }

}

package com.example.test;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.blankj.utilcode.util.ToastUtils;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPageTree;
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import com.xsooy.icc.IccUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
//import com.xsooy.pdfannots.PDFUtils;

public class MainActivity extends AppCompatActivity {

    private int REQUEST_FILE = 100;
//    private PDFUtils pdfUtils = new PDFUtils();
    private ImageView imageView;
//    private IccUtils iccUtils;
    private boolean isSys = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        iccUtils = new IccUtils();
//        iccUtils.loadProfile(getExternalFilesDir("").getAbsolutePath()+"/ISOcoated_v2_300_bas.icc");
//        int ceshi = iccUtils.apply(25666);
//        Log.w("ceshi",String.format("r:%d,g:%d,b:%d",ceshi>>16&0xff,ceshi>>8&0xff,ceshi&0xff));
        imageView = findViewById(R.id.iv_image);
        PDFBoxResourceLoader.init(getApplicationContext());
        findViewById(R.id.tv_left).setOnClickListener(view -> {
            isSys = true;
            openFile();
        });
        findViewById(R.id.tv_right).setOnClickListener(view -> {
            isSys = false;
            openFile();
        });
    }

    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        String [] mimeTypes = {
//                "image/*",
                "application/pdf"
//                ,"text/plain","application/vnd.ms-powerpoint",
//                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
//                "application/msword",
//                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
//                "application/vnd.ms-excel",
//                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        };
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent,REQUEST_FILE);
    }

    private void _doForPdfAnnots(String path) {
        try {
            File file = new File(path);
            ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);
            PdfRenderer.Page currentPage = pdfRenderer.openPage(0);
            float scale = 300/72f;
            int width = (int) (currentPage.getWidth()*scale);
            int height = (int) (currentPage.getHeight()*scale);
            Log.w("ceshi","文件大小："+width+","+height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
            currentPage.close();
            pdfRenderer.close();
            fileDescriptor.canDetectErrors();
            if (!isSys) {
                PDDocument document = PDDocument.load(new File(path));
//            PDPageTree pdPageTree = document.getPages();
                YJPDFRenderer renderer = new YJPDFRenderer(document);
                renderer.renderImage(bitmap,0);
                String filePath = getPreviewDir()+"temp.jpg";
                saveBitmap(bitmap,filePath);
                document.close();
            }
            imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }catch (SecurityException e) {
            e.printStackTrace();
            ToastUtils.showShort("不支持加密文档");
        }
    }

    private void _doForPdf(String path,String pwd) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                PDDocument document;
                try {

                    if (TextUtils.isEmpty(pwd)){
                        document = PDDocument.load(new File(path));
                    } else {
                        document = PDDocument.load(new File(path),pwd);
                    }
                    Paint paint = new Paint();
                    paint.setColor(Color.WHITE);
                    paint.setStyle(Paint.Style.FILL);
                    PDFRenderer renderer = new PDFRenderer(document);
                    Bitmap bitmap = renderer.renderImageWithDPI(0, 300);
                    String filePath = getPreviewDir()+"1.jpg";
                    saveBitmap(bitmap,filePath);
                    imageView.post(()->{
                        try {
                            imageView.setImageBitmap(BitmapUtils.compressBitmapFormPath(getPreviewDir()+"1.jpg",imageView.getWidth(),imageView.getHeight()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    document.close();
                } catch (InvalidPasswordException e){
                    e.printStackTrace();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void saveBitmap(Bitmap bitmap,String filePath) {
        try {
            File file = new File(filePath);
            if(!file.exists()){
                file.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(filePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getPreviewDir() {
        File storageDir = new File(getExternalFilesDir("").getAbsolutePath()+"/preview/");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return storageDir.getAbsolutePath()+"/";
    }

    public String getScanImageDir(){
        File storageDir = new File(getExternalFilesDir("").getAbsolutePath()+"/scanHistory/");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return storageDir.getAbsolutePath()+"/";
    }

    public void saveFileByPath(FileInputStream inputStream, String filePath) {
        Log.w("ceshi","保存路径："+filePath);
//        Log.w("ceshi","目录："+filePath.substring(0,filePath.lastIndexOf("/")+1));

        File dirFile = new File(getScanImageDir());
        if (!dirFile.exists()) {
            Log.w("ceshi","临时文件路径创建："+dirFile.mkdirs());
        }
        File target = new File(filePath);
        Log.w("ceshi","saveFileByPath="+filePath);
        try {
            FileOutputStream outputStream = new FileOutputStream(target);
            int temp = 0;
            byte[] data = new byte[1024];
            while((temp = inputStream.read(data))!=-1) {
                outputStream.write(data,0,temp);
//                Log.w("ceshi","文件读取中"+temp);
            }
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            Log.w("ceshi","saveFileByPathException="+e.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri = data.getData();
        if (uri!= null) {
            Log.w("ceshi","返回的路径:"+uri.getEncodedPath());
            try {
                DocumentFile documentFile = DocumentFile.fromSingleUri(this,uri);
                saveFileByPath((FileInputStream) getContentResolver().openInputStream(documentFile.getUri()),getScanImageDir()+documentFile.getName());
                if (isSys) {
                    _doForPdf(getScanImageDir()+documentFile.getName(),"");
                } else
                    _doForPdfAnnots(getScanImageDir()+documentFile.getName());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
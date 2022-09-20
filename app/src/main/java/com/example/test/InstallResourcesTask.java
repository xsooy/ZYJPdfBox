package com.example.test;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import androidx.fragment.app.FragmentManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class InstallResourcesTask extends AsyncTask<String, Void, Void> {

    public interface InstallResultCallBack{
        void finish();
    }

    private String TAG = "InstallResourcesTask";
    private ProgressDialog mResourceProgressDialog = null;
//    private ResoureDialog resoureDialog = null;
    private Context mContext;
//    private FragmentManager fragmentManager;
    private InstallResultCallBack callBack;

    public InstallResourcesTask(Context context, InstallResultCallBack callBack) {
        mContext = context;
        this.callBack = callBack;
    }

    public InstallResourcesTask(FragmentManager fragmentManager, Context context, InstallResultCallBack callBack) {
//        this.fragmentManager = fragmentManager;
//        mContext = context;
        this.callBack = callBack;
    }

    @Override
    protected void onPreExecute() {
//        resoureDialog = new ResoureDialog();
//        resoureDialog.setCancelable(false);
//        resoureDialog.show(fragmentManager,"123");
        mResourceProgressDialog = new ProgressDialog(mContext);
//        mContext.getResources().getString(R.string.)
        mResourceProgressDialog.setTitle("1234");
        mResourceProgressDialog.setCancelable(false);
        mResourceProgressDialog.setIndeterminate(true);
        mResourceProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mResourceProgressDialog.setProgressNumberFormat(null);
        mResourceProgressDialog.setProgressPercentFormat(null);
        mResourceProgressDialog.show();
    }


    @Override
    protected void onPostExecute(Void result) {
//        resoureDialog.dismiss();
        if (mResourceProgressDialog != null)
            mResourceProgressDialog.dismiss();
        if (callBack!=null) callBack.finish();
    }


    @Override
    protected Void doInBackground(String... dest) {
        try {
            unzipResources(dest[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    // Uncompress and copy resource files stored in assets to resourcesDestDir
    private void unzipResources(String resourceDestDir) throws IOException {
        File resources = new File(resourceDestDir);
        // Fixing a Zip Path Traversal Vulnerability. See notes below.
        String canonicalResourcesDestDir = resources.getCanonicalPath();
        //Our resource zip file lives in assets/Resources.zip
        String resourceFileInAssets = "Resources.zip";
        Log.w("ceshi","解压路径："+canonicalResourcesDestDir);
        ZipInputStream zipResources = null;
        AssetManager am = mContext.getAssets();
        if (am != null) {
            zipResources = new ZipInputStream(am.open(resourceFileInAssets));
            if (zipResources.available() == 0) {
                Log.e(TAG, "zipResources could not be accessed");
                return;
            }
        } else {
            Log.e(TAG, "AssetManager was unable to be accessed");
        }

        // Create output directory if not present
        resources.mkdirs();
        if (!resources.exists()) {
            Log.e(TAG, "Resource directory could not be created");
            return;
        }
        ZipEntry entry = null;
        while ((entry = zipResources.getNextEntry()) != null) {
            // Fixing a Zip Path Traversal Vulnerability
            //
            // NOTE: This is from Google Help Center Article https://support.google.com/faqs/answer/9294009
            //
            // "Zip files can contain an entry (file or directory) having
            // path traversal characters (“../”) in its name.
            // If developers unzip such zip file entries without validating their name,
            // it can potentially cause a path traversal attack, leading to writes in arbitrary
            // directories or even overwriting the files in the app's private folders.
            //
            // We recommend fixing this issue in your app by checking if canonical paths to
            // unzipped files are underneath an expected directory.
            // Specifically, before using a File object created using the return value of
            // ZipEntry's getName() method, always check if the return value of
            // File.GetCanonicalPath() belongs to the intended directory path."

            File exceptionTest = new File(resourceDestDir, entry.getName());
            String canonicalPath = exceptionTest.getCanonicalPath();
            if (!canonicalPath.startsWith(canonicalResourcesDestDir)) {
                Log.e(TAG, "Security issue with traversal character in the path");
                return;
            }
            // Fixing a Zip Path Traversal Vulnerability - END

            String fileName = entry.getName();
            File newFile = new File(resources, fileName);

            // Create all non existent folders
            newFile.getParentFile().mkdirs();
            if (entry.isDirectory()) {
                continue;
            }
            FileOutputStream outputStream = new FileOutputStream(newFile);
            BufferedOutputStream out = new BufferedOutputStream(outputStream);
            try {
                int read = 0;
                byte[] bytes = new byte[1024];

                while ((read = zipResources.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
                out.flush();
            } finally {
                outputStream.getFD().sync();
                out.close();
            }
            zipResources.closeEntry();
        }
        zipResources.close();
//        LogUtils.file(TAG, "Resources have been installed!");
//        resoureDialog.over();
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException interruptedException) {
//            interruptedException.printStackTrace();
//        }
    }
}

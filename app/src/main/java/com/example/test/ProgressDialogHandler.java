package com.example.test;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.github.ybq.android.spinkit.SpinKitView;
import com.github.ybq.android.spinkit.style.ChasingDots;


public class ProgressDialogHandler extends Handler {

    public static final int SHOW_PROGRESS_DIALOG = 1;
    public static final int DISMISS_PROGRESS_DIALOG = 2;

    private ProgressDialog pd;
    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;

    private Context context;
    private boolean cancelable;
    //    private ProgressCancel
    private ChasingDots chasingDots;
    private Window window;
    private WindowManager.LayoutParams lp;

    public ProgressDialogHandler(Context context) {
        super();
        this.context = context;
    }

    private void initProgressDialog() {
        chasingDots = new ChasingDots();
        View inflate = LayoutInflater.from(context).inflate(R.layout.loading_dialog,null);

        SpinKitView viewById = inflate.findViewById(R.id.spin_kit);
        viewById.setIndeterminateDrawable(chasingDots);
        builder = new AlertDialog.Builder(context);
        builder.setCustomTitle(inflate);
        alertDialog = builder.create();
        alertDialog.setCancelable(false);
        if (!alertDialog.isShowing()) {
            alertDialog.show();
        }
    }


    private void dismissProgressDialog() {
        if (alertDialog != null) {
            if (alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
//        super.handleMessage(msg);
        switch (msg.what) {
            case SHOW_PROGRESS_DIALOG:
                initProgressDialog();
                break;
            case DISMISS_PROGRESS_DIALOG:
                dismissProgressDialog();
                break;
        }
    }
}

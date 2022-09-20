package com.example.test;

import android.content.Context;
import android.text.TextUtils;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.gson.JsonSyntaxException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import io.reactivex.subscribers.ResourceSubscriber;

/**
 * Created by Administrator on 2017/11/3.
 */

public abstract class SimpleSubscriber<T> extends ResourceSubscriber<T> implements ProgressCancelListener {



    private ProgressDialogHandler mProgressDialogHandler;

//    private Context mContext;

    public SimpleSubscriber() {

    }

    public SimpleSubscriber(Context context) {
//        mContext = context;
        mProgressDialogHandler = new ProgressDialogHandler(context);
    }

    private void showProgressDialog() {
        if (mProgressDialogHandler != null) {
            mProgressDialogHandler.obtainMessage(ProgressDialogHandler.SHOW_PROGRESS_DIALOG).sendToTarget();
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialogHandler != null) {
            mProgressDialogHandler.obtainMessage(ProgressDialogHandler.DISMISS_PROGRESS_DIALOG).sendToTarget();
            mProgressDialogHandler = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        showProgressDialog();
    }

    @Override
    public void onComplete() {
        dismissProgressDialog();
    }

    @Override
    public void onError(Throwable e) {
        dismissProgressDialog();
        e.printStackTrace();
//        LogUtils.file(e.getMessage());
//        Log.d("ceshi", "网络测试回调" + e.getMessage());
        if (e instanceof SocketTimeoutException) {
            LogUtils.d("SocketTimeoutException");
//            ToastUtils.showShort("连接超时");
        } else if (e instanceof ConnectException) {
            LogUtils.d("连接错误");
            ToastUtils.showShort("连接错误");
        } else if (e instanceof JsonSyntaxException) {
//            ToastUtils.showShort("网络请求异常，请重试！");
        } else if (e instanceof NullPointerException) {
//            ToastUtils.showShort("网络请求异常，请重试！");
//            LogUtils.i("nullPointerException");
        } else {
            if (!TextUtils.isEmpty(e.getMessage())) {
                LogUtils.file(e.getMessage());
//                ToastUtils.showShort(e.getMessage());
//                if (e.getMessage().equals("token不正确") || e.getMessage().equals("无权限访问该门店")) {
//                    ActivityColletor.finishActivity();
//                        Intent intent = new Intent(DDPayApplication.getContext(),LoginActivity.class);

//                    Intent intent = new Intent(FJApplication.getContext(), LoginActivity.class);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    FxcarApplication.getContext().startActivity(intent);

//                } else {
//                    ToastUtils.showShort(e.getMessage());
//                    ToastUtils.showShort("网络请求异常，请重试！");
//                }
            } else {
//                ToastUtils.showShort("网络请求异常，请重试！");
            }
        }
//        e.printStackTrace();
    }

    @Override
    public void onCancelProgress() {
        if (!this.isDisposed()) {
            this.dispose();
        }
    }
}


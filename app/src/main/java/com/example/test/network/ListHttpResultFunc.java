package com.example.test.network;

import android.util.Log;

import com.example.test.bean.ListHttpBean;
import com.example.test.bean.ListInfoBean;

import io.reactivex.functions.Function;

public class ListHttpResultFunc<T> implements Function<ListHttpBean<T>, ListInfoBean<T>> {
    @Override
    public ListInfoBean<T> apply(ListHttpBean<T> tListHttpBean) throws Exception {
        if (tListHttpBean.getCode() == 0) {
            return tListHttpBean.getData();
        } else {
            throw new ApiException(tListHttpBean.getMessage());
        }
    }
}

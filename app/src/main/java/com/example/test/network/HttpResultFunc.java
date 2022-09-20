package com.example.test.network;

/**
 * Created by Administrator on 2017/11/3.
 */

import com.example.test.bean.HttpBean;

import io.reactivex.functions.Function;

public class HttpResultFunc<T> implements Function<HttpBean<T>, T> {
//    @Override
//    public T call(BaseHttpBean<T> tBaseHttpBeanResult) {
//        if (tBaseHttpBeanResult.getStatus()==0) {
//            throw new ApiException("#" + tBaseHttpBeanResult.getInfo().toString());
//        }else if (tBaseHttpBeanResult.getStatus()==-1){
//            throw new ApiException("#请重新登录");
//        }else {
//            return tBaseHttpBeanResult.getInfo();
//        }
//    }
    @Override
    public T apply(HttpBean<T> tBaseHttpBean) throws Exception {
        if (tBaseHttpBean.getCode() == 0) {
            return tBaseHttpBean.getData();
        } else {
            throw new ApiException(tBaseHttpBean.getMessage());
        }
    }
}


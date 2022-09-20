package com.example.test.network;

import com.example.test.HttpMap;
import com.example.test.bean.CategoriesBean;
import com.example.test.bean.HttpBean;
import com.example.test.bean.ListInfoBean;
import com.example.test.bean.ResourceBean;
import com.google.gson.JsonObject;

import java.util.List;

import io.reactivex.Flowable;

/**
 * Created by Administrator on 2017/11/3.
 */

public class Service {

//    public Observable<LoginBean> login(String phone,String password) {
//        return Api.getApi().login(phone,password).map(new HttpResultFunc<LoginBean>())
//                .compose(RxSchedulers.<LoginBean>io_main2());
//    }

//    public Flowable<LoginBean> login(String username,String password) {
//        return Api.getApi().login(username,password).map(new HttpResultFunc<LoginBean>())
//                .compose(RxSchedulers.<LoginBean>io_main());
//    }

//    public Flowable<UserBean> login(String user, String pwd) {
//        return Api.getApi().login(user,pwd).map(new HttpResultFunc<UserBean>())
//                .compose(RxSchedulers.<UserBean>io_main());
//    }

    public Flowable<JsonObject> userRegister(HttpMap httpMap) {
        return Api.getApi().userRegister(httpMap.sign()).map(new HttpResultFunc<JsonObject>()).compose(RxSchedulers.io_main());
    }

    public Flowable<List<JsonObject>> stages(HttpMap httpMap) {
        return Api.getApi().stages(httpMap.sign()).map(new HttpResultFunc<List<JsonObject>>()).compose(RxSchedulers.io_main());
    }

    public Flowable<JsonObject> userRegister2(HttpMap httpMap) {
        return Api.getApi().userRegister2(httpMap.sign().getBody()).map(new HttpResultFunc<JsonObject>()).compose(RxSchedulers.io_main());
    }

    public Flowable<List<CategoriesBean>> categories(HttpMap httpMap) {
        return Api.getApi().categories(httpMap.sign()).map(new HttpResultFunc<>()).compose(RxSchedulers.io_main());
    }

    public Flowable<ListInfoBean<ResourceBean>> resources(HttpMap httpMap) {
        return Api.getApi().resources(httpMap.sign()).map(new ListHttpResultFunc<>()).compose(RxSchedulers.io_main());
    }

    public Flowable<JsonObject> resourceDetail(String id,HttpMap httpMap) {
        return Api.getApi().resourceDetail(id,httpMap.sign()).map(new HttpResultFunc<>()).compose(RxSchedulers.io_main());
    }

}

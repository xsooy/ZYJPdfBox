package com.example.test.network;

import com.example.test.bean.CategoriesBean;
import com.example.test.bean.HttpBean;
import com.example.test.bean.ListHttpBean;
import com.example.test.bean.ListInfoBean;
import com.example.test.bean.ResourceBean;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

/**
 * Created by Administrator on 2017/11/3.
 */

public interface ApiService {

//    String BASE_URL="http://47.107.155.230:9010/";
//    String BASE_URL="https://admin.ifxys.com";
//    String BASE_URL="https://nuwa-stg.gongfudou.com";
    String BASE_URL="https://nuwa.gongfudou.com";

//    @POST("userapp.do?method=login")
//    @FormUrlEncoded
//    Flowable<HttpBean<UserBean>> login(@Field("username") String phone, @Field("password") String password);

    @POST("/api/v1/users")
    @FormUrlEncoded
    Flowable<HttpBean<JsonObject>> userRegister(@FieldMap Map<String,String> map);

    @POST("/api/v1/users")
    Flowable<HttpBean<JsonObject>> userRegister2(@Body RequestBody body);

    @GET("/api/v1/stages")
    Flowable<HttpBean<List<JsonObject>>> stages(@QueryMap Map<String,String> map);

    @GET("/api/v1/categories")
    Flowable<HttpBean<List<CategoriesBean>>> categories(@QueryMap Map<String,String> map);

    @GET("/api/v1/resources")
    Flowable<ListHttpBean<ResourceBean>> resources(@QueryMap Map<String,String> map);

//     /api/v1/resource/:id

    @GET("/api/v1/resource/{id}")
    Flowable<HttpBean<JsonObject>> resourceDetail(@Path("id") String id,@QueryMap Map<String,String> map);


//    @GET("/app/index/deviceMemberInfo")
//    Flowable<HttpBean<MemberInfo>> deviceMemberInfo(@QueryMap Map<String,String> map);


}

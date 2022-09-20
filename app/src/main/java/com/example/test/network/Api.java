package com.example.test.network;

/**
 * Created by Administrator on 2017/11/3.
 */

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.example.test.BuildConfig;
import com.example.test.bean.HttpBean;
import com.example.test.bean.ListHttpBean;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by xsooy on 2017/11/3.
 */

public class Api {

    private static Retrofit retrofit;
    private static ApiService apiService;
    private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private static OkHttpClient mOkHttpClient;

    //构造方法私有
    private Api() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        mOkHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60,TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .addInterceptor(new HttpHeadInterceptor())
                .retryOnConnectionFailure(true)
//                .addInterceptor(new LogInterceptor()) //日志测试
//                .cache(getCache())
//                .addNetworkInterceptor(new HttpCacheInterceptor())
                .build();

        Gson gson = new GsonBuilder().
                setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").
                serializeNulls().
                registerTypeHierarchyAdapter(HttpBean.class, new ResultJsonDeser()).
                registerTypeHierarchyAdapter(ListHttpBean.class, new ListResultJsonDeser()).
                //去除null字段
//                registerTypeHierarchyAdapter(BaseBean.class,new BeanJsonDeser()).
//                registerTypeAdapterFactory(new NullStringToEmptyAdapterFactory()).
                setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getName().contains("_on");
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> incomingClass) {
                        return incomingClass == Date.class || incomingClass == boolean.class;
                    }
                }).
                create();

        retrofit = new Retrofit.Builder()
                .client(mOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(ApiService.BASE_URL)
//                .baseUrl(baseUrl)
                .build();
//        for (int i=0;i<retrofit.converterFactories().size();i++) {
//            Log.d("ceshi","retrofit.converterFactories().get(i).toString()"+retrofit.converterFactories().get(i).toString());
//        }

        apiService = retrofit.create(ApiService.class);
    }

    //在访问HttpMethods时创建单例
    private static class SingletonHolder {
        private static final Api INSTANCE = new Api();
    }

    public static void delApi() {
        apiService = null;
    }

    //获取单例
    public static Api getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public static ApiService getApi() {

        if (apiService != null) {
            return apiService;
        }
        getInstance();

        return getApi();
    }

//    @NonNull
//    private Cache getCache() {
//        Long cacheSize;
//        File cacheFile = new File(CMApplication.getContext().getCacheDir(), "cache");
//        if (!cacheFile.exists()) {
//            //noinspection ResultOfMethodCallIgnored
//            cacheFile.mkdirs();
//        }
//        long size = MIN_DISK_CACHE_SIZE;
//
//        try {
//            StatFs statFs = new StatFs(cacheFile.getAbsolutePath());
//            long available = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
//            // Target 2.5% of the total space.
//            size = available / 40;
//        } catch (IllegalArgumentException ignored) {
//        }
//        cacheSize = Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
//
//        return new Cache(cacheFile, cacheSize);
//    }

    //添加head头
    class HttpHeadInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

//            String token = AuthSesseion.token;
//            String token = "62fa005f7e910500276dedc7";
            String token = "62f6251f53f095001534cb99";

            if (!TextUtils.isEmpty(token)) {
                Log.d("ceshi","token:"+token);
                request = request
                        .newBuilder()
//                        .addHeader("Cookie", "Authorization=" + token + "; path=/; domain=; DEVICE=android; VERSION=" + BuildConfig.VERSION_NAME + "; MODEL=" + Build.MODEL + ";")
//                        .addHeader("Connection","close")
                        .addHeader("Content-Type","application/json; charset=utf-8")
                        .addHeader("AppId",token)
                        .addHeader("UserId","17666010245")
                        .build();
            } else {
//                Log.d("ceshi","head为空");
                request = request
                        .newBuilder()
                        .addHeader("Cookie", "path=/; domain=; DEVICE=android; VERSION=" + BuildConfig.VERSION_NAME + "; MODEL=" + Build.MODEL + ";")
//                        .addHeader("Connection","close")
                        .addHeader("Content-Type","application/json; charset=utf-8")
//                        .addHeader("Authorization","")
                        .build();
            }
//            Log.d("headers",request.header("Content-Type"));
//             Test专用, 勿删
//            request=request
//                    .newBuilder()
//                    .header("Cookie","PHPSESSID="+"u546qf1km54tn9tq4jtsn606g3"+"; path=/; domain=duoduofenqi.com")
//                    .build();

            return chain.proceed(request);
        }
    }

//    private class HttpCacheInterceptor implements Interceptor {
//
//        @SuppressLint("MissingPermission")
//        @Override
//        public Response intercept(Chain chain) throws IOException {
//            Request request = chain.request();
//            if (!NetworkUtils.isConnected()) {
//                request = request.newBuilder()
//                        .cacheControl(CacheControl.FORCE_CACHE)
//                        .build();
//                Log.d("Okhttp", "no network");
//            }
//
//            Response originalResponse = chain.proceed(request);
//            if (NetworkUtils.isConnected()) {
//                //有网的时候读接口上的@Headers里的配置，你可以在这里进行统一的设置
//                int maxAge = 60;
//                return originalResponse.newBuilder()
//                        .removeHeader("Pragma")
//                        .removeHeader("Cache-Control")
//                        .header("Cache-Control", "public, max-age=" + maxAge)
//                        .build();
//            } else {
//                int maxStale = 60 * 60 * 24 * 2;
//                return originalResponse.newBuilder()
//                        .removeHeader("Pragma")
//                        .removeHeader("Cache-Control")
//                        .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
//                        .build();
//            }
//        }
//    }

//    private class LogInterceptor implements Interceptor {
//        @Override
//        public Response intercept(Chain chain) throws IOException {
//            Request request = chain.request();
////            Log.v("ceshi", "request:" + request.toString());
//            long t1 = System.nanoTime();
//
//            Response response = chain.proceed(chain.request());
//            long t2 = System.nanoTime();
////            Log.v("ceshi", String.format(Locale.getDefault(), "Received response for %s in %.1fms%n%s",
////                    response.request().url(), (t2 - t1) / 1e6d, response.headers()));
//            okhttp3.MediaType mediaType = response.body().contentType();
////            Log.d("ceshi", "返回结果状态码：" + response.code());
//            String content = response.body().string();
////            Log.i("ceshi", "response body:" + content);
////            LogUtils.file("url:"+chain.request().url().toString());
////            LogUtils.file("url_host:"+chain.request().url().host());
////            LogUtils.file("response body:" + content);
//
//            if (chain.request().url().toString().contains("setDeviceEventStatus")) {
//                LogUtils.file("response body:" + content);
//            }
////            if (chain.request().url().toString().contains("deviceEventData")) {
////                LogUtils.file("response body:" + content);
////            }
//            if (response.code() != 200) {
//                LogUtils.file("url:"+chain.request().url().toString());
//                LogUtils.file("response body:" + content);
////                Service mModel = new Service();
////                mModel.appLog("{" + content + ".Android}").subscribe(new SimpleSubscriber<Object>() {
////                    @Override
////                    public void onNext(Object o) {
////                        Log.d("ceshi","日志上传成功");
////                    }
////                });
//            }
//            return response.newBuilder()
//                    .body(okhttp3.ResponseBody.create(mediaType, content))
//                    .build();
//        }
//    }
}


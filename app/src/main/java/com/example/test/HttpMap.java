package com.example.test;

import android.text.TextUtils;
import android.util.Log;

import com.blankj.utilcode.util.EncryptUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class HttpMap extends HashMap<String,String> {

//    private String secret = "$2a$12$JhtHguJtjzcae.WBG462PuFQzWazsZjJF68SAg7VojnWob9eXGO26";
    private String secret = "$2a$12$tcSuIBwMrUPjndmx774OkewGqCbGTT0XFx.sKNgIrD2OUnzwNOWwK";


    public HttpMap() {
        put("timestamp",(new Date().getTime() /1000)+"");
//        put("timestamp","1660816184");
//        put("nonce", "01234567890123456789012345678912");
        put("nonce", UUID.randomUUID().toString().replace("-",""));
    }

    public synchronized HttpMap sign() {
        this.remove("sign");
        if (this.size()==0){
            return this;
        }
        StringBuilder builder = new StringBuilder();
        Set<String> set = new TreeSet<String>(new ASCIIComparator());
        set.addAll(this.keySet());
        for (String key:set) {
            if (TextUtils.isEmpty(this.get(key))) {
                continue;
            }
            builder.append(key);
            builder.append("=");
            builder.append(this.get(key));
            builder.append("&");
        }

        Log.w("ceshi","原始数据："+secret+builder.substring(0,builder.length()-1)+secret);

        this.put("sign",EncryptUtils.encryptMD5ToString(secret+builder.substring(0,builder.length()-1)+secret));
        Log.w("ceshi",this.get("sign"));
//        Log.w("ceshi","最终参数："+new Gson().toJson(this));
        return this;
    }

    class ASCIIComparator implements Comparator<String> {
        boolean sortString(String str1,String str2) {
            int length = str1.length() > str2.length() ? str2.length() : str1.length();
            for (int j = 0; j < length; j++) {
                if (str1.charAt(j) == str2.charAt(j)) {
                    continue;
                }else if (str1.charAt(j) < str2.charAt(j)) {
                    break;
                } else {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int compare(String s, String t1) {
            return sortString(s,t1)?1:-1;
        }
    }

    public RequestBody getBody() {
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization()
                .create();
//        Log.d("ceshi",this.toString());
        Log.d("ceshi",gson.toJson(this));
//        Log.d("ceshi",new JSONObject(this).toString());

        return RequestBody.create(MediaType.parse("Content-Type, application/json"),
                new JSONObject(this).toString());
//        return RequestBody.create(MediaType.parse("Content-Type, application/json"),
//                new Gson().toJson(this));
    }
}

package com.example.test.network;

import android.util.Log;

import com.example.test.bean.HttpBean;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by xsooy on 2017/11/3.
 */
public class ResultJsonDeser implements JsonDeserializer<HttpBean<?>> {

    @Override
    public HttpBean<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        HttpBean<?> result=new HttpBean();
        if (json.isJsonObject()){
            JsonObject jsonObject=json.getAsJsonObject();
//            Log.w("ceshi","接口回调数据："+jsonObject.toString());
//            System.out.println(jsonObject.toString());
            int code = jsonObject.get("code").getAsInt();
            result.setCode(code);
            if (code == 0) {
                Type itemType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
                result.setData(context.deserialize(jsonObject.get("data"),itemType));
            } else {
                result.setMessage(jsonObject.get("message").getAsString());
            }
            return result;
        }
        return result;
    }
}

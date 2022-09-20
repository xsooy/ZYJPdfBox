package com.example.test.network;

import android.util.Log;

import com.example.test.bean.ListHttpBean;
import com.example.test.bean.ListInfoBean;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ListResultJsonDeser implements JsonDeserializer<ListHttpBean<?>> {

    @Override
    public ListHttpBean<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Log.d("ceshi","BaseListHttpBean()====>"+typeOfT.toString());
        ListHttpBean result=new ListHttpBean();
        if (json.isJsonObject()){
            JsonObject jsonObject=json.getAsJsonObject();
            System.out.println(jsonObject.toString());
            int code = jsonObject.get("code").getAsInt();
            result.setCode(code);
            if (code==0) {
                JsonObject dataJson = jsonObject.get("data").getAsJsonObject();
                Type itemType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
                ListInfoBean data = new ListInfoBean();
                List list = new ArrayList();
                JsonArray jsonArray = dataJson.get("resources").getAsJsonArray();
                for (int i=0;i<jsonArray.size();i++) {
                    list.add(context.deserialize(jsonArray.get(i),itemType));
//                    data.setList(context.deserialize(dataJson.get("list"),itemType));
                }
//                data.setList(context.deserialize(dataJson.get("list"),itemType));
                data.setResources(list);
                data.setTotalCount(dataJson.get("total_count").getAsInt());
                result.setData(data);
//                result.setData(context.deserialize(jsonObject.get("data"),itemType));
            } else {
                result.setMessage(jsonObject.get("message").getAsString());
            }
            return result;
        }
        return result;
    }

}

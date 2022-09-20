package com.example.test.network;

import android.util.Log;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class StringNullAdapter extends TypeAdapter<String> {
    @Override
    public void write(JsonWriter writer, String value) throws IOException {
        if (value == null) {
            writer.value("");
            return;
        }
        Log.d("ceshi","value====<>"+value);
        writer.value(value);
    }

    @Override
    public String read(JsonReader reader) throws IOException {


        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return "";
        }
        return reader.nextString();
    }

}

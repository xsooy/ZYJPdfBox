package com.example.test.bean;

public class ListHttpBean<T> {

    private int code;

    private ListInfoBean<T> data;

    private String message;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public ListInfoBean<T> getData() {
        return data;
    }

    public void setData(ListInfoBean<T> data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}

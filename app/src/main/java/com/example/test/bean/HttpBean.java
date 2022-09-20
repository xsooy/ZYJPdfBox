package com.example.test.bean;

/**
 * Created by Administrator on 2017/11/3.
 */
public class HttpBean<T> {

    /**
     * info : [{"redirect":"www.sample.com","imgsource":"http://sample.com/aaa.png","title":"sample"}]
     * status : 1
     * url :
     */

    private int code;
    /**
     * redirect : www.sample.com
     * imgsource : http://sample.com/aaa.png
     * title : sample
     */

    private T data;

    private String message;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

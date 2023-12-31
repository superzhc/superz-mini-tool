package com.github.superzhc.core.http.response;

/**
 * @author superz
 * @create 2021/7/20 19:54
 */
public class ResultT {
    private int code;
    private String msg;
    private Object data;

    public static ResultT success() {
        return success(null);
    }

    public static ResultT success(Object data) {
        return success(0, data);
    }

    public static ResultT success(int code, Object data) {
        return create(code, null, data);
    }

    public static ResultT fail(Throwable e) {
        return fail(0, e);
    }

    public static ResultT fail(int code, Throwable e) {
        return create(code, e.toString(), null);
    }

    public static ResultT fail(String msg, Object... params) {
        return fail(1, msg, params);
    }

    public static ResultT fail(int code, String msg, Object... params) {
        return create(code, String.format(msg, params), null);
    }

    public static ResultT msg(int code, String msg, Object... params) {
        return create(code, String.format(msg, params), null);
    }

    private static ResultT create(int code, String msg, Object data) {
        ResultT r = new ResultT();
        r.setCode(code);
        if (null != msg) {
            r.setMsg(msg);
        }
        if (null != data) {
            r.setData(data);
        }
        return r;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    /**
     * 自动进行数据转换
     *
     * @param <T>
     * @return
     */
    public <T> T data() {
        if (null == data) {
            return null;
        }

//        try {
//            Method method = getClass().getDeclaredMethod("data");
//            // 获取返回值类型
//            Type genericReturnType = method.getGenericReturnType();
//
//        } catch (Exception e) {
//            // 直接进行转换，报错也就报错了
        return (T) data;
//        }
    }

    @Override
    public String toString() {
        return "ResultT{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}


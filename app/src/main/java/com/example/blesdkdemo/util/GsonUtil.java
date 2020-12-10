package com.example.blesdkdemo.util;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.List;

/**
 * desc
 *
 * @author xiongyl 2020/11/9 11:49
 */
public class GsonUtil {
    private static Gson gson = new Gson();

    public static String toJson(List list) {
        if (list != null) {
            return gson.toJson(list);
        }
        return null;
    }

    public static <T> T getJsonData(String str, Object obj) {
        return obj instanceof Type ? (T) gson.fromJson(str, (Type) obj) : (T) gson.fromJson(str, (Class) ((Class) obj));
    }

}

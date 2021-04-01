package com.example.blesdkdemo.util;

import java.security.MessageDigest;

/**
 * desc
 *
 * @author xiongyl 2021/4/1 22:27
 */
public class ChatUrlUtil {
    /**
     * @param appId
     * @param appSecret
     * @param unionid
     * @param age
     * @param pregnantWeek
     * @param hardwareType
     * @param macAddress
     * @return
     */
    public static String getChatUrl(String appId, String appSecret, String unionid, int age, int pregnantWeek, int hardwareType, String macAddress) {
        String channel = "im_sdk";
        String type = "fetalHeartMonitorSdk";
        long bindTime = 1616554632;
        long timestamp = System.currentTimeMillis()/1000;
        String signature = getSha1(appId + appSecret + unionid + channel + type + age + pregnantWeek + hardwareType + bindTime + macAddress + timestamp);
        String chatUrl = "https://static.shecarefertility.com/shecare/im/dist/#/Customer?appId=%s&unionid=%s&channel=%s&type=%s&age=%s&pregnantWeek=%s&hardwareType=%s&bindTime=%s&macAddress=%s&timestamp=%s&signature=%s";
        chatUrl = String.format(chatUrl, appId, unionid, channel, type, age + "", pregnantWeek + "", hardwareType + "", bindTime + "", macAddress, timestamp + "", signature);
        return chatUrl;
    }

    public static String getSha1(String str) {

        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            MessageDigest mdTemp = MessageDigest.getInstance("SHA1");
            mdTemp.update(str.getBytes("UTF-8"));
            byte[] md = mdTemp.digest();
            int j = md.length;
            char buf[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                buf[k++] = hexDigits[byte0 >>> 4 & 0xf];
                buf[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(buf);
        } catch (Exception e) {
            return null;
        }
    }

}

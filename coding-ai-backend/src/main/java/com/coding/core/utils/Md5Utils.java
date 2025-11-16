package com.coding.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5工具类
 */
@Slf4j
public class Md5Utils {

    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * 计算文件的MD5值
     *
     * @param file 文件
     * @return MD5值
     */
    public static String calculateMd5(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return calculateMd5(inputStream);
        } catch (IOException e) {
            log.error("计算文件MD5失败", e);
            throw new RuntimeException("计算文件MD5失败: " + e.getMessage());
        }
    }

    /**
     * 计算输入流的MD5值
     *
     * @param inputStream 输入流
     * @return MD5值
     */
    public static String calculateMd5(InputStream inputStream) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, length);
            }
            return bytesToHex(md5.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("计算MD5失败", e);
            throw new RuntimeException("计算MD5失败: " + e.getMessage());
        }
    }

    /**
     * 字节数组转换为十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(HEX_DIGITS[(b >> 4) & 0xf]);
            result.append(HEX_DIGITS[b & 0xf]);
        }
        return result.toString();
    }
}

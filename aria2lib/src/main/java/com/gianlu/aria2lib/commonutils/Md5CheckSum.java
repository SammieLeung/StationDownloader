package com.gianlu.aria2lib.commonutils;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5CheckSum {
    public static String checkMd5Sum(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        DigestInputStream digestInputStream = new DigestInputStream(inputStream, md5Digest);
        byte[] buffer = new byte[8192];
        while (digestInputStream.read(buffer) != -1) {
            // 通过读取文件内容更新 MessageDigest
        }
        digestInputStream.close();
        byte[] digest = md5Digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        Log.d("Md5CheckSum", "checkMd5Sum: " + sb.toString());
        return sb.toString();
    }

    public static String checkMd5Sum(File file) throws NoSuchAlgorithmException, IOException {
        return checkMd5Sum(new FileInputStream(file));
    }
}

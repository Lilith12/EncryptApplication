package com.example.aleksandra.encryptapplication;

import android.content.Context;
import android.util.Base64;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Created by Aleksandra on 2017-09-02.
 */
public class FileUtils {
    public static File createEmptyTempFile(Context context, String username) {
        return new File(context.getCacheDir().getPath(), encodeFileName(username));
    }

    public static String encodeFileName(String username) {
        try {
            byte[] data = username.getBytes("UTF-8");
            return Base64.encodeToString(data, Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveMessageToTempFile(File file, String username, String message) {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            PrintWriter out = new PrintWriter(bw)) {
            out.println(username);
            out.println(message);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }
}

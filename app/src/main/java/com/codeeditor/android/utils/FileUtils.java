package com.codeeditor.android.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FileUtils {
    
    public static String readFromUri(Context context, Uri uri) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        StringBuilder stringBuilder = new StringBuilder();
        
        try (InputStream inputStream = resolver.openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        }
        
        if (stringBuilder.length() > 0) {
            stringBuilder.setLength(stringBuilder.length() - 1);
        }
        
        return stringBuilder.toString();
    }
    
    public static void writeToUri(Context context, Uri uri, String content) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        
        try (OutputStream outputStream = resolver.openOutputStream(uri, "wt")) {
            if (outputStream != null) {
                outputStream.write(content.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        }
    }
    
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver()
                    .query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        
        return result;
    }
    
    public static String getMimeType(String filename) {
        if (filename == null) {
            return "text/plain";
        }
        
        String extension = "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = filename.substring(lastDot + 1).toLowerCase();
        }
        
        switch (extension) {
            case "java": return "text/x-java-source";
            case "py": return "text/x-python";
            case "js": return "application/javascript";
            case "ts": return "application/typescript";
            case "html": return "text/html";
            case "htm": return "text/html";
            case "css": return "text/css";
            case "json": return "application/json";
            case "xml": return "text/xml";
            case "md": return "text/markdown";
            case "txt": return "text/plain";
            default: return "text/plain";
        }
    }
}

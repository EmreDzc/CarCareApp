package com.example.carcare.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FirebaseStorageService {
    private static final String TAG = "FirebaseStorageService";
    private FirebaseStorage storage;

    public FirebaseStorageService() {
        storage = FirebaseStorage.getInstance();
    }

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }

    // Uri'den resim yükleme (galeriden seçilen resim için)
    public void uploadImage(Context context, Uri imageUri, String storagePath, UploadCallback callback) {
        StorageReference storageRef = storage.getReference();
        StorageReference imageRef = storageRef.child(storagePath);

        UploadTask uploadTask = imageRef.putFile(imageUri);
        uploadTask.addOnFailureListener(exception -> {
            Log.e(TAG, "Upload failed", exception);
            callback.onFailure(exception);
        }).addOnSuccessListener(taskSnapshot -> {
            // Yükleme tamamlandı, indirme URL'sini al
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d(TAG, "Upload success, URL: " + downloadUrl);
                callback.onSuccess(downloadUrl);
            }).addOnFailureListener(exception -> {
                Log.e(TAG, "Failed to get download URL", exception);
                callback.onFailure(exception);
            });
        });
    }

    // Bitmap'ten resim yükleme (kamera ile çekilen resim için)
    public void uploadImage(Context context, Bitmap bitmap, String storagePath, UploadCallback callback) {
        StorageReference storageRef = storage.getReference();
        StorageReference imageRef = storageRef.child(storagePath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = imageRef.putBytes(data);
        uploadTask.addOnFailureListener(exception -> {
            Log.e(TAG, "Upload failed", exception);
            callback.onFailure(exception);
        }).addOnSuccessListener(taskSnapshot -> {
            // Yükleme tamamlandı, indirme URL'sini al
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d(TAG, "Upload success, URL: " + downloadUrl);
                callback.onSuccess(downloadUrl);
            }).addOnFailureListener(exception -> {
                Log.e(TAG, "Failed to get download URL", exception);
                callback.onFailure(exception);
            });
        });
    }

    // Dosyadan resim yükleme (cihazda saklanan dosya için)
    public void uploadImageFromFile(Context context, File file, String storagePath, UploadCallback callback) {
        StorageReference storageRef = storage.getReference();
        StorageReference imageRef = storageRef.child(storagePath);

        try {
            InputStream stream = new FileInputStream(file);
            UploadTask uploadTask = imageRef.putStream(stream);
            uploadTask.addOnFailureListener(exception -> {
                Log.e(TAG, "Upload failed", exception);
                callback.onFailure(exception);
            }).addOnSuccessListener(taskSnapshot -> {
                // Yükleme tamamlandı, indirme URL'sini al
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    Log.d(TAG, "Upload success, URL: " + downloadUrl);
                    callback.onSuccess(downloadUrl);
                }).addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to get download URL", exception);
                    callback.onFailure(exception);
                });
            });
        } catch (IOException e) {
            Log.e(TAG, "Error reading file", e);
            callback.onFailure(e);
        }
    }
}

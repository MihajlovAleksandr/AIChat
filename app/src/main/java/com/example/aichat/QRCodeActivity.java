package com.example.aichat;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import com.example.aichat.view.OverlayView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRCodeActivity extends AppCompatActivity {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private OverlayView overlayView;
    private PreviewView previewView;

    private static final String TAG = "QRScanner";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        previewView = findViewById(R.id.preview_view);
        overlayView = findViewById(R.id.overlay_view);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Настройка сканера QR-кодов
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        // Запрос разрешений камеры
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Настройка превью камеры
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Настройка анализа изображения с ограниченной областью сканирования
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    processImageProxy(barcodeScanner, imageProxy);
                });

                // Выбор задней камеры
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Отвязываем все use cases перед повторной привязкой
                cameraProvider.unbindAll();

                // Привязываем use cases к камере
                cameraProvider.bindToLifecycle(
                        (LifecycleOwner) this, cameraSelector, preview, imageAnalysis);
            } catch (Exception exc) {
                Log.e(TAG, "Use case binding failed", exc);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageProxy(BarcodeScanner barcodeScanner, ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            // Получаем координаты области сканирования из оверлея
            Rect scanArea = overlayView.getScanAreaRect();

            // Преобразуем Image (YUV_420_888) в Bitmap
            Bitmap originalBitmap = imageToBitmap(mediaImage);

            // Получаем размеры Bitmap и оверлея для расчёта масштабов
            float imageWidth = originalBitmap.getWidth();
            float imageHeight = originalBitmap.getHeight();
            float viewWidth = overlayView.getWidth();
            float viewHeight = overlayView.getHeight();

            float scaleX = imageWidth / viewWidth;
            float scaleY = imageHeight / viewHeight;

            Rect scaledScanArea = new Rect(
                    (int) (scanArea.left * scaleX),
                    (int) (scanArea.top * scaleY),
                    (int) (scanArea.right * scaleX),
                    (int) (scanArea.bottom * scaleY)
            );
            // Гарантируем, что область обрезки находится внутри границ изображения
            scaledScanArea.intersect(new Rect(0, 0, originalBitmap.getWidth(), originalBitmap.getHeight()));

            // Обрезаем Bitmap до нужной зоны
            Bitmap croppedBitmap = Bitmap.createBitmap(
                    originalBitmap,
                    scaledScanArea.left,
                    scaledScanArea.top,
                    scaledScanArea.width(),
                    scaledScanArea.height()
            );

            // Создаём InputImage из обрезанного Bitmap
            InputImage inputImage = InputImage.fromBitmap(croppedBitmap, imageProxy.getImageInfo().getRotationDegrees());

            barcodeScanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String value = barcode.getRawValue();
                            if (value == null) continue;
                            runOnUiThread(() -> {
                                Intent intent = new Intent();
                                intent.putExtra("QRCodeResult", value);
                                setResult(RESULT_OK, intent);
                                finish();

                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Ошибка сканирования QR-кода", e);
                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close();
                    });
        } else {
            imageProxy.close();
        }
    }

    // Метод для проверки разрешений
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(
                        this,
                        getText(R.string.no_permissions),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        barcodeScanner.close();
    }

    // Вспомогательный метод для преобразования Image (YUV_420_888) в Bitmap
    private Bitmap imageToBitmap(Image mediaImage) {
        byte[] nv21 = yuv420ToNv21(mediaImage);
        YuvImage yuvImage = new YuvImage(
                nv21,
                ImageFormat.NV21,
                mediaImage.getWidth(),
                mediaImage.getHeight(),
                null
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, mediaImage.getWidth(), mediaImage.getHeight()), 100, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    // Метод для преобразования YUV_420_888 в NV21
    private byte[] yuv420ToNv21(Image image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // Копируем Y данные
        yBuffer.get(nv21, 0, ySize);

        // Получаем U и V данные
        byte[] uBytes = new byte[uSize];
        byte[] vBytes = new byte[vSize];
        uBuffer.get(uBytes);
        vBuffer.get(vBytes);

        // Интерливируем V и U (NV21 ожидает порядок VU)
        for (int i = 0; i < uSize; i++) {
            nv21[ySize + i * 2] = vBytes[i];
            nv21[ySize + i * 2 + 1] = uBytes[i];
        }
        return nv21;
    }
}

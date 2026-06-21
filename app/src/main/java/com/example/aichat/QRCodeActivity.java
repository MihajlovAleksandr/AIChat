package com.example.aichat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.Manifest;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import com.example.aichat.view.main.BaseActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.aichat.view.helpers.DialogHelper;
import com.example.aichat.view.helpers.FullScreenHelper;
import com.example.aichat.view.helpers.OverlayView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class QRCodeActivity extends BaseActivity {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private OverlayView overlayView;
    private PreviewView previewView;
    private Camera camera;

    private static final String TAG = "QRScanner";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final AtomicBoolean isFinished = new AtomicBoolean(false);
    private MediaPlayer successSound;
    private boolean isFlashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);
        FullScreenHelper.enableFullScreen(getWindow());

        previewView = findViewById(R.id.preview_view);
        overlayView = findViewById(R.id.overlay_view);

        cameraExecutor = Executors.newSingleThreadExecutor();

        setupButtons();
        setupBarcodeScanner();
        setupLayout();
        setupTapToFocus();
        setupCustomSuccessSound();

        new Handler(Looper.getMainLooper()).postDelayed(this::checkPermissionsAndShowDialog, 1000);
    }

    private void setupButtons() {
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ImageView btnFlash = findViewById(R.id.btnFlash);
        btnFlash.setEnabled(false);
        btnFlash.setAlpha(0.3f);
        btnFlash.setOnClickListener(v -> toggleFlash(btnFlash));
    }

    private void toggleFlash(ImageView btnFlash) {
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) return;

        isFlashOn = !isFlashOn;
        camera.getCameraControl().enableTorch(isFlashOn);
        btnFlash.animate()
                .rotationBy(360f)
                .setDuration(300)
                .withEndAction(() -> btnFlash.setImageResource(
                        isFlashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off
                ))
                .start();
    }

    private void setupBarcodeScanner() {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);
    }

    private void setupLayout() {
        FullScreenHelper.enableFullScreen(getWindow());
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
    }

    private void setupTapToFocus() {
        previewView.setOnTouchListener((v, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP || camera == null) return true;
            focusAtPoint(event.getX(), event.getY());
            v.performClick();
            return true;
        });
    }

    private void focusAtPoint(float x, float y) {
        MeteringPointFactory factory = previewView.getMeteringPointFactory();
        MeteringPoint point = factory.createPoint(x, y);
        FocusMeteringAction action = new FocusMeteringAction.Builder(
                point, FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        camera.getCameraControl().startFocusAndMetering(action);
    }

    private void checkPermissionsAndShowDialog() {
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            showCameraPermissionPrompt();
        }
    }

    private void showCameraPermissionPrompt() {
        DialogHelper.showBottomDialog(
                this,
                "Необходим доступ к камере",
                "Хотите дать доступ к камере для сканирования QR-кодов?",
                "Разрешить",
                () -> ActivityCompat.requestPermissions(
                        this,
                        REQUIRED_PERMISSIONS,
                        REQUEST_CODE_PERMISSIONS
                ),
                "Отказ",
                this::showPermissionExplanationDialog
        );
    }

    private void showPermissionExplanationDialog() {
        DialogHelper.showBottomDialog(
                this,
                "Функция недоступна",
                "Без доступа к камере сканирование QR-кодов невозможно.",
                "Разрешить",
                () -> {
                    boolean canRequest = ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.CAMERA
                    );
                    if (canRequest) {
                        ActivityCompat.requestPermissions(
                                this,
                                REQUIRED_PERMISSIONS,
                                REQUEST_CODE_PERMISSIONS
                        );
                    } else {
                        showFinalBlockedDialog();
                    }
                },
                "Отказ",
                this::finish
        );
    }

    private void showFinalBlockedDialog() {
        DialogHelper.showBottomDialog(
                this,
                "Камера заблокирована",
                "Вы окончательно запретили доступ к камере. Чтобы использовать сканирование QR-кодов, разрешите доступ через настройки приложения.",
                "Открыть настройки",
                () -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                },
                "Отказ",
                this::finish
        );
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                if (cameraProvider == null) return;

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new android.util.Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxy);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

                focusAtPoint(previewView.getWidth() / 2f, previewView.getHeight() / 2f);
                updateFlashButton();
            } catch (Exception exc) {
                Log.e(TAG, "Use case binding failed", exc);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void updateFlashButton() {
        ImageView btnFlash = findViewById(R.id.btnFlash);
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            btnFlash.setEnabled(true);
            btnFlash.setAlpha(1f);
        } else {
            btnFlash.setEnabled(false);
            btnFlash.setAlpha(0.3f);
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageProxy(ImageProxy imageProxy) {
        if (isProcessing.get() || isFinished.get()) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        isProcessing.set(true);

        InputImage inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );

        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    Rect frame = overlayView.getScanAreaRect();
                    int previewWidth = previewView.getWidth();
                    int previewHeight = previewView.getHeight();

                    for (Barcode barcode : barcodes) {
                        if (barcode.getBoundingBox() == null) continue;

                        int centerX = barcode.getBoundingBox().centerX() * previewWidth / imageProxy.getWidth();
                        int centerY = barcode.getBoundingBox().centerY() * previewHeight / imageProxy.getHeight();

                        if (!frame.contains(centerX, centerY)) continue;

                        String value = barcode.getRawValue();
                        if (value == null) continue;

                        isFinished.set(true);

                        overlayView.highlightDetectedQRStatic();
                        vibrateDevice();

                        if (successSound != null) {
                            successSound.setOnCompletionListener(mp -> sendResultAndFinish(value));
                            successSound.start();
                        } else {
                            sendResultAndFinish(value);
                        }
                        break;
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "QR scan error", e))
                .addOnCompleteListener(task -> {
                    isProcessing.set(false);
                    imageProxy.close();
                });
    }

    private void sendResultAndFinish(String value) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("QRCodeResult", value);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void setupCustomSuccessSound() {
        SharedPreferences prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        String soundUriStr = prefs.getString("custom_qr_sound", null);
        try {
            if (soundUriStr != null) {
                Uri soundUri = Uri.parse(soundUriStr);
                successSound = MediaPlayer.create(this, soundUri);
            } else {
                successSound = MediaPlayer.create(this, R.raw.scan_success);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load custom QR sound, using default", e);
            successSound = MediaPlayer.create(this, R.raw.scan_success);
        }
    }

    private void vibrateDevice() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(120);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                boolean canRequest = ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.CAMERA
                );
                if (canRequest) {
                    showPermissionExplanationDialog();
                } else {
                    showFinalBlockedDialog();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullScreenHelper.enableFullScreen(getWindow());
        if (cameraExecutor == null || cameraExecutor.isShutdown())
            cameraExecutor = Executors.newSingleThreadExecutor();
        if (allPermissionsGranted() && camera == null) startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraExecutor != null && !cameraExecutor.isShutdown())
            cameraExecutor.shutdown();
        camera = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (barcodeScanner != null) barcodeScanner.close();
        if (successSound != null) successSound.release();
    }
}

package com.example.v_attendance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MarkAttendanceActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 1001;
    private PreviewView previewView;
    private CardView resultCard;
    private TextView tvStudentInfo, tvAttendanceStatus;
    private Button btnNextScan;
    private ExecutorService cameraExecutor;
    private DatabaseHelper dbHelper;
    private boolean isScanning = true;
    private String currentEventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        currentEventId = getIntent().getStringExtra("event_id");
        if (currentEventId == null) {
            Toast.makeText(this, "No Event Selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        previewView = findViewById(R.id.previewView);
        resultCard = findViewById(R.id.resultCard);
        tvStudentInfo = findViewById(R.id.tvStudentInfo);
        tvAttendanceStatus = findViewById(R.id.tvAttendanceStatus);
        btnNextScan = findViewById(R.id.btnNextScan);

        dbHelper = new DatabaseHelper(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE);
        }

        btnNextScan.setOnClickListener(v -> {
            resultCard.setVisibility(View.GONE);
            isScanning = true;
        });
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, new BarcodeAnalyzer());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private class BarcodeAnalyzer implements ImageAnalysis.Analyzer {
        private BarcodeScanner scanner;
        public BarcodeAnalyzer() {
            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build();
            scanner = BarcodeScanning.getClient(options);
        }
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            if (!isScanning) {
                imageProxy.close();
                return;
            }
            @SuppressWarnings("UnsafeOptInUsageError")
            android.media.Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                scanner.process(image)
                        .addOnSuccessListener(barcodes -> {
                            for (Barcode barcode : barcodes) {
                                String rawValue = barcode.getRawValue();
                                if (rawValue != null) {
                                    runOnUiThread(() -> processStudentId(rawValue));
                                    isScanning = false;
                                    break;
                                }
                            }
                        })
                        .addOnCompleteListener(task -> imageProxy.close());
            } else {
                imageProxy.close();
            }
        }
    }

    private void processStudentId(String studentId) {
        Cursor eventCursor = dbHelper.getEventById(currentEventId);
        if (!eventCursor.moveToFirst()) {
            Toast.makeText(this, "Event error", Toast.LENGTH_SHORT).show();
            eventCursor.close();
            return;
        }

        // 1. Check Deadline
        String startDateStr = eventCursor.getString(eventCursor.getColumnIndexOrThrow("start_date"));
        String endDateStr = eventCursor.getString(eventCursor.getColumnIndexOrThrow("end_date"));
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            Date startDate = sdf.parse(startDateStr);
            Date endDate = sdf.parse(endDateStr);
            Date now = new Date();

            if (now.before(startDate) || now.after(endDate)) {
                showError("OUTSIDE EVENT DATES", "Deadline reached or not yet started.\nCurrent Time: " + sdf.format(now));
                eventCursor.close();
                return;
            }
        } catch (Exception e) {
            showError("DATE ERROR", "Invalid event date format.");
            eventCursor.close();
            return;
        }

        // 2. Get Allowed Criteria
        String allowedYears = eventCursor.getString(eventCursor.getColumnIndexOrThrow("allowed_year_ids"));
        String allowedCourses = eventCursor.getString(eventCursor.getColumnIndexOrThrow("allowed_course_ids"));
        String allowedSubjects = eventCursor.getString(eventCursor.getColumnIndexOrThrow("allowed_subject_ids"));
        eventCursor.close();

        // 3. Check Student
        Cursor studentCursor = dbHelper.getStudentById(studentId);
        if (studentCursor.moveToFirst()) {
            String firstName = studentCursor.getString(studentCursor.getColumnIndexOrThrow("first_name"));
            String lastName = studentCursor.getString(studentCursor.getColumnIndexOrThrow("last_name"));
            String studentYear = String.valueOf(studentCursor.getInt(studentCursor.getColumnIndexOrThrow("year_id")));
            String studentCourse = studentCursor.getString(studentCursor.getColumnIndexOrThrow("course_id"));
            String studentSubjects = studentCursor.getString(studentCursor.getColumnIndexOrThrow("subject_ids"));

            boolean yearOk = allowedYears.isEmpty() || Arrays.asList(allowedYears.split(",")).contains(studentYear);
            boolean courseOk = allowedCourses.isEmpty() || Arrays.asList(allowedCourses.split(",")).contains(studentCourse);
            
            // Check if any of the student's subjects are allowed
            boolean subjectOk = allowedSubjects.isEmpty();
            if (!subjectOk) {
                List<String> allowedSubList = Arrays.asList(allowedSubjects.split(","));
                List<String> studentSubList = Arrays.asList(studentSubjects.split(","));
                for (String sub : studentSubList) {
                    if (allowedSubList.contains(sub)) {
                        subjectOk = true;
                        break;
                    }
                }
            }

            if (yearOk && courseOk && subjectOk) {
                long result = dbHelper.recordAttendance(studentId, currentEventId);
                resultCard.setVisibility(View.VISIBLE);
                tvStudentInfo.setText("Student: " + firstName + " " + lastName + "\nID: " + studentId);
                if (result == -2) {
                    tvAttendanceStatus.setText("Already Recorded");
                    tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
                } else {
                    tvAttendanceStatus.setText("Attendance Marked: PRESENT");
                    tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                }
            } else {
                showError("NOT ALLOWED", "Criteria mismatch (Year/Course/Subject)");
            }
        } else {
            showError("NOT FOUND", "Student ID " + studentId + " not in system.");
        }
        studentCursor.close();
    }

    private void showError(String title, String message) {
        resultCard.setVisibility(View.VISIBLE);
        tvStudentInfo.setText(title);
        tvAttendanceStatus.setText(message);
        tvAttendanceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}

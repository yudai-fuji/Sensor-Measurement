package com.example.sensormeasurement;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accel;
    private Sensor linearAccel;
    private Sensor gyroscope;
    private Sensor gameRo;
    private Sensor pressure;

    private TextView accelerometerText;
    private TextView linearAccelerometerText;
    private TextView gyroscopeText;
    private TextView gameRoText;
    private TextView pressureText;

    private Button startButton;
    private Button stopButton;
    private Button resetButton;
    private Button saveButton;

    private Chronometer stopwatch;

    private StringBuilder csvData;
    private int dataRowCount = 0;

    private float ax, ay, az;
    private float lax, lay, laz;
    private float gyx, gyy, gyz;
    private float gx, gy, gz, gw;
    private float px;

    private boolean isMeasuring = false;
    private long measurementStartTimeMs = 0L;
    private long elapsedTimeMs = 0L;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long currentElapsed;
            if (isMeasuring) {
                currentElapsed = SystemClock.elapsedRealtime() - measurementStartTimeMs;
            } else {
                currentElapsed = elapsedTimeMs;
            }

            stopwatch.setText(formatElapsedTime(currentElapsed));

            if (isMeasuring) {
                timerHandler.postDelayed(this, 10);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        csvData = new StringBuilder();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        gameRo = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        accelerometerText = findViewById(R.id.accelerometer_text);
        linearAccelerometerText = findViewById(R.id.linear_accelerometer_text);
        gyroscopeText = findViewById(R.id.gyroscope_text);
        gameRoText = findViewById(R.id.game_rotation_text);
        pressureText = findViewById(R.id.pressure_text);

        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
        resetButton = findViewById(R.id.reset_button);
        saveButton = findViewById(R.id.save_button);

        stopwatch = findViewById(R.id.stopwatch);
        stopwatch.setText("00:00:000");

        stopButton.setEnabled(false);
        resetButton.setEnabled(false);
        saveButton.setEnabled(false);

        startButton.setOnClickListener(v -> startMeasurement());
        stopButton.setOnClickListener(v -> stopMeasurement());
        resetButton.setOnClickListener(v -> resetSensorValues());

        saveButton.setOnClickListener(v -> {
            if (dataRowCount > 0) {
                saveDataToCsv();
            } else {
                Toast.makeText(this, "No data to save", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // onPauseで自動停止しない
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private void startMeasurement() {
        csvData.setLength(0);
        csvData.append("Timestamp,Sensor,X,Y,Z,W\n");
        dataRowCount = 0;

        if (sensorManager != null) {
            if (accel != null) {
                sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
            }
            if (linearAccel != null) {
                sensorManager.registerListener(this, linearAccel, SensorManager.SENSOR_DELAY_GAME);
            }
            if (gyroscope != null) {
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
            }
            if (gameRo != null) {
                sensorManager.registerListener(this, gameRo, SensorManager.SENSOR_DELAY_GAME);
            }
            if (pressure != null) {
                sensorManager.registerListener(this, pressure, SensorManager.SENSOR_DELAY_GAME);
            }
        }

        elapsedTimeMs = 0L;
        measurementStartTimeMs = SystemClock.elapsedRealtime();
        isMeasuring = true;

        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);

        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        resetButton.setEnabled(false);
        saveButton.setEnabled(false);
    }

    private void stopMeasurement() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        if (isMeasuring) {
            elapsedTimeMs = SystemClock.elapsedRealtime() - measurementStartTimeMs;
        }

        isMeasuring = false;
        timerHandler.removeCallbacks(timerRunnable);
        stopwatch.setText(formatElapsedTime(elapsedTimeMs));

        stopButton.setEnabled(false);
        resetButton.setEnabled(true);
        saveButton.setEnabled(dataRowCount > 0);
    }

    private void resetSensorValues() {
        accelerometerText.setText("Accelerometer: 待機中...");
        linearAccelerometerText.setText("Linear Acceleration: 待機中...");
        gyroscopeText.setText("Gyroscope: 待機中...");
        gameRoText.setText("Game Rotation: 待機中...");
        pressureText.setText("Pressure: 待機中...");

        csvData.setLength(0);
        dataRowCount = 0;

        elapsedTimeMs = 0L;
        measurementStartTimeMs = 0L;
        isMeasuring = false;

        timerHandler.removeCallbacks(timerRunnable);
        stopwatch.setText("00:00:000");

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        resetButton.setEnabled(false);
        saveButton.setEnabled(false);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null) {
            return;
        }

        long timestamp = event.timestamp;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                ax = event.values[0];
                ay = event.values[1];
                az = event.values[2];

                accelerometerText.setText(String.format(
                        Locale.US,
                        "Accelerometer:\nX: %.3f\nY: %.3f\nZ: %.3f",
                        ax, ay, az
                ));

                appendCsvLine(timestamp, "acc", ax, ay, az, null);
                break;

            case Sensor.TYPE_LINEAR_ACCELERATION:
                lax = event.values[0];
                lay = event.values[1];
                laz = event.values[2];

                linearAccelerometerText.setText(String.format(
                        Locale.US,
                        "Linear Acceleration:\nX: %.3f\nY: %.3f\nZ: %.3f",
                        lax, lay, laz
                ));

                appendCsvLine(timestamp, "Lacc", lax, lay, laz, null);
                break;

            case Sensor.TYPE_GYROSCOPE:
                gyx = event.values[0];
                gyy = event.values[1];
                gyz = event.values[2];

                gyroscopeText.setText(String.format(
                        Locale.US,
                        "Gyroscope:\nX: %.3f\nY: %.3f\nZ: %.3f",
                        gyx, gyy, gyz
                ));

                appendCsvLine(timestamp, "Gyro", gyx, gyy, gyz, null);
                break;

            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                float[] quaternion = new float[4];
                SensorManager.getQuaternionFromVector(quaternion, event.values);

                // getQuaternionFromVector の返り値は [w, x, y, z]
                gw = quaternion[0];
                gx = quaternion[1];
                gy = quaternion[2];
                gz = quaternion[3];

                gameRoText.setText(String.format(
                        Locale.US,
                        "Game Rotation:\nX: %.3f\nY: %.3f\nZ: %.3f\nW: %.3f",
                        gx, gy, gz, gw
                ));

                appendCsvLine(timestamp, "GameRo", gx, gy, gz, gw);
                break;

            case Sensor.TYPE_PRESSURE:
                px = event.values[0];

                pressureText.setText(String.format(
                        Locale.US,
                        "Pressure:\n%.3f hPa",
                        px
                ));

                appendCsvLine(timestamp, "pre", px, null, null, null);
                break;
        }
    }

    private void appendCsvLine(long timestamp, String sensorName,
                               Float x, Float y, Float z, Float w) {
        csvData
                .append(timestamp).append(",")
                .append(sensorName).append(",")
                .append(formatCsvValue(x)).append(",")
                .append(formatCsvValue(y)).append(",")
                .append(formatCsvValue(z)).append(",")
                .append(formatCsvValue(w)).append("\n");

        dataRowCount++;
    }

    private String formatCsvValue(Float value) {
        if (value == null) {
            return "";
        }
        return String.format(Locale.US, "%.3f", value);
    }

    private String formatElapsedTime(long elapsedMs) {
        long minutes = elapsedMs / 60000;
        long seconds = (elapsedMs % 60000) / 1000;
        long millis = elapsedMs % 1000;

        return String.format(Locale.US, "%02d:%02d:%03d", minutes, seconds, millis);
    }

    private void saveDataToCsv() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.JAPAN);
        String fileName = "SensorData_" + sdf.format(new Date()) + ".csv";

        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(dir, fileName);
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(csvData.toString());
            bw.close();

            Toast.makeText(this, "Saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.d("FileSave", "Saved to " + file.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
package com.example.phonestats;

import static android.content.ContentValues.TAG;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLES20;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SizeF;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.phonestats.databinding.ActivityMainBinding;

import java.io.File;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private ActivityMainBinding binding;
    private float megapixels;
    private float aperture;
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope, rotationVector, proximity, ambientLight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // To retrieve Manufacturer, Model Name, and Model Number
        String manufacturer = Build.MANUFACTURER;
        String modelName = Build.MODEL;
        String modelNumber = Build.DEVICE;

        // To retrieve RAM and Storage
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        double totalMemory = (double) memoryInfo.totalMem / (1024*1024*1024);
        double availableMemory = (double) memoryInfo.availMem / (1024*1024*1024);

        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        double totalSize = (double) (totalBlocks * blockSize) / (1024 * 1024 * 1024);
        double availableSize = (double) (availableBlocks * blockSize) / (1024 * 1024 * 1024);

        // To retrieve battery current charging level
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level / (float) scale * 100;

        // To retrieve Android version
        String androidVersion = Build.VERSION.RELEASE;

        // To retrieve Camera Megapixel and Aperture
        // Get the camera manager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            // Get the first camera device
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            // Get the sensor size in pixels
            SizeF sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            megapixels = sensorSize.getWidth() * sensorSize.getHeight();

            // Get the aperture value
            aperture = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)[0];

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // To retrieve Processor (CPU) Information and GPU Information
        String arch = Build.CPU_ABI + " " + Build.CPU_ABI2;
        String renderer = GLES20.glGetString(GLES20.GL_RENDERER);

        // To retrieve Live Sensor reading (GPS, Gyroscope, Barometer, Accelerometer, Rotation Vector, Proximity, Ambient light sensor)
        // Initialize sensor manager and sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        ambientLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);


        binding.infoTv.setText("Manufacturer: " + manufacturer + "\n" +
                "Model name: " + modelName + "\n" +
                "Model Number: " + modelNumber + "\n" +
                "RAM Status: " + String.format("%.2f", availableMemory) + "/" + String.format("%.2f", totalMemory) + " GB"+ "\n" +
                "ROM Status: " + String.format("%.2f", availableSize) + "/" + String.format("%.2f", totalSize) + " GB" + "\n" +
                "Battery: " + batteryPct + "\n" +
                "Android Version: " + androidVersion + "\n" +
                "Camera MegaPixel: " + String.format("%.2f", megapixels) + "\n" +
                "Camera Aperture: " + aperture + "\n" +
                "Processor (CPU) Information: " + arch + "\n" +
                "GPU Information: " + renderer + "\n"
        );
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Register sensor listeners
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, ambientLight, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister sensor listeners
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Update text views with new sensor data
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                binding.accelerometerText.setText("Accelerometer: " + Arrays.toString(event.values));
                break;
            case Sensor.TYPE_GYROSCOPE:
                binding.gyroscopeText.setText("Gyroscope: " + Arrays.toString(event.values));
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                binding.rotationVectorText.setText("Rotation Vector: " + Arrays.toString(event.values));
                break;
            case Sensor.TYPE_PROXIMITY:
                binding.proximityText.setText("Proximity: " + event.values[0] + " cm");
                break;
            case Sensor.TYPE_LIGHT:
                binding.ambientLightText.setText("Ambient Light: " + event.values[0] + " lux");
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

}
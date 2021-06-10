package com.example.gpstest;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class ImuReader implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor sensor;
    private Sensor sensorGravity;
    float EPSILON = 0.01f;
    MainActivity mainActivity;
    double directionZ = 0;
    GyroDriftCounter gyroDriftCounter = new GyroDriftCounter();

    ImuReader(MainActivity activity) {
        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        sensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, sensorGravity, SensorManager.SENSOR_DELAY_UI);
        this.mainActivity = activity;
    }

    void unregister() {
        sensorManager.unregisterListener(this);

    }

    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;

    public void onSensorChanged(SensorEvent event) {
        // Log.d("imuReader", event.sensor.getType() + " " + event.sensor.getName());

        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {

            // This timestep's delta rotation to be multiplied by the current rotation
            // after computing it from the gyro sample data.
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];
                directionZ += dT * event.values[2];
                gyroDriftCounter.onGyroEvent(dT, dT * event.values[2]);
                // Calculate the angular speed of the sample
                float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

                // Normalize the rotation vector if it's big enough to get the axis
                // (that is, EPSILON should represent your maximum allowable margin of error)
                if (omegaMagnitude > EPSILON) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Integrate around this axis with the angular speed by the timestep
                // in order to get a delta rotation from this sample over the timestep
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
                deltaRotationVector[0] = sinThetaOverTwo * axisX;
                deltaRotationVector[1] = sinThetaOverTwo * axisY;
                deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                deltaRotationVector[3] = cosThetaOverTwo;
            }
            timestamp = event.timestamp;
            float[] deltaRotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
            // User code should concatenate the delta rotation we computed with the current rotation
            // in order to get the updated rotation.
            // rotationCurrent = rotationCurrent * deltaRotationMatrix;
        }
        mainActivity.pmv.invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    class GyroDriftCounter {
        float startTime = 0;
        float endTime = 0;
        double gyroSum = 0;
        double driftRadSec = 0;

        void reset() {
            startTime = 0;
            endTime = 0;
            gyroSum = 0;
        }

        void onGyroEvent(float dt, double rotation) {//rad/s
            gyroSum += rotation;
            endTime += dt;
            driftRadSec = gyroSum / endTime;
        }
    }

}

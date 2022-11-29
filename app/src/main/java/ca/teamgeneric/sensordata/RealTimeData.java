package ca.teamgeneric.sensordata;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class RealTimeData extends Fragment implements SensorEventListener {
    final short POLL_FREQUENCY = 200;
    private long lastUpdate = -1;
    private SensorManager sensorManager;
    Sensor sensor;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor gravity;
    private Sensor magnetic;

    MainActivity mainActivity;
    TextView accX;
    TextView accY;
    TextView accZ;
    TextView gyroX;
    TextView gyroY;
    TextView gyroZ;
    TextView gravX;
    TextView gravY;
    TextView gravZ;
    TextView magX;
    TextView magY;
    TextView magZ;

    float[] accelerometerMatrix = new float[3];
    float[] gyroscopeMatrix = new float[3];
    float[] gravityMatrix = new float[3];
    float[] magneticMatrix = new float[3];

    public RealTimeData() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.data_realtime, container, false);

        mainActivity = (MainActivity)getActivity();
        mainActivity.navigationView.setCheckedItem(R.id.nav_real);

        mainActivity.setTitle("Realtime Data");

        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(MainActivity.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(MainActivity.TYPE_GYROSCOPE);
        gravity = sensorManager.getDefaultSensor(MainActivity.TYPE_GRAVITY);
        magnetic = sensorManager.getDefaultSensor(MainActivity.TYPE_MAGNETIC);

        accX = (TextView) view.findViewById(R.id.real_value_acc_x);
        accY = (TextView) view.findViewById(R.id.real_value_acc_y);
        accZ = (TextView) view.findViewById(R.id.real_value_acc_z);
        gyroX = (TextView) view.findViewById(R.id.real_value_gyro_x);
        gyroY = (TextView) view.findViewById(R.id.real_value_gyro_y);
        gyroZ = (TextView) view.findViewById(R.id.real_value_gyro_z);
        gravX = (TextView) view.findViewById(R.id.real_value_grav_x);
        gravY = (TextView) view.findViewById(R.id.real_value_grav_y);
        gravZ = (TextView) view.findViewById(R.id.real_value_grav_z);
        magX = (TextView) view.findViewById(R.id.real_value_mag_x);
        magY = (TextView) view.findViewById(R.id.real_value_mag_y);
        magZ = (TextView) view.findViewById(R.id.real_value_mag_z);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        sensor = event.sensor;

        int i = sensor.getType();
        if (i == MainActivity.TYPE_ACCELEROMETER) {
            accelerometerMatrix = event.values;
        } else if (i == MainActivity.TYPE_GYROSCOPE) {
            gyroscopeMatrix = event.values;
        } else if (i == MainActivity.TYPE_GRAVITY) {
            gravityMatrix = event.values;
        } else if (i == MainActivity.TYPE_MAGNETIC) {
            magneticMatrix = event.values;
        }

        long curTime = System.currentTimeMillis();
        long diffTime = (curTime - lastUpdate);

        if(diffTime > POLL_FREQUENCY) {
            lastUpdate = curTime;

            accX.setText(String.format("%.2f", accelerometerMatrix[0]));
            accY.setText(String.format("%.2f", accelerometerMatrix[1]));
            accZ.setText(String.format("%.2f", accelerometerMatrix[2]));
            gyroX.setText(String.format("%.2f", gyroscopeMatrix[0]));
            gyroY.setText(String.format("%.2f", gyroscopeMatrix[1]));
            gyroZ.setText(String.format("%.2f", gyroscopeMatrix[2]));
            gravX.setText(String.format("%.2f", gravityMatrix[0]));
            gravY.setText(String.format("%.2f", gravityMatrix[1]));
            gravZ.setText(String.format("%.2f", gravityMatrix[2]));
            magX.setText(String.format("%.2f", magneticMatrix[0]));
            magY.setText(String.format("%.2f", magneticMatrix[1]));
            magZ.setText(String.format("%.2f", magneticMatrix[2]));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

package ca.teamgeneric.sensordata;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.SQLException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SensorService extends Service implements SensorEventListener {

    public static final String TAG = "SensorService";

    public static final int SCREEN_OFF_RECEIVER_DELAY = 100;

    final short POLL_FREQUENCY = 9;

    private long lastUpdate = -1;
    long curTime;

    private Messenger messageHandler;

    private SensorManager sensorManager = null;
    private WakeLock wakeLock = null;
    ExecutorService executor;
    DBHelper dbHelper;
    Sensor sensor;
    Sensor accelerometer;
    Sensor gyroscope;
    Sensor gravity;
    Sensor magnetic;

    float[] accelerometerMatrix = new float[3];
    float[] gyroscopeMatrix = new float[3];
    float[] gravityMatrix = new float[3];
    float[] magneticMatrix = new float[3];

    private void registerListener() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void unregisterListener() {
        sensorManager.unregisterListener(this);
    }

    public BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive(" + intent + ")");

            if (!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                return;
            }

            Runnable runnable = new Runnable() {
                public void run() {
                    Log.i(TAG, "Runnable executing...");
                    unregisterListener();
                    registerListener();
                }
            };

            new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);
        }
    };

    public void sendMessage(String state) {
        Message message = Message.obtain();
        switch (state) {
            case "HIDE" :
                message.arg1 = 0;
                break;
            case "SHOW":
                message.arg1 = 1;
                break;
        }
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

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

        curTime = event.timestamp;

        if((curTime - lastUpdate) > POLL_FREQUENCY*1000000) {
            lastUpdate = curTime;

            try{
                Runnable insertHandler = new InsertHandler(curTime, accelerometerMatrix, gyroscopeMatrix, gravityMatrix, magneticMatrix);
                executor.execute(insertHandler);
            } catch (SQLException e) {
                Log.e(TAG, "insertData: " + e.getMessage(), e);
            }
        }
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate() {
        super.onCreate();

        dbHelper = DBHelper.getInstance(getApplicationContext());

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(MainActivity.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(MainActivity.TYPE_GYROSCOPE);
        gravity = sensorManager.getDefaultSensor(MainActivity.TYPE_GRAVITY);
        magnetic = sensorManager.getDefaultSensor(MainActivity.TYPE_MAGNETIC);

        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        registerReceiver(receiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        startForeground(Process.myPid(), new Notification());
        registerListener();
        wakeLock.acquire();

        Bundle extras = intent.getExtras();
        messageHandler = (Messenger) extras.get("MESSENGER");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        sendMessage("SHOW");

        unregisterReceiver(receiver);
        unregisterListener();

        executor.shutdown();
        Log.d(TAG, "Executor shutdown is called");

        new Thread(new Runnable() {

            public void run() {
                try {
                    while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                        Log.i(TAG, "Waiting for current tasks to finish");
                    }
                    Log.i(TAG, "No queue to clear");
                } catch (InterruptedException e) {
                    Log.e(TAG, "Exception caught while waiting for finishing executor tasks");
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }

                if (executor.isTerminated()) {
                    wakeLock.release();
                    stopForeground(true);

                    sendMessage("HIDE");
                }
            }
        }).start();
    }

    class InsertHandler implements Runnable {
        final long curTime;
        final float[] accelerometerMatrix;
        final float[] gyroscopeMatrix;
        final float[] gravityMatrix;
        final float[] magneticMatrix;

        public InsertHandler(long curTime, float[] accelerometerMatrix, float[] gyroscopeMatrix, float[] gravityMatrix, float[] magneticMatrix) {
            this.curTime = curTime;
            this.accelerometerMatrix = accelerometerMatrix;
            this.gyroscopeMatrix = gyroscopeMatrix;
            this.gravityMatrix = gravityMatrix;
            this.magneticMatrix = magneticMatrix;
        }

        public void run() {
            dbHelper.insertDataTemp(Short.parseShort(dbHelper.getTempSessionInfo("sessionNum")),
                    this.curTime,
                    this.accelerometerMatrix,
                    this.gyroscopeMatrix,
                    this.gravityMatrix,
                    this.magneticMatrix);
        }
    }
}
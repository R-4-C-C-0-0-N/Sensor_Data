package ca.teamgeneric.sensordata;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG = "MainActivity";

    NavigationView navigationView;
    Menu optionsMenu;
    Toolbar toolbar;
    DrawerLayout drawer;
    LayoutInflater inflater;
    ActionBarDrawerToggle hamburger;
    FragmentManager fragmentManager;
    SensorManager mSensorManager;
    DBHelper dbHelper;
    public Logger logger;

    public static Boolean dataRecordStarted;
    public static Boolean dataRecordCompleted;
    public static Boolean sessionCreated;

    public final static short TYPE_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    public final static short TYPE_GYROSCOPE = Sensor.TYPE_GYROSCOPE;
    public final static short TYPE_GRAVITY = Sensor.TYPE_GRAVITY;
    public final static short TYPE_MAGNETIC = Sensor.TYPE_MAGNETIC_FIELD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate called");

        setContentView(R.layout.activity_main);

        inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        String pathToExternalStorage = Environment.getExternalStorageDirectory().toString();
        File logFileDir = new File(pathToExternalStorage, "/SensorData/");
        logger = new Logger(logFileDir);

        fragmentManager = getSupportFragmentManager();

        addFragment(new NewSession(), true);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        hamburger = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(hamburger);
        hamburger.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().findItem(R.id.nav_start).setEnabled(false);
        navigationView.getMenu().findItem(R.id.nav_save).setEnabled(false);

        dbHelper = DBHelper.getInstance(this);

        dataRecordStarted = false;
        dataRecordCompleted = false;
        sessionCreated = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_settings, menu);
        optionsMenu = menu;
        return true;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called");
        dbHelper.closeDB();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (fragmentManager.getBackStackEntryCount() != 0) {
                fragmentManager.popBackStack();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_device_info){
            View deviceLayout = inflater.inflate(R.layout.window_device_info, (ViewGroup) findViewById(R.id.window_device_info));

            TextView deviceModel = (TextView) deviceLayout.findViewById(R.id.device_model_content);
            deviceModel.setText(android.os.Build.MODEL);

            TextView deviceAndroidVersion = (TextView) deviceLayout.findViewById(R.id.device_android_version_content);
            deviceAndroidVersion.setText(Build.VERSION.RELEASE);

            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

            TextView deviceAccelerometer = (TextView) deviceLayout.findViewById(R.id.device_accelerometer_content);
            deviceAccelerometer.setText(getSensorAvailable(TYPE_ACCELEROMETER));

            TextView deviceGyroscope = (TextView) deviceLayout.findViewById(R.id.device_gyroscope_content);
            deviceGyroscope.setText(getSensorAvailable(TYPE_GYROSCOPE));

            TextView deviceGravity = (TextView) deviceLayout.findViewById(R.id.device_gravity_content);
            deviceGravity.setText(getSensorAvailable(TYPE_GRAVITY));

            TextView deviceMagnetometer = (TextView) deviceLayout.findViewById(R.id.device_magnetic_content);
            deviceMagnetometer.setText(getSensorAvailable(TYPE_MAGNETIC));

            final PopupWindow popupDeviceInfo = new PopupWindow(deviceLayout, 800, 850, true);
            popupDeviceInfo.showAtLocation(deviceLayout, Gravity.CENTER, 0, 0);

            Button deviceInfoCloseButton = (Button) deviceLayout.findViewById(R.id.device_info_close);
            deviceInfoCloseButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View popupView) {
                    popupDeviceInfo.dismiss();
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_new:
                if (!sessionCreated) {
                    addFragment(new NewSession(), true);
                } else {
                    addFragment(new SessionInfo(), true);
                }
                break;
            case R.id.nav_start:
                addFragment(new StartRecording(), true);
                break;
            case R.id.nav_save:
                addFragment(new SaveRecording(), true);
                break;
            case R.id.nav_real:
                addFragment(new RealTimeData(), true);
                break;
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void addFragment(Fragment fragment, Boolean addToBackStack) {
        if (fragment != null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            if (fragmentManager.getFragments() == null || !addToBackStack) {
                fragmentTransaction.replace(R.id.fragment_container, fragment, fragment.getClass().getSimpleName()).commit();
            } else {
                fragmentTransaction.replace(R.id.fragment_container, fragment, fragment.getClass().getSimpleName()).addToBackStack(null).commit();
            }
        }
    }

    public String getSensorAvailable(short sensor_type){
        Sensor curSensor = mSensorManager.getDefaultSensor(sensor_type);
        if (curSensor != null){
            return("Available  " + "(" + curSensor.getVendor() + ")");
        } else {
            return("Not available");
        }
    }
}

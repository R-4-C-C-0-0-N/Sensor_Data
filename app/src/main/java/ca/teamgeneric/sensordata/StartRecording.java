package ca.teamgeneric.sensordata;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class StartRecording extends Fragment implements View.OnClickListener {

    public static final String TAG = "StartRecording";

    Button startButton;
    CoordinatorLayout coordinatorLayout;
    DBHelper dbHelper;
    TextView recordProgressMessage;
    static MainActivity mainActivity;
    static ProgressDialog stopDialog;

    public StartRecording() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.recording_start, container, false);

        coordinatorLayout = (CoordinatorLayout) getActivity().findViewById(R.id.coordinator_layout);

        mainActivity = (MainActivity)getActivity();
        mainActivity.navigationView.setCheckedItem(R.id.nav_start);

        mainActivity.setTitle("Start Data Recording");

        dbHelper = DBHelper.getInstance(getActivity());

        recordProgressMessage = (TextView) view.findViewById(R.id.start_recording_progress);
        TextView sessionNum = (TextView) view.findViewById(R.id.start_value_sessionNum);
        sessionNum.setText(dbHelper.getTempSessionInfo("sessionNum"));

        startButton = (Button) view.findViewById(R.id.startButton);
        startButton.setOnClickListener(this);

        if(MainActivity.dataRecordStarted){
            if(MainActivity.dataRecordCompleted){
                startButton.setEnabled(false);
                startButton.setText(R.string.start_button_label_stop);
            } else {
                startButton.setEnabled(true);
                startButton.setText(R.string.start_button_label_stop);
            }
        } else {
            startButton.setEnabled(true);
            startButton.setText(R.string.start_button_label_start);
        }

        return view;
    }

    @Override
    public void onClick(View v) {
        if (!MainActivity.dataRecordStarted){
            try{
                mainActivity.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                mainActivity.hamburger.setDrawerIndicatorEnabled(false);
                mainActivity.hamburger.setHomeAsUpIndicator(new DrawerArrowDrawable(getActivity()));
                mainActivity.hamburger.syncState();

                mainActivity.optionsMenu.setGroupEnabled(0, false);

                recordProgressMessage.setText(R.string.start_recording_progress);
                MainActivity.dataRecordStarted = true;
                startButton.setText(R.string.start_button_label_stop);

                dbHelper.setStartTime(Short.parseShort(dbHelper.getTempSessionInfo("sessionNum")), System.currentTimeMillis());

                Intent startService = new Intent(mainActivity, SensorService.class);
                startService.putExtra("MESSENGER", new Messenger(messageHandler));
                getContext().startService(startService);

                Snackbar.make(coordinatorLayout, R.string.start_recording, Snackbar.LENGTH_SHORT).show();

            } catch (SQLException e){
                mainActivity.logger.e(getActivity(),TAG, "SQL error insertSession()", e);
            }
        } else {
            MainActivity.dataRecordCompleted = true;
            startButton.setEnabled(false);
            recordProgressMessage.setText("");

            mainActivity.stopService(new Intent(mainActivity, SensorService.class));

            mainActivity.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            mainActivity.hamburger.setDrawerIndicatorEnabled(true);
            mainActivity.hamburger.syncState();

            mainActivity.optionsMenu.setGroupEnabled(0, true);

            Snackbar.make(coordinatorLayout, R.string.start_recording_complete, Snackbar.LENGTH_SHORT).show();
        }
    }

    public static Handler messageHandler = new MessageHandler();

    public static class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            int state = message.arg1;
            switch (state) {
                case 0:
                    stopDialog.dismiss();
                    Log.d(TAG, "Stop dialog dismissed");
                    break;

                case 1:
                    stopDialog = new ProgressDialog(mainActivity);
                    stopDialog.setTitle("Stopping sensors");
                    stopDialog.setMessage("Please wait...");
                    stopDialog.setProgressNumberFormat(null);
                    stopDialog.setCancelable(false);
                    stopDialog.setMax(100);
                    stopDialog.show();
                    Log.d(TAG, "Stop dialog displayed");
                    break;
            }
        }
    }
}

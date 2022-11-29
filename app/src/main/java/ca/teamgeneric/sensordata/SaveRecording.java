package ca.teamgeneric.sensordata;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class SaveRecording extends Fragment implements View.OnClickListener {

    private static final String TAG = "SaveRecording";

    boolean exportDataCSV = true;

    Button saveButton;
    TextView explanationText;
    MainActivity mainActivity;
    CoordinatorLayout coordinatorLayout;
    DBHelper dbHelper;
    Boolean sessionDataExists;
    MediaScanner mediaScanner;
    static ProgressDialog dialog;

    public SaveRecording() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.recording_save, container, false);
        coordinatorLayout = (CoordinatorLayout) getActivity().findViewById(R.id.coordinator_layout);

        mainActivity = (MainActivity) getActivity();
        mainActivity.navigationView.setCheckedItem(R.id.nav_save);

        mainActivity.setTitle("Save & Quit Session");

        explanationText = (TextView) view.findViewById(R.id.saveExplanationText);

        saveButton = (Button) view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(this);

        dbHelper = DBHelper.getInstance(getActivity(), new DatabaseHandler());

        if(MainActivity.dataRecordStarted & !MainActivity.dataRecordCompleted){
            explanationText.setText(getResources().getString(R.string.save_message_recording));
            saveButton.setEnabled(false);
        } else {
            saveButton.setEnabled(true);

            sessionDataExists = dbHelper.checkSessionDataExists(Short.parseShort(dbHelper.getTempSessionInfo("sessionNum")));

            if (sessionDataExists) {
                explanationText.setText(getResources().getString(R.string.save_instruction_text));
                saveButton.setText(getResources().getString(R.string.save_button_text));
            } else {
                explanationText.setText(getResources().getString(R.string.save_no_data_text));
                saveButton.setText(getResources().getString(R.string.save_button_text_quit));
            }
        }

        return view;
    }

    @Override
    public void onClick(View v) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mainActivity);

        if (sessionDataExists) {
            alertDialogBuilder.setTitle("Save & quit session?");
            alertDialogBuilder.setMessage("Are you sure you want to save the data and quit the current session?");
        } else {
            alertDialogBuilder.setTitle("Quit?");
            alertDialogBuilder.setMessage("Are you sure you want to quit the current session? \n\n No data will be saved.");
        }

        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (sessionDataExists) {
                    new ExportDatabaseCSVTask().execute();
                } else {
                    quitSession();
                }
            }
        });

        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog quitAlertDialog = alertDialogBuilder.create();
        quitAlertDialog.show();
    }

    private void quitSession(){
        Intent intent = new Intent(getActivity(), RootActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        getActivity().finishAffinity();
    }

    private static class DatabaseHandler extends Handler {
        @Override
        public void handleMessage (Message msg){
            Double progressPercent = (Double) msg.obj;

            Integer progressValue = 40 + (int) Math.ceil(progressPercent/2);

            dialog.setProgress(progressValue);
        }
    }

    public class ExportDatabaseCSVTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(mainActivity);
            dialog.setTitle("Saving data");
            dialog.setMessage("Please wait...");
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setProgressNumberFormat(null);
            dialog.setCancelable(false);
            dialog.setMax(100);
            dialog.show();
        }

        protected Boolean doInBackground(final String... args) {

            String pathToExternalStorage = Environment.getExternalStorageDirectory().toString();
            File exportDir = new File(pathToExternalStorage, "/SensorData");
            File sessionDataDir = new File(exportDir, "/Sessions");

            publishProgress(5);
            SystemClock.sleep(100);

            if (!exportDir.exists()) {
                Boolean created = exportDir.mkdirs();
                mainActivity.logger.i(getActivity(), TAG, "Export Dir created: " + created);
            }

            publishProgress(10);
            SystemClock.sleep(100);

            if (!sessionDataDir.exists()) {
                Boolean created = sessionDataDir.mkdirs();
                mainActivity.logger.i(getActivity(), TAG, "Session Dir created: " + created);
            }

            publishProgress(15);
            SystemClock.sleep(100);

            if (exportDir.exists() && sessionDataDir.exists()) {
                try {
                    dbHelper.copyTempData();

                    publishProgress(20);
                    SystemClock.sleep(200);

                    File data = Environment.getDataDirectory();
                    String currentDBPath = "//data//ca.teamgeneric.sensordata//databases//" + DBHelper.DATABASE_NAME;
                    File currentDB = new File(data, currentDBPath);
                    File destDB = new File(exportDir, DBHelper.DATABASE_NAME);

                    publishProgress(25);
                    SystemClock.sleep(100);

                    if (exportDir.canWrite()) {
                        if (currentDB.exists()) {
                            FileChannel src = new FileInputStream(currentDB).getChannel();
                            FileChannel dst = new FileOutputStream(destDB).getChannel();
                            dst.transferFrom(src, 0, src.size());
                            src.close();
                            dst.close();
                        }
                    }

                    publishProgress(35);
                    SystemClock.sleep(300);

                    File trackingSheet = new File(exportDir, "trackingSheet.csv");

                    try {
                        dbHelper.exportTrackingSheet(trackingSheet);
                    } catch (SQLException | IOException e) {
                        mainActivity.logger.e(getActivity(), TAG, "exportTrackingSheet error", e);
                    }

                    publishProgress(40);
                    SystemClock.sleep(300);

                    if(exportDataCSV) {
                        String sessionNum = dbHelper.getTempSessionInfo("sessionNum");
                        File subjectFile = new File(sessionDataDir, sessionNum + ".csv");

                        try {
                            dbHelper.exportSessionData(subjectFile, sessionNum);
                        } catch (SQLException | IOException e) {
                            mainActivity.logger.e(getActivity(), TAG, "exportSessionData error", e);
                        }
                    }

                    publishProgress(90);
                    SystemClock.sleep(300);

                    List<String> fileList = getListFiles(exportDir);
                    String[] allFiles = new String[fileList.size()];
                    allFiles = fileList.toArray(allFiles);

                    mediaScanner = new MediaScanner();

                    try{
                        mediaScanner.scanFile(getContext(), allFiles, null, mainActivity.logger);
                    } catch (Exception e) {
                        mainActivity.logger.e(getActivity(), TAG, "Media scanner exception", e);
                    }

                    publishProgress(100);
                    SystemClock.sleep(400);

                    return true;
                } catch (SQLException | IOException e) {
                    mainActivity.logger.e(getActivity(), TAG, "Save data exception", e);

                    Snackbar.make(coordinatorLayout, "Error: " + e.getMessage(), Snackbar.LENGTH_INDEFINITE).setAction(R.string.snackbar_dismiss, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                        }
                    }).show();
                    return false;
                }
            } else {
                if (!exportDir.exists()) {
                    mainActivity.logger.e(getActivity(), TAG, "Data directory not found");

                    Snackbar.make(coordinatorLayout, "Error: Data directory doesn't exist", Snackbar.LENGTH_INDEFINITE).setAction(R.string.snackbar_dismiss, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                        }
                    }).show();
                } else if (!sessionDataDir.exists()) {
                    mainActivity.logger.e(getActivity(), TAG, "Session directory not found");

                    Snackbar.make(coordinatorLayout, "Error: Session directory doesn't exist", Snackbar.LENGTH_INDEFINITE).setAction(R.string.snackbar_dismiss, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                        }
                    }).show();
                }

                return false;
            }
        }

        public void onProgressUpdate(Integer ... progress){
            dialog.setProgress(progress[0]);
            if (progress[0] == 100){
                dialog.setMessage("Quitting...");
            }
        }

        protected void onPostExecute(final Boolean success) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (success) {
                quitSession();
            }
        }

        private List<String> getListFiles(File parentDir) {
            ArrayList<String> inFiles = new ArrayList<>();
            File[] files = parentDir.listFiles();

            for (File file : files) {
                if (file.isDirectory()) {
                    inFiles.addAll(getListFiles(file));
                } else {
                    inFiles.add(file.getAbsolutePath());
                }
            }
            return inFiles;
        }
    }
}


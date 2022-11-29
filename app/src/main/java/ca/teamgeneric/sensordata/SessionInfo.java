package ca.teamgeneric.sensordata;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class SessionInfo extends Fragment implements View.OnClickListener{

    public static final String TAG = "SessionInfo";

    MainActivity mainActivity;
    Button deleteButton;
    TextView deleteMessage;
    DBHelper dbHelper;
    ProgressDialog dialog;
    CoordinatorLayout coordinatorLayout;

    public SessionInfo() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.session_info, container, false);

        coordinatorLayout = (CoordinatorLayout) getActivity().findViewById(R.id.coordinator_layout);

        mainActivity = (MainActivity) getActivity();
        mainActivity.navigationView.setCheckedItem(R.id.nav_new);

        mainActivity.setTitle("Session Information");

        deleteButton = (Button) view.findViewById(R.id.sessionInfo_button_delete);
        deleteButton.setOnClickListener(this);
        deleteMessage = (TextView) view.findViewById(R.id.sessionInfo_delete_message);

        if(MainActivity.dataRecordStarted & !MainActivity.dataRecordCompleted){
            deleteButton.setEnabled(false);
            deleteMessage.setText(R.string.sessionInfo_message_recording);
        } else {
            deleteButton.setEnabled(true);
            deleteMessage.setText("");
        }

        dbHelper = DBHelper.getInstance(getActivity());

        TextView date = (TextView) view.findViewById(R.id.sessionInfo_value_date);
        TextView sessionNum = (TextView) view.findViewById(R.id.sessionInfo_value_sessionNum);

        date.setText(dbHelper.getTempSessionInfo("date"));
        sessionNum.setText(dbHelper.getTempSessionInfo("sessionNum"));

        return view;
    }

    @Override
    public void onClick(View v) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mainActivity);
        alertDialogBuilder.setTitle("Delete?");
        alertDialogBuilder.setMessage("Are you sure you want to delete the current session?\n\n This action is irreversible.");

        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                new DeleteSessionTask().execute();
            }
        });

        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog deleteAlertDialog = alertDialogBuilder.create();
        deleteAlertDialog.show();
    }

    public class DeleteSessionTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(mainActivity);
            dialog.setTitle("Delete session");
            dialog.setMessage("Please wait...");
            dialog.setCancelable(false);
            dialog.show();
        }

        protected Boolean doInBackground(final String... args) {
            try{
                mainActivity.logger.i(getActivity(), TAG, "Session deleted: #" + dbHelper.getTempSessionInfo("sessionNum"));

                dbHelper.deleteSession();

                MainActivity.sessionCreated = false;

                return true;
            } catch (SQLException e){
                mainActivity.logger.e(getActivity(), TAG, "SQL error deleteSession()",e);

                Snackbar.make(coordinatorLayout, "Error: " + e.getMessage(), Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.snackbar_dismiss, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                            }
                        }).show();

                return false;
            }
        }

        protected void onPostExecute(final Boolean success) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (success) {
                Snackbar.make(coordinatorLayout, "Session deleted", Snackbar.LENGTH_SHORT).show();
                mainActivity.recreate();
            }
        }
    }

}

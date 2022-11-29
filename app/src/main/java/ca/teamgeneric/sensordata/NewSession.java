package ca.teamgeneric.sensordata;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NewSession extends Fragment implements View.OnClickListener {

    public static final String TAG = "NewSession";

    MainActivity mainActivity;
    CoordinatorLayout coordinatorLayout;
    DBHelper dbHelper;

    TextInputLayout sessionNumWrapper;
    Button createButton;

    public NewSession() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        coordinatorLayout = (CoordinatorLayout) getActivity().findViewById(R.id.coordinator_layout);
        View view = inflater.inflate(R.layout.new_session, container, false);

        mainActivity = (MainActivity) getActivity();
        mainActivity.navigationView.setCheckedItem(R.id.nav_new);

        mainActivity.setTitle("New Session");

        dbHelper = DBHelper.getInstance(getActivity());

        sessionNumWrapper = (TextInputLayout) view.findViewById(R.id.input_sessionnum_wrapper);

        createButton = (Button) view.findViewById(R.id.input_submit);
        createButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        String sessionNum = sessionNumWrapper.getEditText().getText().toString();

        if (!isEmpty(sessionNum)) {
            sessionNumWrapper.setError(null);

            if(!dbHelper.checkSessionExists(Short.parseShort(sessionNum))){
                MainActivity.sessionCreated = true;

                dbHelper.insertSessionTemp(
                        Short.parseShort(sessionNum),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(new Date()),
                        0
                );

                showKeyboard(false, mainActivity);

                mainActivity.navigationView.getMenu().findItem(R.id.nav_start).setEnabled(true);
                mainActivity.navigationView.getMenu().findItem(R.id.nav_save).setEnabled(true);
                mainActivity.navigationView.getMenu().findItem(R.id.nav_new).setTitle("Session Info");

                Snackbar.make(coordinatorLayout, "Session created", Snackbar.LENGTH_SHORT).show();
                mainActivity.logger.i(getActivity(),TAG, "Session #" + sessionNum + " created");

                mainActivity.addFragment(new SessionInfo(), false);
            } else {
                Snackbar.make(coordinatorLayout,"Session number already exists...", Snackbar.LENGTH_SHORT).show();
                sessionNumWrapper.requestFocus();
            }
        } else {

            if (isEmpty(sessionNum)) {
                sessionNumWrapper.setError("Session number required");
            } else {
                sessionNumWrapper.setError(null);
            }
        }
    }

    public boolean isEmpty(String string) {
        return string.equals("");
    }

    public void showKeyboard(Boolean show, MainActivity mainActivity){
        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (show){
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        } else {
            View v = mainActivity.getCurrentFocus();
            if (v != null){
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
    }

}

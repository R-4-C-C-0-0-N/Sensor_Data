package ca.teamgeneric.sensordata;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class RootActivity extends AppCompatActivity {

    Button proceedButton;

    final String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isTaskRoot() && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER) && getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_MAIN)) {

            finish();
            return;
        }

        setContentView(R.layout.activity_root);
        proceedButton = (Button) findViewById(R.id.btn_proceed);
        showItems(true);

        if (!hasPermission(PERMISSIONS[0]) || !hasPermission(PERMISSIONS[1]) || !hasPermission(PERMISSIONS[2])){

            ActivityCompat.requestPermissions(this, PERMISSIONS, 10);
        }
    }

    public boolean hasPermission(String permission){
        return (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 0 || !arePermissionsGranted(grantResults)){
            Snackbar.make(findViewById(android.R.id.content), R.string.permission_explain, Snackbar.LENGTH_INDEFINITE).setAction(R.string.snackbar_request_permission, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityCompat.requestPermissions(RootActivity.this, PERMISSIONS, 10);
                }
            }).show();

            showItems(false);

        } else {
            showItems(true);
        }
    }

    public void showItems(Boolean show){
        if (show){
            proceedButton.setEnabled(true);
            proceedButton.setAlpha(1);
        } else {
            proceedButton.setEnabled(false);
            proceedButton.setAlpha(0.3f);
        }
    }

    public boolean arePermissionsGranted(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void authenticateUser(View view) {
        Intent intent = new Intent(RootActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
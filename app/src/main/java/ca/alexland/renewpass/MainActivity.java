package ca.alexland.renewpass;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import ca.alexland.renewpass.schools.School;
import ca.alexland.renewpass.schools.SimonFraserUniversity;
import ca.alexland.renewpass.views.LoadingFloatingActionButton;
import ca.alexland.renewpass.utils.DrawableUtil;
import ca.alexland.renewpass.utils.PreferenceHelper;
import ca.alexland.renewpass.utils.UPassLoader;

public class MainActivity extends AppCompatActivity {
    PreferenceHelper preferenceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.preferenceHelper = new PreferenceHelper(getApplicationContext());

        doFirstRun();

        final LoadingFloatingActionButton loadingFab = (LoadingFloatingActionButton) findViewById(R.id.loading_fab);
        loadingFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRenew(loadingFab);
            }
        });
        Drawable completeIcon = loadingFab.getCompleteIconDrawable();
        if (completeIcon != null) {
            DrawableUtil.tint(completeIcon, Color.WHITE);
        }

        Drawable failureIcon = loadingFab.getFailureIconDrawable();
        if (failureIcon != null) {
            DrawableUtil.tint(failureIcon, Color.WHITE);
        }
    }

    private void doFirstRun() {
        boolean firstRun = preferenceHelper.getFirstRun();
        if (firstRun) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Add Username");

            final EditText usernameInput = new EditText(this);
            usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(usernameInput)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferenceHelper.addUsername(usernameInput.getText().toString());
                        makePasswordPopup();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
            }}).show();

            if (preferenceHelper.credentialsEntered()) {
                preferenceHelper.setFirstRun(false);
            }
        }
    }

    private void makePasswordPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Add Password");

        final EditText passwordInput = new EditText(this);
        passwordInput.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(passwordInput)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferenceHelper.addPassword(passwordInput.getText().toString());
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }}).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem settingsItem = menu.findItem(R.id.action_settings);
        Drawable settingsIcon = DrawableCompat.wrap(settingsItem.getIcon());
        DrawableCompat.setTint(settingsIcon, Color.WHITE);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onRenew(LoadingFloatingActionButton fab) {
        UPassLoader mService = new UPassLoader();
        School school = new SimonFraserUniversity();
        String username = preferenceHelper.getUsername();
        String password = preferenceHelper.getPassword();
        mService.renewUPass(fab, school, username, password);
    }
}

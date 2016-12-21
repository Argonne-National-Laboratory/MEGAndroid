package gov.anl.coar.meg;

import android.os.Bundle;
import android.support.annotation.BoolRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Button;

import gov.anl.coar.meg.R;

public class AdvancedOptions extends AppCompatActivity implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
    public static final String TAG = "Advanced Options";

    EditText etPrivateKey;
    EditText etServer;
    EditText etUpdateInterval;
    int selectedUpdateInterval;
    Switch swSyncContacts;
    Switch swUpdateOnWifi;
    Switch swOOB;
    Button bSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_options);

        //Set references to visual items
        etPrivateKey = (EditText) findViewById(R.id.etPrivateKey);
        etServer = (EditText) findViewById(R.id.etServer);
        etUpdateInterval = (EditText) findViewById(R.id.etUpdateInterval);
        swSyncContacts = (Switch) findViewById(R.id.swSyncContacts);
        swUpdateOnWifi = (Switch) findViewById(R.id.swUpdateOnWifi);
        swOOB = (Switch) findViewById(R.id.swOOB);
        bSave = (Button) findViewById(R.id.bSave);


        //Populate with saved settings
        etServer.setText(Util.getConfigVar(getApplicationContext(), Constants.MEG_SERVER_FILENAME));
        updateIntervalSelection(Integer.valueOf( Util.getConfigVar(getApplicationContext(), Constants.UPDATE_INTERVAL_FILENAME)));
        swSyncContacts.setChecked(Boolean.valueOf(Util.getConfigVar(getApplicationContext(), Constants.SYNC_CONTACTS_FILENAME)));
        swUpdateOnWifi.setChecked(Boolean.valueOf(Util.getConfigVar(getApplicationContext(), Constants.UPDATE_ON_WIFI_FILENAME)));
        swOOB.setChecked((Boolean.valueOf(Util.getConfigVar(getApplicationContext(),Constants.OOB_FILENAME))));


        //Set this class as listener for visual items
        etUpdateInterval.setOnClickListener(this);
        bSave.setOnClickListener(this);
    }

    private void updateIntervalSelection(int id) {
        selectedUpdateInterval = id;
        switch(selectedUpdateInterval) {
            case (R.id.m5minutes):
                etUpdateInterval.setText("5 Minutes");
                break;
            case (R.id.m10minutes):
                etUpdateInterval.setText("10 Minutes");
                break;
            case (R.id.m30minutes):
                etUpdateInterval.setText("30 Minutes");
                break;
            default:
                Log.d(TAG, "Unknown options selected");
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        //Update selected text based on selected choice
        updateIntervalSelection(item.getItemId());
        return true;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            //Inflate popup menu for update interval choices
            case (R.id.etUpdateInterval):
                PopupMenu popupMenu = new PopupMenu(this, v);
                popupMenu.setOnMenuItemClickListener(this);
                popupMenu.inflate(R.menu.menu_adv_options_key_update_intervals);
                popupMenu.show();
                break;
            //Write out new info to save files if we hit save
            case (R.id.bSave):
                Util.writeConfigVarToFile(getApplicationContext(), Constants.MEG_SERVER_FILENAME, etServer.getText().toString());
                Util.writeConfigVarToFile(getApplicationContext(), Constants.UPDATE_INTERVAL_FILENAME, String.valueOf(selectedUpdateInterval));
                Util.writeConfigVarToFile(getApplicationContext(), Constants.SYNC_CONTACTS_FILENAME, String.valueOf(swSyncContacts.isChecked()));
                Util.writeConfigVarToFile(getApplicationContext(), Constants.UPDATE_ON_WIFI_FILENAME, String.valueOf(swUpdateOnWifi.isChecked()));
                Util.writeConfigVarToFile(getApplicationContext(), Constants.OOB_FILENAME, String.valueOf(swOOB.isChecked()));
                finish();
                break;
            default:
                Log.d(TAG, "Unknown click sender");
        }
    }

}

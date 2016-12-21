package gov.anl.coar.meg;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import gov.anl.coar.meg.R;

public class AdvancedOptions extends AppCompatActivity implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
    public static final String TAG = "Advanced Options";

    EditText etPrivateKey;
    EditText etServer;
    EditText etUpdateInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_options);

        //Set references to visual items
        etPrivateKey = (EditText) findViewById(R.id.etPrivateKey);
        etServer = (EditText) findViewById(R.id.etServer);
        etUpdateInterval = (EditText) findViewById(R.id.etUpdateInterval);

        //Set this class as listener for popup selections
        etUpdateInterval.setOnClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        //Update selected text based on selected choice
        switch(item.getItemId()) {
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
            default:
                Log.d(TAG, "Unknown click sender");
        }
    }

}

package gov.anl.coar.meg;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Joshua Lyle on 7/21/16.
 */

public class QRManageActivity extends AppCompatActivity implements ListView.OnItemClickListener, NameSymKeyDialog.NameSymKeyListener {

    ListView keyListView;
    ArrayAdapter<String> keyAdapter;
    List<String> symKeyFiles;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Get intent info and check if it holds the clientID coming from a scan
        Bundle scanInfo = null;
        if (data != null)
            scanInfo = data.getExtras();

        try {
            if (scanInfo != null && scanInfo.getString("action").toString().equals("nameClientKey")) {
                //Remove the action so the dialog doesn't show up multiple times
                getIntent().removeExtra("action");

                //Create the dialog to name the new client ID
                FragmentManager fm = getFragmentManager();
                NameSymKeyDialog nskd = new NameSymKeyDialog();

                //Pass along the clientID to the dialog and show the dialog
                Bundle b = new Bundle();
                String clientID = scanInfo.getString("clientID");
                b.putString("clientID", clientID);
                nskd.setArguments(b);
                nskd.show(fm, "Show Sym Key Naming Dialog");
            }
        }
        catch (Exception e) {
            Log.d("QRManageActivity", "Exception because of action comparison");
        }
    }

    //Called by NameSymKeyDialog
    public void onNameSymKey(String name, String clientID) {
        //Show the name it got
        Log.d("QRManage", "Got name  ".concat(name));
        Toast.makeText(getBaseContext(), "Added ".concat(name), Toast.LENGTH_SHORT).show();

        //Write out the name to file and repopulate the list
        try {
            Util.writeSymmetricKeyNameFile(getApplicationContext(), name, clientID);
            populateList();
        }
        catch (IOException e) {
            Log.d("QRManage", "Failed to write name file");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrmanage);
        Toast.makeText(getApplicationContext(), "Tap to remove key", Toast.LENGTH_SHORT).show();
        populateList();
    }

    public void populateList() {
        //Setup array to hold sym keys
        symKeyFiles = new ArrayList<String>();
        symKeyFiles.add("+ Add Key");

        //Populate symKeyFiles with symmetric key file names
        File[] files = getApplicationContext().getFilesDir().listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().contains(".id")) {
                symKeyFiles.add(files[i].getName().substring(0, files[i].getName().length()-3));
                Log.d("QRManage", "Added:  ".concat(files[i].getName()));
            }
        }

        //Create list
        keyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, symKeyFiles);
        keyListView = (ListView) findViewById(R.id.lvQRKeys);
        keyListView.setAdapter(keyAdapter);

        //Respond to clicks in this activity
        keyListView.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //If adding a key, open the scanner
        if (position == 0) {
            startActivityForResult(new Intent(this, ScanQRActivity.class), 0);
        }
        //Otherwise delete the selected key
        else {
            //Delete symmetric key
            String symmetricKeyClientName = String.valueOf(parent.getItemAtPosition(position));
            Util.deleteSymmetricKeyFile(getBaseContext(), symmetricKeyClientName);

            //Remove the key from the list
            symKeyFiles.remove(position);

            //Show the changes to the list
            keyAdapter.notifyDataSetChanged();
            Toast.makeText(view.getContext(), "Removed ".concat(symmetricKeyClientName), Toast.LENGTH_SHORT).show();
        }
    }
}

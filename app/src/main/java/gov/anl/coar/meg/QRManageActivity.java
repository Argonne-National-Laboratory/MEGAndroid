package gov.anl.coar.meg;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Joshua Lyle on 7/21/16.
 */

public class QRManageActivity extends AppCompatActivity implements ListView.OnItemClickListener {

    ListView keyListView;
    ArrayAdapter<String> keyAdapter;
    List<String> symKeyFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrmanage);

        //Setup array to hold sym keys
        symKeyFiles = new ArrayList<String>();

        //Populate symKeyFiles with symmetric key file names
        File[] files = getApplicationContext().getFilesDir().listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().contains(".sym")) {
                symKeyFiles.add(files[i].getName());
                Log.d("QRManage", "Added:  ".concat(files[i].getName()));
            }
        }

        //Create list
        keyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, symKeyFiles);
        keyListView = (ListView) findViewById(R.id.lvQRKeys);
        keyListView.setAdapter(keyAdapter);

        keyListView.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Delete symmetric key
        String symmetricKeyClientId = String.valueOf(parent.getItemAtPosition(position));
        Util.deleteSymmetricKeyFile(getApplicationContext(), symmetricKeyClientId);

        //Remove the key from the list
        symKeyFiles.remove(position);

        //Show the changes to the list
        keyAdapter.notifyDataSetChanged();
        Toast.makeText(view.getContext(), "Removed ".concat(symmetricKeyClientId), Toast.LENGTH_SHORT).show();

    }
}

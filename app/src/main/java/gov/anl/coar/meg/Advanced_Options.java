package gov.anl.coar.meg;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.spongycastle.jce.exception.ExtCertPathBuilderException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import gov.anl.coar.meg.receiver.MEGResultReceiver;
import gov.anl.coar.meg.receiver.MEGResultReceiver.Receiver;
import gov.anl.coar.meg.receiver.ReceiverCode;
import gov.anl.coar.meg.service.KeyRevocationService;

/** Class to provide functionality to the Advanced Options page of MEG
 *
 */
public class Advanced_Options extends AppCompatActivity
        implements View.OnClickListener, Receiver {
    Button bDebugRemoveAES;
    Button bRevokePGP;
    Button bSave;
    Button bBackup;

    Intent mKeyRevocationService;
    MEGResultReceiver mReceiver;

	/** Default method */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_options);

        bRevokePGP = (Button) findViewById(R.id.bRevoke);
        bRevokePGP.setOnClickListener(this);
        bDebugRemoveAES = (Button) findViewById(R.id.bDebugRevokeAES);
        bDebugRemoveAES.setOnClickListener(this);
        bSave = (Button) findViewById(R.id.bSave);
        bSave.setOnClickListener(this);
        bBackup = (Button) findViewById(R.id.bBackup);
        bBackup.setOnClickListener(this);
    }

    private AlertDialog alertBuilder(int messageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(messageId);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
        });
        return builder.create();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bRevoke: {
                mReceiver = new MEGResultReceiver(new Handler());
                mReceiver.setReceiver(this);
                mKeyRevocationService = new Intent(this, KeyRevocationService.class);
                mKeyRevocationService.putExtra(Constants.RECEIVER_KEY, mReceiver);
                startService(mKeyRevocationService);
                break;
            }
            case R.id.bSave: {
                startActivity(new Intent(this, Installation.class));
                break;
            }
            case R.id.bDebugRevokeAES: {
                Util.removeAESKey(this);
            }
            case R.id.bBackup: {
                try {
                    backupKey(v.getContext());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void backupKey(Context context) throws Exception {
        try {
            String state = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(state)) {
                throw new Exception("External storage is not writable!");
            }
            File backupFile = new File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    Constants.SECRETKEYRING_FILENAME);
            FileInputStream fis = context.openFileInput(Constants.SECRETKEYRING_FILENAME);
            FileOutputStream fos = new FileOutputStream(backupFile);
            fos.write(Util.inputStreamToOutputStream(fis).toByteArray());
            fis.close();
            fos.close();
        } catch (FileNotFoundException e) {
            // TODO Something Something you didn't create the key yet.
            e.printStackTrace();
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        // The user can try again. We don't want to continually serve them
        // up error warnings
        if (resultCode != ReceiverCode.IID_CODE_SUCCESS) {
            alertBuilder(resultData.getInt(Constants.ALERT_MESSAGE_KEY)).show();
        }
        stopService(mKeyRevocationService);
    }
}

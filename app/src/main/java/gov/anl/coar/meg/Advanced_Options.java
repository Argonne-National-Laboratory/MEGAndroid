package gov.anl.coar.meg;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import gov.anl.coar.meg.receiver.MEGResultReceiver;
import gov.anl.coar.meg.receiver.MEGResultReceiver.Receiver;
import gov.anl.coar.meg.receiver.ReceiverCode;
import gov.anl.coar.meg.service.KeyRevocationService;

/** Class to provide functionality to the Advanced Options page of MEG
 *
 * @author Bridget Basan
 * @author Joshua Lyle
 */
public class Advanced_Options extends AppCompatActivity
        implements View.OnClickListener, Receiver {
    Button bDebugRemoveAES;
    Button bRevokePGP;
    Button bSave;

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

                // Popup message to let the know user know the email was sent
                Toast.makeText(v.getContext(), "Sent Revocation Confirmation Email", Toast.LENGTH_SHORT).show();

                break;
            }
            case R.id.bSave: {
                startActivity(new Intent(this, Installation.class));
                break;
            }
            case R.id.bDebugRevokeAES: {
                Util.removeAESKey(this);
            }
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultCode != ReceiverCode.IID_CODE_SUCCESS) {
            alertBuilder(resultData.getInt(Constants.ALERT_MESSAGE_KEY)).show();
        }
        // The user can try again. We don't want to continually serve them
        // up error warnings
        stopService(mKeyRevocationService);
    }
}

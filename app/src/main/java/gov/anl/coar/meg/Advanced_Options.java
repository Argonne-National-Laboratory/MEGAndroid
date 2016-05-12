package gov.anl.coar.meg;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.spongycastle.openpgp.PGPPublicKey;

import gov.anl.coar.meg.http.MEGServerRequest;
import gov.anl.coar.meg.pgp.MEGPublicKeyRing;

/** Class to provide functionality to the Advanced Options page of MEG
 *
 * @author Bridget Basan
 */
public class Advanced_Options extends AppCompatActivity implements View.OnClickListener {
    Button bRevoke;
    Button bSave;

	/** Default method */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_options);

        bRevoke = (Button) findViewById(R.id.bRevoke);
        bRevoke.setOnClickListener(this);
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

    private void revokeKey() {
        long keyId = 0;
        try {
            PGPPublicKey pub = MEGPublicKeyRing.fromFile(this).getMasterPublicKey();
            keyId = pub.getKeyID();
        } catch (Exception e) {
            alertBuilder(R.string.no_key_to_revoke_msg).show();
            return;
        }
        MEGServerRequest request = new MEGServerRequest();
        try {
            request.revokeKey(Long.toHexString(keyId).toUpperCase());
        } catch (Exception e) {
            alertBuilder(R.string.something_wrong_server_msg).show();
            return;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bRevoke: {
                revokeKey();
                break;
            }
            case R.id.bSave: {
                startActivity(new Intent(this, Installation.class));
                break;
            }
        }
    }
}

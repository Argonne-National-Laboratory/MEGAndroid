package gov.anl.coar.meg;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import gov.anl.coar.meg.pgp.PrivateKeyCache;

/**
 * Created by greg on 4/12/16.
 * Edited by Joshua Lyle
 */
public class EnterPasswordDialog extends DialogFragment{

    public EnterPasswordDialog() {}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.enter_password_msg);
        View view = inflater.inflate(R.layout.password_prompt, null);
        builder.setView(view);
        final EditText etLoginPassword = (EditText) view.findViewById(R.id.etLoginPassword);
        builder
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            //Try password to unlock private key
                            PrivateKeyCache cache = (PrivateKeyCache) getActivity().getApplication();
                            char[] password = etLoginPassword.getText().toString().toCharArray();
                            cache.refreshPK(getActivity(), password);

                            //Make sure calling activity is MainActivity
                            if (getActivity() instanceof MainActivity)
                                //So we can disable the login button and show the logged in indicator
                                ((MainActivity)getActivity()).setImgCheckVisibility();

                            //If there isn't any symmetric keys, ask to scan the first one
                            if (!Util.doesSymmetricKeyExist((getActivity()).getApplicationContext())) {
                                Toast.makeText((getActivity()).getApplicationContext(), "Please scan your first client QR code", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(getActivity(), ScanQRActivity.class));
                            }

                        } catch (Exception e) {
                            FragmentManager fm = getFragmentManager();
                            new RetryPasswordDialog().show(fm, "bar");
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }

                });
        return builder.create();
    }

    
}

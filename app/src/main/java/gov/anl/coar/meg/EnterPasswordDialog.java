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

import gov.anl.coar.meg.pgp.PrivateKeyCache;

/**
 * Created by greg on 4/12/16.
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
                            PrivateKeyCache cache = (PrivateKeyCache) getActivity().getApplication();
                            char[] password = etLoginPassword.getText().toString().toCharArray();
                            cache.refreshPK(getActivity(), password);
                            if (!Util.doesSymmetricKeyExist(getActivity())) {
                                startActivity(new Intent(getActivity(), ScanQRActivity.class));
                            } else {
                                startActivity(new Intent(getActivity(), Login.class));
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

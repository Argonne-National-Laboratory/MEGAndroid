package gov.anl.coar.meg;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * Created by Joshua Lyle on 7/21/16.
 */
public class NameSymKeyDialog extends DialogFragment{

    public NameSymKeyDialog() {}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.enter_client_name_msg);
        View view = inflater.inflate(R.layout.name_sym_key_dialog, null);
        builder.setView(view);
        final EditText etClientName = (EditText) view.findViewById(R.id.etClientName);
        builder
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            String password = etClientName.getText().toString();
                            getActivity().getApplicationContext();
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

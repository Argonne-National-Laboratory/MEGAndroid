package gov.anl.coar.meg;

import android.app.Activity;
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

    NameSymKeyListener mCallback;

    public interface NameSymKeyListener {
        void onNameSymKey(String name, String clientID);
    }

    public NameSymKeyDialog() {}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (NameSymKeyListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement NameSymKeyListener");
        }
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

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
                            String ClientName = etClientName.getText().toString();
                            String clientID = getArguments().getString("clientID");
                            mCallback.onNameSymKey(ClientName, clientID);

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

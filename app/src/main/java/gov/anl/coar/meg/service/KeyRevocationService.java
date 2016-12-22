package gov.anl.coar.meg.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import org.spongycastle.openpgp.PGPPublicKey;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.R;
import gov.anl.coar.meg.http.MEGServerRequest;
import gov.anl.coar.meg.pgp.MEGPublicKeyRing;
import gov.anl.coar.meg.receiver.ReceiverCode;

/**
 * Created by greg on 5/12/16.
 */
public class KeyRevocationService extends IntentService {
    public static final String TAG = "KeyRevocationService";

    public KeyRevocationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Attempt key revocation");
        Bundle bundle = new Bundle();
        ResultReceiver result = intent.getParcelableExtra(Constants.RECEIVER_KEY);
        revokeKey(bundle, result);
    }

    private void revokeKey(Bundle bundle, ResultReceiver result) {
        String keyId;
        try {
            PGPPublicKey pub = MEGPublicKeyRing.fromFile(this).getMasterPublicKey();
            keyId = Long.toHexString(pub.getKeyID()).toUpperCase();
        } catch (Exception e) {
            bundle.putInt(Constants.ALERT_MESSAGE_KEY, R.string.no_key_to_revoke_msg);
            result.send(ReceiverCode.IID_NO_PUBLIC_KEY_FAILURE, bundle);
            return;
        }
        MEGServerRequest request = new MEGServerRequest(getApplicationContext());
        try {
            request.revokeKey(keyId);
        } catch (Exception e) {
            bundle.putInt(Constants.ALERT_MESSAGE_KEY, R.string.something_wrong_server_msg);
            result.send(ReceiverCode.IID_CODE_MEGSERVER_FAILURE, bundle);
        }
        result.send(ReceiverCode.IID_CODE_SUCCESS, bundle);
    }
}

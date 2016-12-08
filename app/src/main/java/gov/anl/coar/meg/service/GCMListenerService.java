package gov.anl.coar.meg.service;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import gov.anl.coar.meg.Util;
import gov.anl.coar.meg.pgp.MEGMessage;

/**
 * Created by greg on 3/6/16.
 */
public class GCMListenerService extends GcmListenerService {
    private static final String TAG = "GCMListenerService";

    LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(this);

    //Broadcast to views that we've revoked the keys
    public void sendResult() {
        Intent i = new Intent("keyHasBeenRevoked");
        broadcaster.sendBroadcast(i);
    }

    @Override
    public void onMessageReceived(
            String from,
            Bundle data
    ) {
        try {
            //Get info from GCM
            String action = data.getString("action");
            String clientId = data.getString("client_id");
            String messageId = data.getString("message_id");
            Log.d(TAG, "message received from: " + from);
            Log.d(TAG, "message has id: " + messageId);
            Log.d(TAG, "message action: " + action);

            //Call appropriate action
            if (action.contains("decrypt")) {
                MEGMessage msg = MEGMessage.getEncryptedFromServer(clientId, messageId, this, getApplication());
                msg.performDecryptionFlow();
            }
            else if (action.contains("encrypt")) {
                MEGMessage msg = MEGMessage.getDecryptedFromServer(clientId, messageId, this, getApplication());
                msg.performEncryptionFlow();
            }
            else if (action.contains("revoke")) {
                Util.deleteAllFiles(this);
                sendResult();
                Log.d("GCMListener", "sent result");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

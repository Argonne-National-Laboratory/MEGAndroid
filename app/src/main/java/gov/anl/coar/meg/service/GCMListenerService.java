package gov.anl.coar.meg.service;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import org.spongycastle.openpgp.PGPPublicKey;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import gov.anl.coar.meg.http.MEGServerRequest;
import gov.anl.coar.meg.pgp.EncryptionLogic;
import gov.anl.coar.meg.pgp.MEGPublicKeyRing;

/**
 * Created by greg on 3/6/16.
 */
public class GCMListenerService extends GcmListenerService {
    private static final String TAG = "GCMListenerService";

    @Override
    public void onMessageReceived(
            String from,
            Bundle data
    ) {
        String messageId = data.getString("message_id");
        String action = data.getString("action");
        Log.d(TAG, "message received from: " + from);
        Log.d(TAG, "message id: " + messageId + " message action: " + action);
        if (action.contains("decrypt")) {
            decryptMessage(messageId);
        } else {
            encryptMessage(messageId);
        }
    }

    private void encryptMessage(
            String messageId
    ) {
        try {
            MEGServerRequest request = new MEGServerRequest();
            InputStream response = request.getDecryptedMessage(messageId);
            EncryptionLogic logic = new EncryptionLogic();
            InputStream clear = logic.decryptMessageWithSymKey(response);
            InputStream pubkeyStream = request.getAssociatedPublicKey(messageId);
            PGPPublicKey pubKey = MEGPublicKeyRing.fromInputStream(pubkeyStream).getMasterPublicKey();
            ByteArrayOutputStream inter = logic.encryptMessageWithPubKey(clear, pubKey);
            ByteArrayInputStream enc = new ByteArrayInputStream(inter.toByteArray());
            request.putEncryptedMessage(messageId, enc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void decryptMessage(
            String messageId
    ) {
        try {
            MEGServerRequest request = new MEGServerRequest();
            InputStream response = request.getEncryptedMessage(messageId);
            EncryptionLogic logic = new EncryptionLogic();
            BufferedInputStream decBuffer = logic.decryptMessageWithPK(response, getApplication());
            InputStream symInBuffer = logic.encryptMessageWithSymKey(decBuffer);
            request.putDecryptedMessage(messageId, symInBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

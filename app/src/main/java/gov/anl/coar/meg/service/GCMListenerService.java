package gov.anl.coar.meg.service;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONObject;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import gov.anl.coar.meg.Util;
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
        String action = data.getString("action");
        Log.d(TAG, "message received from: " + from);
        Log.d(TAG, "message action: " + action);
        if (action.contains("decrypt")) {
            String messageId = data.getString("message_id");
            decryptMessage(messageId);
        } else if (action.contains("encrypt")) {
            String messageId = data.getString("message_id");
            encryptMessage(messageId);
        } else if (action.contains("revoke")) {
            Util.deleteAllFiles(this);
        }
    }

    private void encryptMessage(
            String messageId
    ) {
        try {
            MEGServerRequest request = new MEGServerRequest();
            // TODO This logic is clunky. We should replace server logic to just get
            // TODO a base64 encoded message in a JSON blob. But for now... time.
            InputStream pubkeyStream = request.getAssociatedPublicKey(messageId);
            JSONObject getResponse = request.getDecryptedMessage(messageId);
            EncryptionLogic logic = new EncryptionLogic();
            InputStream message = new ByteArrayInputStream(Base64.decode(getResponse.getString("message")));
            InputStream clear = logic.decryptClientSymmetricData(this, message);
            PGPPublicKey pubKey = MEGPublicKeyRing.fromInputStream(pubkeyStream).getMasterPublicKey();
            ByteArrayInputStream enc = new ByteArrayInputStream(
                    Base64.encode(logic.pgpEncrypt(clear, pubKey).toByteArray())
            );
            request.putEncryptedMessage(
                    getResponse.getString("email_to"), getResponse.getString("email_from"), enc
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void decryptMessage(
            String messageId
    ) {
        try {
            MEGServerRequest request = new MEGServerRequest();
            JSONObject getResponse = request.getDecryptedMessage(messageId);
            EncryptionLogic logic = new EncryptionLogic();
            InputStream message = new ByteArrayInputStream(Base64.decode(getResponse.getString("message")));
            ByteArrayInputStream decBuffer = logic.pgpDecrypt(message, getApplication());
            ByteArrayInputStream symInBuffer = logic.encryptAsClientBoundSymmetricData(this, decBuffer);
            ByteArrayInputStream b64sym = new ByteArrayInputStream(
                    Base64.encode(Util.inputStreamToOutputStream(symInBuffer).toByteArray())
            );
            request.putDecryptedMessage(
                    getResponse.getString("email_to"), getResponse.getString("email_from"), b64sym
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

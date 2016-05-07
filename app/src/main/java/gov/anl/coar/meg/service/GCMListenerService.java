package gov.anl.coar.meg.service;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONObject;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.util.encoders.Base64;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

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
            // TODO This logic is clunky. We should replace server logic to just get
            // TODO a base64 encoded message in a JSON blob. But for now... time.
            InputStream pubkeyStream = request.getAssociatedPublicKey(messageId);
            JSONObject getResponse = request.getDecryptedMessage(messageId);
            EncryptionLogic logic = new EncryptionLogic();
            InputStream message = new ByteArrayInputStream(Base64.decode(getResponse.getString("message")));
            InputStream clear = logic.decryptClientSymmetricData(this, message);
            PGPPublicKey pubKey = MEGPublicKeyRing.fromInputStream(pubkeyStream).getMasterPublicKey();
            ByteArrayOutputStream out = logic.pgpEncrypt(clear, pubKey);
            // TODO DEBUG
            byte[] bytes = Base64.encode(out.toByteArray());
            Log.i(TAG, "send new encrypted: " + new String(bytes, Charset.forName("UTF-8")));
            ByteArrayInputStream enc = new ByteArrayInputStream(bytes);
            // TODO END DEBUG
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
            Log.i(TAG, "decrypt message: " + getResponse.getString("message"));
            InputStream message = new ByteArrayInputStream(Base64.decode(getResponse.getString("message")));
            ByteArrayInputStream decBuffer = logic.pgpDecrypt(message, getApplication());
            ByteArrayOutputStream bar = Util.inputStreamToOutputStream(decBuffer);
            // TODO
            byte[] arr = bar.toByteArray();
            Log.i(TAG, "plain: " + new String(arr, Charset.forName("US-ASCII")));
            ByteArrayInputStream bin = new ByteArrayInputStream(arr);
            ByteArrayInputStream symInBuffer = logic.encryptAsClientBoundSymmetricData(this, bin);
            ByteArrayOutputStream out = Util.inputStreamToOutputStream(symInBuffer);
            byte[] b64encoded = Base64.encode(out.toByteArray());
            Log.i(TAG, "Send decrypted message: " + new String(b64encoded, Charset.forName("US-ASCII")));
            ByteArrayInputStream decrypted = new ByteArrayInputStream(b64encoded);
            // TODO END
            request.putDecryptedMessage(
                    getResponse.getString("email_to"), getResponse.getString("email_from"), decrypted
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

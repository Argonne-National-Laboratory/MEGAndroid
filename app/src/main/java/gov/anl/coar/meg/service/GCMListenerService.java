package gov.anl.coar.meg.service;

import android.os.Bundle;
import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.android.gms.gcm.GcmListenerService;

import java.io.BufferedInputStream;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.logic.PrivacyLogic;

/**
 * Created by greg on 3/6/16.
 */
public class GCMListenerService extends GcmListenerService{
    private static final String TAG = "GCMListenerService";
    private static final String RETRIEVE_MSG_URL = "/get_message/";
    private static final String SEND_MSG_URL = "/send_message/";
    private static final int MAX_RETRIES = 100;

    @Override
    public void onMessageReceived(String from, Bundle data){
        try {
            String messageId = data.getString("message_id");
            Log.d(TAG, "Message received from: " + from);
            Log.d(TAG, "Message Id: " + messageId);
            HttpRequest response = retrieveEncryptedMessage(messageId, 0);
            // get message, decrypt, then re-encrypt with symmetric key
            BufferedInputStream responseBuffer = response.buffer();
            // Somehow I need to get the password. Crud...
            BufferedInputStream decryptedInBuffer =
                    PrivacyLogic.decryptMessageWithPK(responseBuffer);
            BufferedInputStream symInBuffer =
                    PrivacyLogic.encryptMessageWithSymKey(decryptedInBuffer);
            // I'm sure I need to add headers or something. For now don't do anything
            // else. I'm good with just stubbing out the code
            HttpRequest.post(Constants.MEG_API_URL + SEND_MSG_URL).send(symInBuffer);
        } catch (Exception e) {
            // Handle later
            e.printStackTrace();
        }
    }

    public HttpRequest retrieveEncryptedMessage(String messageId, int numberRetries)
            throws Exception{
        HttpRequest response = HttpRequest.get(
                Constants.MEG_API_URL + RETRIEVE_MSG_URL, true, "msg_id", messageId);
        // What do we do in the case of stuff like 404 errors? Why don't we just
        // retry right now and worry about hardening later
        if (response.code() != 200 && numberRetries < MAX_RETRIES) {
            Log.w(TAG, "Could not get message with id: " + messageId);
            Thread.sleep(Constants.MESSAGE_RETRIEVAL_TIMEOUT);
            retrieveEncryptedMessage(messageId, numberRetries + 1);
        } else if (response.code() != 200 && numberRetries >= MAX_RETRIES) {
            // Another one of those things where I don't know what can go wrong so I'm
            // just trying to code carefully (or maybe not so carefully) around it right now
            throw new Exception("Something went wrong. We could not find message with id " + messageId);
        }
        return response;
    }
}

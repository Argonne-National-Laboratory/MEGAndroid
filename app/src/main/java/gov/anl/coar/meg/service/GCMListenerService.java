package gov.anl.coar.meg.service;

import android.os.Bundle;
import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.android.gms.gcm.GcmListenerService;

import java.io.BufferedInputStream;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.pgp.EncryptionLogic;

/**
 * Created by greg on 3/6/16.
 */
public class GCMListenerService extends GcmListenerService{
    private static final String TAG = "GCMListenerService";
    private static final String ENCRYPTED_MSG_URL = "/encrypted_message/";
    private static final String DECRYPTED_MSG_URL = "/decrypted_message/";
    private static final int MAX_RETRIES = 100;

    @Override
    public void onMessageReceived(
            String from,
            Bundle data
    ){
        try {
            String messageId = data.getString("message_id");
            String action = data.getString("action");
            Log.d(TAG, "Message received from: " + from);
            Log.d(TAG, "Message Id: " + messageId);
            if (action == "decrypt")
                decryptMessage(messageId);
            else
                encryptMessage(messageId);
        } catch (Exception e) {
            // Handle later
            e.printStackTrace();
        }
    }

    private void encryptMessage(String messageId) {
        // stub for now TODO later
    }

    private void decryptMessage(
            String messageId
    )
            throws Exception
    {
        // We give it 0 retries because this is the first method call. It is up to later method
        // calls for recursive function
        HttpRequest response = getEncryptedMessage(messageId, 0);
        // Extract JSON from the response.
        BufferedInputStream responseBuffer = response.buffer();
        Log.d(TAG, "Received response: " + response.body() + " from server");
        // TODO add symmetric key encryption
        BufferedInputStream decryptedInBuffer =
                EncryptionLogic.decryptMessageWithPK(responseBuffer);
        BufferedInputStream symInBuffer =
                EncryptionLogic.encryptMessageWithSymKey(decryptedInBuffer);
        // I'm sure I need to add headers or something. For now don't do anything
        // else. I'm good with just stubbing out the code
        HttpRequest.post(Constants.MEG_API_URL + DECRYPTED_MSG_URL).send(symInBuffer);
    }

    public HttpRequest getEncryptedMessage(
            String messageId,
            int numberRetries
    )
            throws Exception
    {
        HttpRequest response = HttpRequest.get(
                Constants.MEG_API_URL + ENCRYPTED_MSG_URL, true, "message_id", messageId);
        response = response.acceptJson();
        // What do we do in the case of stuff like 404 errors? Why don't we just
        // retry right now and worry about hardening later
        if (response.code() != 200 && numberRetries < MAX_RETRIES) {
            Log.w(TAG, "Could not get message with id: " + messageId);
            Thread.sleep(Constants.MESSAGE_RETRIEVAL_TIMEOUT);
            getEncryptedMessage(messageId, numberRetries + 1);
        } else if (response.code() != 200 && numberRetries >= MAX_RETRIES) {
            // Another one of those things where I don't know what can go wrong so I'm
            // just trying to code carefully (or maybe not so carefully) around it right now
            throw new Exception("Something went wrong. We could not find message with id " + messageId);
        }
        return response;
    }

    public HttpRequest getMessageToBeEncrypted(
            String messageId,
            int numberRetries
    ) {
        HttpRequest response = HttpRequest.get(
                Constants.MEG_API_URL + DECRYPTED_MSG_URL, true, "message_id", messageId
        );
        // XXX TODO error handling and such
        return response;
    }
}

package gov.anl.coar.meg.http;

import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.receiver.ReceiverCode;

/**
 * Created by greg on 4/9/16.
 */
public class MEGServerRequest {
    private final String TAG = "MEGServerRequest";
    private final String mServerUrl = Constants.MEG_API_URL;

    private static final String ADD_PUBLIC_KEY_URL = "/addkey/";
    private static final String ENCRYPTED_MSG_URL = "/encrypted_message/";
    private static final String DECRYPTED_MSG_URL = "/decrypted_message/";
    private static final String GET_KEY_BY_MSG_ID_URL = "/getkey_by_message_id/";
    private static final String REVOKE_KEY_URL = "/request_revoke/";
    private static final String STORE_INSTANCE_ID_API_ROUTE = "/gcm_instance_id/";
    private static final String STORE_REVOCATION_URL = "/store_revocation_cert";

    private int mCurRetries = 0;
    private int mMaxRetries = Constants.HTTP_MAX_RETRIES;
    private long mRetryTimeout = Constants.HTTP_RETRY_TIMEOUT;

    public JSONObject getDecryptedMessage(
            String messageId
    )
        throws Exception
    {
        mCurRetries = 0;
        return getMessage(mServerUrl + DECRYPTED_MSG_URL, messageId);
    }

    public JSONObject getEncryptedMessage(
            String messageId
    )
            throws Exception
    {
        mCurRetries = 0;
        return getMessage(mServerUrl + ENCRYPTED_MSG_URL, messageId);
    }

    public void putEncryptedMessage(
            String email_to,
            String email_from,
            InputStream inBuffer
    )
            throws Exception
    {
        mCurRetries = 0;
        putMessage(mServerUrl + ENCRYPTED_MSG_URL, email_to, email_from, inBuffer);
    }

    public void putDecryptedMessage(
            String email_to,
            String email_from,
            InputStream inBuffer
    )
            throws Exception
    {
        mCurRetries = 0;
        putMessage(mServerUrl + DECRYPTED_MSG_URL, email_to, email_from, inBuffer);
    }

    private void putMessage(
            String url,
            String email_to,
            String email_from,
            InputStream inBuffer
    )
            throws Exception
    {
        Log.d(TAG, "put new message on server @ url: " + url);
        try {
            HttpRequest request = HttpRequest.put(
                    url, true,
                    "email_to", email_to,
                    "email_from", email_from,
                    "action", Constants.TO_CLIENT_ACTION
            );
            request = request.header("Content-Type", Constants.PUT_MESSAGE_CONTENT_TYPE);
            HttpRequest response = request.send(inBuffer);
            if (response.ok())
                return;
        } catch(Exception e) {}  // Likely connection problem
        if (mCurRetries < mMaxRetries) {
            mCurRetries += 1;
            TimeUnit.SECONDS.sleep(mRetryTimeout);
            putMessage(url, email_to, email_from, inBuffer);
        }
        throw new Exception("Something went wrong. We could not put the message back onto the server");
    }

    private JSONObject getMessage(
            String url,
            String messageId
    )
            throws Exception
    {
        Log.d(TAG, "Get encrypted message from url: " + url + " with id: " + messageId);
        try {
            HttpRequest response = HttpRequest.get(url, true, "message_id", messageId);
            if (response.ok())
                return new JSONObject(response.body());
        } catch(Exception e) {}  // Likely connection problem
        if (mCurRetries < mMaxRetries) {
            mCurRetries += 1;
            TimeUnit.SECONDS.sleep(mRetryTimeout);
            return getMessage(url, messageId);
        }
        throw new Exception("Something went wrong. We could not find message with id " + messageId);
    }

    public void putToken(
            ResultReceiver result,
            Bundle bundle,
            String token,
            String phoneNumber,
            String email
    )
            throws Exception
    {
        String url = Constants.MEG_API_URL + STORE_INSTANCE_ID_API_ROUTE;
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("gcm_instance_id", token);
        data.put("phone_number", phoneNumber);
        data.put("email", email);
        try {
            HttpRequest response = HttpRequest.put(url).form(data);
            int code = response.code();
            Log.d(TAG, "Received code " + code + " from server with url: " + url);
            bundle.putInt("statusCode", code);
            bundle.putString("message", response.message());
            result.send(ReceiverCode.IID_CODE_SUCCESS, bundle);
            if (code == 200)
                return;
        } catch (Exception e) {} // Likely connection error
        TimeUnit.SECONDS.sleep(mRetryTimeout);
        throw new MEGServerException("Unable to put GCM token on server");
    }

    public void putPublicKey(
            String publicKeyText
    )
            throws Exception
    {
        try {
            String url = Constants.MEG_API_URL + ADD_PUBLIC_KEY_URL;
            Log.d(TAG, "Register public key with meg url:  " + url);
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("keydata", publicKeyText);
            HttpRequest request = HttpRequest.put(url).form(data);
            if (request.ok())
                return;
        } catch (Exception e) {}
        TimeUnit.SECONDS.sleep(mRetryTimeout);
        throw new MEGServerException("Unable to put public key on server!");
    }

    public void putRevocationKey(
            String revocationKeyText
    )
            throws Exception
    {
        try {
            String revocationUrl = Constants.MEG_API_URL + STORE_REVOCATION_URL;
            Log.d(TAG, "Register revocation key at url: " + revocationUrl);
            HashMap<String, String> revocationData = new HashMap<String, String>();
            revocationData.put("keydata", revocationKeyText);
            HttpRequest revocationRequest = HttpRequest.put(revocationUrl).form(revocationData);
            if (revocationRequest.ok())
                return;
        } catch (Exception e) {}
        TimeUnit.SECONDS.sleep(mRetryTimeout);
        throw new MEGServerException("Unable to send revocation key to server");
    }

    public InputStream getAssociatedPublicKey(
            String messageId
    )
            throws Exception
    {
        String url = mServerUrl + GET_KEY_BY_MSG_ID_URL;
        Log.d(TAG, "Get associated public key for message: " + messageId);
        try {
            HttpRequest response = HttpRequest.get(url, true, "associated_message_id", messageId);
            if (response.ok())
                return response.stream();
        } catch(Exception e) {}  // Likely ConnectionError
        if (mCurRetries < mMaxRetries) {
            mCurRetries += 1;
            TimeUnit.SECONDS.sleep(mRetryTimeout);
            return getAssociatedPublicKey(messageId);
        }
        throw new Exception("Unable to get associated public key for message id: " + messageId);
    }

    public void revokeKey(
            String keyId
    )
            throws Exception
    {
        if (keyId.length() > 8) {
            keyId = keyId.substring(0, 8);
        }
        String url = mServerUrl + REVOKE_KEY_URL;
        Log.d(TAG, "Request revoke key for key id: " + keyId);
        try {
            HttpRequest response = HttpRequest.post(url, true, "keyid", keyId);
            if (response.ok())
                return;
        } catch (Exception e) {}
        if (mCurRetries < mMaxRetries) {
            mCurRetries += 1;
            TimeUnit.SECONDS.sleep(mRetryTimeout);
            revokeKey(keyId);
        }
        throw new Exception("Was unable to make request to revoke key");
    }
}

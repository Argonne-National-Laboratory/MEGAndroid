package gov.anl.coar.meg.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.R;
import gov.anl.coar.meg.Util;
import gov.anl.coar.meg.receiver.ReceiverCode;

/**
 * Created by greg on 3/5/16.
 */
public class GCMInstanceIdIntentService extends IntentService{

    private static final String STORE_INSTANCE_ID_API_ROUTE = "/gcm_instance_id/";
    private static final String TAG = "InstanceIdIntentService";

    public GCMInstanceIdIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle bundle = new Bundle();
        ResultReceiver result = intent.getParcelableExtra("receiver");
        try {
            String phoneNumber = Util.getPhoneNumber(this);
            // This is essentially a hasRegistered check. We should probably make this a
            // preference in the future
            if (phoneNumber == null) {
                bundle.putString("results", "phone number not found");
                result.send(ReceiverCode.IID_CODE_PHONE_NUMBER_FAILURE, bundle);
                TimeUnit.SECONDS.sleep(Constants.INSTANCE_ID_RETRY_TIMEOUT);
                return;
            }
            String email = Util.getConfigVar(this, Constants.EMAIL_FILENAME);
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // send the token to the server for storage
            Log.d(TAG, "Received token " + token);
            sendTokenToServer(result, bundle, token, phoneNumber, email);
            // no subscription to topics is necessary.
        } catch (Exception e) {
            Log.w(TAG, "Failed to grab instance id " + e.toString());
            bundle.putString("result", e.toString());
            result.send(ReceiverCode.IID_CODE_GCM_FAILURE, bundle);
        }
    }

    public void sendTokenToServer(
            ResultReceiver result, Bundle bundle, String token, String phoneNumber, String email)
            throws InterruptedException {
        String url = Constants.MEG_API_URL + STORE_INSTANCE_ID_API_ROUTE;
        Map<String, String> data = new HashMap<String, String>();
        data.put("gcm_instance_id", token);
        data.put("phone_number", phoneNumber);
        data.put("email", email);
        HttpRequest response = HttpRequest.put(url).form(data);
        int code = response.code();
        Log.d(TAG, "Received code " + code + " from server with url: " + url);
        bundle.putInt("statusCode", code);
        bundle.putString("message", response.message());
        if (code != 200) {
            result.send(ReceiverCode.IID_CODE_MEGSERVER_FAILURE, bundle);
            TimeUnit.SECONDS.sleep(Constants.INSTANCE_ID_RETRY_TIMEOUT);
        } else {
            result.send(ReceiverCode.IID_CODE_SUCCESS, bundle);
        }
    }
}

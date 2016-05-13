package gov.anl.coar.meg.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.R;
import gov.anl.coar.meg.Util;
import gov.anl.coar.meg.http.MEGServerRequest;
import gov.anl.coar.meg.receiver.ReceiverCode;

/**
 * Created by greg on 3/5/16.
 */
public class GCMInstanceIdRegistrationService extends IntentService{

    private static final String TAG = "InstanceIdIntentService";

    public GCMInstanceIdRegistrationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(
            Intent intent
    ) {
        Bundle bundle = new Bundle();
        ResultReceiver result = intent.getParcelableExtra(Constants.RECEIVER_KEY);
        try {
            String phoneNumber = Util.getPhoneNumber(this);
            // This is essentially a hasRegistered check.
            if (phoneNumber == null) {
                bundle.putString("results", "phone number not found");
                result.send(ReceiverCode.IID_CODE_PHONE_NUMBER_FAILURE, bundle);
                TimeUnit.SECONDS.sleep(Constants.NO_REGISTRATION_RETRY_TIMEOUT);
                return;
            }
            String email = Util.getConfigVar(this, Constants.EMAIL_FILENAME);
            // TODO Should probably catch case where email is null
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(
                    getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null
            );
            Log.d(TAG, "Received token " + token);
            // send the token to the server for storage
            MEGServerRequest request = new MEGServerRequest();
            request.putToken(result, bundle, token, phoneNumber, email);
        } catch (Exception e) {
            handleException(e, bundle, result);
        }
    }

    private void handleException(Exception e, Bundle bundle, ResultReceiver result) {
        try {
            Log.w(TAG, "Failed to grab instance id " + e.toString());
            bundle.putString("result", e.toString());
            result.send(ReceiverCode.IID_CODE_GCM_FAILURE, bundle);
            TimeUnit.SECONDS.sleep(Constants.NO_REGISTRATION_RETRY_TIMEOUT);
        } catch (InterruptedException ie) {}
    }
}

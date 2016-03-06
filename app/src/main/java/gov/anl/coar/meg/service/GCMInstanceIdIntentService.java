package gov.anl.coar.meg.service;

import android.app.IntentService;
import android.content.Intent;
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
import gov.anl.coar.meg.exception.NoInstanceIdException;

/**
 * Created by greg on 3/5/16.
 */
public class GCMInstanceIdIntentService extends IntentService{
    private static final String INSTANCE_ID_API_ROUTE = "/fooapi/";
    private static final String TAG = "InstanceIdIntentService";

    public GCMInstanceIdIntentService() {
        super(TAG);
    }

    protected void retryHandler(Intent intent) {
        Log.i(TAG, "Failed to get instance id. Retrying");
        try {
            TimeUnit.SECONDS.sleep(Constants.INSTANCE_ID_RETRY_TIMEOUT);
            onHandleIntent(intent);
        } catch (InterruptedException e) {
            // This shouldn't happen. But I imagine that it could happen as a result
            // of some crash on android. Either way I'm pretty sure we have bigger
            // problems if this goes down
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            String phoneNumber = Util.getPhoneNumber(this);
            // This is essentially a hasRegistered check
            if (phoneNumber == null) {
                retryHandler(intent);
            }
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // send the token to the server for storage
            Log.d(TAG, "Received token " + token);
            sendTokenToServer(token, phoneNumber);
            // no subscription to topics is necessary.
        } catch (NoInstanceIdException e) {
            // For now I'm not sure how to handle this. So don't bother. Just
            // pring the stack.
            Log.i(TAG, e.getMessage());
            e.printStackTrace();
            retryHandler(intent);
        } catch (Exception e) {
            // Example code stresses that some signal is sent so that we can attempt
            // this step at a later time. Why not just re-call directly with some
            // exponential backoff? Anyhow for now just leave blank.
            e.printStackTrace();
            retryHandler(intent);
        }
    }

    public void sendTokenToServer(String token, String phoneNumber) throws NoInstanceIdException{
        String url = Constants.MEG_API_URL + INSTANCE_ID_API_ROUTE;
        Map<String, String> data = new HashMap<String, String>();
        data.put("gcm_instance_id", token);
        data.put("phone_number", phoneNumber);
        HttpRequest response = HttpRequest.put(url).form(data);
        if (response.code() != 200) {
            throw new NoInstanceIdException(response);
        }
    }
}

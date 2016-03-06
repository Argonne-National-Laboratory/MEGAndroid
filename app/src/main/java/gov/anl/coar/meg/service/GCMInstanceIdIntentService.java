package gov.anl.coar.meg.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.util.HashMap;
import java.util.Map;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.R;
import gov.anl.coar.meg.exception.NoInstanceIdException;

/**
 * Created by greg on 3/5/16.
 */
public class GCMInstanceIdIntentService extends IntentService{
    private static final String TAG = "InstanceIdIntentService";

    public GCMInstanceIdIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // send the token to the server for storage
            Log.d(TAG, "Received token " + token);
            sendTokenToServer(token);
            // no subscription to topics is necessary.
        } catch (NoInstanceIdException e) {
            // For now I'm not sure how to handle this. So don't bother. Just
            // pring the stack.
            e.printStackTrace();
        } catch (Exception e) {
            // Example code stresses that some signal is sent so that we can attempt
            // this step at a later time. Why not just re-call directly with some
            // exponential backoff? Anyhow for now just leave blank.
            e.printStackTrace();
        }
    }

    public void sendTokenToServer(String token) throws NoInstanceIdException{
        Map<String, String> data = new HashMap<String, String>();
        data.put("gcm_instance_id", token);
        HttpRequest response = HttpRequest.put(Constants.MEG_API_URL).form(data);
        if (response.code() != 200) {
            throw new NoInstanceIdException(response);
        }
    }
}

package gov.anl.coar.meg.service;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.R;

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
            Unirest.put(Constants.MEG_API_URL).field("instance_id", token).asJson();
            // no subscription to topics is necessary.
        } catch (UnirestException e) {
            // Don'r know what to do with this for now. So just leave blank.
            e.printStackTrace();
        } catch (Exception e) {
            // Example code stresses that some signal is sent so that we can attempt
            // this step at a later time. Why not just re-call directly with some
            // exponential backoff? Anyhow for now just leave blank.
            e.printStackTrace();
        }
    }
}

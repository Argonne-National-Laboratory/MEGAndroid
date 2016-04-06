package gov.anl.coar.meg.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.github.kevinsawicki.http.HttpRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.Util;
import gov.anl.coar.meg.receiver.ReceiverCode;

/**
 * Created by greg on 4/4/16.
 */
public class KeyRegistrationService extends IntentService{
    public static final String TAG = "KeyRegistrationService";
    public static final String ADDKEY_PATH = "/addkey/";

    public KeyRegistrationService() {
        super(TAG);
    }

    protected void handleMissingPublicKeyFailures(ResultReceiver result, Bundle bundle) throws InterruptedException {
        bundle.putString("results", "no public key found");
        result.send(ReceiverCode.IID_NO_PUBLIC_KEY_FAILURE, bundle);
        TimeUnit.SECONDS.sleep(Constants.PUBLIC_KEY_RETRY_TIMEOUT);
    }
    @Override
    protected void onHandleIntent(
            Intent intent
    ) {
        Bundle bundle = new Bundle();
        bundle.putInt("statusCode", -1);
        ResultReceiver result = intent.getParcelableExtra("receiver");
        try {
            if (!Util.doesPublicKeyExist(this)) {
                handleMissingPublicKeyFailures(result, bundle);
                return;
            }
            String publicKey = Util.getPublicKey(this);
            if (publicKey == "") {
                handleMissingPublicKeyFailures(result, bundle);
                return;
            }
            Map<String, String> data = new HashMap<String, String>();
            data.put("keydata", publicKey);
            HttpRequest request = HttpRequest.put(Constants.MEG_API_URL + ADDKEY_PATH).form(data);
            int code = request.code();
            bundle.putInt("statusCode", code);
            if (code != 200)
                result.send(ReceiverCode.IID_CODE_MEGSERVER_FAILURE, bundle);
            // TODO Send revocation cert

        } catch (InterruptedException e) {
            result.send(ReceiverCode.IID_APP_FAILURE, bundle);
        }
    }
}

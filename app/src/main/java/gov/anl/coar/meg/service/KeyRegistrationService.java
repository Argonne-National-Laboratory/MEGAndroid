package gov.anl.coar.meg.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.system.ErrnoException;
import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;

import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.Util;
import gov.anl.coar.meg.pgp.MEGPublicKeyRing;
import gov.anl.coar.meg.pgp.MEGRevocationKey;
import gov.anl.coar.meg.receiver.ReceiverCode;

/**
 * Created by greg on 4/4/16.
 */
public class KeyRegistrationService extends IntentService{
    public static final String TAG = "KeyRegistrationService";
    public static final String ADD_PUBLIC_KEY_PATH = "/addkey/";
    public static final String STORE_REVOCATION = "/store_revocation_cert";

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
            MEGPublicKeyRing megPublicKeyRing = MEGPublicKeyRing.fromFile(this);
            if (megPublicKeyRing == null) {
                result.send(ReceiverCode.IID_APP_FAILURE, bundle);
                return;
            }
            PGPPublicKey publicKey = megPublicKeyRing.getMasterPublicKey();
            if (publicKey == null) {
                result.send(ReceiverCode.IID_APP_FAILURE, bundle);
                return;
            }
            String publicKeyText = Util.getArmoredPublicKeyText(publicKey);
            Map<String, String> data = new HashMap<String, String>();
            data.put("keydata", publicKeyText);
            String url = Constants.MEG_API_URL + ADD_PUBLIC_KEY_PATH;
            Log.d(TAG, "Register public key with meg url:  " + url);
            HttpRequest request = HttpRequest.put(url).form(data);
            int code = request.code();
            bundle.putInt("statusCode", code);
            if (code != 200) {
                result.send(ReceiverCode.IID_CODE_MEGSERVER_FAILURE, bundle);
                TimeUnit.SECONDS.sleep(Constants.PUBLIC_KEY_RETRY_TIMEOUT);
                return;
            }
            MEGRevocationKey revocationKey = MEGRevocationKey.fromFile(this);
            if (revocationKey == null) {
                handleMissingPublicKeyFailures(result, bundle);
                return;
            }
            String revocationKeyText = Util.getArmoredPublicKeyText(revocationKey.getKey());
            Map<String, String> revocationData = new HashMap<String, String>();
            revocationData.put("keydata", revocationKeyText);
            String revocationUrl = Constants.MEG_API_URL + STORE_REVOCATION;
            Log.d(TAG, "Register revocation key at url: " + revocationUrl);
            HttpRequest revocationRequest = HttpRequest.put(revocationUrl).form(revocationData);
            if (revocationRequest.code() != 200) {
                result.send(ReceiverCode.IID_CODE_MEGSERVER_FAILURE, bundle);
                TimeUnit.SECONDS.sleep(Constants.PUBLIC_KEY_RETRY_TIMEOUT);
                return;
            }
            result.send(ReceiverCode.IID_CODE_SUCCESS, bundle);
        } catch (InterruptedException e) {
            e.printStackTrace();
            result.send(ReceiverCode.IID_APP_FAILURE, bundle);
        } catch (IOException e) {
            e.printStackTrace();
            result.send(ReceiverCode.IID_APP_FAILURE, bundle);
        } catch (PGPException e) {
            e.printStackTrace();
            result.send(ReceiverCode.IID_APP_FAILURE, bundle);
        } catch (Exception e) {
            // If I had my way I wouldn't nest try statements
            e.printStackTrace();
            try {
                TimeUnit.SECONDS.sleep(Constants.PUBLIC_KEY_RETRY_TIMEOUT);
            } catch (InterruptedException err) {
                err.printStackTrace();
                result.send(ReceiverCode.IID_APP_FAILURE, bundle);
            }
            // I should change this code.
            result.send(ReceiverCode.IID_CODE_MEGSERVER_FAILURE, bundle);
        }
    }
}

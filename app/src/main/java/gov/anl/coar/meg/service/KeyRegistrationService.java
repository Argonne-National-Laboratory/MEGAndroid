package gov.anl.coar.meg.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import org.spongycastle.openpgp.PGPPublicKey;

import java.util.concurrent.TimeUnit;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.Util;
import gov.anl.coar.meg.http.MEGServerException;
import gov.anl.coar.meg.http.MEGServerRequest;
import gov.anl.coar.meg.pgp.MEGPublicKeyRing;
import gov.anl.coar.meg.pgp.MEGRevocationKey;
import gov.anl.coar.meg.receiver.ReceiverCode;

/**
 * Created by greg on 4/4/16.
 * Edited by Joshua Lyle on 12/21/16
 */
public class KeyRegistrationService extends IntentService{
    public static final String TAG = "KeyRegistrationService";

    public KeyRegistrationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(
            Intent intent
    ) {
        Bundle bundle = new Bundle();
        ResultReceiver result = intent.getParcelableExtra("receiver");
        MEGServerRequest megServerRequest = new MEGServerRequest(getApplicationContext());
        try {
            if (!Util.doesPublicKeyExist(this)) {
                result.send(ReceiverCode.IID_NO_PUBLIC_KEY_FAILURE, bundle);
                TimeUnit.SECONDS.sleep(Constants.NO_PUBLIC_KEY_RETRY_TIMEOUT);
                return;
            }
            MEGPublicKeyRing megPublicKeyRing = MEGPublicKeyRing.fromFile(this);
            // TODO have these methods throw IllegalArgumentExceptions
            if (megPublicKeyRing == null) {
                result.send(ReceiverCode.IID_APP_FAILURE, bundle);
                return;
            }
            PGPPublicKey publicKey = megPublicKeyRing.getMasterPublicKey();
            // TODO have these methods throw IllegalArgumentExceptions
            if (publicKey == null) {
                result.send(ReceiverCode.IID_APP_FAILURE, bundle);
                return;
            }
            String publicKeyText = Util.getArmoredPublicKeyText(publicKey);
            megServerRequest.putPublicKey(publicKeyText);
            MEGRevocationKey revocationKey = MEGRevocationKey.fromFile(this);
            String revocationKeyText = Util.getArmoredPublicKeyText(revocationKey.getKey());
            megServerRequest.putRevocationKey(revocationKeyText);
            result.send(ReceiverCode.IID_CODE_SUCCESS, bundle);
        } catch (MEGServerException e ) {
            // The timeout is embedded in the MEGServerRequest class.
            result.send(ReceiverCode.IID_CODE_MEGSERVER_FAILURE, bundle);
        } catch (IllegalArgumentException e) {
            result.send(ReceiverCode.IID_APP_FAILURE, bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

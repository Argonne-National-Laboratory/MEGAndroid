package gov.anl.coar.meg.service;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.iid.InstanceIDListenerService;

import gov.anl.coar.meg.receiver.GCMInstanceIdResultReceiver;
import gov.anl.coar.meg.receiver.GCMInstanceIdResultReceiver.Receiver;
import gov.anl.coar.meg.receiver.ReceiverCode;

/**
 * Created by greg on 3/8/16.
 *
 * This class is pretty much copied code from MainActivity. I'm not familiar
 * enough with Java to utilize polymorphism properly without spending a bunch of
 * time on it so I'm fine with just copying code for now until I can figure
 * out how to handle this better.
 */
public class GCMInstanceIdListenerService
        extends InstanceIDListenerService implements Receiver{

    private static final String TAG = "GcmListener";

    Intent mInstanceIdIntent;
    GCMInstanceIdResultReceiver mReceiver;

    @Override
    public void onTokenRefresh() {
        Log.i(TAG, "Attempting to retrieve new instance id.");
        mReceiver = new GCMInstanceIdResultReceiver(new Handler());
        mReceiver.setReceiver(this);
        mInstanceIdIntent = new Intent(this, GCMInstanceIdRegistrationService.class);
        mInstanceIdIntent.putExtra("receiver", mReceiver);
        startService(mInstanceIdIntent);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        Log.d(TAG, "received result " + resultCode + " from " + GCMInstanceIdRegistrationService.class.toString());
        // We can add more fine grained handling of the error types later.
        if (resultCode != ReceiverCode.IID_CODE_SUCCESS)
            startService(mInstanceIdIntent);
        else
            stopService(mInstanceIdIntent);
    }
}

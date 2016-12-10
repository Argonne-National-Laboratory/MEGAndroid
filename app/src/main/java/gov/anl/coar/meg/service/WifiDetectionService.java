package gov.anl.coar.meg.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.util.Log;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.receiver.ReceiverCode;

/**
 * Created by Josh on 12/9/16.
 */

public class WifiDetectionService extends IntentService {
    public static final String TAG = "WifiDetectionService";

    public WifiDetectionService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Checking Wifi Status");
        Bundle bundle = new Bundle();
        ResultReceiver result = intent.getParcelableExtra(Constants.RECEIVER_KEY);
        connectionStatus(bundle, result);
    }

    private void connectionStatus(Bundle b, ResultReceiver receiver) {
        Bundle bundle = new Bundle();

        //Sleep to save processor time and battery
        SystemClock.sleep(Constants.WIFI_STATUS_POLL_MS);

        ConnectivityManager connManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        //Send to receiver the status of the wifi connection
        if (networkInfo != null) {
            bundle.putBoolean("connected", true);
            receiver.send(ReceiverCode.IID_CODE_CONNECTED, bundle);
        } else {
            bundle.putBoolean("connected", false);
            receiver.send(ReceiverCode.IID_CODE_DISCONNECTED, bundle);
        }
    }
}

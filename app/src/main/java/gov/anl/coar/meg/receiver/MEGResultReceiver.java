package gov.anl.coar.meg.receiver;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Created by greg on 3/7/16.
 */
@SuppressLint("ParcelCreator")
public class MEGResultReceiver extends ResultReceiver{

    private static final String TAG = "MEGResultReceiver";
    private Receiver mReceiver;

    public MEGResultReceiver(Handler handler) {
        super(handler);
    }

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle resultData);
    }

    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }
    }
}

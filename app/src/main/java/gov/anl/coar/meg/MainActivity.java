package gov.anl.coar.meg;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import gov.anl.coar.meg.receiver.MEGResultReceiver;
import gov.anl.coar.meg.receiver.MEGResultReceiver.Receiver;
import gov.anl.coar.meg.receiver.ReceiverCode;
import gov.anl.coar.meg.service.GCMInstanceIdRegistrationService;


/** Class to provide functionality to the opening welcome page of MEG
 * Adds functionality to buttons which open new intents
 *
 * @author Bridget Basan
 * @author Greg Rehm
 */
public class MainActivity extends AppCompatActivity
    implements View.OnClickListener, Receiver{

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    Button bInstall;
    Button bLogin;
    Intent mInstanceIdIntent;
    MEGResultReceiver mReceiver;

    /**	Instantiate bInstall and bLogin buttons
     *
     * 	@author Bridget Basan
     * 	@author Greg Rehm
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bInstall = (Button) findViewById(R.id.bInstall);
        bInstall.setOnClickListener(this);

        bLogin = (Button) findViewById(R.id.bLogin);
        bLogin.setOnClickListener(this);

        if (checkPlayServices()) {
            mReceiver = new MEGResultReceiver(new Handler());
            mReceiver.setReceiver(this);
            mInstanceIdIntent = new Intent(this, GCMInstanceIdRegistrationService.class);
            mInstanceIdIntent.putExtra("receiver", mReceiver);
            startService(mInstanceIdIntent);
        } else {
            // TODO Some kind of warning to do something to set up google
            // TODO play services
        }
    }

    /** Instantiate new intents on button clicks
     *
     * @author Bridget Basan
     * @author Greg Rehm
     */
    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.bInstall:
                startActivity(new Intent(this,Installation.class));
                break;
            case R.id.bLogin:
                if (!new Util().doesSecretKeyExist(this)) {
                    bLogin.setError(Constants.LOGIN_BUT_NO_KEY);
                    break;
                } else {
                    startActivity(new Intent(this, Login.class));
                }
                break;
            default:
                break;
        }
    }

    /** Default method */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /** Default method */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
             return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
                Log.i(TAG, "This device is not supported");
            } else {
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        Log.d(TAG, "received result " + resultCode + " from " + GCMInstanceIdRegistrationService.class.toString());
        // We can add more fine grained handling of the error types later.
        if (resultCode != ReceiverCode.IID_CODE_SUCCESS) {
            Log.i(TAG,
                    "Failure to grab instance id code: " +
                            resultData.getString("statusCode") + " message: " +
                            resultData.getString("message"));
            startService(mInstanceIdIntent);
        }
        else {
            stopService(mInstanceIdIntent);
        }
    }
}

package gov.anl.coar.meg;

import android.*;
import android.Manifest;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import gov.anl.coar.meg.receiver.MEGResultReceiver;
import gov.anl.coar.meg.receiver.MEGResultReceiver.Receiver;
import gov.anl.coar.meg.service.WifiDetectionService;

/** Class to provide functionality to the Login page of MEG
 *
 *  @author Joshua Lyle
 * 	@author Bridget Basan
 */
public class Login extends AppCompatActivity implements View.OnClickListener, Receiver{

    Button bQRCode;
    TextView tvStatus;
    TextView tvNetworkName;
    ImageView imgLock;
    //MenuItem mRegistrationSettings;

    Intent mWifiStatusIntent;
    MEGResultReceiver mReceiver;

    boolean connected;
    boolean updateWifiStatus = true;

	/** Default method */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Assign variables to interface pieces
        bQRCode = (Button) findViewById(R.id.bQRCode);
        bQRCode.setOnClickListener(this);

        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvNetworkName = (TextView) findViewById(R.id.tvNetworkName);

        imgLock = (ImageView) findViewById(R.id.imgLock);

        //See if we're connected to the internet and update interface accordingly
        updateConnectionStatus();

        //Create receiver for wifi status service information
        mReceiver = new MEGResultReceiver(new Handler());
        mReceiver.setReceiver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Update the interface with the current connection status
        updateConnectionStatus();

        //Start the wifi status service
        mWifiStatusIntent = new Intent(this, WifiDetectionService.class);
        mWifiStatusIntent.putExtra(Constants.RECEIVER_KEY, mReceiver);
        mWifiStatusIntent.putExtra("connected", connected);
        updateWifiStatus = true;
        startService(mWifiStatusIntent);

    }

    @Override
    protected  void onPause() {
        //Prevent further wifi status updates and stop update service
        updateWifiStatus = false;
        stopService(mWifiStatusIntent);
        super.onPause();
    }

    //Updates the interface with the based on connection status details
    protected void updateConnectionStatus() {
        String ssid = getCurrentSsid(getApplicationContext());
        if (ssid != null) {
            tvStatus.setText("On");
            tvStatus.setTextColor(Color.parseColor("#66ba6a"));
            tvNetworkName.setText(ssid);
            tvNetworkName.setTextColor(Color.parseColor("#66ba6a"));
            imgLock.setImageResource(R.drawable.lock_login);
            connected = true;
        }
        else {
            tvStatus.setText("Off");
            tvStatus.setTextColor(Color.RED);
            tvNetworkName.setText("Disconnected");
            tvNetworkName.setTextColor(Color.RED);
            imgLock.setImageResource(R.drawable.unlock_login);
            connected = false;
        }
    }

	/** Default method */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
//        MenuItem item = menu.findItem(R.id.mRegistrationSettings);
//        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem m) {
//                Log.d("Login", Integer.toString(m.getItemId()));
//                switch(m.getItemId()) {
//                    case R.id.mRegistrationSettings:
//                        startActivity(new Intent(getApplicationContext(), Installation.class));
//                        break;
//                    default:
//                        break;
//                }
//                return true;
//            }
//        });
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
        if (id == R.id.mRegistrationSettings) {
            startActivity(new Intent(getApplicationContext(), Installation.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Prevent going back to login again
    @Override
    public void onBackPressed() {
        //Intent i = new Intent(this, MainActivity.class);
        //startActivity(i);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.bQRCode:
                startActivity(new Intent(this, QRManageActivity.class));
                break;
            default:
                break;
        }
    }

    //TODO: Do we need these permissions requested?
    private void requestNetworkPermissions() {
        // Do we need to ask for camera permissions?
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_WIFI_STATE) == -1) {
            // Request them if so
            ActivityCompat.requestPermissions(Login.this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 1);
        }
        // Do we need to ask for camera permissions?
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_NETWORK_STATE) == -1) {
            // Request them if so
            ActivityCompat.requestPermissions(Login.this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 1);
        }
    }

    //Get the ssid and update the connected boolean
    public static String getCurrentSsid(Context context) {
        String ssid = null;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid = connectionInfo.getSSID();
            }
        }
        return ssid;
    }

    //Receives wifi status updates and updates interface accordingly
    public void onReceiveResult(int resultCode, Bundle resultData) {
        Log.d("Login", "Got Result");
        connected = resultData.getBoolean("connected");
        updateConnectionStatus();
        mWifiStatusIntent.putExtra("connected", connected);
        if (updateWifiStatus)
            startService(mWifiStatusIntent);
    }
}

/**
 * Mostly created by zbar developers in example project at: https://github.com/ZBar/ZBar/tree/master/android/examples/CameraTest/src/net/sourceforge/zbar/android/CameraTest
 * so all credit should go to them.
 *
 * TODO a lot of the classes here are deprecated but in the interest of time I took
 * TODO liberty of ignoring these warnings. We should refactor this to include
 * TODO modern implementation eventually.
 */
package gov.anl.coar.meg;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import org.json.JSONObject;


/**
 * Created by greg on 4/15/16.
 * Edited by Joshua Lyle
 */
public class ScanQRActivity extends Activity{
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;
    TextView scanText;
    ImageScanner scanner;

    String TAG = "ScanQRActivity";

    private boolean previewing = true;

    static {
        System.loadLibrary("iconv");
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.camera_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
        preview.addView(mPreview);

        scanText = (TextView) findViewById(R.id.scanText);
    }

    public void onPause() {
        super.onPause();
        //releaseCamera();
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e){
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                SystemClock.sleep(1000);
                mCamera.autoFocus(autoFocusCB);
        }
    };

    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = scanner.scanImage(barcode);
            boolean scanSuccess = false;
            String clientId = "";

            if (result != 0) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();

                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {
                    scanText.setText("Processing ...");
                    try {
                        //Get sym key and clientID from QR
                        Log.d(TAG, sym.getData());
                        JSONObject QRInfo = new JSONObject(sym.getData());
                        String aes = QRInfo.getString("aes");
                        Log.d(TAG, aes);
                        clientId = QRInfo.getString("clientID");
                        Log.d(TAG, clientId);

                        //Ensure the sym key data is good
                        Util.validateSymmetricKeyData(aes);

                        //Write sym key to file named after clientId
                        Util.writeSymmetricMetadataFile(getApplicationContext(), aes, clientId);

                        scanSuccess = true;
                    } catch (Exception e) {
                        scanText.setText("Something went wrong. Try again.");
                        e.printStackTrace();
                    }
                }
            }

            // If the client was scanned successfully, send the clientID
            // to the QRManage activity to give it a name
            if (scanSuccess) {
                Intent i = new Intent(getApplicationContext(), QRManageActivity.class);
                i.putExtra("clientID", clientId);
                i.putExtra("action", "nameClientKey");
                if (getCallingActivity().getClassName().equals("gov.anl.coar.meg.QRManageActivity")) {
                    Log.d("QRManage", "Finishing");
                    setResult(Activity.RESULT_OK, i);
                    finish();
                }
                else {
                    Log.d("QRManage", "Did not use finish");
                }
                    //startActivity(i);
            }
        }
    };

    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };
}

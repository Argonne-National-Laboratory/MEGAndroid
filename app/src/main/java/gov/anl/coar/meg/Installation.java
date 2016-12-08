package gov.anl.coar.meg;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import gov.anl.coar.meg.pgp.KeyGenerationLogic;
import gov.anl.coar.meg.receiver.MEGResultReceiver;
import gov.anl.coar.meg.receiver.MEGResultReceiver.Receiver;
import gov.anl.coar.meg.receiver.ReceiverCode;
import gov.anl.coar.meg.service.KeyRegistrationService;
import gov.anl.coar.meg.service.KeyRevocationService;

/** Class to provide functionality to the installation page of MEG
 * Adds functionality to buttons which open new intents
 *
 * @author Bridget Basan
 * @author Greg Rehm
 * Edited by Joshua Lyle
 */
public class Installation extends AppCompatActivity
    implements View.OnClickListener, Receiver {

    Button bRegister;
    Button bRevoke;
    EditText etFirstName;
    EditText etLastName;
    EditText etEmail;
    EditText etPassword;
    EditText etPhone;
    MEGResultReceiver publicKeyResultReceiver;
    Intent mKeyRegistrationIntent;
    private static final String TAG = "InstallationActivity";

    Intent mKeyRevocationService;
    MEGResultReceiver mReceiver;

    BroadcastReceiver receiver;

    /**
     * Instantiate the screen
     *
     * @author Bridget Basan
     * @author Greg Rehm
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installation);

        //Set references to visible buttons
        bRegister = (Button) findViewById(R.id.bRegister);
        bRegister.setOnClickListener(this);
        bRevoke = (Button) findViewById(R.id.bRevoke);
        bRevoke.setOnClickListener(this);

        //Set references to text inputs
        etFirstName = (EditText) findViewById(R.id.etFname);
        etLastName = (EditText) findViewById(R.id.etLname);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        etPhone = (EditText) findViewById(R.id.etPhone);

        //Register receivers for results of registration
        publicKeyResultReceiver = new MEGResultReceiver(new Handler());
        publicKeyResultReceiver.setReceiver(this);
        mKeyRegistrationIntent = new Intent(this, KeyRegistrationService.class);
        mKeyRegistrationIntent.putExtra("receiver", publicKeyResultReceiver);

        //Create a receiver to wipe registration info if we revoke keys
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                fillRegistrationInfo();
            }
        };
    }

    //Refresh the registration info every time we come back to the activity
    @Override
    protected void onResume() {
        super.onResume();
        fillRegistrationInfo();
    }

    //Register the receiver on start
    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), new IntentFilter("keyHasBeenRevoked"));
    }

    //Unregister the receiver on stop
    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    public void fillRegistrationInfo() {
        String firstName = "";
        String lastName = "";
        String email = "";
        String phone = "";
        String password = "";

        boolean enableRegistration = true;

        //Fill strings with info and disable another registration if we have already registered
        if (new Util().doesSecretKeyExist(this)) {
            firstName = Util.getConfigVar(getApplicationContext(), Constants.FIRSTNAME_FILENAME);
            lastName = Util.getConfigVar(getApplicationContext(), Constants.LASTNAME_FILENAME);
            email = Util.getConfigVar(getApplicationContext(), Constants.EMAIL_FILENAME);
            phone = Util.getConfigVar(getApplicationContext(), Constants.PHONENUMBER_FILENAME);
            password = "12345678";
            enableRegistration = false;

            bRegister.setClickable(false);
            bRegister.setBackgroundColor(Color.GRAY);

            bRevoke.setClickable(true);
            bRevoke.setBackgroundColor(Color.RED);
        }
        //Otherwise enable registration and disable revoke
        else {
            bRegister.setClickable(true);
            bRegister.setBackgroundColor(Color.parseColor("#66ba6a"));

            bRevoke.setClickable(false);
            bRevoke.setBackgroundColor(Color.GRAY);
        }

        //Fill in text fields with registration info and disable them
        etFirstName.setText(firstName);
        etFirstName.setEnabled(enableRegistration);
        etLastName.setText(lastName);
        etLastName.setEnabled(enableRegistration);
        etEmail.setText(email);
        etEmail.setEnabled(enableRegistration);
        etPhone.setText(phone);
        etPhone.setEnabled(enableRegistration);
        etPassword.setText(password);
        etPassword.setEnabled(enableRegistration);

        //Disable another registration

    }

    public void writeConfigVarToFile(String filename, String item) {
        try {
            File varFile = new File(this.getFilesDir(), filename);
            PrintWriter output = new PrintWriter(varFile);
            output.write(item);
            output.close();
        } catch (FileNotFoundException e) {
            // Once again kick handling this down the line
            e.printStackTrace();
        }
    }

    /**
     * Return AlertDialog to tell the user they already created their PGP key
     *
     * @return
     */
    private AlertDialog alreadyCreatedKeyBuilder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.already_created_key_msg);
        // TODO For now just have the user click ok but later we should
        // TODO direct them to the revoke key flow if they forgot their password
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
        });
        return builder.create();
    }

    /**
     * Return alert dialog to tell the user something went wrong when creating
     * their PGP key
     *
     * @return
     */
    private AlertDialog somethingWrongAlertBuilder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.something_wrong_pgp_msg);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
        });
        return builder.create();
    }

    /**
     * Generate the alert dialog that notifies the user their password
     * is password upon key creation
     *
     * @return
     */
    private AlertDialog passwordConfirmBuilder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.install_alert_msg);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //startActivity(new Intent(((Dialog) dialog).getContext(), ScanQRActivity.class));
                Toast.makeText(getApplicationContext(), "Registration Completed", Toast.LENGTH_LONG).show();
            }
        });
        return builder.create();
    }

    /**
     * Instantiate new intents on button clicks
     *
     * @author Bridget Basan
     * @author Greg Rehm
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // Eventually we want to re-add the advanced options button but not now
            case R.id.bRegister: {
                Toast.makeText(getApplicationContext(), "Sending Registration", Toast.LENGTH_SHORT).show();
                bRegister.setEnabled(false);
                // TODO This is just debug startService. remove this when we're set on implementation
                startService(mKeyRegistrationIntent);
                try {
                    performValidation();
                    generateKeys();
                    passwordConfirmBuilder().show();
                    startService(mKeyRegistrationIntent);
                } catch(NotValidatedException e) {
                    // Don't do anything. The user already has errors on the text boxes
                } catch (KeyAlreadyCreatedException e) {
                    alreadyCreatedKeyBuilder().show();
                } catch (Exception e) {
                    // Something went wrong don't know what and I need to
                    // eventually figure out how to handle this
                    bRegister.setEnabled(true);
                    somethingWrongAlertBuilder().show();
                    e.printStackTrace();
                }
                return;
            }
            case R.id.bRevoke: {
                mReceiver = new MEGResultReceiver(new Handler());
                mReceiver.setReceiver(this);
                mKeyRevocationService = new Intent(this, KeyRevocationService.class);
                mKeyRevocationService.putExtra(Constants.RECEIVER_KEY, mReceiver);
                startService(mKeyRevocationService);

                // Popup message to let the know user know the email was sent
                Toast.makeText(v.getContext(), "Sent Revocation Confirmation Email", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void generateKeys()
            throws Exception
    {
        //Get the information from the text inputs
        String firstName = etFirstName.getText().toString();
        String lastName = etLastName.getText().toString();
        String email = etEmail.getText().toString();
        String phoneNumber = etPhone.getText().toString();
        char[] password = etPassword.getText().toString().toCharArray();

        //Save the information to files
        writeConfigVarToFile(Constants.FIRSTNAME_FILENAME, firstName);
        writeConfigVarToFile(Constants.LASTNAME_FILENAME, lastName);
        writeConfigVarToFile(Constants.PHONENUMBER_FILENAME, phoneNumber);
        writeConfigVarToFile(Constants.EMAIL_FILENAME, email);

        //Create keys with the info
        KeyGenerationLogic keyGeneration = new KeyGenerationLogic();
        keyGeneration.generateNewKeyRingAndKeys(
                getApplication(), this, firstName, lastName, email, password);
        keyGeneration.generateRevocationCert(getApplication(), this);
    }

    private void performValidation()
            throws KeyAlreadyCreatedException, NotValidatedException
    {
        // generate some kind of alert then break or go back to the
        // main screen after user hits OK. The whole deal on this is
        // that we eventually want to direct the user to some forgot
        // password screen so that they can reset their key. However at
        // the moment this is a TODO
        if (new Util().doesSecretKeyExist(this)) {
            throw new KeyAlreadyCreatedException();
        }
        boolean throwNotValidatedError = false;
        String firstName = etFirstName.getText().toString();
        String lastName = etLastName.getText().toString();
        String email = etEmail.getText().toString();
        String phoneNumber = etPhone.getText().toString();
        String password = etPassword.getText().toString();
        if (firstName.isEmpty()) {
            etFirstName.setError("Enter your first name");
            throwNotValidatedError = true;
        }
        if (lastName.isEmpty()) {
            etLastName.setError("Enter your last name");
            throwNotValidatedError = true;
        }
        // TODO email validation regex will be necessary.
        if (email.isEmpty()) {
            etEmail.setError("Enter your email");
            throwNotValidatedError = true;
        }
        if (phoneNumber.isEmpty()) {
            etPhone.setError("Enter your phone number");
            throwNotValidatedError = true;
        } else if (phoneNumber.length() != 10) {
            etPhone.setError("Enter a valid 10 digit (US) phone number");
            throwNotValidatedError = true;
        }
        // TODO the error warning presents below the ? icon on android. We should
        // TODO probably remove the ? icon
        if (password.isEmpty()) {
            etPassword.setError("Enter a password!");
            throwNotValidatedError = true;
        }
        if (throwNotValidatedError) {
            throw new NotValidatedException("Installation user input is incomplete!");
        }
    }
    /**
     * Default method
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_installation, menu);
        return true;
    }

    /**
     * Default method
     */
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

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        Log.d(TAG, "Received result code: " + resultCode);
        // Only if we cannot reach MEG or the public key ring has not been written do we retry.
        if (resultCode == ReceiverCode.IID_CODE_MEGSERVER_FAILURE || resultCode == ReceiverCode.IID_NO_PUBLIC_KEY_FAILURE) {
            startService(mKeyRegistrationIntent);
        } else {
            stopService(mKeyRegistrationIntent);
            fillRegistrationInfo();
        }
    }
}

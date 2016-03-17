package gov.anl.coar.meg;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.spongycastle.openpgp.PGPException;

import gov.anl.coar.meg.exception.InvalidKeyException;
import gov.anl.coar.meg.pgp.KeyGenerationLogic;

/** Class to provide functionality to the installation page of MEG
 * Adds functionality to buttons which open new intents
 *
 * @author Bridget Basan
 * @author Greg Rehm
 */
public class Installation extends AppCompatActivity
    implements View.OnClickListener {

    Button bNext;
    EditText etFirstName;
    EditText etLastName;
    EditText etEmail;
    EditText etPassword;
    EditText etPhone;

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

        bNext = (Button) findViewById(R.id.bNext);
        bNext.setOnClickListener(this);

        etFirstName = (EditText) findViewById(R.id.etFname);
        etLastName = (EditText) findViewById(R.id.etLname);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        etPhone = (EditText) findViewById(R.id.etPhone);
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
                startActivity(new Intent(((Dialog) dialog).getContext(), Login.class));
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
            case R.id.bNext: {
                if (new Util().doesSecretKeyExist(this)) {
                    // generate some kind of alert then break or go back to the
                    // main screen after user hits OK
                    alreadyCreatedKeyBuilder().show();
                    break;
                }
                // Eventually we want to add form validation
                try {
                    String firstName = etFirstName.getText().toString();
                    String lastName = etLastName.getText().toString();
                    String email = etEmail.getText().toString();
                    String phoneNumber = etPhone.getText().toString();
                    char[] password = etPassword.getText().toString().toCharArray();
                    writeConfigVarToFile(Constants.PHONENUMBER_FILENAME, phoneNumber);
                    writeConfigVarToFile(Constants.EMAIL_FILENAME, email);
                    KeyGenerationLogic keyGeneration = new KeyGenerationLogic();
                    keyGeneration.generateNewKeyRingAndKeys(
                            getApplication(), this, firstName, lastName, email, password);
                    passwordConfirmBuilder().show();
                } catch (InvalidKeyException e) {
                    // Something went wrong don't know what and I need to
                    // eventually figure out how to handle this
                    somethingWrongAlertBuilder().show();
                    e.printStackTrace();
                } catch (PGPException e) {
                    somethingWrongAlertBuilder().show();
                    e.printStackTrace();
                } catch (IOException e) {
                    somethingWrongAlertBuilder().show();
                    e.printStackTrace();
                } catch (NoSuchProviderException e) {
                    somethingWrongAlertBuilder().show();
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    somethingWrongAlertBuilder().show();
                    e.printStackTrace();
                }
                return;
            }
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
}

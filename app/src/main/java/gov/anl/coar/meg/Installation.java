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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;

import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyPair;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.operator.PGPDigestCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;

import gov.anl.coar.meg.exception.InvalidKeyException;

/** Class to provide functionality to the installation page of MEG
 * Adds functionality to buttons which open new intents
 *
 * @author Bridget Basan
 * @author Greg Rehm
 */
public class Installation extends AppCompatActivity
    implements View.OnClickListener {

    Button bAdvanced;
    Button bNext;
    EditText etFirstName;
    EditText etLastName;
    EditText etEmail;
    EditText etPassword;
    EditText etPhone;

    /**
     * Instantiate bAdvanced button
     *
     * @author Bridget Basan
     * @author Greg Rehm
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installation);

        bAdvanced = (Button) findViewById(R.id.bAdvanced);
        bAdvanced.setOnClickListener(this);

        bNext = (Button) findViewById(R.id.bNext);
        bNext.setOnClickListener(this);

        etFirstName = (EditText) findViewById(R.id.etFname);
        etLastName = (EditText) findViewById(R.id.etLname);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        etPhone = (EditText) findViewById(R.id.etPhone);
    }

    /**
     * Ensure that the user does not already have a public/private
     * key pair on the phone already
     *
     * @return
     */
    private boolean validateDoesNotHaveKey() {
        File keyFile = new File(this.getFilesDir(), Constants.SECRETKEY_FILENAME);
        if (keyFile.exists()) {
            return true;
        } else {
            return false;
        }
    }

    private void writeKeyStrToFile(String key, String filename) throws IOException {
        File keyFile = new File(this.getFilesDir(), filename);
        BufferedWriter output = new BufferedWriter(new FileWriter(keyFile));
        output.write(key);
        output.close();
    }

    private void generateRSAKey(String firstName, String lastName, String email, char[] password)
            throws InvalidKeyException, PGPException, IOException {
        try {
            Security.addProvider(new BouncyCastleProvider());
            // Make the algorithm configurable in the future
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(Constants.RSA, Constants.SPONGY_CASTLE);
            // Probably can make number of bytes configurable in the futue
            kpg.initialize(Constants.ENCRYPTION_BITS, new SecureRandom());
            KeyPair keyPair = kpg.generateKeyPair();
            if (keyPair == null) {
                throw new InvalidKeyException();
            }
            PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder()
                    .build().get(HashAlgorithmTags.SHA1);
            PGPKeyPair pgpKeyPair = new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, keyPair, new Date());
            String identity = firstName + " " + lastName + " " + email;
            //CAST5 is 128 bit cipher. CAST6 is 256 bits
            PGPSecretKey secretKey = new PGPSecretKey(
                    PGPSignature.DEFAULT_CERTIFICATION, pgpKeyPair, identity, sha1Calc, null, null,
                    new JcaPGPContentSignerBuilder(pgpKeyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA256),
                    new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.CAST5, sha1Calc).
                            setProvider(Constants.SPONGY_CASTLE).build(password)
            );
            writeKeyStrToFile(secretKey.toString(), Constants.SECRETKEY_FILENAME);
            writeKeyStrToFile(pgpKeyPair.getPublicKey().toString(), Constants.PUBLICKEY_FILENAME);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
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
            case R.id.bAdvanced: {
                startActivity(new Intent(this, Advanced_Options.class));
            }
            case R.id.bNext: {
                try {
                    if (validateDoesNotHaveKey()) {
                        // generate some kind of alert then break or go back to the
                        // main screen after user hits OK
                        alreadyCreatedKeyBuilder().show();
                        break;
                    }
                    // Eventually we want to add form validation but not yet.
                    // this would slow down development
                    String firstName = etFirstName.getText().toString();
                    String lastName = etLastName.getText().toString();
                    String email = etEmail.getText().toString();
                    char[] password = etPassword.getText().toString().toCharArray();
                    generateRSAKey(firstName, lastName, email, password);
                    passwordConfirmBuilder().show();
                } catch (InvalidKeyException e) {
                    // Something went wrong don't know what and I need to
                    // eventually figure out how to handle this
                    somethingWrongAlertBuilder().show();
                    e.printStackTrace();
                    return;
                } catch (PGPException e) {
                    somethingWrongAlertBuilder().show();
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    somethingWrongAlertBuilder().show();
                    e.printStackTrace();
                    return;

                }
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

package gov.anl.coar.meg;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


/**
 * Created by greg on 2/29/16.
 *
 * I dunno, is it worth making all the methods here static? For now punt.
 */
public class Util {
    /**
     * Return true if secret key exists otherwise false
     *
     * @return
     */
    public boolean doesSecretKeyExist(Context context) {
        File keyFile = new File(context.getFilesDir(), Constants.SECRETKEY_FILENAME);
        if (keyFile.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public static String getPhoneNumber(Context context) {
        // I imagine theres a case where the phone number is null. We can probably
        // ignore this for now though
        try {
            File phoneNumberFile = new File(context.getFilesDir(), Constants.PHONENUMBER_FILENAME);
            FileReader reader = new FileReader(phoneNumberFile);
            BufferedReader buffer = new BufferedReader(reader);
            return buffer.readLine();
        } catch (FileNotFoundException e) {
            // Once again ignore this for now
            e.printStackTrace();
        } catch (IOException e) {
            // Once again ignore this for now. The phone number file should have
            // one line of text. Not much reason to overthink this atm.
            e.printStackTrace();
        }
        return null;
    }
}
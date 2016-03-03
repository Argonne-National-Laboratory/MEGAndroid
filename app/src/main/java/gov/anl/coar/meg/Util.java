package gov.anl.coar.meg;

import android.content.Context;

import java.io.File;


/**
 * Created by greg on 2/29/16.
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

}

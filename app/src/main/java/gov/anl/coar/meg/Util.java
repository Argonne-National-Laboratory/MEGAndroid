package gov.anl.coar.meg;

import android.content.Context;

import java.io.File;

/**
 * Created by greg on 2/29/16.
 */
public class Util {
    /**
     * Ensure that the user does not already have a public/private
     * key pair on the phone already
     *
     * @return
     */
    public boolean validateDoesNotHaveKey(Context context) {
        File keyFile = new File(context.getFilesDir(), Constants.SECRETKEY_FILENAME);
        if (keyFile.exists()) {
            return true;
        } else {
            return false;
        }
    }

}

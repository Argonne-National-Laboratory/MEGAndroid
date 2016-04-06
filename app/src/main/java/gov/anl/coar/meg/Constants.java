package gov.anl.coar.meg;

/**
 * Created by greg on 2/28/16.
 */
public class Constants {
    // For now debug... later make configurable
    public static final boolean DEBUG = true;
    public static final String TAG = "MEG";

    public static final int ENCRYPTION_BITS = 2048;
    public static final String RSA = "RSA";
    public static final String SPONGY_CASTLE = "SC";

    public static final String SECRETKEYRING_FILENAME = "secret.skr";
    public static final String PUBLICKEYRING_FILENAME = "pub.asc";
    public static final String REVOCATIONKEY_FILENAME = "revocation.asc";
    public static final String PHONENUMBER_FILENAME = "phone_number.txt";
    public static final String LOGIN_BUT_NO_KEY = "It seems like you still need to complete the MEG installation step";

    // Obviously this will change in the future
    public static final String MEG_API_URL = "http://grehm.us/megserver";
    public static final long INSTANCE_ID_RETRY_TIMEOUT = 5;
    public static final long PUBLIC_KEY_RETRY_TIMEOUT = 5;
    public static final long MESSAGE_RETRIEVAL_TIMEOUT = 2;
    public static final String EMAIL_FILENAME = "email.txt";
}

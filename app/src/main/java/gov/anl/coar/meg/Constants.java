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
    public static final String SYMMETRICKEY_META_FILENAME = "symmetric.dat";
    public static final String PUBLICKEYRING_FILENAME = "pub.asc";
    public static final String REVOCATIONKEY_FILENAME = "revocation.asc";
    public static final String PHONENUMBER_FILENAME = "phone_number.txt";
    public static final String EMAIL_FILENAME = "email.txt";
    public static final String FIRSTNAME_FILENAME = "first_name.txt";
    public static final String LASTNAME_FILENAME = "last_name.txt";
    public static final String MEG_SERVER_FILENAME = "server_addr.txt";
    public static final String UPDATE_INTERVAL_FILENAME = "update_interval.txt";
    public static final String SYNC_CONTACTS_FILENAME = "sync_contacts.txt";
    public static final String UPDATE_ON_WIFI_FILENAME = "update_on_wifi.txt";
    public static final String OOB_FILENAME = "oob.txt";

    public static final int IO_BUFFER_SIZE = 102400;

    // This will change in the future
    public static final String MEG_API_URL = "http://mobileencryptiongateway.org/megserver";
    // Just use this for debugging if we don't want to hit the server
    //public static final String MEG_API_URL = "http://foobar";
    public static final String PUT_MESSAGE_CONTENT_TYPE = "text/plain; charset=us-ascii";
    public static final String TO_CLIENT_ACTION = "toclient";
    public static final long HTTP_RETRY_TIMEOUT = 5;
    public static final int HTTP_MAX_RETRIES = 10;

    public static final long NO_REGISTRATION_RETRY_TIMEOUT = 20;
    public static final long NO_PUBLIC_KEY_RETRY_TIMEOUT = 5;

    public static final int AES_KEY_BYTES = 32;  // AES 256
    public static final int AES_IV_BYTES = 16;  // AES*
    public static final String SYMMETRIC_KEY_FIELD_DELIMETER = "&&";

    public static final String SHA_256 = "SHA-256";
    public static final String MD5 = "MD5";

    public static final String ALERT_MESSAGE_KEY = "alertMessage";
    public static final String RECEIVER_KEY = "receiver";

    public static final int WIFI_STATUS_POLL_MS = 3000;
}

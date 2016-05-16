package gov.anl.coar.meg.pgp;

import android.app.Application;
import android.content.Context;

import org.json.JSONObject;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import gov.anl.coar.meg.http.MEGServerRequest;

/**
 * Created by greg on 5/14/16.
 */
public class MEGMessage {

    // Keep track of the original message we received
    private static String mOriginalMsg;
    // Keep track of the current bytes of the message
    private byte[] mCurMsg;
    private SignatureLogic mSigLogic;
    private EncryptionLogic mEncryptionLogic;
    private Context mContext;
    private PrivateKeyCache mPKCache;
    private MEGServerRequest mServerRequest;
    private PGPPublicKey mPartnerPubKey;
    private String mEmailFrom;
    private String mEmailTo;

    public MEGMessage(
            String message,
            String emailFrom,
            String emailTo,
            Context context,
            Application application,
            MEGServerRequest request,
            PGPPublicKey partnerPubKey
    ) {
        mOriginalMsg = message;
        mEmailFrom = emailFrom;
        mEmailTo = emailTo;
        mCurMsg = Base64.decode(mOriginalMsg);
        mSigLogic = new SignatureLogic();
        mEncryptionLogic = new EncryptionLogic();
        mContext = context;
        mPKCache = (PrivateKeyCache) application;
        mServerRequest = request;
        mPartnerPubKey = partnerPubKey;
        if (mPKCache.needsRefresh())
            throw new IllegalArgumentException("The private key cache needs to be unlocked!");
    }

    /**
     * Secondary constructor for MEGMessage. Intention is to grab a decrypted message
     * from the server with the intention of encrypting it and sending it back to the
     * client for eventual transmission.
     *
     * @param messageId
     * @param context
     * @param application
     * @return
     * @throws Exception
     */
    public static MEGMessage getDecryptedFromServer(
            String messageId,
            Context context,
            Application application
    )
            throws Exception
    {
        MEGServerRequest request = new MEGServerRequest();
        InputStream pubkeyStream = request.getAssociatedPublicKey(messageId);
        PGPPublicKey pubKey = MEGPublicKeyRing.fromInputStream(pubkeyStream).getMasterPublicKey();
        JSONObject getResponse = request.getDecryptedMessage(messageId);
        String message = getResponse.getString("message");
        String emailFrom = getResponse.getString("email_from");
        String emailTo = getResponse.getString("email_to");
        return new MEGMessage(message, emailFrom, emailTo, context, application, request, pubKey);
    }

    /**
     * Secondary constructor for MEGMessage. Gets an encrypted from the server for the eventual
     * goal of decrypting the message.
     *
     * @param messageId
     * @param context
     * @param application
     * @return
     * @throws Exception
     */
    public static MEGMessage getEncryptedFromServer(
            String messageId,
            Context context,
            Application application
    )
            throws Exception
    {
        MEGServerRequest request = new MEGServerRequest();
        JSONObject getResponse = request.getDecryptedMessage(messageId);
        String message = getResponse.getString("message");
        String emailFrom = getResponse.getString("email_from");
        String emailTo = getResponse.getString("email_to");
        return new MEGMessage(message, emailFrom, emailTo, context, application, request, null);
    }

    private void decryptWithClientSymKey()
            throws IOException, InvalidCipherTextException
    {
        mCurMsg = mEncryptionLogic.decryptClientSymmetricData(mContext, mCurMsg);
    }

    private void encryptWithClientSymKey()
            throws IOException, InvalidCipherTextException
    {
        mCurMsg = mEncryptionLogic.encryptAsClientBoundSymmetricData(mContext, mCurMsg);
    }

    private void encryptMessage()
            throws InvalidCipherTextException, PGPException, NoSuchAlgorithmException, IOException
    {
        mCurMsg = mEncryptionLogic.pgpEncrypt(mCurMsg, mPartnerPubKey).toByteArray();
    }

    private void decryptMessage()
            throws IOException, InvalidCipherTextException
    {
        mCurMsg = mEncryptionLogic.pgpDecrypt(mCurMsg, mPKCache.getPrivateKey());
    }

    private void signMessage()
            throws PGPException, IOException
    {
        mCurMsg = mSigLogic.sign(mPKCache, mCurMsg);
    }

    private void submitEncryptedToServer()
            throws Exception
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64.encode(mCurMsg));
        mServerRequest.putEncryptedMessage(mEmailTo, mEmailFrom, bais);
    }

    private void submitDecryptedToServer()
        throws Exception
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64.encode(mCurMsg));
        mServerRequest.putDecryptedMessage(mEmailTo, mEmailFrom, bais);
    }

    public void performEncryptionFlow()
        throws Exception
    {
        performEncryptionFlow();
        decryptWithClientSymKey();
        signMessage();
        encryptMessage();
        submitEncryptedToServer();
    }

    public void performDecryptionFlow()
        throws Exception
    {
        decryptMessage();
        encryptWithClientSymKey();
        submitDecryptedToServer();
    }
}
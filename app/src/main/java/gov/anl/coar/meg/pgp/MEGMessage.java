package gov.anl.coar.meg.pgp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.json.JSONObject;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import gov.anl.coar.meg.http.MEGServerRequest;

/**
 * Created by greg on 5/14/16.
 */
public class MEGMessage {
    private static final String TAG = "MEGMessage";

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
    private String mMessageId;
    private String mClientId;

    public MEGMessage(
            String message,
            String emailFrom,
            String emailTo,
            String msgId,
            String clientId,
            Context context,
            Application application,
            MEGServerRequest request,
            PGPPublicKey partnerPubKey
    ) {
        mOriginalMsg = message;
        mEmailFrom = emailFrom;
        mEmailTo = emailTo;
        mMessageId = msgId;
        mClientId = clientId;
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
     * @param clientId
     * @param messageId
     * @param context
     * @param application
     * @return
     * @throws Exception
     */
    public static MEGMessage getDecryptedFromServer(
            String clientId,
            String messageId,
            Context context,
            Application application
    )
            throws Exception
    {
        MEGServerRequest request = new MEGServerRequest(context);

        //Get public key to encrypt with
        InputStream pubkeyStream = request.getAssociatedPublicKey(messageId);
        PGPPublicKey pubKey = MEGPublicKeyRing.fromInputStream(pubkeyStream).getMasterPublicKey();

        //Get message contents
        JSONObject getResponse = request.getDecryptedMessage(messageId);
        String message = getResponse.getString("message");
        String emailFrom = getResponse.getString("email_from");
        String emailTo = getResponse.getString("email_to");
        return new MEGMessage(message, emailFrom, emailTo, messageId, clientId, context, application, request, pubKey);
    }

    /**
     * Secondary constructor for MEGMessage. Gets an encrypted from the server for the eventual
     * goal of decrypting the message.
     *
     * @param clientId
     * @param messageId
     * @param context
     * @param application
     * @return
     * @throws Exception
     */
    public static MEGMessage getEncryptedFromServer(
            String clientId,
            String messageId,
            Context context,
            Application application
    )
            throws Exception
    {
        MEGServerRequest request = new MEGServerRequest(context);
        JSONObject getResponse = request.getDecryptedMessage(messageId);
        String message = getResponse.getString("message");
        String emailFrom = getResponse.getString("email_from");
        String emailTo = getResponse.getString("email_to");
        return new MEGMessage(message, emailFrom, emailTo, messageId, clientId, context, application, request, null);
    }

    private void decryptWithClientSymKey()
            throws IOException, InvalidCipherTextException
    {
        mCurMsg = mEncryptionLogic.decryptClientSymmetricData(mContext, mCurMsg, mClientId);
    }

    private void encryptWithClientSymKey()
            throws IOException, InvalidCipherTextException
    {
        mCurMsg = mEncryptionLogic.encryptAsClientBoundSymmetricData(mContext, mCurMsg, mClientId);
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

    private void validateSignature()
            throws Exception
    {
        ArrayList<byte[]> array = mSigLogic.splitSignatureAndMessage(mCurMsg);
        byte[] signature = array.get(0);
        mCurMsg = array.get(1);
        String keyId = mSigLogic.getKeyId(signature);
        ByteArrayInputStream bais = new ByteArrayInputStream(mServerRequest.getPublicKey(keyId).getString("key").getBytes());
        mPartnerPubKey = MEGPublicKeyRing.fromInputStream(bais).getMasterPublicKey();
        bais.close();
        mSigLogic.validateSignature(mPartnerPubKey, signature, mCurMsg);
    }

    private void submitEncryptedToServer()
            throws Exception
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64.encode(mCurMsg));
        mServerRequest.putEncryptedMessage(mEmailTo, mEmailFrom, mMessageId, bais);
    }

    private void submitDecryptedToServer()
        throws Exception
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64.encode(mCurMsg));
        mServerRequest.putDecryptedMessage(mEmailTo, mEmailFrom, mMessageId, bais);
    }

    public void performEncryptionFlow()
        throws Exception
    {
        decryptWithClientSymKey();
        encryptMessage();
        signMessage();
        submitEncryptedToServer();
    }

    public void performDecryptionFlow()
        throws Exception
    {
        validateSignature();
        decryptMessage();
        encryptWithClientSymKey();
        submitDecryptedToServer();
    }
}

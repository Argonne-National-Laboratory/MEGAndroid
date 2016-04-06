package gov.anl.coar.meg.pgp;

import android.app.Application;
import android.util.Log;

import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPOnePassSignatureList;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import gov.anl.coar.meg.Constants;

/**
 * Created by greg on 3/6/16.
 */
public class EncryptionLogic {
    // Motivating examples for this code can be found here:
    //
    // https://github.com/bcgit/bc-java/blob/master/pg/src/main/java/org/bouncycastle/openpgp/examples/KeyBasedFileProcessor.java
    //
    public static final String TAG = "EncryptionLogic";

    public static BufferedInputStream decryptMessageWithPK(
            BufferedInputStream inBuffer,
            Application application
    ) {
        BufferedInputStream responseBuffer = null;
        try {
            InputStream input = PGPUtil.getDecoderStream(inBuffer);
            JcaPGPObjectFactory pgpFactory = new JcaPGPObjectFactory(input);
            Object object = pgpFactory.nextObject();
            PGPEncryptedDataList encrypted;
            // the first object might be a PGP marker packet.
            if (object instanceof PGPEncryptedDataList)
                encrypted = (PGPEncryptedDataList) object;
            else
                encrypted = (PGPEncryptedDataList) pgpFactory.nextObject();
            PrivateKeyCache pkCache = (PrivateKeyCache) application;
            // XXX TODO I haven't quite figured out what to do if the PK isn't cached
            if (pkCache.needsRefresh())
                return responseBuffer;
            // TODO Should only be encrypted with one public key right??
            PGPPublicKeyEncryptedData pbe = (PGPPublicKeyEncryptedData) encrypted.getEncryptedDataObjects().next();
            InputStream clearText = pbe.getDataStream(
                    new JcePublicKeyDataDecryptorFactoryBuilder().setProvider(Constants.SPONGY_CASTLE).build(pkCache.getPrivateKey())
            );
            JcaPGPObjectFactory plainFact = new JcaPGPObjectFactory(clearText);
            Object message = plainFact.nextObject();
            if (message instanceof PGPCompressedData) {
                PGPCompressedData cData = (PGPCompressedData) message;
                JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(cData.getDataStream());
                message = pgpFact.nextObject();
            }
            if (message instanceof PGPLiteralData) {
                PGPLiteralData literal = (PGPLiteralData) message;
                responseBuffer = new BufferedInputStream(literal.getDataStream());
            } else if (message instanceof PGPOnePassSignatureList) {
                // XXX TODO
                Log.w(TAG, "Message contains a signed message not literal data!");
            } else {
                // XXX TODO
                Log.w(TAG, "Message contains an unknown type of data!");
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to decrypt message " + inBuffer.toString());
            e.printStackTrace();
        } catch (PGPException e) {
            Log.e(TAG, "Unable to decrypt message " + inBuffer.toString());
            e.printStackTrace();
        }
        // just a stub for now
        return responseBuffer;
    }

    public static BufferedInputStream encryptMessageWithSymKey(
            BufferedInputStream buffer
    ) {
        // just a stub
        return buffer;
    }
}


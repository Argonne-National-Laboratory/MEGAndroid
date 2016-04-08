package gov.anl.coar.meg.pgp;

import android.content.Context;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPPublicKeyRingCollection;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import gov.anl.coar.meg.Constants;

/**
 * Created by greg on 4/6/16.
 */
public class MEGPublicKeyRing {
    private PGPPublicKeyRing mKeyRing;
    private PGPPublicKeyRing keyRing;

    public MEGPublicKeyRing(PGPPublicKeyRing publicKeyRing) {
        mKeyRing = publicKeyRing;
    }
    /**
     * Get public key ring from a file
     */
    protected static MEGPublicKeyRing fromFile(
            Context context,
            String filename
    )
            throws IOException, PGPException
    {
        MEGPublicKeyRing response = null;
        File pubkeyRingFile = new File(context.getFilesDir(), filename);
        InputStream keyIn = new BufferedInputStream(new FileInputStream(pubkeyRingFile.getPath()));
        PGPPublicKeyRingCollection collection = new PGPPublicKeyRingCollection(
                PGPUtil.getDecoderStream(keyIn), new JcaKeyFingerprintCalculator()
        );
        Iterator keyRingIter = collection.getKeyRings();
        while (keyRingIter.hasNext()) {
            PGPPublicKeyRing keyRing = (PGPPublicKeyRing) keyRingIter.next();

            Iterator keyIter = keyRing.getPublicKeys();
            while (keyIter.hasNext()) {
                PGPPublicKey key = (PGPPublicKey) keyIter.next();
                if (key.isEncryptionKey()) {
                    response = new MEGPublicKeyRing(keyRing);
                    break;
                }
            }
        }
        return response;
    }

    public static MEGPublicKeyRing fromFile(
            Context context
    )
            throws IOException, PGPException
    {
        return fromFile(context, Constants.PUBLICKEYRING_FILENAME);
    }

    public PGPPublicKey getMasterPublicKey() {
        PGPPublicKey response = null;
        Iterator keyIter = mKeyRing.getPublicKeys();
        while (keyIter.hasNext()) {
            PGPPublicKey key = (PGPPublicKey) keyIter.next();
            if (key.isEncryptionKey() && !key.hasRevocation()) {
                response = key;
                break;
            }
        }
        return response;
    }

    public PGPPublicKey getRevocationKey() {
        PGPPublicKey response = null;
        Iterator keyIter = mKeyRing.getPublicKeys();
        while (keyIter.hasNext()) {
            PGPPublicKey key = (PGPPublicKey) keyIter.next();
            if (key.hasRevocation()) {
                response = key;
                break;
            }
        }
        return response;
    }

    public void toFile(
            Context context
    )
            throws IOException
    {
        FileOutputStream pubKeyRingOutput = context.openFileOutput(
                Constants.PUBLICKEYRING_FILENAME, Context.MODE_PRIVATE
        );
        mKeyRing.encode(pubKeyRingOutput);
        pubKeyRingOutput.close();
    }

    public PGPPublicKeyRing getKeyRing() {
        return mKeyRing;
    }
}

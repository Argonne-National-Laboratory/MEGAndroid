package gov.anl.coar.meg.pgp;

import android.content.Context;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPPublicKeyRingCollection;
import org.spongycastle.openpgp.PGPUtil;
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
        File pubkeyRingFile = new File(context.getFilesDir(), filename);
        InputStream keyIn = new BufferedInputStream(new FileInputStream(pubkeyRingFile.getPath()));
        return fromInputStream(keyIn);
    }

    public static MEGPublicKeyRing fromFile(
            Context context
    )
            throws IOException, PGPException
    {
        return fromFile(context, Constants.PUBLICKEYRING_FILENAME);
    }

    public static MEGPublicKeyRing fromInputStream(
            InputStream inStream
    )
            throws IOException, PGPException
    {
        PGPPublicKeyRingCollection collection = new PGPPublicKeyRingCollection(
                PGPUtil.getDecoderStream(inStream), new JcaKeyFingerprintCalculator()
        );
        Iterator keyRingIter = collection.getKeyRings();
        while (keyRingIter.hasNext()) {
            PGPPublicKeyRing keyRing = (PGPPublicKeyRing) keyRingIter.next();

            Iterator keyIter = keyRing.getPublicKeys();
            while (keyIter.hasNext()) {
                PGPPublicKey key = (PGPPublicKey) keyIter.next();
                if (key.isEncryptionKey()) {
                    return new MEGPublicKeyRing(keyRing);
                }
            }
        }
        throw new IllegalArgumentException("Could not get a new public key ring from stream");
    }

    public PGPPublicKey getMasterPublicKey() {
        Iterator keyIter = mKeyRing.getPublicKeys();
        while (keyIter.hasNext()) {
            PGPPublicKey key = (PGPPublicKey) keyIter.next();
            if (key.isEncryptionKey() && !key.hasRevocation()) {
                return key;
            }
        }
        throw new IllegalArgumentException("Could not get master public key!");
    }

    public PGPPublicKey getRevocationKey() {
        Iterator keyIter = mKeyRing.getPublicKeys();
        while (keyIter.hasNext()) {
            PGPPublicKey key = (PGPPublicKey) keyIter.next();
            if (key.hasRevocation()) {
                return key;
            }
        }
        throw new IllegalArgumentException("Could not find revocation key!");
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
}

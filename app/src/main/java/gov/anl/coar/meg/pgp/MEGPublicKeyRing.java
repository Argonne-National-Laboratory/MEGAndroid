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
public class MEGPublicKeyRing extends PGPPublicKeyRing {
    PGPPublicKeyRing mKeyRing;

    public MEGPublicKeyRing(byte[] encoding, KeyFingerPrintCalculator fingerPrintCalculator) throws IOException {
        super(encoding, fingerPrintCalculator);
    }

    public static MEGPublicKeyRing fromFile(
            Context context
    )
            throws IOException, PGPException
    {
        MEGPublicKeyRing response = null;
        File pubkeyRingFile = new File(context.getFilesDir(), Constants.PUBLICKEYRING_FILENAME);
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
                    response = (MEGPublicKeyRing) keyRing;
                    break;
                }
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
        this.encode(pubKeyRingOutput);
        pubKeyRingOutput.close();
    }
}

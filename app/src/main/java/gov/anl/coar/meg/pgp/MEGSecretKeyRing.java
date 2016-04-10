package gov.anl.coar.meg.pgp;

import android.content.Context;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSecretKeyRingCollection;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import gov.anl.coar.meg.Constants;

/**
 * Created by greg on 4/9/16.
 */
public class MEGSecretKeyRing {
    static PGPSecretKeyRing fromFile(
            Context context
    )
            throws IllegalArgumentException, IOException, PGPException
    {
        File secRingFile = new File(context.getFilesDir(), Constants.SECRETKEYRING_FILENAME);
        InputStream secIn = new BufferedInputStream(new FileInputStream(secRingFile));
        PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(
                PGPUtil.getDecoderStream(secIn), new JcaKeyFingerprintCalculator()
        );
        Iterator ringIter = pgpSec.getKeyRings();
        while (ringIter.hasNext()) {
            PGPSecretKeyRing ring = (PGPSecretKeyRing) ringIter.next();
            Iterator keyIter = ring.getSecretKeys();
            while (keyIter.hasNext()) {
                PGPSecretKey secretKey = (PGPSecretKey) keyIter.next();
                if (secretKey.isMasterKey()) {
                    return ring;
                }
            }
        }
        throw new IllegalArgumentException("Unable to get secret key ring from file: " + Constants.SECRETKEYRING_FILENAME);
    }
}

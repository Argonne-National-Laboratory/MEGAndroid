package gov.anl.coar.meg.pgp;

import android.app.Application;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import java.io.IOException;

import gov.anl.coar.meg.Constants;

/**
 * Created by greg on 3/17/16.
 */
public class PrivateKeyCache extends Application {
    private static PGPSecretKeyRing mSecretKeyRing;
    private static PGPPrivateKey mPrivateKey;

    void unlockSecretKey(char[] passphrase) throws PGPException {
        mPrivateKey = mSecretKeyRing.getSecretKey().extractPrivateKey(
                new JcePBESecretKeyDecryptorBuilder().setProvider(Constants.SPONGY_CASTLE).build(passphrase)
        );
    }

    // No modifier means its only available to the package
    PGPSecretKeyRing getSecretKeyRing() {
        return mSecretKeyRing;
    }

    PGPPrivateKey getPrivateKey() {
        return mPrivateKey;
    }

    void setSecretKeyRing(PGPSecretKeyRing secretKeyRing) throws IOException {
        mSecretKeyRing = secretKeyRing;
    }
}

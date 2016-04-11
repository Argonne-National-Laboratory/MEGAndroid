package gov.anl.coar.meg.pgp;

import android.app.Application;
import android.content.Context;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import java.io.IOException;

import gov.anl.coar.meg.Constants;

/**
 * Created by greg on 3/17/16.
 */
public class PrivateKeyCache extends Application {
    // I need some kind of constructor to set up the SpongyCastle security provider.
    private static PGPSecretKeyRing mSecretKeyRing;
    // We need to do some investigation on the persistence of this item. Will it be
    // alive for the entire time the phone is alive? The last thing the user wants is
    // to have to continuously log in to MEG over and over again.
    // if this isn't the right path to take then passing the Private Key around between
    // java classes might be preferable. I'm currently not sure how to go about doing this
    // though in a way that can ensure persistence. A reasonable explanation of this
    // whole thing can be found here
    // http://www.developerphil.com/dont-store-data-in-the-application-object/
    //
    // For now a global object under the Application should work ok. Especially as
    // we try to get MEG into a place where it can be demo'd. It's just when
    // we actually have real users we want to consider them
    private static PGPPrivateKey mPrivateKey;

    public void refreshPK(
            Context context,
            char[] passphrase
    )
        throws Exception
    {
        mSecretKeyRing = MEGSecretKeyRing.fromFile(context);
        mPrivateKey = mSecretKeyRing.getSecretKey().extractPrivateKey(
                new JcePBESecretKeyDecryptorBuilder().setProvider(Constants.SPONGY_CASTLE).build(passphrase)
        );
    }

    public boolean needsRefresh()
    {
        if (mPrivateKey == null)
            return true;
        else
            return false;
    }
    // No modifier means its only available to the package
    PGPSecretKeyRing getSecretKeyRing() {
        return mSecretKeyRing;
    }

    PGPSecretKey getSecretKey() {
        return mSecretKeyRing.getSecretKey();
    }

    PGPPrivateKey getPrivateKey() {
        return mPrivateKey;
    }

    void setSecretKeyRing(PGPSecretKeyRing secretKeyRing) throws IOException {
        mSecretKeyRing = secretKeyRing;
    }
}

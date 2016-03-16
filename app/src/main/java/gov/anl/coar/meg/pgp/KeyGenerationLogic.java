package gov.anl.coar.meg.pgp;

import android.content.Context;

import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyPair;
import org.spongycastle.openpgp.PGPKeyRingGenerator;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.operator.PGPDigestCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.exception.InvalidKeyException;

/**
 * Created by greg on 3/8/16.
 */
public class KeyGenerationLogic {

    public KeyGenerationLogic() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private PGPKeyPair generateNewKeyPair()
            throws InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException, PGPException
    {
        // Make the algorithm configurable in the future
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(Constants.RSA, Constants.SPONGY_CASTLE);
        // Probably can make number of bytes configurable in the futue
        kpg.initialize(Constants.ENCRYPTION_BITS, new SecureRandom());
        KeyPair keyPair = kpg.generateKeyPair();
        if (keyPair == null) {
            throw new InvalidKeyException();
        }
        PGPKeyPair pgpKeyPair = new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, keyPair, new Date());
        return pgpKeyPair;
    }

    public PGPKeyRingGenerator generateKeyRing(
            String identity,
            char[] password
    )
            throws NoSuchAlgorithmException, PGPException, NoSuchProviderException, InvalidKeyException
    {
        PGPKeyPair keyRingPair = generateNewKeyPair();
        PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder()
                .build().get(HashAlgorithmTags.SHA1);
        PGPKeyRingGenerator keyRingGenerator = new PGPKeyRingGenerator(
                PGPSignature.POSITIVE_CERTIFICATION, keyRingPair, identity, sha1Calc, null, null,
                new JcaPGPContentSignerBuilder(keyRingPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1),
                new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha1Calc).setProvider(Constants.SPONGY_CASTLE).build(password)
        );
        return keyRingGenerator;
    }

    public void generateNewKeyRingAndKeys(
            Context context,
            String firstName,
            String lastName,
            String email,
            char[] password
    )
            throws InvalidKeyException, PGPException, IOException
    {
        try {
            String identity = firstName + " " + lastName + " " + email;
            PGPKeyPair keyPair = generateNewKeyPair();
            PGPKeyRingGenerator keyRingGenerator = generateKeyRing(identity, password);
            PGPSecretKeyRing secretKeyRing = keyRingGenerator.generateSecretKeyRing();
            PGPPublicKeyRing publicKeyRing = keyRingGenerator.generatePublicKeyRing();
            writeRings(context, secretKeyRing, publicKeyRing);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
    }

        private void writeRings(
                Context context,
                PGPSecretKeyRing secretKeyRing,
                PGPPublicKeyRing pubKeyRing
        )
                throws IOException
        {
                File secretKeyRingFile = new File(context.getFilesDir(), Constants.SECRETKEYRING_FILENAME);
                FileOutputStream secKeyRingOutput = new FileOutputStream(secretKeyRingFile);
                secretKeyRing.encode(secKeyRingOutput);
                secKeyRingOutput.close();
                File publicKeyRingFile = new File(context.getFilesDir(), Constants.PUBLICKEYRING_FILENAME);
                FileOutputStream pubKeyRingOutput = new FileOutputStream(publicKeyRingFile);
                pubKeyRing.encode(pubKeyRingOutput);
                pubKeyRingOutput.close();
        }
}

package gov.anl.coar.meg.pgp;

import android.content.Context;

import org.spongycastle.bcpg.PublicKeyPacket;
import org.spongycastle.bcpg.sig.RevocationKey;
import org.spongycastle.bcpg.sig.RevocationReasonTags;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;

import java.io.IOException;

import gov.anl.coar.meg.Constants;

/**
 * Created by greg on 4/6/16.
 */
public class MEGRevocationKey {
    PGPPublicKey mRevocationKey;

    public MEGRevocationKey(PGPPublicKey revocationKey) {
        mRevocationKey = revocationKey;
    }

    protected static MEGRevocationKey generate(
            PGPPublicKey publicKeyToRevoke,
            PGPPrivateKey privateKey,
            String description
    )
            throws PGPException
    {
        PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(
                publicKeyToRevoke.getAlgorithm(),
                PGPUtil.SHA1
        ).setProvider(Constants.SPONGY_CASTLE));
        signatureGenerator.init(PGPSignature.KEY_REVOCATION, privateKey);
        PGPSignatureSubpacketGenerator subGenerator = new PGPSignatureSubpacketGenerator();
        byte[] fingerprint = new JcaKeyFingerprintCalculator().calculateFingerprint(publicKeyToRevoke.getPublicKeyPacket());
        // TODO not sure what isCritical means
        boolean isCritical = true;
        subGenerator.setRevocationKey(isCritical, publicKeyToRevoke.getAlgorithm(), fingerprint);
        subGenerator.setRevocationReason(isCritical, RevocationReasonTags.KEY_RETIRED, description);
        PGPSignatureSubpacketVector vector = subGenerator.generate();
        signatureGenerator.setHashedSubpackets(vector);
        PGPPublicKey revocationKey = PGPPublicKey.addCertification(publicKeyToRevoke, signatureGenerator.generate());
        return new MEGRevocationKey(revocationKey);
    }

    /**
     * Add the revocation key to the public key ring. Overwrite the key ring file
     */
    protected void toFile(
            Context context,
            MEGPublicKeyRing megPublicKeyRing
    )
            throws IOException
    {
        PGPPublicKeyRing publicKeyRing = megPublicKeyRing.getKeyRing();
        PGPPublicKeyRing modifiedRing = PGPPublicKeyRing.insertPublicKey(publicKeyRing, mRevocationKey);
        MEGPublicKeyRing newRing = new MEGPublicKeyRing(modifiedRing);
        newRing.toFile(context);
    }

    protected PGPPublicKey fromKeyRing(PGPPublicKeyRing keyRing) {
        // TODO
        return keyRing.getPublicKey();
    }
}

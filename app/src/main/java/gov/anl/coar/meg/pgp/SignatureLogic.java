package gov.anl.coar.meg.pgp;

import android.app.Application;
import android.renderscript.ScriptGroup;

import org.spongycastle.bcpg.BCPGOutputStream;
import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedDataGenerator;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.spongycastle.util.Arrays;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;

import gov.anl.coar.meg.Constants;

/**
 * Created by greg on 5/10/16.
 */
public class SignatureLogic {
    private static final String TAG = "SignatureLogic";

    public SignatureLogic() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public byte[] sign(
            Application application,
            OutputStream encrypted
    )
            throws PGPException, IOException
    {
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        PrivateKeyCache pkCache = (PrivateKeyCache) application;
        if (pkCache.needsRefresh())
            throw new IllegalArgumentException("Private key is not cached. Cannot sign message");

        final PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(
                pkCache.getSecretKey().getPublicKey()
                .getAlgorithm(), HashAlgorithmTags.SHA1).setProvider(new BouncyCastleProvider()));
        signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, pkCache.getPrivateKey());
        final Iterator<?> it = pkCache.getSecretKey().getPublicKey().getUserIDs();
        if (it.hasNext()) {
            final PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            spGen.setSignerUserID(false, (String) it.next());
            signatureGenerator.setHashedSubpackets(spGen.generate());
        }
        signatureGenerator.generateOnePassVersion(false).encode(encrypted);

        final PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
        final OutputStream literalOut = literalDataGenerator.open(
                encrypted, PGPLiteralData.BINARY, "", new Date(), new byte[4096]
        );
        final byte[] buf = new byte[4096];
        for (int len = 0; (len = inputStream.read(buf)) > 0;) {
            literalOut.write(buf, 0, len);
            signatureGenerator.update(buf, 0, len);
        }
        literalDataGenerator.close();
        signatureGenerator.generate().encode(encrypted);
        compressedDataGenerator.close();
        encryptedDataGenerator.close();
    }

    public void validate(
            PGPPublicKey signedWith,
            InputStream input
    )
            throws IOException, PGPException, BadSignatureException
    {
        boolean isValid = validateSignature(signedWith, input);
        // TODO should send message back to client warning them
        if (!isValid) {
            throw new BadSignatureException();
        }
    }

    public String getKeyId(
        InputStream input
    )
            throws IOException, PGPException
    {
        PGPSignature sig = getSignature(input);
        return Long.toHexString(sig.getKeyID()).toUpperCase();
    }

    private boolean validateSignature(
        PGPPublicKey signedWith,
        InputStream input
    )
            throws IOException, PGPException
    {
        PGPSignature sig = getSignature(input);
        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider(Constants.SPONGY_CASTLE), signedWith);

        int ch;
        input.reset();
        while ((ch = input.read()) >= 0) {
            sig.update((byte)ch);
        }

        input.close();
        return sig.verify();
    }

    private PGPSignature getSignature(
            InputStream input
    )
            throws IOException, PGPException
    {
        InputStream decoderInput = PGPUtil.getDecoderStream(input);

        JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(decoderInput);
        PGPSignatureList pgpSigList;

        Object obj = pgpFact.nextObject();
        if (obj instanceof PGPCompressedData) {
            PGPCompressedData c1 = (PGPCompressedData) obj;
            pgpFact = new JcaPGPObjectFactory(c1.getDataStream());
            pgpSigList = (PGPSignatureList) pgpFact.nextObject();
        } else {
            pgpSigList = (PGPSignatureList) obj;
        }

        return pgpSigList.get(0);
    }
}

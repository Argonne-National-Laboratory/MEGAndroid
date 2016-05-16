package gov.anl.coar.meg.pgp;

import org.spongycastle.bcpg.BCPGOutputStream;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;

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
            PrivateKeyCache pkCache,
            byte[] msg
    )
            throws PGPException, IOException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(msg);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(msg);

        PGPSignatureGenerator sGen = new PGPSignatureGenerator(
                new JcaPGPContentSignerBuilder(
                        pkCache.getSecretKey().getPublicKey().getAlgorithm(), PGPUtil.SHA1
                ).setProvider(Constants.SPONGY_CASTLE)
        );
        sGen.init(PGPSignature.BINARY_DOCUMENT, pkCache.getPrivateKey());
        BCPGOutputStream bOut = new BCPGOutputStream(out);
        int ch;
        while ((ch = bais.read()) >= 0) {
            sGen.update((byte)ch);
        }
        bais.close();
        sGen.generate().encode(bOut);
        out.close();
        return out.toByteArray();
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

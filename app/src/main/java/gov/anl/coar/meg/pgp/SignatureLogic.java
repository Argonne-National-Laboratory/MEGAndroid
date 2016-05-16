package gov.anl.coar.meg.pgp;

import android.util.Log;

import org.spongycastle.bcpg.BCPGOutputStream;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.bc.BcPGPObjectFactory;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;

import gov.anl.coar.meg.Constants;

/**
 * Created by greg on 5/10/16.
 */
public class SignatureLogic {
    private static final String TAG = "SignatureLogic";
    public static final byte[] DELINEATOR = "&&^&&***&&^&&".getBytes();

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
        out.write(DELINEATOR);
        out.write(msg);
        out.close();
        return out.toByteArray();
    }

    public String getKeyId(
            byte[] signature
    )
            throws IOException, PGPException
    {
        PGPSignature sig = getSignature(signature);
        return Long.toHexString(sig.getKeyID()).toUpperCase();
    }

    private int indexOfDelineator(byte[] array) {
        for(int i = 0; i < array.length - DELINEATOR.length+1; ++i) {
            boolean found = true;
            for(int j = 0; j < DELINEATOR.length; ++j) {
                if (array[i+j] != DELINEATOR[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }

    public ArrayList<byte[]> splitSignatureAndMessage(
            byte[] bytesIn
    )
            throws IllegalArgumentException
    {
        ArrayList<byte[]> output = new ArrayList<>(2);
        int idx = indexOfDelineator(bytesIn);
        if (idx == -1)
            throw new IllegalArgumentException("Could not find delineator in message");
        // Get the byte array for the signature
        output.add(Arrays.copyOfRange(bytesIn, 0, idx));
        // Get the byte array for the message
        output.add(Arrays.copyOfRange(bytesIn, idx + DELINEATOR.length, bytesIn.length));
        return output;
    }

    public void validateSignature(
        PGPPublicKey signedWith,
        byte[] signature,
        byte[] msg
    )
            throws IOException, PGPException, BadSignatureException
    {
        PGPSignature sig = getSignature(signature);
        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider(Constants.SPONGY_CASTLE), signedWith);

        ByteArrayInputStream bais = new ByteArrayInputStream(msg);
        int ch;
        while ((ch = bais.read()) >= 0) {
            sig.update((byte)ch);
        }
        bais.close();
        if (!sig.verify()) {
            throw new BadSignatureException("The signature found is invalid.");
        }
    }

    private PGPSignature getSignature(
            byte[] bytesIn
    )
            throws IOException, PGPException
    {
        PGPObjectFactory objectFactory = new BcPGPObjectFactory(bytesIn);
        PGPSignatureList pgpSigList;

        Object obj = objectFactory.nextObject();
        if (obj instanceof PGPCompressedData) {
            PGPCompressedData c1 = (PGPCompressedData) obj;
            objectFactory = new BcPGPObjectFactory(c1.getDataStream());
            pgpSigList = (PGPSignatureList) objectFactory.nextObject();
        } else {
            pgpSigList = (PGPSignatureList) obj;
        }

        return pgpSigList.get(0);
    }
}

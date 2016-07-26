package gov.anl.coar.meg.pgp;

import android.content.Context;
import android.util.Log;

import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPEncryptedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPOnePassSignatureList;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.spongycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.Util;

/**
 * Created by greg on 3/6/16.
 */
public class EncryptionLogic {
    // Motivating examples for this code can be found here:
    //
    // https://github.com/bcgit/bc-java/blob/master/pg/src/main/java/org/bouncycastle/openpgp/examples/KeyBasedFileProcessor.java
    //
    public static final String TAG = "EncryptionLogic";
    // I really need a way to mathematically compute the 372 constant. I know
    // it has to do with the key length which is 2048 bits = 256 bytes. I'm not sure
    // where the extra 116 bytes are coming from though. It will change for different
    // bit sizes too. So for 4096 the number of bytes is 672. Once again where the
    // additional 160 bytes come from I have no idea.
    public static final int BYTES_IN_ENCRYPTED_AES = 372;

    public EncryptionLogic() {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Since RSA doesn't do bulk encryption we get a previously encrypted message,
     * extract the portion with encrypted key data and AES encrypted message data. We
     * then decrypt the key data so we can decrypt the message data.
     *
     * @param bytesIn
     * @param privateKey
     * @return
     */
    public byte[] pgpDecrypt(
            byte[] bytesIn,
            PGPPrivateKey privateKey
    )
            throws IOException, InvalidCipherTextException
    {
        KeyGenerationLogic keygen = new KeyGenerationLogic();
        // Cut up the message. Get the message portion and the key portion
        byte[] encKeyData = Arrays.copyOfRange(bytesIn, bytesIn.length - BYTES_IN_ENCRYPTED_AES, bytesIn.length);
        byte[] encMsgData = Arrays.copyOfRange(bytesIn, 0, bytesIn.length - BYTES_IN_ENCRYPTED_AES);
        // Decrypt the key portion to get key data we need.
        BufferedInputStream decKeyData = decryptWithPK(new ByteArrayInputStream(encKeyData), privateKey);
        byte[] keyDecBytes = Util.inputStreamToOutputStream(decKeyData).toByteArray();
        decKeyData.close();
        byte[] keyBytes = Arrays.copyOfRange(keyDecBytes, 0, Constants.AES_KEY_BYTES);
        byte[] ivBytes = Arrays.copyOfRange(keyDecBytes, Constants.AES_KEY_BYTES, keyDecBytes.length);
        BufferedBlockCipher cipher = keygen.generateSymmetricKey(keyBytes, ivBytes, false);
        return performSymmetricKeyAction(cipher, encMsgData);
    }

    /**
     * Since RSA doesn't do bulk encryption we need to generate a random AES key
     * encrypt the message with this key, then save the message parameters to the
     * end of the message with the RSA key so that the receiver will understand
     * how to decrypt the message.
     *
     * @param bytesIn
     * @param encKey
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws InvalidCipherTextException
     * @throws PGPException
     */
    public ByteArrayOutputStream pgpEncrypt(
            byte[] bytesIn,
            PGPPublicKey encKey
    )
            throws NoSuchAlgorithmException, IOException, InvalidCipherTextException, PGPException
    {
        KeyGenerationLogic keygen = new KeyGenerationLogic();
        ArrayList<byte[]> keyData = keygen.generateRandomKeyAndIV();
        ByteArrayOutputStream keyDataConcat = new ByteArrayOutputStream();
        keyDataConcat.write(keyData.get(0));
        keyDataConcat.write(keyData.get(1));
        ByteArrayOutputStream encKeyData = encryptWithPubKey(keyDataConcat.toByteArray(), encKey);
        keyDataConcat.close();
        BufferedBlockCipher cipher = keygen.generateSymmetricKey(keyData.get(0), keyData.get(1), true);
        byte[] encMsg = performSymmetricKeyAction(cipher, bytesIn);
        ByteArrayOutputStream output = Util.inputStreamToOutputStream(new ByteArrayInputStream(encMsg));
        output.write(encKeyData.toByteArray());
        encKeyData.close();
        return output;
    }

    /**
     * Decrypt data using our private key.
     *
     * @param inBuffer
     * @param privateKey
     * @return
     * @throws IllegalArgumentException
     */
    private BufferedInputStream decryptWithPK(
            InputStream inBuffer,
            PGPPrivateKey privateKey
    )
        throws IllegalArgumentException
    {
        try {
            InputStream input = PGPUtil.getDecoderStream(inBuffer);
            JcaPGPObjectFactory pgpFactory = new JcaPGPObjectFactory(input);
            Object object = pgpFactory.nextObject();
            PGPEncryptedDataList encrypted;
            // the first object might be a PGP marker packet.
            if (object instanceof PGPEncryptedDataList)
                encrypted = (PGPEncryptedDataList) object;
            else
                encrypted = (PGPEncryptedDataList) pgpFactory.nextObject();
            PGPPublicKeyEncryptedData pbe = (PGPPublicKeyEncryptedData) encrypted.getEncryptedDataObjects().next();
            Log.d(TAG, "Message intended for id: " + Long.toHexString(pbe.getKeyID()).toUpperCase() + " using key id: " + Long.toHexString(privateKey.getKeyID()).toUpperCase() + " to decrypt");
            InputStream clearText = pbe.getDataStream(
                    new JcePublicKeyDataDecryptorFactoryBuilder().setProvider(Constants.SPONGY_CASTLE).build(privateKey)
            );
            JcaPGPObjectFactory plainFact = new JcaPGPObjectFactory(clearText);
            Object message = plainFact.nextObject();
            if (message instanceof PGPCompressedData) {
                PGPCompressedData cData = (PGPCompressedData) message;
                JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(cData.getDataStream());
                message = pgpFact.nextObject();
            }
            if (message instanceof PGPLiteralData) {
                PGPLiteralData literal = (PGPLiteralData) message;
                return new BufferedInputStream(literal.getInputStream());
            } else if (message instanceof PGPOnePassSignatureList) {
                Log.e(TAG, "Message contains a signed message not literal data!");
            } else {
                Log.e(TAG, "Message contains an unknown type of data!");
            }
        } catch (Exception e) {
            // TODO this is a bit clunky and is a remnant of debugging.
            e.printStackTrace();
        }
        throw new IllegalArgumentException("Unable to decrypt message.");
    }

    /**
     * Encrypt data with our public key
     *
     * @param clearData
     * @param encKey
     * @return
     * @throws IOException
     * @throws PGPException
     */
    private ByteArrayOutputStream encryptWithPubKey(
            byte[] clearData,
            PGPPublicKey encKey
    )
            throws IOException, PGPException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(
                PGPCompressedDataGenerator.ZIP);
        PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
        OutputStream pOut = lData.open(
                comData.open(bOut), PGPLiteralData.BINARY, Constants.TAG, clearData.length, new Date());
        pOut.write(clearData);
        comData.close();
        pOut.close();

        PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
                new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5).setWithIntegrityPacket(
                        true
                ).setSecureRandom(new SecureRandom()).setProvider(Constants.SPONGY_CASTLE)
        );
        encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(encKey).setProvider(Constants.SPONGY_CASTLE));
        OutputStream cOut = encGen.open(out, bOut.toByteArray().length);
        cOut.write(bOut.toByteArray());
        cOut.close();
        return out;
    }

    /**
     * Decrypt data that was symmetrically encrypted by the mail client
     *
     * @param context
     * @param bytesIn
     * @return
     * @throws IOException
     * @throws InvalidCipherTextException
     */
    public byte[] decryptClientSymmetricData(
            Context context,
            byte[] bytesIn,
            String clientId
    )
            throws IOException, InvalidCipherTextException
    {
        KeyGenerationLogic keygen = new KeyGenerationLogic();
        BufferedBlockCipher cipher = keygen.generateSymmetricKeyFromQRData(context, clientId, false);
        return performSymmetricKeyAction(cipher, bytesIn);
    }

    /**
     * Symmetrically encrypt data that is bound for the client
     *
     * @param context
     * @param bytesIn
     * @return
     * @throws IOException
     * @throws InvalidCipherTextException
     */
    public byte[] encryptAsClientBoundSymmetricData(
            Context context,
            byte[] bytesIn,
            String clientId
    )
            throws IOException, InvalidCipherTextException
    {
        KeyGenerationLogic keygen = new KeyGenerationLogic();
        BufferedBlockCipher cipher = keygen.generateSymmetricKeyFromQRData(context, clientId, true);
        return performSymmetricKeyAction(cipher, bytesIn);
    }

    /**
     * Either decrypt or encrypt some inputted buffer using a supplied cipher.
     *
     * @param cipher
     * @param bytesIn
     * @return
     * @throws IOException
     * @throws InvalidCipherTextException
     */
    private byte[] performSymmetricKeyAction(
            BufferedBlockCipher cipher,
            byte[] bytesIn
    )
            throws IOException, InvalidCipherTextException
    {
        int buflen = cipher.getOutputSize(bytesIn.length);
        byte[] bytesOut = new byte[buflen];
        int nBytesEnc = cipher.processBytes(bytesIn, 0, bytesIn.length, bytesOut, 0);
        cipher.doFinal(bytesOut, nBytesEnc);
        return bytesOut;
    }
}

/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This software has been modified by Greg Rehm 3/9/16
 */

package gov.anl.coar.meg.pgp;


import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Date;
import java.util.Map;

import org.spongycastle.bcpg.S2K;
import org.spongycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.PGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPKeyConverter;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.exception.PgpGeneralException;
import gov.anl.coar.meg.exception.PgpKeyNotFoundException;
import gov.anl.coar.meg.util.Log;
import gov.anl.coar.meg.util.Passphrase;


/**
 * Wrapper for a PGPSecretKey.
 * <p/>
 * This object can only be obtained from a WrappedSecretKeyRing, and stores a
 * back reference to its parent.
 * <p/>
 * This class represents known secret keys which are stored in the database.
 * All "crypto operations using a known secret key" should be implemented in
 * this class, to ensure on type level that these operations are performed on
 * properly imported secret keys only.
 */
public class CanonicalizedSecretKey extends CanonicalizedPublicKey {

    private final PGPSecretKey mSecretKey;
    private PGPPrivateKey mPrivateKey = null;

    private int mPrivateKeyState = PRIVATE_KEY_STATE_LOCKED;
    final private static int PRIVATE_KEY_STATE_LOCKED = 0;
    final private static int PRIVATE_KEY_STATE_UNLOCKED = 1;
    final private static int PRIVATE_KEY_STATE_DIVERT_TO_CARD = 2;

    CanonicalizedSecretKey(CanonicalizedSecretKeyRing ring, PGPSecretKey key) {
        super(ring, key.getPublicKey());
        mSecretKey = key;
    }

    public CanonicalizedSecretKeyRing getRing() {
        return (CanonicalizedSecretKeyRing) mRing;
    }

    public enum SecretKeyType {
        UNAVAILABLE(0), GNU_DUMMY(1), PASSPHRASE(2), PASSPHRASE_EMPTY(3), DIVERT_TO_CARD(4), PIN(5),
        PATTERN(6);

        final int mNum;

        SecretKeyType(int num) {
            mNum = num;
        }

        public static SecretKeyType fromNum(int num) {
            switch (num) {
                case 1:
                    return GNU_DUMMY;
                case 2:
                    return PASSPHRASE;
                case 3:
                    return PASSPHRASE_EMPTY;
                case 4:
                    return DIVERT_TO_CARD;
                case 5:
                    return PIN;
                case 6:
                    return PATTERN;
                // if this case happens, it's probably a check from a database value
                default:
                    return UNAVAILABLE;
            }
        }

        public int getNum() {
            return mNum;
        }

        public boolean isUsable() {
            return this != UNAVAILABLE && this != GNU_DUMMY;
        }

    }

    /**
     * Returns true on right passphrase
     */
    public boolean unlock(final Passphrase passphrase) throws PgpGeneralException {
        // handle keys on OpenPGP cards like they were unlocked
        S2K s2k = mSecretKey.getS2K();
        if (s2k != null
                && s2k.getType() == S2K.GNU_DUMMY_S2K
                && s2k.getProtectionMode() == S2K.GNU_PROTECTION_MODE_DIVERT_TO_CARD) {
            mPrivateKeyState = PRIVATE_KEY_STATE_DIVERT_TO_CARD;
            return true;
        }

        // try to extract keys using the passphrase
        try {

            int keyEncryptionAlgorithm = mSecretKey.getKeyEncryptionAlgorithm();
            if (keyEncryptionAlgorithm == SymmetricKeyAlgorithmTags.NULL) {
                mPrivateKey = mSecretKey.extractPrivateKey(null);
                mPrivateKeyState = PRIVATE_KEY_STATE_UNLOCKED;
                return true;
            }

            byte[] sessionKey;
            sessionKey = passphrase.getCachedSessionKeyForParameters(keyEncryptionAlgorithm, s2k);
            PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                    Constants.SPONGY_CASTLE).build(passphrase.getCharArray());
            if (sessionKey == null) {
                // this operation is EXPENSIVE, so we cache its result in the passed Passphrase object!
                sessionKey = keyDecryptor.makeKeyFromPassPhrase(keyEncryptionAlgorithm, s2k);
                passphrase.addCachedSessionKeyForParameters(keyEncryptionAlgorithm, s2k, sessionKey);
            }
            mPrivateKey = mSecretKey.extractPrivateKey(keyDecryptor);
            mPrivateKeyState = PRIVATE_KEY_STATE_UNLOCKED;
        } catch (PGPException e) {
            return false;
        }
        if (mPrivateKey == null) {
            throw new PgpGeneralException("error extracting key");
        }
        return true;
    }

    private PGPContentSignerBuilder getContentSignerBuilder(int hashAlgo,
            Map<ByteBuffer,byte[]> signedHashes) {
        return new JcaPGPContentSignerBuilder(
                mSecretKey.getPublicKey().getAlgorithm(), hashAlgo)
                .setProvider(Constants.SPONGY_CASTLE);
    }

    public PGPSignatureGenerator getCertSignatureGenerator(Map<ByteBuffer, byte[]> signedHashes) {
        PGPContentSignerBuilder contentSignerBuilder = getContentSignerBuilder(
                PgpSecurityConstants.CERTIFY_HASH_ALGO, signedHashes);

        if (mPrivateKeyState == PRIVATE_KEY_STATE_LOCKED) {
            throw new PrivateKeyNotUnlockedException();
        }

        PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
        try {
            signatureGenerator.init(PGPSignature.DEFAULT_CERTIFICATION, mPrivateKey);
            return signatureGenerator;
        } catch (PGPException e) {
            Log.e(Constants.TAG, "signing error", e);
            return null;
        }
    }

    public PGPSignatureGenerator getDataSignatureGenerator(int hashAlgo, boolean cleartext,
            Map<ByteBuffer, byte[]> signedHashes, Date creationTimestamp)
            throws PgpGeneralException {
        if (mPrivateKeyState == PRIVATE_KEY_STATE_LOCKED) {
            throw new PrivateKeyNotUnlockedException();
        }

        // We explicitly create a signature creation timestamp in this place.
        // That way, we can inject an artificial one from outside, ie the one
        // used in previous runs of this function.
        if (creationTimestamp == null) {
            // to sign using nfc PgpSignEncrypt is executed two times.
            // the first time it stops to return the PendingIntent for nfc connection and signing the hash
            // the second time the signed hash is used.
            // to get the same hash we cache the timestamp for the second round!
            creationTimestamp = new Date();
        }

        PGPContentSignerBuilder contentSignerBuilder = getContentSignerBuilder(hashAlgo, signedHashes);

        int signatureType;
        if (cleartext) {
            // for sign-only ascii text (cleartext signature)
            signatureType = PGPSignature.CANONICAL_TEXT_DOCUMENT;
        } else {
            signatureType = PGPSignature.BINARY_DOCUMENT;
        }

        try {
            PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(contentSignerBuilder);
            signatureGenerator.init(signatureType, mPrivateKey);

            PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            spGen.setSignerUserID(false, mRing.getPrimaryUserIdWithFallback());
            spGen.setSignatureCreationTime(false, creationTimestamp);
            signatureGenerator.setHashedSubpackets(spGen.generate());
            return signatureGenerator;
        } catch (PgpKeyNotFoundException | PGPException e) {
            // TODO: simply throw PGPException!
            throw new PgpGeneralException("Error initializing signature!", e);
        }
    }

    // For use only in card export; returns the secret key in Chinese Remainder Theorem format.
    public RSAPrivateCrtKey getCrtSecretKey() throws PgpGeneralException {
        if (mPrivateKeyState == PRIVATE_KEY_STATE_LOCKED) {
            throw new PgpGeneralException("Cannot get secret key attributes while key is locked.");
        }

        if (mPrivateKeyState == PRIVATE_KEY_STATE_DIVERT_TO_CARD) {
            throw new PgpGeneralException("Cannot get secret key attributes of divert-to-card key.");
        }

        JcaPGPKeyConverter keyConverter = new JcaPGPKeyConverter();
        PrivateKey retVal;
        try {
            retVal = keyConverter.getPrivateKey(mPrivateKey);
        } catch (PGPException e) {
            throw new PgpGeneralException("Error converting private key!", e);
        }

        return (RSAPrivateCrtKey)retVal;
    }

    static class PrivateKeyNotUnlockedException extends RuntimeException {
        // this exception is a programming error which happens when an operation which requires
        // the private key is called without a previous call to unlock()
    }

    public UncachedSecretKey getUncached() {
        return new UncachedSecretKey(mSecretKey);
    }
}

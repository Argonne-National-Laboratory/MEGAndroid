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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.PublicKeyAlgorithmTags;
import org.spongycastle.bcpg.SignatureSubpacketTags;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPUserAttributeSubpacketVector;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import gov.anl.coar.meg.exception.PgpGeneralException;
import gov.anl.coar.meg.util.IterableIterator;
import gov.anl.coar.meg.util.Log;
import gov.anl.coar.meg.util.Utf8Util;

/** Wrapper around PGPKeyRing class, to be constructed from bytes.
 *
 * This class and its relatives UncachedPublicKey and UncachedSecretKey are
 * used to move around pgp key rings in non crypto related (UI, mostly) code.
 * It should be used for simple inspection only until it saved in the database,
 * all actual crypto operations should work with CanonicalizedKeyRings
 * exclusively.
 *
 * This class is also special in that it can hold either the PGPPublicKeyRing
 * or PGPSecretKeyRing derivate of the PGPKeyRing class, since these are
 * treated equally for most purposes in UI code. It is up to the programmer to
 * take care of the differences.
 *
 * @see CanonicalizedKeyRing
 * @see gov.anl.coar.meg.pgp.UncachedPublicKey
 * @see gov.anl.coar.meg.pgp.UncachedSecretKey
 *
 */
public class UncachedKeyRing {

    private static final String TAG = "UncachedKeyRing";
    final PGPKeyRing mRing;
    final boolean mIsSecret;


    private static final int CANONICALIZE_MAX_USER_IDS = 100;

    UncachedKeyRing(PGPKeyRing ring) {
        mRing = ring;
        mIsSecret = ring instanceof PGPSecretKeyRing;
    }

    public long getMasterKeyId() {
        return mRing.getPublicKey().getKeyID();
    }

    public UncachedPublicKey getPublicKey() {
        return new UncachedPublicKey(mRing.getPublicKey());
    }

    public UncachedPublicKey getPublicKey(long keyId) {
        return new UncachedPublicKey(mRing.getPublicKey(keyId));
    }

    public Iterator<UncachedPublicKey> getPublicKeys() {
        final Iterator<PGPPublicKey> it = mRing.getPublicKeys();
        return new Iterator<UncachedPublicKey>() {
            public void remove() {
                throw new UnsupportedOperationException();
            }
            public UncachedPublicKey next() {
                return new UncachedPublicKey(it.next());
            }
            public boolean hasNext() {
                return it.hasNext();
            }
        };
    }

    /** Returns the dynamic (though final) property if this is a secret keyring or not. */
    public boolean isSecret() {
        return mIsSecret;
    }

    public byte[] getEncoded() throws IOException {
        return mRing.getEncoded();
    }

    public void encode(OutputStream out) throws IOException {
        mRing.encode(out);
    }

    public byte[] getFingerprint() {
        return mRing.getPublicKey().getFingerprint();
    }

    public int getVersion() {
        return mRing.getPublicKey().getVersion();
    }

    public static UncachedKeyRing decodeFromData(byte[] data)
            throws PgpGeneralException, IOException {

        IteratorWithIOThrow<UncachedKeyRing> parsed = fromStream(new ByteArrayInputStream(data));

        if ( ! parsed.hasNext()) {
            throw new PgpGeneralException("Object not recognized as PGPKeyRing!");
        }

        UncachedKeyRing ring = parsed.next();

        if (parsed.hasNext()) {
            throw new PgpGeneralException("Expected single keyring in stream, found at least two");
        }

        return ring;

    }

    public static IteratorWithIOThrow<UncachedKeyRing> fromStream(final InputStream stream) {

        return new IteratorWithIOThrow<UncachedKeyRing>() {

            UncachedKeyRing mNext = null;
            PGPObjectFactory mObjectFactory = null;

            private void cacheNext() throws IOException {
                if (mNext != null) {
                    return;
                }

                try {
                    while (stream.available() > 0) {
                        // if there are no objects left from the last factory, create a new one
                        if (mObjectFactory == null) {
                            InputStream in = PGPUtil.getDecoderStream(stream);
                            mObjectFactory = new PGPObjectFactory(in, new JcaKeyFingerprintCalculator());
                        }

                        // go through all objects in this block
                        Object obj;
                        while ((obj = mObjectFactory.nextObject()) != null) {
                            Log.d(TAG, "Found class: " + obj.getClass());
                            if (!(obj instanceof PGPKeyRing)) {
                                Log.i(TAG, "Skipping object of bad type " + obj.getClass().getName() + " in stream");
                                // skip object
                                continue;
                            }
                            mNext = new UncachedKeyRing((PGPKeyRing) obj);
                            return;
                        }
                        // if we are past the while loop, that means the objectFactory had no next
                        mObjectFactory = null;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new IOException(e);
                }
            }

            @Override
            public boolean hasNext() throws IOException {
                cacheNext();
                return mNext != null;
            }

            @Override
            public UncachedKeyRing next() throws IOException {
                try {
                    cacheNext();
                    return mNext;
                } finally {
                    mNext = null;
                }
            }
        };

    }

    public interface IteratorWithIOThrow<E> {
        public boolean hasNext() throws IOException;
        public E next() throws IOException;
    }
    public void encodeArmored(OutputStream out, String version) throws IOException {
        ArmoredOutputStream aos = new ArmoredOutputStream(out);
        if (version != null) {
            aos.setHeader("Version", version);
        }
        aos.write(mRing.getEncoded());
        aos.close();
    }

    // An array of known algorithms. Note this must be numerically sorted for binarySearch() to work!
    static final int[] KNOWN_ALGORITHMS = new int[] {
        PublicKeyAlgorithmTags.RSA_GENERAL, // 1
        PublicKeyAlgorithmTags.RSA_ENCRYPT, // 2
        PublicKeyAlgorithmTags.RSA_SIGN, // 3
        PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT, // 16
        PublicKeyAlgorithmTags.DSA, // 17
        PublicKeyAlgorithmTags.ECDH, // 18
        PublicKeyAlgorithmTags.ECDSA, // 19
        PublicKeyAlgorithmTags.ELGAMAL_GENERAL, // 20
        // PublicKeyAlgorithmTags.DIFFIE_HELLMAN, // 21
    };

    /** "Canonicalizes" a public key, removing inconsistencies in the process.
     *
     * More specifically:
     *  - Remove all non-verifying self-certificates
     *  - Remove all "future" self-certificates
     *  - Remove all certificates flagged as "local"
     *  - For UID certificates, remove all certificates which are
     *      superseded by a newer one on the same target, including
     *      revocations with later re-certifications.
     *  - For subkey certifications, remove all certificates which
     *      are superseded by a newer one on the same target, unless
     *      it encounters a revocation certificate. The revocation
     *      certificate is considered to permanently revoke the key,
     *      even if contains later re-certifications.
     *  This is the "behavior in practice" used by (e.g.) GnuPG, and
     *  the rationale for both can be found as comments in the GnuPG
     *  source.
     *  UID signatures:
     *  https://github.com/mtigas/gnupg/blob/50c98c7ed6b542857ee2f902eca36cda37407737/g10/getkey.c#L1668-L1674
     *  Subkey signatures:
     *  https://github.com/mtigas/gnupg/blob/50c98c7ed6b542857ee2f902eca36cda37407737/g10/getkey.c#L1990-L1997
     *  - Remove all certificates in other positions if not of known type:
     *   - key revocation signatures on the master key
     *   - subkey binding signatures for subkeys
     *   - certifications and certification revocations for user ids
     *  - If a subkey retains no valid subkey binding certificate, remove it
     *  - If a user id retains no valid self certificate, remove it
     *  - If the key is a secret key, remove all certificates by foreign keys
     *  - If no valid user id remains, log an error and return null
     *
     * This operation writes an OperationLog which can be used as part of an OperationResultParcel.
     *
     * @return A canonicalized key, or null on fatal error (log will include a message in this case)
     *
     */
    @SuppressWarnings("ConstantConditions")
    public CanonicalizedKeyRing canonicalize(int indent) {
        return canonicalize(indent, false);
    }


    /** "Canonicalizes" a public key, removing inconsistencies in the process.
     *
     * More specifically:
     *  - Remove all non-verifying self-certificates
     *  - Remove all "future" self-certificates
     *  - Remove all certificates flagged as "local"
     *  - For UID certificates, remove all certificates which are
     *      superseded by a newer one on the same target, including
     *      revocations with later re-certifications.
     *  - For subkey certifications, remove all certificates which
     *      are superseded by a newer one on the same target, unless
     *      it encounters a revocation certificate. The revocation
     *      certificate is considered to permanently revoke the key,
     *      even if contains later re-certifications.
     *  This is the "behavior in practice" used by (e.g.) GnuPG, and
     *  the rationale for both can be found as comments in the GnuPG
     *  source.
     *  UID signatures:
     *  https://github.com/mtigas/gnupg/blob/50c98c7ed6b542857ee2f902eca36cda37407737/g10/getkey.c#L1668-L1674
     *  Subkey signatures:
     *  https://github.com/mtigas/gnupg/blob/50c98c7ed6b542857ee2f902eca36cda37407737/g10/getkey.c#L1990-L1997
     *  - Remove all certificates in other positions if not of known type:
     *   - key revocation signatures on the master key
     *   - subkey binding signatures for subkeys
     *   - certifications and certification revocations for user ids
     *  - If a subkey retains no valid subkey binding certificate, remove it
     *  - If a user id retains no valid self certificate, remove it
     *  - If the key is a secret key, remove all certificates by foreign keys
     *  - If no valid user id remains, log an error and return null
     *
     * This operation writes an OperationLog which can be used as part of an OperationResultParcel.
     *
     * @param forExport if this is true, non-exportable signatures will be removed
     * @return A canonicalized key, or null on fatal error (log will include a message in this case)
     *
     */
    @SuppressWarnings("ConstantConditions")
    public CanonicalizedKeyRing canonicalize(int indent, boolean forExport) {

        indent += 1;

        // do not accept v3 keys
        if (getVersion() <= 3) {
            return null;
        }

        Calendar nowCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // allow for diverging clocks up to one day when checking creation time
        nowCal.add(Calendar.DAY_OF_YEAR, 1);
        final Date nowPlusOneDay = nowCal.getTime();

        int redundantCerts = 0, badCerts = 0;

        PGPKeyRing ring = mRing;
        PGPPublicKey masterKey = mRing.getPublicKey();
        final long masterKeyId = masterKey.getKeyID();

        if (Arrays.binarySearch(KNOWN_ALGORITHMS, masterKey.getAlgorithm()) < 0) {
            return null;
        }

        {
            indent += 1;

            PGPPublicKey modified = masterKey;
            PGPSignature revocation = null;
            PGPSignature notation = null;
            for (PGPSignature zert : new IterableIterator<PGPSignature>(masterKey.getKeySignatures())) {
                int type = zert.getSignatureType();

                // These should most definitely not be here...
                if (type == PGPSignature.NO_CERTIFICATION
                        || type == PGPSignature.DEFAULT_CERTIFICATION
                        || type == PGPSignature.CASUAL_CERTIFICATION
                        || type == PGPSignature.POSITIVE_CERTIFICATION
                        || type == PGPSignature.CERTIFICATION_REVOCATION) {
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }
                WrappedSignature cert = new WrappedSignature(zert);

                if (type != PGPSignature.KEY_REVOCATION && type != PGPSignature.DIRECT_KEY) {
                    // Unknown type, just remove
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }

                if (cert.getCreationTime().after(nowPlusOneDay)) {
                    // Creation date in the future? No way!
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }

                try {
                    cert.init(masterKey);
                    if (!cert.verifySignature(masterKey)) {
                        modified = PGPPublicKey.removeCertification(modified, zert);
                        badCerts += 1;
                        continue;
                    }
                } catch (PgpGeneralException e) {
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }

                // if this is for export, we always remove any non-exportable certs
                if (forExport && cert.isLocal()) {
                    // Remove revocation certs with "local" flag
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    continue;
                }

                // special case: non-exportable, direct key signatures for notations!
                if (cert.getSignatureType() == PGPSignature.DIRECT_KEY) {
                    // must be local, otherwise strip!
                    if (!cert.isLocal()) {
                        modified = PGPPublicKey.removeCertification(modified, zert);
                        badCerts += 1;
                        continue;
                    }

                    // first notation? fine then.
                    if (notation == null) {
                        notation = zert;
                        // more notations? at least one is superfluous, then.
                    } else if (notation.getCreationTime().before(zert.getCreationTime())) {
                        modified = PGPPublicKey.removeCertification(modified, notation);
                        redundantCerts += 1;
                        notation = zert;
                    } else {
                        modified = PGPPublicKey.removeCertification(modified, zert);
                        redundantCerts += 1;
                    }
                    continue;
                } else if (cert.isLocal()) {
                    // Remove revocation certs with "local" flag
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }

                // first revocation? fine then.
                if (revocation == null) {
                    revocation = zert;
                    // more revocations? at least one is superfluous, then.
                } else if (revocation.getCreationTime().before(zert.getCreationTime())) {
                    modified = PGPPublicKey.removeCertification(modified, revocation);
                    redundantCerts += 1;
                    revocation = zert;
                } else {
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    redundantCerts += 1;
                }
            }

            // If we have a notation packet, check if there is even any data in it?
            if (notation != null) {
                // If there isn't, might as well strip it
                if (new WrappedSignature(notation).getNotation().isEmpty()) {
                    modified = PGPPublicKey.removeCertification(modified, notation);
                    redundantCerts += 1;
                }
            }

            ArrayList<String> processedUserIds = new ArrayList<>();
            for (byte[] rawUserId : new IterableIterator<byte[]>(masterKey.getRawUserIDs())) {
                String userId = Utf8Util.fromUTF8ByteArrayReplaceBadEncoding(rawUserId);

                // check for duplicate user ids
                if (processedUserIds.contains(userId)) {
                    // strip out the first found user id with this name
                    modified = PGPPublicKey.removeCertification(modified, rawUserId);
                }
                if (processedUserIds.size() > CANONICALIZE_MAX_USER_IDS) {
                    // strip out the user id
                    modified = PGPPublicKey.removeCertification(modified, rawUserId);
                }
                processedUserIds.add(userId);

                PGPSignature selfCert = null;
                revocation = null;

                // look through signatures for this specific user id
                @SuppressWarnings("unchecked")
                Iterator<PGPSignature> signaturesIt = masterKey.getSignaturesForID(rawUserId);
                if (signaturesIt != null) {
                    for (PGPSignature zert : new IterableIterator<>(signaturesIt)) {
                        WrappedSignature cert = new WrappedSignature(zert);
                        long certId = cert.getKeyId();

                        int type = zert.getSignatureType();
                        if (type != PGPSignature.DEFAULT_CERTIFICATION
                                && type != PGPSignature.NO_CERTIFICATION
                                && type != PGPSignature.CASUAL_CERTIFICATION
                                && type != PGPSignature.POSITIVE_CERTIFICATION
                                && type != PGPSignature.CERTIFICATION_REVOCATION) {
                            modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                            badCerts += 1;
                            continue;
                        }

                        if (cert.getCreationTime().after(nowPlusOneDay)) {
                            // Creation date in the future? No way!
                            modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                            badCerts += 1;
                            continue;
                        }

                        if (cert.isLocal()) {
                            // Creation date in the future? No way!
                            modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                            badCerts += 1;
                            continue;
                        }

                        // If this is a foreign signature, ...
                        if (certId != masterKeyId) {
                            // never mind any further for public keys, but remove them from secret ones
                            if (isSecret()) {
                                modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                                badCerts += 1;
                            }
                            continue;
                        }

                        // Otherwise, first make sure it checks out
                        try {
                            cert.init(masterKey);
                            if (!cert.verifySignature(masterKey, rawUserId)) {
                                modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                                badCerts += 1;
                                continue;
                            }
                        } catch (PgpGeneralException e) {
                            modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                            badCerts += 1;
                            continue;
                        }

                        switch (type) {
                            case PGPSignature.DEFAULT_CERTIFICATION:
                            case PGPSignature.NO_CERTIFICATION:
                            case PGPSignature.CASUAL_CERTIFICATION:
                            case PGPSignature.POSITIVE_CERTIFICATION:
                                if (selfCert == null) {
                                    selfCert = zert;
                                } else if (selfCert.getCreationTime().before(cert.getCreationTime())) {
                                    modified = PGPPublicKey.removeCertification(modified, rawUserId, selfCert);
                                    redundantCerts += 1;
                                    selfCert = zert;
                                } else {
                                    modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                                    redundantCerts += 1;
                                }
                                // If there is a revocation certificate, and it's older than this, drop it
                                if (revocation != null
                                        && revocation.getCreationTime().before(selfCert.getCreationTime())) {
                                    modified = PGPPublicKey.removeCertification(modified, rawUserId, revocation);
                                    revocation = null;
                                    redundantCerts += 1;
                                }
                                break;

                            case PGPSignature.CERTIFICATION_REVOCATION:
                                // If this is older than the (latest) self cert, drop it
                                if (selfCert != null && selfCert.getCreationTime().after(zert.getCreationTime())) {
                                    modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                                    redundantCerts += 1;
                                    continue;
                                }
                                // first revocation? remember it.
                                if (revocation == null) {
                                    revocation = zert;
                                    // more revocations? at least one is superfluous, then.
                                } else if (revocation.getCreationTime().before(cert.getCreationTime())) {
                                    modified = PGPPublicKey.removeCertification(modified, rawUserId, revocation);
                                    redundantCerts += 1;
                                    revocation = zert;
                                } else {
                                    modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                                    redundantCerts += 1;
                                }
                                break;
                        }
                    }
                }

                // If no valid certificate (if only a revocation) remains, drop it
                if (selfCert == null && revocation == null) {
                    modified = PGPPublicKey.removeCertification(modified, rawUserId);
                }
            }

            // If NO user ids remain, error out!
            if (modified == null || !modified.getUserIDs().hasNext()) {
                return null;
            }

            ArrayList<PGPUserAttributeSubpacketVector> processedUserAttributes = new ArrayList<>();
            for (PGPUserAttributeSubpacketVector userAttribute :
                    new IterableIterator<PGPUserAttributeSubpacketVector>(masterKey.getUserAttributes())) {
                try {
                    indent += 1;

                    // check for duplicate user attributes
                    if (processedUserAttributes.contains(userAttribute)) {
                        // strip out the first found user id with this name
                        modified = PGPPublicKey.removeCertification(modified, userAttribute);
                    }
                    processedUserAttributes.add(userAttribute);

                    PGPSignature selfCert = null;
                    revocation = null;

                    // look through signatures for this specific user id
                    @SuppressWarnings("unchecked")
                    Iterator<PGPSignature> signaturesIt = masterKey.getSignaturesForUserAttribute(userAttribute);
                    if (signaturesIt != null) {
                        for (PGPSignature zert : new IterableIterator<>(signaturesIt)) {
                            WrappedSignature cert = new WrappedSignature(zert);
                            long certId = cert.getKeyId();

                            int type = zert.getSignatureType();
                            if (type != PGPSignature.DEFAULT_CERTIFICATION
                                    && type != PGPSignature.NO_CERTIFICATION
                                    && type != PGPSignature.CASUAL_CERTIFICATION
                                    && type != PGPSignature.POSITIVE_CERTIFICATION
                                    && type != PGPSignature.CERTIFICATION_REVOCATION) {
                                modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                badCerts += 1;
                                continue;
                            }

                            if (cert.getCreationTime().after(nowPlusOneDay)) {
                                // Creation date in the future? No way!
                                modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                badCerts += 1;
                                continue;
                            }

                            if (cert.isLocal()) {
                                // Creation date in the future? No way!
                                modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                badCerts += 1;
                                continue;
                            }

                            // If this is a foreign signature, ...
                            if (certId != masterKeyId) {
                                // never mind any further for public keys, but remove them from secret ones
                                if (isSecret()) {
                                    modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                    badCerts += 1;
                                }
                                continue;
                            }

                            // Otherwise, first make sure it checks out
                            try {
                                cert.init(masterKey);
                                if (!cert.verifySignature(masterKey, userAttribute)) {
                                    modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                    badCerts += 1;
                                    continue;
                                }
                            } catch (PgpGeneralException e) {
                                modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                badCerts += 1;
                                continue;
                            }

                            switch (type) {
                                case PGPSignature.DEFAULT_CERTIFICATION:
                                case PGPSignature.NO_CERTIFICATION:
                                case PGPSignature.CASUAL_CERTIFICATION:
                                case PGPSignature.POSITIVE_CERTIFICATION:
                                    if (selfCert == null) {
                                        selfCert = zert;
                                    } else if (selfCert.getCreationTime().before(cert.getCreationTime())) {
                                        modified = PGPPublicKey.removeCertification(modified, userAttribute, selfCert);
                                        redundantCerts += 1;
                                        selfCert = zert;
                                    } else {
                                        modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                        redundantCerts += 1;
                                    }
                                    // If there is a revocation certificate, and it's older than this, drop it
                                    if (revocation != null
                                            && revocation.getCreationTime().before(selfCert.getCreationTime())) {
                                        modified = PGPPublicKey.removeCertification(modified, userAttribute, revocation);
                                        revocation = null;
                                        redundantCerts += 1;
                                    }
                                    break;

                                case PGPSignature.CERTIFICATION_REVOCATION:
                                    // If this is older than the (latest) self cert, drop it
                                    if (selfCert != null && selfCert.getCreationTime().after(zert.getCreationTime())) {
                                        modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                        redundantCerts += 1;
                                        continue;
                                    }
                                    // first revocation? remember it.
                                    if (revocation == null) {
                                        revocation = zert;
                                        // more revocations? at least one is superfluous, then.
                                    } else if (revocation.getCreationTime().before(cert.getCreationTime())) {
                                        modified = PGPPublicKey.removeCertification(modified, userAttribute, revocation);
                                        redundantCerts += 1;
                                        revocation = zert;
                                    } else {
                                        modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                        redundantCerts += 1;
                                    }
                                    break;
                            }
                        }
                    }

                    // If no valid certificate (if only a revocation) remains, drop it
                    if (selfCert == null && revocation == null) {
                        modified = PGPPublicKey.removeCertification(modified, userAttribute);
                    }

                } finally {
                    indent -= 1;
                }
            }


            // Replace modified key in the keyring
            ring = replacePublicKey(ring, modified);
            indent -= 1;

        }

        // Keep track of ids we encountered so far
        Set<Long> knownIds = new HashSet<>();

        // Process all keys
        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(ring.getPublicKeys())) {
            // Make sure this is not a duplicate, avoid undefined behavior!
            if (knownIds.contains(key.getKeyID())) {
                return null;
            }
            // Add the key id to known
            knownIds.add(key.getKeyID());

            // Don't care about the master key any further, that one gets special treatment above
            if (key.isMasterKey()) {
                continue;
            }

            indent += 1;

            if (Arrays.binarySearch(KNOWN_ALGORITHMS, key.getAlgorithm()) < 0) {
                ring = removeSubKey(ring, key);

                indent -= 1;
                continue;
            }

            Date keyCreationTime = key.getCreationTime(), keyCreationTimeLenient;
            {
                Calendar keyCreationCal = Calendar.getInstance();
                keyCreationCal.setTime(keyCreationTime);
                // allow for diverging clocks up to one day when checking creation time
                keyCreationCal.add(Calendar.MINUTE, -5);
                keyCreationTimeLenient = keyCreationCal.getTime();
            }

            // A subkey needs exactly one subkey binding certificate, and optionally one revocation
            // certificate.
            PGPPublicKey modified = key;
            PGPSignature selfCert = null, revocation = null;
            uids: for (PGPSignature zert : new IterableIterator<PGPSignature>(key.getSignatures())) {
                // remove from keyring (for now)
                modified = PGPPublicKey.removeCertification(modified, zert);

                WrappedSignature cert = new WrappedSignature(zert);
                int type = cert.getSignatureType();

                // filter out bad key types...
                if (cert.getKeyId() != masterKey.getKeyID()) {
                    badCerts += 1;
                    continue;
                }

                if (type != PGPSignature.SUBKEY_BINDING && type != PGPSignature.SUBKEY_REVOCATION) {
                    badCerts += 1;
                    continue;
                }

                if (cert.getCreationTime().after(nowPlusOneDay)) {
                    // Creation date in the future? No way!
                    badCerts += 1;
                    continue;
                }

                if (cert.getCreationTime().before(keyCreationTime)) {
                    // Signature is earlier than key creation time
                    // due to an earlier accident, we generated keys which had creation timestamps
                    // a few seconds after their signature timestamp. for compatibility, we only
                    // error out with some margin of error
                    if (cert.getCreationTime().before(keyCreationTimeLenient)) {
                        badCerts += 1;
                        continue;
                    }
                }

                if (cert.isLocal()) {
                    // Creation date in the future? No way!
                    badCerts += 1;
                    continue;
                }

                if (type == PGPSignature.SUBKEY_BINDING) {

                    // make sure the certificate checks out
                    try {
                        cert.init(masterKey);
                        if (!cert.verifySignature(masterKey, key)) {
                            badCerts += 1;
                            continue;
                        }
                    } catch (PgpGeneralException e) {
                        badCerts += 1;
                        continue;
                    }

                    boolean needsPrimaryBinding = false;

                    // If the algorithm is even suitable for signing
                    if (isSigningAlgo(key.getAlgorithm())) {

                        // If this certificate says it allows signing for the key
                        if (zert.getHashedSubPackets() != null &&
                                zert.getHashedSubPackets().hasSubpacket(SignatureSubpacketTags.KEY_FLAGS)) {
                            int flags = ((KeyFlags) zert.getHashedSubPackets()
                                    .getSubpacket(SignatureSubpacketTags.KEY_FLAGS)).getFlags();
                            if ((flags & KeyFlags.SIGN_DATA) == KeyFlags.SIGN_DATA) {
                                needsPrimaryBinding = true;
                            }
                        } else {
                            // If there are no key flags, we STILL require this because the key can sign!
                            needsPrimaryBinding = true;
                        }

                    }

                    // If this key can sign, it MUST have a primary key binding certificate
                    if (needsPrimaryBinding) {
                        boolean ok = false;
                        if (zert.getUnhashedSubPackets() != null) try {
                            // Check all embedded signatures, if any of them fits
                            PGPSignatureList list = zert.getUnhashedSubPackets().getEmbeddedSignatures();
                            for (int i = 0; i < list.size(); i++) {
                                WrappedSignature subsig = new WrappedSignature(list.get(i));
                                if (subsig.getSignatureType() == PGPSignature.PRIMARYKEY_BINDING) {
                                    subsig.init(key);
                                    if (subsig.verifySignature(masterKey, key)) {
                                        ok = true;
                                    } else {
                                        badCerts += 1;
                                        continue uids;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            badCerts += 1;
                            continue;
                        }
                        // if it doesn't, get rid of this!
                        if (!ok) {
                            badCerts += 1;
                            continue;
                        }
                    }

                    // if we already have a cert, and this one is older: skip it
                    if (selfCert != null && cert.getCreationTime().before(selfCert.getCreationTime())) {
                        redundantCerts += 1;
                        continue;
                    }

                    selfCert = zert;

                // it must be a revocation, then (we made sure above)
                } else {

                    // make sure the certificate checks out
                    try {
                        cert.init(masterKey);
                        if (!cert.verifySignature(masterKey, key)) {
                            badCerts += 1;
                            continue;
                        }
                    } catch (PgpGeneralException e) {
                        badCerts += 1;
                        continue;
                    }

                    // If we already have a newer revocation cert, skip this one.
                    if (revocation != null &&
                        revocation.getCreationTime().after(cert.getCreationTime())) {
                        redundantCerts += 1;
                        continue;
                    }

                    revocation = zert;
                }
            }

            // it is not properly bound? error!
            if (selfCert == null) {
                ring = removeSubKey(ring, key);

                indent -= 1;
                continue;
            }

            // If we have flags, check if the algorithm supports all of them
            if (selfCert.getHashedSubPackets() != null
                    && selfCert.getHashedSubPackets().hasSubpacket(SignatureSubpacketTags.KEY_FLAGS)) {
                int flags = ((KeyFlags) selfCert.getHashedSubPackets().getSubpacket(SignatureSubpacketTags.KEY_FLAGS)).getFlags();
                int algo = key.getAlgorithm();
                // If this is a signing key, but not a signing algorithm, warn the user
                if (!isSigningAlgo(algo) && (flags & KeyFlags.SIGN_DATA) == KeyFlags.SIGN_DATA) {
                }
                // If this is an encryption key, but not an encryption algorithm, warn the user
                if (!isEncryptionAlgo(algo) && (
                           (flags & KeyFlags.ENCRYPT_STORAGE) == KeyFlags.ENCRYPT_STORAGE
                        || (flags & KeyFlags.ENCRYPT_COMMS) == KeyFlags.ENCRYPT_COMMS
                    )) {
                }
            }

            // re-add certification
            modified = PGPPublicKey.addCertification(modified, selfCert);
            // add revocation, if any
            if (revocation != null) {
                modified = PGPPublicKey.addCertification(modified, revocation);
            }
            // replace pubkey in keyring
            ring = replacePublicKey(ring, modified);
            indent -= 1;
        }

        if (badCerts > 0 && redundantCerts > 0) {
            // multi plural would make this complex, just leaving this as is...
        } else if (badCerts > 0) {
        } else if (redundantCerts > 0) {
        } else {
        }

        return isSecret() ? new CanonicalizedSecretKeyRing((PGPSecretKeyRing) ring, 1)
                          : new CanonicalizedPublicKeyRing((PGPPublicKeyRing) ring, 0);
    }

    private static PGPKeyRing replacePublicKey(PGPKeyRing ring, PGPPublicKey key) {
        if (ring instanceof PGPPublicKeyRing) {
            PGPPublicKeyRing pubRing = (PGPPublicKeyRing) ring;
            return PGPPublicKeyRing.insertPublicKey(pubRing, key);
        } else {
            PGPSecretKeyRing secRing = (PGPSecretKeyRing) ring;
            PGPSecretKey sKey = secRing.getSecretKey(key.getKeyID());
            sKey = PGPSecretKey.replacePublicKey(sKey, key);
            return PGPSecretKeyRing.insertSecretKey(secRing, sKey);
        }
    }

    public UncachedKeyRing extractPublicKeyRing() throws IOException {
        if(!isSecret()) {
            throw new RuntimeException("Tried to extract public keyring from non-secret keyring. " +
                    "This is a programming error and should never happen!");
        }

        Iterator<PGPPublicKey> it = mRing.getPublicKeys();
        ByteArrayOutputStream stream = new ByteArrayOutputStream(2048);
        while (it.hasNext()) {
            stream.write(it.next().getEncoded());
        }

        return new UncachedKeyRing(
                new PGPPublicKeyRing(stream.toByteArray(), new JcaKeyFingerprintCalculator()));
    }

    /** This method removes a subkey in a keyring.
     *
     * This method essentially wraps PGP*KeyRing.remove*Key, where the keyring may be of either
     * the secret or public subclass.
     *
     * @return the resulting PGPKeyRing of the same type as the input
     */
    private static PGPKeyRing removeSubKey(PGPKeyRing ring, PGPPublicKey key) {
        if (ring instanceof PGPPublicKeyRing) {
            return PGPPublicKeyRing.removePublicKey((PGPPublicKeyRing) ring, key);
        } else {
            PGPSecretKey sKey = ((PGPSecretKeyRing) ring).getSecretKey(key.getKeyID());
            return PGPSecretKeyRing.removeSecretKey((PGPSecretKeyRing) ring, sKey);
        }
    }


    /** Returns true if the algorithm is of a type which is suitable for signing. */
    static boolean isSigningAlgo(int algorithm) {
        return algorithm == PGPPublicKey.RSA_GENERAL
                || algorithm == PGPPublicKey.RSA_SIGN
                || algorithm == PGPPublicKey.DSA
                || algorithm == PGPPublicKey.ELGAMAL_GENERAL
                || algorithm == PGPPublicKey.ECDSA;
    }

    /** Returns true if the algorithm is of a type which is suitable for encryption. */
    static boolean isEncryptionAlgo(int algorithm) {
        return algorithm == PGPPublicKey.RSA_GENERAL
                || algorithm == PGPPublicKey.RSA_ENCRYPT
                || algorithm == PGPPublicKey.ELGAMAL_ENCRYPT
                || algorithm == PGPPublicKey.ELGAMAL_GENERAL
                || algorithm == PGPPublicKey.ECDH;
    }

}

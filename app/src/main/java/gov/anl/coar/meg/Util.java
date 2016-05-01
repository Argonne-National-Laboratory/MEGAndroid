package gov.anl.coar.meg;

import android.content.Context;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.util.encoders.Base64;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by greg on 2/29/16.
 *
 * Move to util package
 */
public class Util {
    /**
     * Return true if secret key exists otherwise false
     *
     * @return
     */
    public static boolean doesKeyFileExist(
            Context context,
            String keyFileName
    ) {
        File keyFile = new File(context.getFilesDir(), keyFileName);
        if (keyFile.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean doesPublicKeyExist(
            Context context
    ) {
        return doesKeyFileExist(context, Constants.PUBLICKEYRING_FILENAME);
    }

    public boolean doesSecretKeyExist(
            Context context
    ) {
        return doesKeyFileExist(context, Constants.SECRETKEYRING_FILENAME);
    }

    public static boolean doesSymmetricKeyExist(
            Context context
    ) {
        return doesKeyFileExist(context, Constants.SYMMETRICKEY_META_FILENAME);
    }

    public static String getConfigVar(Context context, String varFilename) {
        try {
            File phoneNumberFile = new File(context.getFilesDir(), varFilename);
            FileReader reader = new FileReader(phoneNumberFile);
            BufferedReader buffer = new BufferedReader(reader);
            return buffer.readLine();
        } catch (FileNotFoundException e) {
            // Once again ignore this for now
            e.printStackTrace();
        } catch (IOException e) {
            // Once again ignore this for now. The phone number file should have
            // one line of text. Not much reason to overthink this atm.
            e.printStackTrace();
        }
        return null;
    }

    public static String getPhoneNumber(Context context) {
        // This has now morphed into a convenience method. I imagine we can
        // get rid of it soon.
        return getConfigVar(context, Constants.PHONENUMBER_FILENAME);
    }

    // TODO we should probably move this to the PGP package under MEGPublicKeyRing
    public static String getArmoredPublicKeyText(
            PGPPublicKey publicKey
    ) {
        StringBuffer buf = new StringBuffer();
        try {
            ByteArrayOutputStream arr = new ByteArrayOutputStream();
            ArmoredOutputStream armored = new ArmoredOutputStream(arr);
            publicKey.encode(armored);
            armored.close();
            buf.append(new String(arr.toByteArray(), Charset.forName("ASCII")));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return buf.toString();
    }

    public static String inputBufferToString(
            InputStream input
    ) {
        Scanner s = new java.util.Scanner(input).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static ByteArrayOutputStream inputStreamToOutputStream(
            InputStream in
    )
            throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        int len;
        while ((len = in.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, len);
        }
        out.flush();
        return out;
    }

    /**
     * Write the IV of the AES key to file.
     *
     * @param context
     * @param data
     * @throws IOException
     */
    public static void writeSymmetricMetadataFile(
            Context context,
            String data
    )
            throws IOException
    {
        File file = new File(context.getFilesDir(), Constants.SYMMETRICKEY_META_FILENAME);
        PrintWriter out = new PrintWriter(file);
        out.println(data);
        out.close();
    }

    public static ArrayList<byte[]> getAESKeyData(
            Context context
    )
            throws IOException
    {
        File file = new File(context.getFilesDir(), Constants.SYMMETRICKEY_META_FILENAME);
        FileInputStream in = new FileInputStream(file);
        String [] dataArray = inputBufferToString(in).split(Constants.SYMMETRIC_KEY_FIELD_DELIMETER);
        in.close();
        ArrayList<byte[]> response = new ArrayList<>(2);
        response.add(Base64.decode(dataArray[0]));  // the key data
        response.add(Base64.decode(dataArray[1]));  // the iv
        return response;
    }

    public static void validateSymmetricKeyData(
            String data
    )
            throws Exception
    {
        String [] dataArray = data.split(Constants.SYMMETRIC_KEY_FIELD_DELIMETER);
        if (dataArray.length != 2) {
            throw new Exception("Invalid number of fields in symmetric key data!");
        }
        if (Base64.decode(dataArray[0]).length != Constants.AES_KEY_BYTES) {
            throw new Exception("Invalid number of key bytes");
        } else if (Base64.decode(dataArray[1]).length != Constants.AES_IV_BYTES) {
            throw new Exception("Invalid number of iv bytes");
        }
    }
}

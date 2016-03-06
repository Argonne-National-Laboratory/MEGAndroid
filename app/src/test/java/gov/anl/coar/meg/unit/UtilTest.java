package gov.anl.coar.meg.unit;

import android.content.Context;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.Util;

/**
 * Created by greg on 2/29/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class UtilTest extends Fixtures{

    public static final String PHONE_NUMBER = "9998881212";

    @Mock
    Context mMockContext;

    @Test
    public void doesSecretKeyExist_returnsFalse() {
        Context mockContext = setupContext(mMockContext);
        assertFalse(new Util().doesSecretKeyExist(mockContext));
    }

    @Test
    public void doesSecretKeyExist_returnsTrue() throws IOException {
        Context mockContext = setupContext(mMockContext);
        String path = this.TMP_DIR + Constants.SECRETKEY_FILENAME;
        File temp = new File(path);
        FileOutputStream output = new FileOutputStream(temp);
        output.write("".getBytes());
        output.close();
        Boolean doesExist = new Util().doesSecretKeyExist(mockContext);
        temp.delete();
        assertTrue(doesExist);
    }

    @Test
    public void getPhoneNumber_noFileExists() {
        Context mockContext = setupContext(mMockContext);
        assertNull(Util.getPhoneNumber(mockContext));
    }

    @Test
    public void getPhoneNumber_returnsPhoneNumber() throws FileNotFoundException {
        Context mockContext = setupContext(mMockContext);
        String path = this.TMP_DIR + Constants.PHONENUMBER_FILENAME;
        File temp = new File(path);
        PrintWriter output = new PrintWriter(temp);
        output.write(PHONE_NUMBER);
        output.close();
        String retrievedNumber = Util.getPhoneNumber(mockContext);
        temp.delete();
        assertEquals(PHONE_NUMBER, retrievedNumber);
    }
}

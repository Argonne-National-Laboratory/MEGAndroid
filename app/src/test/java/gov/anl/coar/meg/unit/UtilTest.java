package gov.anl.coar.meg.unit;

import android.content.Context;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.Util;

/**
 * Created by greg on 2/29/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class UtilTest extends Fixtures{

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
        temp.deleteOnExit();
        FileOutputStream output = new FileOutputStream(temp);
        output.write("".getBytes());
        output.close();
        assertTrue(new Util().doesSecretKeyExist(mockContext));
    }
}

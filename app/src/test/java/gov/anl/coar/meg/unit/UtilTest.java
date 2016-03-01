package gov.anl.coar.meg.unit;

import android.content.Context;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.junit.Test;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import gov.anl.coar.meg.Constants;
import gov.anl.coar.meg.Util;

/**
 * Created by greg on 2/29/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class UtilTest {

    @Mock
    Context mMockContext;

    public static final String TMP_DIR = "/tmp/";

    private Context setupContext(){
        File mockDir = new File(this.TMP_DIR);
        when(mMockContext.getFilesDir()).thenReturn(mockDir);
        return mMockContext;
    }

    @Test
    public void validateDoesNotHaveKey_returnsFalse() {
        Context mockContext = setupContext();
        assertFalse(new Util().validateDoesNotHaveKey(mockContext));
    }

    @Test
    public void validateDoesNotHaveKey_returnsTrue() throws IOException {
        Context mockContext = setupContext();
        String path = this.TMP_DIR + Constants.SECRETKEY_FILENAME;
        File temp = new File(path);
        temp.deleteOnExit();
        FileOutputStream output = new FileOutputStream(temp);
        output.write("".getBytes());
        output.close();
        assertTrue(new Util().validateDoesNotHaveKey(mockContext));
    }
}

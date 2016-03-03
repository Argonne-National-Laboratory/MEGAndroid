package gov.anl.coar.meg.unit;

import android.content.Context;

import java.io.File;

import static org.mockito.Mockito.when;

/**
 * Created by greg on 3/1/16.
 */
public class Fixtures {
    public static final String TMP_DIR = "/tmp/";

    public Context setupContext(Context mMockContext){
        File mockDir = new File(this.TMP_DIR);
        when(mMockContext.getFilesDir()).thenReturn(mockDir);
        return mMockContext;
    }
}

package gov.anl.coar.meg.test;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.spongycastle.openpgp.PGPException;

import java.io.IOException;

import gov.anl.coar.meg.Installation;
import gov.anl.coar.meg.Util;
import gov.anl.coar.meg.pgp.InvalidKeyException;

import static junit.framework.Assert.assertFalse;

/**
 * Created by greg on 3/2/16.
 */
public class InstallationTest extends ActivityInstrumentationTestCase2<Installation>{

    Installation installation;
    Util util;

    public InstallationTest() {
        super(Installation.class);
    }

    protected void setUp() throws Exception {
        this.installation = getActivity();
        this.util = new Util();
    }

    public void testInstallation_generateRSAKeySuccess() throws IOException, InvalidKeyException, PGPException {
        this.installation.generateRSAKey("foo", "bar", "baz@bar.com", "foobar".toCharArray());
        Context context = this.getInstrumentation().getTargetContext().getApplicationContext();
        assertTrue(util.doesSecretKeyExist(context));
    }
}

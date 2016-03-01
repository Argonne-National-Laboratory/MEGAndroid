package gov.anl.coar.meg.unit;

import android.content.Context;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.junit.Test;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;

import gov.anl.coar.meg.Util;

/**
 * Created by greg on 2/29/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class UtilTest {

    @Mock
    Context mMockContext;

    @Test
    public void validateDoesNotHaveKey_returnsFalse() {
        assertFalse(new Util().validateDoesNotHaveKey(mMockContext));
    }
}

package gov.anl.coar.meg.test;

import android.app.Application;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;
import junit.framework.TestResult;

import org.junit.Test;
import org.junit.internal.builders.JUnit4Builder;
import org.junit.runner.RunWith;

import gov.anl.coar.meg.MainActivity;

import static org.junit.Assert.assertTrue;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ApplicationTest{
    @Test
    public void doNothingForNow() {
        assertTrue(true);
    }
}

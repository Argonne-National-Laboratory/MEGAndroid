package gov.anl.coar.meg;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.spongycastle.openpgp.PGPException;

import gov.anl.coar.meg.pgp.PrivateKeyCache;

/** Class to provide functionality to the Login page of MEG
 *
 * 	@author Bridget Basan
 */
public class Login extends AppCompatActivity {

	/** Default method */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
      try {
          super.onCreate(savedInstanceState);
          setContentView(R.layout.activity_login);
          PrivateKeyCache privateKeyCache = (PrivateKeyCache) getApplication();
          if (privateKeyCache.needsRefresh()) {
              // XXX TODO This is a temporary hack
              privateKeyCache.refreshPK(this, "foobar".toCharArray());
          }
      } catch (Exception e) {
          e.printStackTrace();
      }

  }

	/** Default method */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_login, menu);
    return true;
  }

	/** Default method */
	@Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}

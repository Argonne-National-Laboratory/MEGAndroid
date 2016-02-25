package gov.anl.coar.meg;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;


/** Class to provide functionality to the opening welcome page of MEG
 * Adds functionality to buttons which open new intents
 *
 * @author Bridget Basan
 */
public class MainActivity extends AppCompatActivity
    implements View.OnClickListener{

  Button bInstall;
  Button bLogin;

	/**	Instantiate bInstall and bLogin buttons
	 *
	 * 	@author Bridget Basan
	 */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    bInstall = (Button) findViewById(R.id.bInstall);
    bInstall.setOnClickListener(this);

    bLogin = (Button) findViewById(R.id.bLogin);
    bLogin.setOnClickListener(this);
  }

	/** Instantiate new intents on button clicks
	 *
	 * @author Bridget Basan
	 */
  @Override
  public void onClick(View v) {
    switch(v.getId()){
      case R.id.bInstall:
        startActivity(new Intent(this,Installation.class));
        break;
      case R.id.bLogin:
        startActivity(new Intent(this,Login.class));
        break;
			default:
				break;
    }
  }

	/** Default method */
	@Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
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

package gov.anl.coar.meg;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

/** Class to provide functionality to the installation page of MEG
 * Adds functionality to buttons which open new intents
 *
 * @author Bridget Basan
 */
public class Installation extends AppCompatActivity
    implements View.OnClickListener{

  Button bAdvanced;

	/**	Instantiate bAdvanced button
	 *
	 * 	@author Bridget Basan
	 */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_installation);

    bAdvanced = (Button) findViewById(R.id.bAdvanced);
    bAdvanced.setOnClickListener(this);
  }

	/** Instantiate new intents on button clicks
	 *
	 * 	@author Bridget Basan
	 */
  @Override
  public void onClick(View v) {
    switch(v.getId()){
      case R.id.bAdvanced:
        startActivity(new Intent(this, Advanced_Options.class));
        break;
			default:
				break;
    }
  }

	/** Default method */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_installation, menu);
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

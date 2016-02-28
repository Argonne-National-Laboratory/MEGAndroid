package gov.anl.coar.meg;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
    implements View.OnClickListener {

    Button bAdvanced;
    Button bNext;

    /**
     * Instantiate bAdvanced button
     *
     * @author Bridget Basan
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installation);

        bAdvanced = (Button) findViewById(R.id.bAdvanced);
        bAdvanced.setOnClickListener(this);

        bNext = (Button) findViewById(R.id.bNext);
        bNext.setOnClickListener(this);
    }

    /**
     * Instantiate new intents on button clicks
     *
     * @author Bridget Basan
     */
    @Override
    public void onClick(View v) {
        int buttonClick = v.getId();
        if (buttonClick == R.id.bAdvanced) {
            startActivity(new Intent(this, Advanced_Options.class));
        } else if (buttonClick == R.id.bNext) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.install_alert_msg);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    startActivity(new Intent(((Dialog) dialog).getContext(), Login.class));
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Default method
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_installation, menu);
        return true;
    }

    /**
     * Default method
     */
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

package wifi.trc;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by Stefan on 04/06/2015.
 */

public class About extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Display some information about this app
        setContentView(R.layout.about_box);
    }
}
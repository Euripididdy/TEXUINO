package wifi.trc;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by Stefan on 04/06/2015.
 */

public class Help extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Display the information about the game
        setContentView(R.layout.help_screen);
    }
}
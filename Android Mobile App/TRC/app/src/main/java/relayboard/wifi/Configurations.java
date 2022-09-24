package relayboard.wifi;

/**
 * Created by Stefan on 23/01/2016.
 */

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

public class Configurations extends Activity
{
    private SharedPreferences storage;
    private Switch alertsToggle, safeDiscoToggle;
    //private EditText ret1, ret2, ret3, ret4, ret5, ret6, ret7, ret8, iet1, iet2, iet3, iet4, iet5, iet6;
    private EditText[] relayEditText = new EditText[8];
    private EditText[] inputEditText = new EditText[8];

    private View alerts, safeDisco;
    private String[] relayNames = new String[8];
    private String[] inputNames = new String[8];

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Show switch
        setContentView(R.layout.prefences_box);
        // Initialise fields
        initialise();
    }

    private void initialise()
    {
        boolean state = false;
        // Get views
        this.alerts = findViewById(R.id.alerts_switch);
        this.safeDisco = findViewById(R.id.safety_switch);

        this.relayEditText[0] = (EditText) findViewById(R.id.r1et);
        this.relayEditText[1] = (EditText) findViewById(R.id.r2et);
        this.relayEditText[2] = (EditText) findViewById(R.id.r3et);
        this.relayEditText[3] = (EditText) findViewById(R.id.r4et);
        this.relayEditText[4] = (EditText) findViewById(R.id.r5et);
        this.relayEditText[5] = (EditText) findViewById(R.id.r6et);
        this.relayEditText[6] = (EditText) findViewById(R.id.r7et);
        this.relayEditText[7] = (EditText) findViewById(R.id.r8et);

        this.inputEditText[0] = (EditText) findViewById(R.id.i1et);
        this.inputEditText[1] = (EditText) findViewById(R.id.i2et);
        this.inputEditText[2] = (EditText) findViewById(R.id.i3et);
        this.inputEditText[3] = (EditText) findViewById(R.id.i4et);
        this.inputEditText[4] = (EditText) findViewById(R.id.i5et);
        this.inputEditText[5] = (EditText) findViewById(R.id.i6et);
        this.inputEditText[6] = (EditText) findViewById(R.id.i7et);
        this.inputEditText[7] = (EditText) findViewById(R.id.i8et);

        // Initialise switches
        this.alertsToggle = (Switch) this.alerts;
        this.safeDiscoToggle = (Switch) this.safeDisco;

        // Initialise shared settings
        this.storage = getSharedPreferences("saved", Context.MODE_PRIVATE);

        // Get notifications preference
        state = this.storage.getBoolean("show_messages", true);
        // Update preference
        this.alertsToggle.setChecked(state);

        // Get safe disco preference
        state = this.storage.getBoolean("all_off_disco", false);
        // Update preference
        this.safeDiscoToggle.setChecked(state);

        //Get relay saved names
        for (int i = 0; i < relayNames.length; i++)
        {
            relayNames[i] = this.storage.getString("relayName" + (i+1), "Relay #" + (i+1));
        }
        //Get input saved names
        for (int i = 0; i < inputNames.length; i++)
        {
            inputNames[i] = this.storage.getString("inputName" + (i+1), "Input " + (i+1));
        }

        // Update relay names
        for (int i = 0; i < relayEditText.length; i++)
        {
            relayEditText[i].setText(relayNames[i]);
        }
        // Update input names
        for (int i = 0; i < inputEditText.length; i++)
        {
            inputEditText[i].setText(inputNames[i]);
        }

    }

    public void onClickButton(View v)
    {
        if (v.getId() == R.id.okBtn)
        {
            SharedPreferences.Editor editor = this.storage.edit();

            boolean tmp = this.alertsToggle.isChecked();
            editor.putBoolean("show_messages", tmp);

            tmp = this.safeDiscoToggle.isChecked();
            editor.putBoolean("all_off_disco", tmp);

            for (int i = 0; i < relayEditText.length; i++)
            {
                editor.putString("relayName" + (i+1), relayEditText[i].getText().toString());
            }

            for (int i = 0; i < inputEditText.length; i++)
            {
                editor.putString("inputName" + (i+1), inputEditText[i].getText().toString());
            }

            editor.commit();
        }
        super.onBackPressed();
    }

}
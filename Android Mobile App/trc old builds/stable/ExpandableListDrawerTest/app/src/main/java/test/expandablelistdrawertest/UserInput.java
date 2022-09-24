package test.expandablelistdrawertest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by Stefan on 23/01/2016.
 */
public class UserInput extends Activity
{
    private TextView heading;
    private EditText input;
    private String text = null;
    private CheckBox showPass;
    private int taskId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        taskId = i.getIntExtra("task id", -1);

        if (taskId == 0 || taskId == 3)
        {
            setContentView(R.layout.user_input_text);
        }

        if (taskId == 1 || taskId == 2)
        {
            setContentView(R.layout.user_input_pass);
        }

        this.heading = (TextView) findViewById(R.id.subject);
        this.input = (EditText) findViewById(R.id.input_field);
        this.showPass = (CheckBox) findViewById(R.id.xShowPass);


        if (taskId == 0)
        {
            //Then we want to rename the esp8266 network name
            //this.showPass.setVisibility(View.INVISIBLE);
            this.heading.setText("Enter new network name:");
        }
        else if (taskId == 3)
        {
            //Then we want to send AT command to Texuino's ESP8266
            this.heading.setText("Enter AT command");
            this.input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(50)});
        }
        else
        {
            if (taskId == 1)
            {
                //Then we want to change the esp8266 passworD
                this.heading.setText("Enter new network password:");

            }
            if (taskId == 2)
            {
                //Then we want to enter the password for the network we selected to join
                this.heading.setText("Enter network password:");

            }
            this.input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    public void onClickButton(View v)
    {
        if (v.getId() == R.id.OK)
        {
            text = this.input.getText().toString();
            Intent i = new Intent(UserInput.this, MainActivity.class);
            i.putExtra("returned value", text);
            setResult(Activity.RESULT_OK, i);
            finish();
            super.onBackPressed();
        }
        else if (v.getId() == R.id.cancel)
        {
            super.onBackPressed();
        }
        else if (v.getId() == R.id.xShowPass)
        {
            if (this.showPass.isChecked())
            {
                this.input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
            }
            else
            {
                this.input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
        }
    }
}

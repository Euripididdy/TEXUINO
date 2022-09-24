package relayboard.wifi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Created by Stefan on 04/06/2015.
 */
public class Devices extends Activity
{
    private static final String TAG = "Devices";

    private ListView networksList;
    private String[] networkName;
    private String[] networkSecurity;
    private int[] networkStrength;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Show list layout
        setContentView(R.layout.devices_list);

        Intent i = getIntent();
        this.networkName = i.getStringArrayExtra("names");
        this.networkSecurity = i.getStringArrayExtra("securities");
        this.networkStrength = i.getIntArrayExtra("strengths");

        this.networksList = (ListView) findViewById(R.id.bluetooth_devices_list);

        String[] listLabels = new String[networkName.length + 1];
        listLabels[0] = "Choose network to join:";

        int[] listIcons = new int[networkName.length + 1];
        listIcons[0] = R.mipmap.wifi_full_locked;

        for (int x = 0; x < networkName.length; x++)
        {
            listLabels[x + 1] = networkName[x];
            if (networkStrength[x] > 0 && networkStrength[x] < 25)
            {
                if (networkSecurity[x].contains("WPA"))
                {
                    listIcons[x + 1] = R.mipmap.wifi_weak_locked;
                }
                else
                {
                    listIcons[x + 1] = R.mipmap.wifi_weak;
                }
            }
            else if (networkStrength[x] > 24 && networkStrength[x] < 50)
            {
                if (networkSecurity[x].contains("WPA"))
                {
                    listIcons[x + 1] = R.mipmap.wifi_medium_locked;
                }
                else
                {
                    listIcons[x + 1] = R.mipmap.wifi_medium;
                }
            }
            else if (networkStrength[x] > 49 && networkStrength[x] < 75)
            {
                if (networkSecurity[x].contains("WPA"))
                {
                    listIcons[x + 1] = R.mipmap.wifi_strong_locked;
                }
                else
                {
                    listIcons[x + 1] = R.mipmap.wifi_strong;
                }
            }
            else if (networkStrength[x] > 74)
            {
                if (networkSecurity[x].contains("WPA"))
                {
                    listIcons[x + 1] = R.mipmap.wifi_full_locked;
                }
                else
                {
                    listIcons[x + 1] = R.mipmap.wifi_full;
                }
            }
        }

        CustomListAdapter devicesAdapter = new CustomListAdapter(this, listLabels, true, false);
        devicesAdapter.setIcons(listIcons);

        this.networksList.setAdapter(devicesAdapter);

        this.networksList.setOnItemClickListener(new ListView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (position > 0)
                {
                    Intent i = new Intent(Devices.this, MainActivity.class);
                    i.putExtra("desired network", networkName[position - 1]);
                    i.putExtra("network security", networkSecurity[position - 1]);
                    setResult(Activity.RESULT_OK, i);
                    finish();
                }
            }
        });

    }
}

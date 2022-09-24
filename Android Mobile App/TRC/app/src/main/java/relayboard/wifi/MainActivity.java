package relayboard.wifi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity
{
    private static final String SERVICE_CHECK = "loop and poll csb";
    private static final int SERVICE_LOOP = 1000;
    private static final String MAIN = "MainActivity";
    private static final String IPADDRESS = "192.168.4.1";
    private static final int PORT = 5050;
    private static final int NO_RESPONSE = -1;
    private static final int CHOOSE_DEVICE = 10;
    private static final int ENTER_PASS = 15;
    private static final int RENAME_DEVICE = 20;
    private static final int REKEY_DEVICE = 30;

    private String relayBoardName = "TEXUINO";
    private String relayBoardPass = "longpasswordhere";

    private IntentFilter wifiFilter;
    private Connection relayBoardConnection = null;

    private boolean activatedWiFi = false;
    private boolean skipSyncro = false;
    private boolean messages = true;
    private boolean safeDisco = false;
    private boolean activityResultCalled = false;
    private Toast alert = null;

    private List<ScanResult> wifiScanList;
    private WifiManager wifiNetwork;

    private SharedPreferences storage;

    private ImageView connectionIcon;
    private ImageView[] inputIcons = new ImageView[8];
    private ImageView[] interuptIcons = new ImageView[2];

    private Switch[] relaySwitches = new Switch[9];
    private TextView[] inputLabels = new TextView[8];

    private IntentFilter bsFilter = null;
    private PendingIntent bsIntent = null;

    private Calendar cal;
    private AlarmManager bsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialiseUserInterface();
        initialiseWiFi();
        initialiseService();
        retrieveBoardDetails();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        getConfiguredNetworks(false);
        if (!activityResultCalled)
        {
            retrieveBoardDetails();
            if (!isJoinedToNetwork(relayBoardName))
            {
                if (searchNetwork(relayBoardName))
                {
                    this.relayBoardConnection = null;
                    joinNetwork(relayBoardName, relayBoardPass);
                }
                else
                {
                    displayMessage(relayBoardName + " not found!", false, Toast.LENGTH_SHORT);
                }
            }
            else
            {
                if (isConnected(false))
                {
                    //bsManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), SERVICE_LOOP, bsIntent);
                }
            }
        }
        activityResultCalled = false;
        registerReceiver(wifiReceiver, wifiFilter);
        registerReceiver(sReceiver, bsFilter);
        Log.d(MAIN, "ON RESUME CALLED");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        unregisterReceiver(wifiReceiver);
        unregisterReceiver(sReceiver);
        //cancel background service
        //bsManager.cancel(bsIntent);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (activatedWiFi)
        {
            wifiNetwork.setWifiEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    //Catch the result from the activity and act accordingly
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        activityResultCalled = true;
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_DEVICE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                relayBoardName = data.getStringExtra("desired network");
                if (!isJoinedToNetwork(relayBoardName))
                {
                    String security = data.getStringExtra("network security");
                    if (security.contains("WPA"))
                    {
                        Intent i = new Intent(MainActivity.this, UserInput.class);
                        i.putExtra("task id", 2);
                        startActivityForResult(i, ENTER_PASS);
                    }
                    else
                    {
                        joinNetwork(relayBoardName, null);
                    }
                }
                else
                {
                    displayMessage("Already connected to " + relayBoardName + "!", true, Toast.LENGTH_SHORT);
                }
            }
        }
        else if (requestCode == ENTER_PASS)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                relayBoardPass = data.getStringExtra("returned value");
                joinNetwork(relayBoardName, relayBoardPass);
            }
        }
        //TODO RENAME ESP8266
        else if (requestCode == RENAME_DEVICE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                String newName = data.getStringExtra("returned value");
                Log.d(MAIN, "New name " + newName);
            }
        }
        //TODO REPASS ESP8266
        else if (requestCode == REKEY_DEVICE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                String newPass = data.getStringExtra("returned value");
                Log.d(MAIN, "New password " + newPass);
            }
        }
        Log.d(MAIN, "ON ACTIVITY RESULT CALLED");
    }

    //Receiver to catch the broadcast from BackgroundService class
    private final BroadcastReceiver sReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (SERVICE_CHECK.equals(action))
            {
                Log.i(MAIN, "Background service executed ");
                syncronise();
            }
        }
    };

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION))
            {
                Log.d("WifiReceiver", ">>>>SUPPLICANT_STATE_CHANGED_ACTION<<<<<<");
                SupplicantState wifiState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                switch (wifiState)
                {
                    case ASSOCIATED:
                        Log.i("SupplicantState", "ASSOCIATED");
                        break;
                    case ASSOCIATING:
                        Log.i("SupplicantState", "ASSOCIATING");
                        break;
                    case AUTHENTICATING:
                        Log.i("SupplicantState", "Authenticating...");
                        break;
                    case COMPLETED:
                        Log.i("SupplicantState", "Connected");
                        saveBoardDetails();
                        new Thread()
                        {
                            final Handler hand = new Handler();

                            public void run()
                            {
                                hand.postDelayed(connectToBoard, 2500);
                            }
                        }.start();
                        break;
                    case DISCONNECTED:
                        Log.i("SupplicantState", "Disconnected");
                        break;
                    case DORMANT:
                        Log.i("SupplicantState", "DORMANT");
                        break;
                    case FOUR_WAY_HANDSHAKE:
                        Log.i("SupplicantState", "FOUR_WAY_HANDSHAKE");
                        break;
                    case GROUP_HANDSHAKE:
                        Log.i("SupplicantState", "GROUP_HANDSHAKE");
                        break;
                    case INACTIVE:
                        Log.i("SupplicantState", "INACTIVE");
                        break;
                    case INTERFACE_DISABLED:
                        Log.i("SupplicantState", "INTERFACE_DISABLED");
                        break;
                    case INVALID:
                        Log.i("SupplicantState", "INVALID");
                        break;
                    case SCANNING:
                        Log.i("SupplicantState", "SCANNING");
                        break;
                    case UNINITIALIZED:
                        Log.i("SupplicantState", "UNINITIALIZED");
                        break;
                    default:
                        Log.i("SupplicantState", "Unknown");
                        break;

                }
                int supl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                if (supl_error == WifiManager.ERROR_AUTHENTICATING)
                {
                    Log.i("ERROR_AUTHENTICATING", "ERROR_AUTHENTICATING!!!!!!!!!");
                    displayMessage("Unable to connect! Authentication problem.", true, Toast.LENGTH_SHORT);
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == 0x12345)
        {
            for (int grantResult : grantResults)
            {
                if (grantResult != PackageManager.PERMISSION_GRANTED)
                {
                    return;
                }
            }
            getScanResults();
        }
    }

    public void initialiseService()
    {
        //this.cal = Calendar.getInstance();
        //this.bsManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(MainActivity.this, BackgroundService.class);
        this.bsIntent = PendingIntent.getService(this, 0, i, 0);
        this.bsFilter = new IntentFilter(SERVICE_CHECK);
        this.bsFilter.addAction("test");
    }


    public void initialiseUserInterface()
    {
        Log.d(MAIN, "Initialising User Interface..");
        this.connectionIcon = (ImageView) findViewById(R.id.connectionIcon);

        this.relaySwitches[0] = (Switch) findViewById(R.id.relay1Switch);
        this.relaySwitches[0].setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                setSkipSyncro(true);
                if (relaySwitches[0].isChecked())
                {
                    write("IR1");
                }
                else
                {
                    write("OR1");
                }
            }
        });

        this.relaySwitches[1] = (Switch) findViewById(R.id.relay2Switch);
        this.relaySwitches[1].setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                setSkipSyncro(true);
                if (relaySwitches[1].isChecked())
                {
                    write("IR2");
                }
                else
                {
                    write("OR2");
                }
            }
        });

        this.relaySwitches[2] = (Switch) findViewById(R.id.relay3Switch);
        this.relaySwitches[2].setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                setSkipSyncro(true);
                if (relaySwitches[2].isChecked())
                {
                    write("IR3");
                }
                else
                {
                    write("OR3");
                }
            }
        });

        this.relaySwitches[3] = (Switch) findViewById(R.id.relay4Switch);
        this.relaySwitches[3].setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                setSkipSyncro(true);
                if (relaySwitches[3].isChecked())
                {
                    write("IR4");
                }
                else
                {
                    write("OR4");
                }
            }
        });

        this.relaySwitches[4] = (Switch) findViewById(R.id.relay5Switch);
        this.relaySwitches[4].setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                setSkipSyncro(true);
                if (relaySwitches[4].isChecked())
                {
                    write("IR5");
                }
                else
                {
                    write("OR5");
                }
            }
        });

        this.relaySwitches[5] = (Switch) findViewById(R.id.relay6Switch);
        this.relaySwitches[5].setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                setSkipSyncro(true);
                if (relaySwitches[5].isChecked())
                {
                    write("IR6");
                }
                else
                {
                    write("OR6");
                }
            }
        });

        this.relaySwitches[6] = (Switch) findViewById(R.id.relay7Switch);
        this.relaySwitches[6].setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                setSkipSyncro(true);
                if (relaySwitches[6].isChecked())
                {
                    write("IR7");
                }
                else
                {
                    write("OR7");
                }
            }
        });

        this.relaySwitches[7] = (Switch) findViewById(R.id.relay8Switch);
        this.relaySwitches[7].setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                setSkipSyncro(true);
                if (relaySwitches[7].isChecked())
                {
                    write("IR8");
                }
                else
                {
                    write("OR8");
                }
            }
        });

        this.relaySwitches[8] = (Switch) findViewById(R.id.relayAllSwitch);
        this.relaySwitches[8].setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                setSkipSyncro(true);
                if (relaySwitches[8].isChecked())
                {
                    //all on
                    write("IAR");
                    flipAllSwitches(true);
                }
                else
                {
                    //all off
                    write("OAR");
                    flipAllSwitches(false);
                }
            }
        });

        this.inputLabels[0] = (TextView) findViewById(R.id.input1Label);
        this.inputLabels[1] = (TextView) findViewById(R.id.input2Label);
        this.inputLabels[2] = (TextView) findViewById(R.id.input3Label);
        this.inputLabels[3] = (TextView) findViewById(R.id.input4Label);
        this.inputLabels[4] = (TextView) findViewById(R.id.input5Label);
        this.inputLabels[5] = (TextView) findViewById(R.id.input6Label);
        this.inputLabels[6] = (TextView) findViewById(R.id.input7Label);
        this.inputLabels[7] = (TextView) findViewById(R.id.input8Label);

        this.inputIcons[0] = (ImageView) findViewById(R.id.input1Icon);
        this.inputIcons[1] = (ImageView) findViewById(R.id.input2Icon);
        this.inputIcons[2] = (ImageView) findViewById(R.id.input3Icon);
        this.inputIcons[3] = (ImageView) findViewById(R.id.input4Icon);
        this.inputIcons[4] = (ImageView) findViewById(R.id.input5Icon);
        this.inputIcons[5] = (ImageView) findViewById(R.id.input6Icon);
        this.inputIcons[6] = (ImageView) findViewById(R.id.input7Icon);
        this.inputIcons[7] = (ImageView) findViewById(R.id.input8Icon);

        this.interuptIcons[0] = (ImageView) findViewById(R.id.interupt1Icon);
        this.interuptIcons[1] = (ImageView) findViewById(R.id.interupt2Icon);

        //Drawer:
        String[] optionTitles = {"Help", "About", "Personalise", "Connect", "Rename", "Security"};
        int[] optionIcons = {R.mipmap.help, R.mipmap.info, R.mipmap.prefs, R.mipmap.ic, R.mipmap.edit, R.mipmap.shield};
        ListView optionsList = (ListView) findViewById(R.id.left_drawer);
        final CustomListAdapter customList = new CustomListAdapter(this, optionTitles, false, false);
        customList.setIcons(optionIcons);
        optionsList.setAdapter(customList);
        optionsList.setOnItemClickListener(new ListView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (position == 0)
                {
                    Intent i = new Intent(MainActivity.this, Help.class);
                    startActivity(i);
                }
                else if (position == 1)
                {
                    Intent i = new Intent(MainActivity.this, About.class);
                    startActivity(i);
                }
                else if (position == 2)
                {
                    Intent i = new Intent(MainActivity.this, Configurations.class);
                    startActivity(i);
                }
                else if (position == 3)
                {
                    Log.i(MAIN, "A search for available devices was requested.");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    {
                        Log.i(MAIN, "REQUESTING PERMISSIONS");
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0x12345);
                        //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method

                    }
                    else
                    {
                        getConfiguredNetworks(true);
                        getScanResults();
                    }
                    customList.getRow(position).setText("Searching..");
                }
                else if (position == 4)
                {
                    if (isConnected(true))
                    {
                        Intent i = new Intent(MainActivity.this, UserInput.class);
                        i.putExtra("task id", 0);
                        startActivityForResult(i, RENAME_DEVICE);
                    }
                }
                else if (position == 5)
                {
                    if (isConnected(true))
                    {
                        Intent i = new Intent(MainActivity.this, UserInput.class);
                        i.putExtra("task id", 1);
                        startActivityForResult(i, REKEY_DEVICE);
                    }
                }
            }
        });
    }

    private void getConfiguredNetworks(boolean enable)
    {
        String networkName;
        WifiManager wifiNetwork = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> list = wifiNetwork.getConfiguredNetworks();

        if (list != null)
        {
            if (!list.isEmpty())
            {
                for (WifiConfiguration i : list)
                {
                    Log.i(MAIN, "Configured network: " + i.SSID + "(ID: " + i.networkId + ")");

                    if (enable)
                    {
                        wifiNetwork.enableNetwork(i.networkId, false);
                    }
                    else
                    {
                        //if networkid is not equal to name we want then disable
                        networkName = i.SSID;
                        networkName = networkName.substring(1, networkName.length() - 1);
                        if (!networkName.equals(relayBoardName))
                        {
                            Log.i(MAIN, "Disabling configured network: " + i.SSID + "(ID: " + i.networkId + ")");
                            wifiNetwork.disableNetwork(i.networkId);
                        }

                    }
                }
            }
            else
            {
                Log.e("Connection Setup", "Function returned empty list!!");
            }
        }
        else
        {
            Log.e("Connection Setup", "Failed to initialise list. List null!");
        }

    }

    private void getScanResults()
    {
        WifiManager wifiNetwork = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiScanList = wifiNetwork.getScanResults();
        //do something, permission was previously granted; or legacy device
        Log.i(MAIN, "SCANLIST SIZE: " + wifiScanList.size());

        HashMap<String, ScanResult> wifiList = new HashMap<>();

        for (int i = 0; i < wifiScanList.size(); i++)
        {
            Log.i(MAIN, "LOOPING THROUGH SCANLIST, PASS - " + i);
            if (!wifiList.containsKey(wifiScanList.get(i).SSID))
            {
                wifiList.put(wifiScanList.get(i).SSID, wifiScanList.get(i));
            }
        }

        String[] networkName = new String[wifiList.size()];
        String[] networkSecurity = new String[wifiList.size()];
        int[] networkStrength = new int[wifiList.size()];
        int count = 0;
        for (String key : wifiList.keySet())
        {
            networkName[count] = wifiList.get(key).SSID;
            networkStrength[count] = wifiList.get(key).level * -1;
            networkSecurity[count] = wifiList.get(key).capabilities;
            Log.i(MAIN, "Found network called: " + networkName[count] + ", Strength: " + (networkStrength[count]) + "%, Security: " + networkSecurity[count]);
            count++;
        }

        Intent i = new Intent(MainActivity.this, Devices.class);
        i.putExtra("names", networkName);
        i.putExtra("strengths", networkStrength);
        i.putExtra("securities", networkSecurity);
        startActivityForResult(i, CHOOSE_DEVICE);
    }

    private void flipAllSwitches(boolean state)
    {
        for (int i = 0; i < this.relaySwitches.length; i++)
        {
            relaySwitches[i].setChecked(state);
        }
    }

    public void displayMessage(String msg, boolean cancelPrevious, int length)
    {
        if (this.messages)
        {
            if (this.alert != null && cancelPrevious)
            {
                this.alert.cancel();
            }
            this.alert = Toast.makeText(this, msg, length);
            this.alert.show();
        }
    }

    private void initialiseWiFi()
    {
        wifiFilter = new IntentFilter();

        wifiFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        wifiFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        wifiFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        wifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        //WTF IS THIS...
        wifiFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        wifiNetwork = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifiNetwork.isWifiEnabled())
        {
            wifiNetwork.setWifiEnabled(true);
            displayMessage("Wi-Fi activated!", true, Toast.LENGTH_SHORT);
            activatedWiFi = true;
        }
    }

    private boolean searchNetwork(String network)
    {
        displayMessage("Searching for " + network + " network", false, Toast.LENGTH_SHORT);
        boolean result = false;

        wifiScanList = wifiNetwork.getScanResults();

        for (int i = 0; i < wifiScanList.size(); i++)
        {
            if (wifiScanList.get(i).SSID.equals(network))
            {
                result = true;
            }
        }
        return result;
    }

    private boolean joinNetwork(String network, String password)
    {
        boolean outcome = false;
        WifiConfiguration wfc = new WifiConfiguration();
        wfc.SSID = String.format("\"%s\"", network);
        wfc.preSharedKey = String.format("\"%s\"", password);
        wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // For WPA
        wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // For WPA2
        wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);


        Log.d(MAIN, "Attempting to join network " + network + ", with password " + password);
        displayMessage("Joining " + relayBoardName + " network...", false, Toast.LENGTH_SHORT);

        int netId = wifiNetwork.addNetwork(wfc);
        wifiNetwork.disconnect();
        wifiNetwork.enableNetwork(netId, true);
        wifiNetwork.saveConfiguration();
        outcome = wifiNetwork.reconnect();
        //checkNetwork();
        if (outcome)
        {
            Log.d(MAIN, "Attempt successful");
        }
        else
        {
            Log.d(MAIN, "Attempt failed");
        }
        return outcome;
    }

    private boolean isJoinedToNetwork(String networkid)
    {
        boolean outcome = false;
        ConnectivityManager networkManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = networkManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (network.isConnected())
        {
            //WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            String joinedNetworkName = wifiNetwork.getConnectionInfo().getSSID();
            joinedNetworkName = joinedNetworkName.substring(1, joinedNetworkName.length() - 1);
            Log.d(MAIN, "Currently connected to " + joinedNetworkName);
            Log.d(MAIN, "Checking if " + joinedNetworkName + " and " + networkid + " match...");

            //if (joinedNetworkName == networkid)
            if (joinedNetworkName.equals(networkid))
            {
                Log.d(MAIN, "They do! No need to reconnect");
                outcome = true;
            }
            else
            {
                Log.d(MAIN, "They don't! We must disconnect from this network");
            }
        }
        else
        {
            Log.d(MAIN, "Not connected to any network!");
        }
        return outcome;
    }

    private final Runnable connectToBoard = new Runnable()
    {
        @Override
        public void run()
        {
            connectToRelayBoard();
        }
    };

    private final Runnable serviceRegister = new Runnable()
    {
        @Override
        public void run()
        {
            registerReceiver(sReceiver, bsFilter);
        }
    };

    private void saveBoardDetails()
    {
        SharedPreferences.Editor editor = this.storage.edit();
        editor.putString("module_name", relayBoardName);
        editor.putString("module_pass", relayBoardPass);
        editor.commit();
    }

    private void retrieveBoardDetails()
    {
        Log.d(MAIN, "Retrieving saved data..");
        // Initialise storage
        this.storage = getSharedPreferences("saved", Context.MODE_PRIVATE);

        // Check if notifications are enabled
        this.messages = this.storage.getBoolean("show_messages", true);

        // Check if safe disconnect is activated
        this.safeDisco = this.storage.getBoolean("all_off_disco", false);
        //TODO according to setting and if connected then set arduino state
        if (safeDisco)
        {
            write("ISD");
        }
        else
        {
            write("OSD");
        }

        // Retrieve ERCU details
        String tmp = this.storage.getString("module_name", null);
        if (tmp != null)
        {
            this.relayBoardName = tmp;
        }
        tmp = this.storage.getString("module_pass", null);
        if (tmp != null)
        {
            this.relayBoardPass = tmp;
        }

        for (int i = 0; i < relaySwitches.length - 1; i++)
        {
            this.relaySwitches[i].setText(this.storage.getString("relayName" + (i + 1), "Relay #" + (i + 1)));
        }

        for (int i = 0; i < inputLabels.length; i++)
        {
            this.inputLabels[i].setText(this.storage.getString("inputName" + (i + 1), "Input" + (i + 1)));
        }
    }

    public String read()
    {
        String value = null;
        if (isConnected(false))
        {
            value = relayBoardConnection.getResponse();
            relayBoardConnection.resetResponse();
        }
        return value;
    }

    public void write(String value)
    {
        if (isConnected(false))
        {
            byte[] msgBuffer = value.getBytes();
            relayBoardConnection.sendRequest(msgBuffer);
            wait(150);
        }
    }

    private void wait(int value)
    {
        try
        {
            Thread.sleep(value);
        } catch (InterruptedException e)
        {
            Log.e(MAIN, "ERROR: " + e.toString());
            e.printStackTrace();
        }
    }

    public String getFeedback(int type)
    {
        Log.d(MAIN, "Getting feedback from the board...");
        String result = null;
        if (type == 1)
        {
            write("GIB");
        }
        else if (type == 2)
        {
            write("GRB");
        }
        result = read();
        if (result == null)
        {
            Log.e(MAIN, "Could not retrieve byte!");
        }
        return result;
    }

    //Method which checks the value of a specific bit in a byte
    public int getBitValue(int value, int index)
    {
        return (value >> index) & 1;
    }

    private boolean isConnected(boolean notify)
    {
        boolean connected = false;
        if (relayBoardConnection != null)
        {
            if (relayBoardConnection.isConnected())
            {
                connected = true;
            }
        }
        if (!connected)
        {
            //connectionIcon.setImageResource(R.mipmap.disconnected);
            if (notify)
            {
                displayMessage("Not connected to " + relayBoardName, true, Toast.LENGTH_SHORT);
            }
        }
        else
        {
            //connectionIcon.setImageResource(R.mipmap.connected);
        }
        return connected;
    }

    @SuppressLint("ShortAlarm")
    private void connectToRelayBoard()
    {
        if (!isConnected(false))
        {
            this.relayBoardConnection = new Connection(IPADDRESS, PORT);
            this.relayBoardConnection.start();
            this.connectionIcon.setImageDrawable(null);
            this.connectionIcon.setImageResource(0);
            this.connectionIcon.setBackground(null);
            wait(150);  //decrease?
            if (this.relayBoardConnection.isConnected())
            {

                this.connectionIcon.setImageResource(R.mipmap.connected);
                displayMessage("Relay board detected!", false, Toast.LENGTH_SHORT);
                //bsManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), SERVICE_LOOP, bsIntent);
                //retrieveSavedData();
                Intent i = new Intent(this, BackgroundService.class);
                this.startService(i);
                if (safeDisco)
                {
                    write("ISD");
                }
                else
                {
                    write("OSD");
                }
            }
            else
            {
                this.connectionIcon.setImageResource(R.mipmap.disconnected);
                displayMessage("No relay board detected! Did you choose the correct network?", false, Toast.LENGTH_SHORT);
            }
        }
    }

    private void setSkipSyncro(boolean value)
    {
        this.skipSyncro = value;
    }

    private void syncronise()
    {
        int inputsByte = -1;
        int relaysByte = -1;
        int interuptsByte = -1;
        String feedback;
        String feedbackArray[];

        write("GFB");
        feedback = read();
        if (feedback.length() > 0)
        {
            feedbackArray = feedback.split(":");
        }
        else
        {
            feedbackArray = new String[4];
        }
        Log.d(MAIN, "Feedback value: " + feedback);

        if (feedbackArray.length > 0)
        {
            try
            {
                inputsByte = Integer.parseInt(feedbackArray[0]);
                //inputsByte = tmp.charAt(0) - '0';
                Log.d(MAIN, "Inputs byte = " + inputsByte);
            } catch (NumberFormatException e)
            {
                Log.e(MAIN, "Inputs byte invalid! Not in correct format: " + e.toString());
            }
        }
        else
        {
            Log.e(MAIN, "Failed to successfully split feedback string!");
        }

        if (inputsByte >= 0)
        {
            int inputStatus[] = new int[8];
            for (int i = 0; i < inputStatus.length; i++)
            {
                this.inputIcons[i].setImageDrawable(null);
                this.inputIcons[i].setImageResource(0);
                this.inputIcons[i].setBackground(null);

                inputStatus[i] = getBitValue(inputsByte, i);

                if (i < 4)
                {
                    if (inputStatus[i] == 1)
                    {
                        this.inputIcons[i].setImageResource(R.mipmap.redled);
                    }
                    else
                    {
                        this.inputIcons[i].setImageResource(R.mipmap.greenled);
                    }
                }
                else
                {
                    if (inputStatus[i] == 1)
                    {
                        this.inputIcons[i].setImageResource(R.mipmap.greenled);
                    }
                    else
                    {
                        this.inputIcons[i].setImageResource(R.mipmap.redled);
                    }
                }
            }
        }

        if (feedbackArray.length > 1)
        {
            try
            {

                relaysByte = Integer.parseInt(feedbackArray[1]);
                Log.d(MAIN, "Relays byte = " + relaysByte);
            } catch (NumberFormatException e)
            {
                Log.e(MAIN, "Relays byte invalid! Not in correct format: " + e.toString());
            }
        }
        else
        {
            Log.e(MAIN, "Failed to successfully split feedback string!");
        }

        if (relaysByte >= 0 && !skipSyncro)
        {
            int relayStatus[] = new int[8];
            for (int i = 0; i < relayStatus.length; i++)
            {
                relayStatus[i] = getBitValue(relaysByte, i);
                if (relayStatus[i] == 1)
                {
                    relaySwitches[i].setChecked(true);
                }
                else
                {
                    relaySwitches[i].setChecked(false);
                }
            }
            if (relaysByte == 255)
            {
                flipAllSwitches(true);
                //relaySwitches[8].setChecked(true);
            }
            if (relaysByte == 0)
            {
                flipAllSwitches(false);
                //relaySwitches[8].setChecked(false);
            }
        }

        if (feedbackArray.length > 2)
        {
            try
            {

                interuptsByte = Integer.parseInt(feedbackArray[2]);
                Log.d(MAIN, "Interupts byte = " + interuptsByte);
            } catch (NumberFormatException e)
            {
                Log.e(MAIN, "Interupts byte invalid! Not in correct format: " + e.toString());
            }
        }
        else
        {
            Log.e(MAIN, "Failed to successfully split feedback string!");
        }

        if (interuptsByte >= 0)
        {
            int interuptStatus[] = new int[8];
            for (int i = 0; i < this.interuptIcons.length; i++)
            {
                this.interuptIcons[i].setImageDrawable(null);
                this.interuptIcons[i].setImageResource(0);
                this.interuptIcons[i].setBackground(null);

                interuptStatus[i] = getBitValue(interuptsByte, i);
                if (i == 0)
                {
                    if (interuptStatus[i] == 1)
                    {
                        this.interuptIcons[i].setImageResource(R.mipmap.redled);

                    }
                    else
                    {
                        this.interuptIcons[i].setImageResource(R.mipmap.greenled);
                    }
                }
                else
                {
                    if (interuptStatus[i] == 1)
                    {
                        this.interuptIcons[i].setImageResource(R.mipmap.greenled);

                    }
                    else
                    {
                        this.interuptIcons[i].setImageResource(R.mipmap.redled);
                    }
                }
            }
        }

        setSkipSyncro(false);
    }
}

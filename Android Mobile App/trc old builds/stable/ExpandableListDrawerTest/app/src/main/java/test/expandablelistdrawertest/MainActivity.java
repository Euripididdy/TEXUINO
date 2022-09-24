package test.expandablelistdrawertest;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity
{
    private static final String SERVICE_CHECK = "loop and poll csb";
    private static final int SERVICE_LOOP = 1000;

    private static final int WIFI_OFF_CONNECT = 7500;
    private static final int WIFI_CHANGE_CONNECT = 5000;
    private static final int WIFI_SAME_CONNECT = 2000;

    private static final String MAIN = "MainActivity";
    private static final String IPADDRESS = "192.168.4.1";
    private static final int PORT = 5050;
    private static final int NO_RESPONSE = -1;
    private static final int CHOOSE_DEVICE = 10;
    private static final int ENTER_PASS = 15;
    private static final int RENAME_DEVICE = 20;
    private static final int REKEY_DEVICE = 30;
    private static final int DEBUG_DEVICE = 40;

    private int connectWait = WIFI_SAME_CONNECT;

    private DirectConnection texuinoDirectConnection = null;

    private String texuinoDirectName = "TEXUINO2";
    private String texuinoDirectPass = "defaultpass";

    private String texuinoNewDirectName = null;
    private String texuinoNewDirectPass = null;

    private String texuinoLocalName = null;
    private String texuinoLocalPass = null;

    private IntentFilter wifiFilter;

    private boolean activatedWiFi = false;
    private boolean skipSync = false;
    private boolean messages = true;
    private boolean safeDisconnect = false;
    private boolean activityResultCalled = false;
    private Toast alert = null;

    private List<ScanResult> wifiScanList;
    private WifiManager wifiNetwork;

    private SharedPreferences storage;

    private ImageView connectionIcon;
    private ImageView[] inputIcons = new ImageView[8];
    private ImageView[] interruptIcons = new ImageView[2];

    private Switch[] relaySwitches = new Switch[9];
    private TextView[] inputLabels = new TextView[8];

    private IntentFilter bsFilter = null;

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        initialiseUserInterface();
        initialiseWiFi();
        initialiseService();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //getConfiguredNetworks(false);
        if (!activityResultCalled)
        {
            retrieveTexuinoDetails();

            //If device is not connected to any wifi network
            if (!deviceIsConnectedToWirelessNetwork())
            {
                displayMessage("Not connected to any network", false, Toast.LENGTH_SHORT);

                //Search for desired network to connect to
                if (searchNetwork(texuinoDirectName))
                {
                    //getConfiguredNetworks(false);
                    this.texuinoDirectConnection = null;
                    joinWirelessNetwork(texuinoDirectName, texuinoDirectPass);
                }
                //Notify desired network not found
                else
                {
                    displayMessage(texuinoDirectName + " not found!", false, Toast.LENGTH_SHORT);
                }

            }
            else
            {
                displayMessage("Connected to SOME network", false, Toast.LENGTH_SHORT);
            }

            /*
            if (!deviceIsConnectedToWirelessNetworkID(texuinoDirectName))
            {
                if (searchNetwork(texuinoDirectName))
                {
                    this.texuinoDirectConnection = null;
                    joinWirelessNetwork(texuinoDirectName, texuinoDirectPass);
                }
                else
                {
                    displayMessage(texuinoDirectName + " not found!", false, Toast.LENGTH_SHORT);
                }
            }
            else
            {
                if (!deviceIsLinkedToTexuino(false))
                {
                    directConnect = true;
                }
            }
            */
        }
        activityResultCalled = false;
        registerReceiver(wifiReceiver, wifiFilter);
        registerReceiver(serviceReceiver, bsFilter);
        Log.i(MAIN, "Application resumed");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        unregisterReceiver(wifiReceiver);
        unregisterReceiver(serviceReceiver);
        Log.i(MAIN, "Application paused");
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
    //Catch the result from the activity and act accordingly
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        activityResultCalled = true;
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_DEVICE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                texuinoDirectName = data.getStringExtra("desired network");
                if (!deviceIsConnectedToWirelessNetworkID(texuinoDirectName))
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
                        joinWirelessNetwork(texuinoDirectName, null);
                    }
                }
                else
                {
                    displayMessage("Already connected to " + texuinoDirectName + "!", true, Toast.LENGTH_SHORT);
                }
            }
        }
        else if (requestCode == ENTER_PASS)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                texuinoDirectPass = data.getStringExtra("returned value");
                joinWirelessNetwork(texuinoDirectName, texuinoDirectPass);
            }
        }
        else if (requestCode == RENAME_DEVICE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                retrieveTexuinoDetails();
                texuinoNewDirectName = data.getStringExtra("returned value");
                texuinoNewDirectPass = texuinoDirectPass;

                editNetworkParameters(texuinoNewDirectName, texuinoNewDirectPass);
            }
        }
        else if (requestCode == REKEY_DEVICE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                retrieveTexuinoDetails();
                texuinoNewDirectPass = data.getStringExtra("returned value");
                texuinoNewDirectName = texuinoDirectName;

                editNetworkParameters(texuinoNewDirectName, texuinoNewDirectPass);
            }
        }
        else if (requestCode == DEBUG_DEVICE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                //TODO Send AT command
                String cmd = data.getStringExtra("returned value");
                sendATcommand(cmd, 150);
            }
        }
        Log.i(MAIN, "Application received data from another activity");
    }

    //Receiver to catch the broadcast from BackgroundService class
    private final BroadcastReceiver serviceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (SERVICE_CHECK.equals(action))
            {
                Log.i(MAIN, "Background service executed ");
                synchronise();
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
                        Log.i(MAIN, "SupplicantState: ASSOCIATED");
                        break;
                    case ASSOCIATING:
                        Log.i(MAIN, "SupplicantState: ASSOCIATING");
                        break;
                    case AUTHENTICATING:
                        Log.i(MAIN, "SupplicantState: Authenticating...");
                        break;
                    case COMPLETED:
                        Log.i(MAIN, "SupplicantState: Completed");
                        if (texuinoNewDirectName != null)
                        {
                            texuinoDirectName = texuinoNewDirectName;
                        }
                        if (texuinoNewDirectPass != null)
                        {
                            texuinoDirectPass = texuinoNewDirectPass;
                        }
                        saveTexuinoDetails();

                        if (deviceIsConnectedToWirelessNetworkID(texuinoDirectName))
                        {
                            //connectToBoard
                            //connectToBoard(connectWait);
                            new Thread()
                            {
                                final Handler hand = new Handler();

                                public void run()
                                {
                                    hand.postDelayed(connectToBoard, connectWait);
                                }
                            }.start();
                        }
                        else
                        {
                            //If desired network exists
                            if (searchNetwork(texuinoDirectName))
                            {
                                connectWait = WIFI_CHANGE_CONNECT;
                                joinWirelessNetwork(texuinoDirectName, texuinoDirectPass);
                            }
                            //Notify desired network not found
                            else
                            {
                                displayMessage(texuinoDirectName + " not found!", false, Toast.LENGTH_SHORT);
                            }
                        }

                        break;
                    case DISCONNECTED:
                        Log.i(MAIN, "SupplicantState: Disconnected");
                        break;
                    case DORMANT:
                        Log.i(MAIN, "SupplicantState: DORMANT");
                        break;
                    case FOUR_WAY_HANDSHAKE:
                        Log.i(MAIN, "SupplicantState: FOUR_WAY_HANDSHAKE");
                        break;
                    case GROUP_HANDSHAKE:
                        Log.i(MAIN, "SupplicantState: GROUP_HANDSHAKE");
                        break;
                    case INACTIVE:
                        Log.i(MAIN, "SupplicantState: INACTIVE");
                        break;
                    case INTERFACE_DISABLED:
                        Log.i(MAIN, "SupplicantState: INTERFACE_DISABLED");
                        break;
                    case INVALID:
                        Log.i(MAIN, "SupplicantState: INVALID");
                        break;
                    case SCANNING:
                        Log.i(MAIN, "SupplicantState: SCANNING");
                        break;
                    case UNINITIALIZED:
                        Log.i(MAIN, "SupplicantState: UNINITIALIZED");
                        break;
                    default:
                        Log.i(MAIN, "SupplicantState: Unknown");
                        break;

                }
                int supl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                if (supl_error == WifiManager.ERROR_AUTHENTICATING)
                {
                    Log.d(MAIN, "Authentication error!");
                    displayMessage("Unable to connect due to invalid authentication!", true, Toast.LENGTH_SHORT);
                }
            }
        }
    };

    private void initialiseUserInterface()
    {
        setContentView(R.layout.activity_main);

        Log.d(MAIN, "Initialising User Interface..");
        this.connectionIcon = (ImageView) findViewById(R.id.connectionIcon);

        this.relaySwitches[0] = (Switch) findViewById(R.id.relay1Switch);
        this.relaySwitches[0].setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                setSkipSync(true);
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
                setSkipSync(true);
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
                setSkipSync(true);
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
                setSkipSync(true);
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
                setSkipSync(true);
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
                setSkipSync(true);
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
                setSkipSync(true);
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
                setSkipSync(true);
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
                setSkipSync(true);
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

        this.interruptIcons[0] = (ImageView) findViewById(R.id.interupt1Icon);
        this.interruptIcons[1] = (ImageView) findViewById(R.id.interupt2Icon);

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.left_drawer);

        // preparing list data
        prepareListData();

        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);
        int[] groupIcons = {R.mipmap.connect_med, R.mipmap.configure_med, R.mipmap.settings_med, R.mipmap.info_med};
        listAdapter.setIcons(groupIcons);

        // setting list adapter
        expListView.setAdapter(listAdapter);

        // Listview Group click listener
        expListView.setOnGroupClickListener(new OnGroupClickListener()
        {

            @Override
            public boolean onGroupClick(ExpandableListView parent, View v,
                                        int groupPosition, long id)
            {
                // Toast.makeText(getApplicationContext(),
                // "Group Clicked " + listDataHeader.get(groupPosition),
                // Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        // Listview Group expanded listener
        expListView.setOnGroupExpandListener(new OnGroupExpandListener()
        {

            @Override
            public void onGroupExpand(int groupPosition)
            {
                Toast.makeText(getApplicationContext(),
                        listDataHeader.get(groupPosition) + " Expanded",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Listview Group collasped listener
        expListView.setOnGroupCollapseListener(new OnGroupCollapseListener()
        {

            @Override
            public void onGroupCollapse(int groupPosition)
            {
                Toast.makeText(getApplicationContext(),
                        listDataHeader.get(groupPosition) + " Collapsed",
                        Toast.LENGTH_SHORT).show();

            }
        });

        // Listview on child click listener
        expListView.setOnChildClickListener(new OnChildClickListener()
        {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id)
            {
                if (groupPosition == 0)
                {
                    if (childPosition == 0)
                    {
                        Log.i(MAIN, "Searching for available networks");

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                        {
                            Log.i(MAIN, "REQUESTING PERMISSIONS");
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0x12345);
                        }
                        else
                        {
                            //getConfiguredNetworks(true);
                            showWirelessNetworksList();
                        }
                    }
                    if (childPosition == 1)
                    {
                        //if (texuinoLocalName
                    }
                }
                if (groupPosition == 1)
                {
                    if (childPosition == 0)
                    {
                        displayMessage("Clicked on parent configure, child 0", false, Toast.LENGTH_SHORT);
                    }
                    if (childPosition == 1)
                    {
                        displayMessage("Clicked on parent configure, child 1", false, Toast.LENGTH_SHORT);
                    }
                }
                if (groupPosition == 2)
                {
                    if (childPosition == 0)
                    {

                    }
                    if (childPosition == 1)
                    {

                    }
                }
                if (groupPosition == 3)
                {
                    if (childPosition == 0)
                    {
                        Intent i = new Intent(MainActivity.this, About.class);
                        startActivity(i);
                    }
                    if (childPosition == 1)
                    {

                    }
                }
                return false;
            }
        });

    }

    private void initialiseWiFi()
    {
        this.wifiNetwork = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.wifiFilter = new IntentFilter();

        this.wifiFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        this.wifiFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        this.wifiFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        this.wifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        //WTF IS THIS...
        this.wifiFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        if (!wifiNetwork.isWifiEnabled())
        {
            wifiNetwork.setWifiEnabled(true);
            displayMessage("Wi-Fi activated!", true, Toast.LENGTH_SHORT);
            //Flag in order to turn off onExit
            activatedWiFi = true;
            // Specify longer time to wait before connecting
            connectWait = WIFI_OFF_CONNECT;
        }
    }

    public void initialiseService()
    {
        Intent i = new Intent(MainActivity.this, BackgroundService.class);
        this.bsFilter = new IntentFilter(SERVICE_CHECK);
        this.bsFilter.addAction("test");
    }

    private void prepareListData()
    {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        // Adding headers
        listDataHeader.add("Connect");
        listDataHeader.add("Configure Texuino");
        listDataHeader.add("Settings");
        listDataHeader.add("Information");

        // Adding child data
        List<String> connect = new ArrayList<String>();
        connect.add("Direct");
        connect.add("Local");
        connect.add("Remote");

        List<String> configure = new ArrayList<String>();
        configure.add("Rename");
        configure.add("Security");
        configure.add("Debug");

        List<String> settings = new ArrayList<String>();
        settings.add("User Preferences");
        settings.add("User Interface");
        settings.add("Connection Parameters");

        List<String> info = new ArrayList<String>();
        info.add("About");
        info.add("Disclaimer");
        info.add("Contact");

        listDataChild.put(listDataHeader.get(0), connect); // Header, Child data
        listDataChild.put(listDataHeader.get(1), configure);
        listDataChild.put(listDataHeader.get(2), settings);
        listDataChild.put(listDataHeader.get(3), info);
    }

    private void getConfiguredNetworks(boolean enable)
    {
        String networkName;
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
                        this.wifiNetwork.enableNetwork(i.networkId, false);
                    }
                    else
                    {
                        //if networkid is not equal to name we want then disable
                        networkName = i.SSID;
                        networkName = networkName.substring(1, networkName.length() - 1);
                        if (!networkName.equals(texuinoDirectName))
                        {
                            Log.i(MAIN, "Disabling configured network: " + i.SSID + "(ID: " + i.networkId + ")");
                            this.wifiNetwork.disableNetwork(i.networkId);
                        }
                    }
                }
            }
            else
            {
                Log.d(MAIN, "[getConfiguredNetworks] Function returned empty list!");
            }
        }
        else
        {
            Log.d(MAIN, "[getConfiguredNetworks] Failed to initialise. Null list!");
        }
    }

    private void showWirelessNetworksList()
    {
        HashMap<String, ScanResult> wifiList = new HashMap<>();

        for (int i = 0; i < getWifiScanList().size(); i++)
        {
            Log.i(MAIN, "LOOPING THROUGH SCANLIST, PASS# - " + i);
            if (!wifiList.containsKey(getWifiScanList().get(i).SSID))
            {
                wifiList.put(getWifiScanList().get(i).SSID, wifiScanList.get(i));
            }
        }

        int index = 0;
        int wifiListSize = wifiList.size();

        String[] networkName = new String[wifiListSize];
        String[] networkSecurity = new String[wifiListSize];
        int[] networkStrength = new int[wifiListSize];

        for (String key : wifiList.keySet())
        {
            networkName[index] = wifiList.get(key).SSID;
            networkStrength[index] = wifiList.get(key).level * -1;
            networkSecurity[index] = wifiList.get(key).capabilities;
            Log.i(MAIN, "NETWORK: " + networkName[index] + " | STRENGTH: " + (networkStrength[index]) + "% | SECURITY: " + networkSecurity[index]);
            index++;
        }

        Intent i = new Intent(MainActivity.this, Devices.class);
        i.putExtra("names", networkName);
        i.putExtra("strengths", networkStrength);
        i.putExtra("securities", networkSecurity);
        startActivityForResult(i, CHOOSE_DEVICE);
    }

    private void flipAllSwitches(boolean state)
    {
        for (Switch relaySwitch : this.relaySwitches)
        {
            relaySwitch.setChecked(state);
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

    private boolean searchNetwork(String network)
    {
        displayMessage("Searching for " + network + " network...", false, Toast.LENGTH_SHORT);
        boolean result = false;

        for (int i = 0; i < getWifiScanList().size(); i++)
        {
            if (getWifiScanList().get(i).SSID.equals(network))
            {
                result = true;
            }
        }
        return result;
    }

    private void sendATcommand(String cmd, int delay)
    {
        if (cmd != null)
        {
            cmd = "ATSEND<" + cmd + ">[" + Integer.toString(delay) + "]";
            Log.d(MAIN, "Sending AT command: " + cmd);
            write(cmd);
        }
    }

    private void editNetworkParameters(String ssid, String pass)
    {
        if (ssid != null)
        {
            if (pass != null)
            {
                if (ssid.length() >= 3)
                {
                    if (pass.length() >= 8)
                    {
                        Log.d(MAIN, "Will reconfigure Texuino network to '" + ssid + "', with password '" + pass + "'.");
                        write("CNET<" + ssid + ">{" + pass + "}");
                        new Thread()
                        {
                            final Handler hand = new Handler();

                            public void run()
                            {
                                hand.postDelayed(reconnectAfterNetworkEdit, 3000);
                            }
                        }.start();
                    }
                    else
                    {
                        this.texuinoNewDirectPass = null;
                        displayMessage("New password must be at least 8 characters long!", true, Toast.LENGTH_SHORT);
                    }
                }
                else
                {
                    this.texuinoNewDirectName = null;
                    displayMessage("New name must be at least 3 characters long!", true, Toast.LENGTH_SHORT);
                }
            }
            else
            {
                displayMessage("New password must be at least 8 characters long!", true, Toast.LENGTH_SHORT);
            }
        }
        else
        {
            displayMessage("New name must be at least 3 characters long!", true, Toast.LENGTH_SHORT);
        }
    }

    //WEIRD METHOD DUE NEW ANDOIRD VERSION REQUESTING PERMISSION TO SHOW WIFI NETWORKS LIST
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
            showWirelessNetworksList();
        }
    }

    public List<ScanResult> getWifiScanList()
    {
        this.wifiScanList = this.wifiNetwork.getScanResults();
        return this.wifiScanList;
    }

    private void joinWirelessNetwork(String ssid, String password)
    {
        getConfiguredNetworks(false);
        WifiConfiguration wfc = new WifiConfiguration();
        wfc.SSID = String.format("\"%s\"", ssid);
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

        Log.d(MAIN, "Attempting to join network " + ssid + ", with password " + password);
        displayMessage("Joining " + ssid + " network...", false, Toast.LENGTH_SHORT);

        int netId = this.wifiNetwork.addNetwork(wfc);
        this.wifiNetwork.disconnect();
        this.wifiNetwork.enableNetwork(netId, true);
        this.wifiNetwork.saveConfiguration();

        this.wifiNetwork.reconnect();
        //workflow continues in wifi broadcaster
    }

    /*
    private boolean deviceIsConnectedToWirelessNetwork()
    {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI)
        {
            return true;
        }
        return false;
    }*/

    public boolean deviceIsConnectedToWirelessNetwork()
    {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi.getConnectionInfo().getNetworkId() != -1)
        {
            /* connected */
            return true;
        }
        else
        {
            return false;
        }
    }

    /*
    private boolean deviceIsConnectedToWirelessNetwork()
    {
        boolean outcome = false;
        ConnectivityManager networkManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = networkManager.getActiveNetworkInfo();


        //WifiInfo wifiInfo = wifiNetwork.getConnectionInfo();

        if (networkInfo != null)
        {
            if (networkInfo.isConnected())
            {
                outcome = true;
            }
            else
            {
                Log.d(MAIN, "Not connected to any network!");
            }
        }
        else
        {
            Log.d(MAIN, "NULL Active network!");
        }
        return outcome;
    }*/

    private boolean deviceIsConnectedToWirelessNetworkID(String networkid)
    {
        boolean outcome = false;

        if (deviceIsConnectedToWirelessNetwork())
        {
            WifiInfo wifiInfo = wifiNetwork.getConnectionInfo();
            if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED)
            {
                //Get name of currently joined network
                String joinedNetworkName = wifiInfo.getSSID();
                //Format name in order to compare
                joinedNetworkName = joinedNetworkName.substring(1, joinedNetworkName.length() - 1);
                //DEBUG
                Log.d(MAIN, "Currently connected to " + joinedNetworkName);
                Log.d(MAIN, "Checking if " + joinedNetworkName + " and " + networkid + " match...");

                //COMPARISON
                if (joinedNetworkName.equals(networkid))
                {
                    Log.d(MAIN, "Currently connected to " + joinedNetworkName + ", no  need to reconnect.");
                    outcome = true;
                }
                else
                {
                    Log.d(MAIN, "Currently connected to " + joinedNetworkName + ", must reconnect!");
                }
            }
            else
            {
                Log.d(MAIN, "Connection not completed.");
            }
        }
        return outcome;
    }

    private void saveTexuinoDetails()
    {
        SharedPreferences.Editor editor = this.storage.edit();
        editor.putString("module_name", texuinoDirectName);
        editor.putString("module_pass", texuinoDirectPass);
        editor.apply();
    }

    private void retrieveTexuinoDetails()
    {
        Log.d(MAIN, "Retrieving saved data..");
        // Initialise storage
        this.storage = getSharedPreferences("saved", Context.MODE_PRIVATE);

        // Retrieve Texuino direct network parameters
        String tmp = this.storage.getString("module_name", null);
        //Log.d(MAIN, "GOT NAME: " + tmp);

        if (tmp != null)
        {
            this.texuinoDirectName = tmp;
        }
        tmp = this.storage.getString("module_pass", null);
        //Log.d(MAIN, "GOT PASS: " + tmp);

        if (tmp != null)
        {
            this.texuinoDirectPass = tmp;
        }

        // Retrieve Texuino local network parameters
        tmp = this.storage.getString("local_texuino_name", null);
        if (tmp != null)
        {
            this.texuinoLocalName = tmp;
        }
        tmp = this.storage.getString("local_texuino_pass", null);
        if (tmp != null)
        {
            this.texuinoLocalPass = tmp;
        }


        // Check if notifications are enabled
        this.messages = this.storage.getBoolean("show_messages", true);

        for (int i = 0; i < relaySwitches.length - 1; i++)
        {
            this.relaySwitches[i].setText(this.storage.getString("relayName" + (i + 1), "Relay #" + (i + 1)));
        }

        for (int i = 0; i < inputLabels.length; i++)
        {
            this.inputLabels[i].setText(this.storage.getString("inputName" + (i + 1), "Input" + (i + 1)));
        }

        // Check if safe disconnect is enabled
        this.safeDisconnect = this.storage.getBoolean("all_off_disco", false);
        if (safeDisconnect)
        {
            write("ISD");
        }
        else
        {
            write("OSD");
        }
    }

    private final Runnable connectToBoard = new Runnable()
    {
        @Override
        public void run()
        {
            connectToBoard();
        }
    };

    private final Runnable reconnectAfterNetworkEdit = new Runnable()
    {
        @Override
        public void run()
        {
            joinWirelessNetwork(texuinoNewDirectName, texuinoNewDirectPass);
        }
    };

    private void connectToBoard()
    {
        //NEW CONNECTION
        if (texuinoDirectConnection == null)
        {
            displayMessage("Establishing connection to Texuino...", false, Toast.LENGTH_SHORT);
            texuinoDirectConnection = new DirectConnection(IPADDRESS, PORT);
            texuinoDirectConnection.start();

            pauseMain(200);  //decrease?
            if (texuinoDirectConnection.isConnected())
            {
                displayMessage("Texuino connected!", true, Toast.LENGTH_SHORT);

                Intent i = new Intent(this, BackgroundService.class);
                this.startService(i);
                if (safeDisconnect)
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
                displayMessage("No Texuino detected! Did you choose the correct network?", false, Toast.LENGTH_SHORT);
            }
        }

        //RECONNECT
        else
        {
            //displayMessage("Reconnecting...", false, Toast.LENGTH_SHORT);
            texuinoDirectConnection.cancel();
            texuinoDirectConnection = null;
            connectToBoard();
        }
    }

    public String read()
    {
        String value = null;
        if (texuinoDirectConnection != null)
        {
            value = texuinoDirectConnection.getResponse();
            texuinoDirectConnection.resetResponse();
        }
        else
        {
            Log.d(MAIN, "Could not read as connection instance is null!");
        }
        return value;
    }

    public void write(String value)
    {
        final byte[] msgBuffer = value.getBytes();
        if (texuinoDirectConnection != null)
        {
            new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        texuinoDirectConnection.sendRequest(msgBuffer);
                        pauseMain(150);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        else
        {
            Log.d(MAIN, "Could not write as connection instance is null!");
        }
    }

    private void changeConnectionIcon(int state)
    {
        this.connectionIcon.setImageDrawable(null);
        this.connectionIcon.setImageResource(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            this.connectionIcon.setBackground(null);
        }

        if (state == 0)
        {
            this.connectionIcon.setImageResource(R.mipmap.disconnected);
        }
        if (state == 1)
        {
            this.connectionIcon.setImageResource(R.mipmap.connected);
        }
    }

    private void pauseMain(int value)
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

    //Method which checks the value of a specific bit in a byte
    public int getBitValue(int value, int index)
    {
        return (value >> index) & 1;
    }

    private void setSkipSync(boolean value)
    {
        this.skipSync = value;
    }

    //TODO Revise this method
    private void synchronise()
    {
        int inputsByte = -1;
        int relaysByte = -1;
        int interruptsByte = -1;
        String feedback;
        String feedbackArray[];

        write("GFB");
        feedback = read();

        if (feedback != null)
        {
            changeConnectionIcon(1);

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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    {
                        this.inputIcons[i].setBackground(null);
                    }

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

            if (relaysByte >= 0 && !skipSync)
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
                }
                if (relaysByte == 0)
                {
                    flipAllSwitches(false);
                }
            }

            if (feedbackArray.length > 2)
            {
                try
                {

                    interruptsByte = Integer.parseInt(feedbackArray[2]);
                    Log.d(MAIN, "Interupts byte = " + interruptsByte);
                } catch (NumberFormatException e)
                {
                    Log.e(MAIN, "Interupts byte invalid! Not in correct format: " + e.toString());
                }
            }
            else
            {
                Log.e(MAIN, "Failed to successfully split feedback string!");
            }

            if (interruptsByte >= 0)
            {
                int interruptStatus[] = new int[8];
                for (int i = 0; i < this.interruptIcons.length; i++)
                {
                    this.interruptIcons[i].setImageDrawable(null);
                    this.interruptIcons[i].setImageResource(0);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    {
                        this.interruptIcons[i].setBackground(null);
                    }

                    interruptStatus[i] = getBitValue(interruptsByte, i);
                    if (i == 0)
                    {
                        if (interruptStatus[i] == 1)
                        {
                            this.interruptIcons[i].setImageResource(R.mipmap.redled);

                        }
                        else
                        {
                            this.interruptIcons[i].setImageResource(R.mipmap.greenled);
                        }
                    }
                    else
                    {
                        if (interruptStatus[i] == 1)
                        {
                            this.interruptIcons[i].setImageResource(R.mipmap.greenled);

                        }
                        else
                        {
                            this.interruptIcons[i].setImageResource(R.mipmap.redled);
                        }
                    }
                }
            }

            setSkipSync(false);
        }
        else
        {
            changeConnectionIcon(0);
            Log.d(MAIN, "Could not get feedback, value returned was null.");
        }
    }

}
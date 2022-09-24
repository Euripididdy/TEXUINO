package test.expandablelistdrawertest;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import java.util.Timer;
import java.util.TimerTask;

public class BackgroundService extends IntentService
{
    private static final String SERVICE_CHECK = "loop and poll csb";
    private static final String TAG = "Background Service";
    Timer t = new Timer();

    public BackgroundService()
    {
        super(BackgroundService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        t.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                Log.i(TAG, "Background service running");
                Intent i = new Intent(SERVICE_CHECK);

                sendBroadcast(i);
            }

        }, 50, 1500);
    }
}

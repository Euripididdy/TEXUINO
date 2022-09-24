package wifi.trc;

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Created by Stefan on 04/06/2015.
 */

final class Connection extends Thread
{
    private static final String TAG = "WIFI Connection";
    private Socket dataSocket;
    private InputStream dataInStream;
    private OutputStream dataOutStream;
    private int portNumber, intByte;
    private String ipAddress, response;
    private char charByte;

    public Connection(String address, int port)
    {
        Log.d(TAG, "Initialising WIFI connection..");
        this.dataSocket = null;
        this.dataInStream = null;
        this.dataOutStream = null;
        this.response = "";
        this.portNumber = port;
        this.ipAddress = address;
        Log.d(TAG, "Connection successfully configured!");
    }

    public void run()
    {
        Log.d(TAG, "Establishing connection..");
        try
        {

            InetAddress ipaddy = InetAddress.getByName(ipAddress);
            dataSocket = new Socket(ipaddy, portNumber);
            //dataSocket.setSoTimeout(1000);
            Log.d(TAG, "Socket created..");
            dataInStream = dataSocket.getInputStream();
            Log.d(TAG, "Input stream created..");
            dataOutStream = dataSocket.getOutputStream();
            Log.d(TAG, "Output stream created..");
        }
        catch (UnknownHostException e)
        {
            Log.e(TAG, "Connection failed! Could not resolve host.");
            e.printStackTrace();
        } catch (IOException e)
        {
            Log.e(TAG, "Connection failed! Socket problems.");
            e.printStackTrace();
        }
        if (this.dataSocket != null)
        {
            while (this.dataSocket.isConnected())
            {
                Log.d(TAG, "Connected!");
                try
                {
                    intByte = this.dataInStream.read();
                    charByte = (char) intByte;
                    //If character is a number
                    //if (intByte >= 47 && intByte < 58)
                    if (intByte >= 31 && intByte < 127)
                    {
                        this.response = this.response + charByte;
                    }
                    Log.i(TAG, "Read from stream: " + intByte + "(dec)");

                } catch (IOException e)
                {
                    Log.e(TAG, "Could not read from stream: " + e.toString());
                    // e.printStackTrace();
                }
            }
        }
        else
        {
            Log.e(TAG, "Connection failed! dataSocket not initialised");
        }
    }

    public boolean isConnected()
    {
        boolean outcome = false;
        if (dataSocket != null)
        {
            outcome = dataSocket.isConnected();
        }
        return outcome;
    }

    public void cancel()
    {
        try
        {
            dataSocket.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public String getResponse()
    {
        return this.response;
    }

    public void resetResponse()
    {
        this.response ="";
    }

    public void sendRequest(byte[] bytes)
    {
        try
        {
            this.dataOutStream.write(bytes);

            ByteBuffer b = ByteBuffer.wrap(bytes);
            String v = new String(b.array());

            Log.i(TAG, "Wrote to stream: " + v);
/*
            try
            {
                Thread.sleep(150);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
*/
        } catch (IOException e)
        {
            Log.e(TAG, "Could not write to stream: " + e.toString());
        }
    }
}

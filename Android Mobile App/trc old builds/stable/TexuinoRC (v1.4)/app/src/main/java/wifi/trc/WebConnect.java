package wifi.trc;

import android.os.AsyncTask;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * Created by Stefan on 25/06/2017.
 */

public class WebConnect extends AsyncTask<String, Void, String>
{
    public static final String REQUEST_METHOD = "GET";
    public static final int READ_TIMEOUT = 15000;
    public static final int CONNECTION_TIMEOUT = 15000;

    URL address;
    HttpURLConnection connection;
    String result;

    public WebConnect(String stringAddress)
    {
        try
        {
            address = new URL(stringAddress);
            connection = (HttpURLConnection) address.openConnection();
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected String doInBackground(String... params)
    {
        //String stringUrl = params[0];
        String inputLine;
        try
        {

            //Set methods and timeouts
            connection.setRequestMethod(REQUEST_METHOD);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);

            //Connect to our url
            connection.connect();

            //Create a new InputStreamReader
            InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());

            //Create a new buffered reader and String Builder
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder stringBuilder = new StringBuilder();

            //Check if the line we are reading is not null
            while ((inputLine = reader.readLine()) != null)
            {
                stringBuilder.append(inputLine);
            }

            //Close our InputStream and Buffered reader
            reader.close();
            streamReader.close();

            //Set our result equal to our stringBuilder
            result = stringBuilder.toString();
        } catch (IOException e)
        {
            e.printStackTrace();
            result = null;
        }

        return result;
    }

    protected void onPostExecute(String result)
    {
        super.onPostExecute(result);
    }

    public void postData(byte[] bytesToSend)
    {
        try
        {
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(0);

            OutputStream out = new BufferedOutputStream(connection.getOutputStream());
            //writeStream(out);
            out.write(bytesToSend);

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public String getData()
    {
        return result;
    }
}

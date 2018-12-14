package com.efi.ingenicopoc2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

public class MainActivity extends ActionBarActivity
{

    public static String TAG = "IngenicoPOC2";
    private String ip = "192.168.1.10";
    private int port = 12000;
    private static ErrorDialogFragment errorDialog;
    private Socket msocket = null;
    private EditText cmdText = null;
    private TextView tvExitType = null;
    private TextView tvTrack1 = null;
    private TextView tvTrack2 = null;
    private TextView tvTrack3 = null;

    private final static char fs = (char)28;

    private final static String CMD_FULLY_AUTO = "23.-DO_THE_FLOW-";
    private final static String CMD_ACCEPTED = "24.ACCEPTED.K3Z";
    private final static String CMD_APPROVED = "24.APPROVED.K3Z";
    private final static String CMD_EACCOUNT= "24.EACCOUNT.K3Z";
    private final static String CMD_MSG = "24.MSG.K3Z"; // This Lane Closed
    private final static String CMD_MSGTHICK = "24.MSGTHICK.K3Z"; // This Lane Closed
    private final static String CMD_OFFLINE = "00.0000";
    private final static String CMD_ONLINE = "01.0000"; // Don't do
    private final static String CMD_REBOOT = "97.";
    private final static String CMD_RESET = "10.";
    private final static String CMD_MSG_SWIPE = "23.Please Slide Card" + fs + "MSG.K3Z";
    private final static String CMD_MSG_SWIPE_AGAIN = "23.Card Read Error. Please Try Again." + fs + "MSG.K3Z";
    private final static String CMD_MSG_SWIPE_DIFFERENT = "23.Please try a different card." + fs + "MSG.K3Z";
    private final static String CMD_GENERIC_SWIPE = "23.Hi ";// + fs + "MSG.K3Z";
    private final static String CMD_THANKS = "24.THANKS.K3Z";
    private final static String CMD_UNIT_DATA = "07.";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        errorDialog = new ErrorDialogFragment();

        cmdText = (EditText)findViewById(R.id.cmdText);
        tvExitType = (TextView)findViewById(R.id.cardExitType);
        tvTrack1 = (TextView)findViewById(R.id.cardTrack1);
        tvTrack2 = (TextView)findViewById(R.id.cardTrack2);
        tvTrack3 = (TextView)findViewById(R.id.cardTrack3);

        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME; // context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;

        TextView versionInfo = (TextView)findViewById(R.id.versionInfo);
        versionInfo.setText("Code: " + versionCode + ", Name: " + versionName);
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

    // Error dialog

    /*
     * OnClick Targets
     */
    /*
    public void sendOnline(View v)
    {
        sendMessage(CMD_ONLINE);
    }
    */

    public void sendOffline(View v)
    {
        sendMessage(CMD_OFFLINE);
    }

    public void sendReboot(View v)
    {
        sendMessage(CMD_REBOOT);
    }

    public void sendReset(View v)
    {
        sendMessage(CMD_RESET);
    }

    public void sendThanks(View v)
    {
        sendMessage(CMD_THANKS);
    }

    public void getUnitData(View v)
    {
        sendMessage(CMD_UNIT_DATA);
    }

    public void sendGenericSwipe(View v)
    {
        clearCardFields();
        sendMessage(CMD_GENERIC_SWIPE);
    }

    public void sendMSGSwipe(View v)
    {
        clearCardFields();
        sendMessage(CMD_MSG_SWIPE);
    }

    public void sendCustomCmd(View v)
    {
        sendMessage(cmdText.getText().toString());
    }

    public void sendAccepted(View v)
    {
        sendMessage(CMD_ACCEPTED);
    }

    public void sendApproved(View v)
    {
        sendMessage(CMD_APPROVED);
    }

    public void sendEaccount(View v)
    {
        sendMessage(CMD_EACCOUNT);
    }

    public void sendMsg(View v)
    {
        sendMessage(CMD_MSG);
    }

    public void sendMsgThick(View v)
    {
        sendMessage(CMD_MSGTHICK);
    }

    public void sendFullFlow(View v)
    {
        sendMessage(CMD_FULLY_AUTO);
    }

    /*
     * Messaging
     */
    public void sendMessage(String msg)
    {
        if (!setSocketInfo())
        {
            return;
        }

        SendMessageTask task = new SendMessageTask();
        task.setIp(ip);
        task.setPort(port);
        task.setMessage(msg);
        task.execute();
    }

    // Misc
    public byte calculateLRC(byte[] bytes)
    {
        byte LRC = 0;
        for (int i = 0; i < bytes.length; i++) {
            LRC ^= bytes[i];
        }
        return LRC;
    }


    public void updateUnitDataDisplay(String rawResponse)
    {
        String manufacturerId = rawResponse.substring(4, 10);
        String deviceId = rawResponse.substring(11, 17);

        int pos = rawResponse.indexOf(fs, 18);
        String serial = rawResponse.substring(18, pos);

        TextView serialView = (TextView)findViewById(R.id.serialTxt);
        serialView.setText(serial);

        TextView deviceIdView = (TextView)findViewById(R.id.deviceIdTxt);
        deviceIdView.setText(deviceId);

        TextView manufacturerIdView = (TextView)findViewById(R.id.manufacturerTxt);
        manufacturerIdView.setText(manufacturerId);
    }

    public void updateCardInfo(String rawResponse)
    {
        // Exit type
        Log.d("***raw response",rawResponse);
        String exitType = rawResponse.substring(4,5);

        switch (exitType)
        {
            case "0":
                tvExitType.setText("Good Read");
                break;
            case "1":
                tvExitType.setText("Bad Read");
                break;
            case "2":
                tvExitType.setText("Cancelled");
                break;
            case "3":
                tvExitType.setText("Button Pressed");
                break;
            case "6":
                tvExitType.setText("Invalid Prompt");
                break;
            case "7":
                tvExitType.setText("Encryption Failed");
                break;
            case "9":
                tvExitType.setText("Declined");
                break;
            default:
                tvExitType.setText("");
        }
        if(exitType.contains("0")) {
            // Track 1
            int track1End = rawResponse.indexOf(fs, 6);
            String track1 = rawResponse.substring(6, track1End);
            tvTrack1.setText(track1);
            Log.d("Track1", track1);

            // Track 2
            int track2End = rawResponse.indexOf(fs, track1End + 1);
            String track2 = rawResponse.substring(track1End + 1, track2End);
            tvTrack2.setText(track2);
            Log.d("Track2", track2);
            // Track 3
            char etx = 0x03;
            int track3End = rawResponse.indexOf(etx, track2End + 1);
            String track3 = rawResponse.substring(track2End + 1, track3End);
            tvTrack3.setText(track3);
            Log.d("Track3", track3);
        }
        else
            Log.d("Exittype","not 0");
    }

    public boolean readSuccess(String rawResponse)
    {
        // Exit type
        String exitType = rawResponse.substring(4,5);
        switch (exitType)
        {
            case "0": // Success
            case "2": // Canceled
                return true;
            case "1": // Bad read
            case "3": // Button pressed
            case "6": // Invalid prompt
            case "7": // Encryption failed
            case "9": // Declined
                break;
        }

        return false;
    }

    private void clearCardFields()
    {
        tvExitType.setText("");
        tvTrack1.setText("");
        tvTrack2.setText("");
        tvTrack3.setText("");
    }

    /*
     * Information Set-up
     */
    public boolean setSocketInfo()
    {
        TextView ipView = (TextView)findViewById(R.id.ip);
        TextView portView = (TextView)findViewById(R.id.port);

        ip = ipView.getText().toString();

        if (ip == null || ip.length() == 0)
        {
            Bundle args = new Bundle();
            args.putString("message", "Please set the Ingenico's IP.");
            errorDialog.setArguments(args);
            errorDialog.show(getFragmentManager(), "error");
            return false;
        }

        if (portView.getText().length() == 0)
        {
            Bundle args = new Bundle();
            args.putString("message", "Please set the Ingenico's port.");
            errorDialog.setArguments(args);
            errorDialog.show(getFragmentManager(), "error");
            return false;
        }

        port = Integer.parseInt(portView.getText().toString());

        if (port <=0 )
        {
            Bundle args = new Bundle();
            args.putString("message", "Ingenico's port must be greater then 0.");
            errorDialog.setArguments(args);
            errorDialog.show(getFragmentManager(), "error");
            return false;
        }

        return true;
    }

    /*
     * Error Dialog
     */
    public static class ErrorDialogFragment extends DialogFragment
    {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            Bundle args = getArguments();
            String message = args.getString("message", "");

            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(message)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    })
                    .setTitle("Error");

            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    // Message task
    /*
    private class SendMessageTask extends AsyncTask<Void, Void, String>
    {
        private String ip = "192.168.1.10";
        private int port = 12000;
        private byte[] msg;
        private String origMsg = "";
        private byte stx = 0x02;
        private byte etx = 0x03;
        private OutputStream out = null;
        private DataInputStream in = null;

        public SendMessageTask()
        {
        }

        public void setMessage(String msg)
        {
            this.origMsg = msg;
            this.msg = origMsg.getBytes();
        }

        public void setIp(String ip)
        {
            this.ip = ip;
        }

        public void setPort(int port)
        {
            this.port = port;
        }

        @Override
        protected void onPostExecute(String result)
        {
            System.out.println("doPostExecute called");
            switch (origMsg)
            {
                case CMD_GENERIC_SWIPE:
                case CMD_MSG_SWIPE:
                    updateCardInfo(result);
                    break;
                case CMD_UNIT_DATA:
                    updateUnitDataDisplay(result);
                    break;
            }
        }

        private void writeOnline(OutputStream out) throws IOException
        {
            write(CMD_ONLINE.getBytes(), out);
        }

        private void write(byte[] message, OutputStream out) throws IOException
        {

            byte[] forLRC = Arrays.copyOf(message, message.length + 1);
            forLRC[forLRC.length - 1] = etx;
            byte lrc = calculateLRC(forLRC);

            out.write(stx);
            out.write(forLRC);
            out.write(lrc);
        }

        @Override
        protected String doInBackground(Void... params)
        {
            String result = "";

            try
            {
                try
                {
                    Log.v(TAG, "IP: " + ip);
                    Log.v(TAG, "Port: " + port);
                    msocket = new Socket(ip, port);
                    msocket.setSoTimeout(1 * 60 * 1000);//Setting timeout to 1 minute
                    Log.v(TAG, "socket connection created");
                    out = msocket.getOutputStream();
                    write(msg, out);

                    switch(origMsg)
                    {
                        case CMD_REBOOT:
                        case CMD_THANKS:
                        case CMD_ONLINE:
                        case CMD_ACCEPTED:
                        case CMD_APPROVED:
                        case CMD_EACCOUNT:
                        case CMD_MSG:
                        case CMD_MSGTHICK:
                            break;
                        default:
                        {
                            in = new DataInputStream(msocket.getInputStream());
                            byte[] messageByte = new byte[4000];
                            boolean end = false;
                            while (!end)
                            {
                                System.out.println("reading");

                                int bytesRead = in.read(messageByte);
                                System.out.println("bytesRead: " + bytesRead);

                                String msgIn = new String(messageByte, 0, bytesRead);
                                System.out.println("msgIn: " + msgIn);

                                if (bytesRead == 1)
                                {
                                    continue;
                                }

                                result += msgIn;
                                end = true;
                            }
                            System.out.println("MESSAGE: " + result);

                            if (origMsg == CMD_UNIT_DATA || origMsg.startsWith("23."))
                            {
                                writeOnline(out); // Needed to session
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    Bundle args = new Bundle();
                    args.putString("message", "Error with socket");
                    errorDialog.setArguments(args);
                    errorDialog.show(getFragmentManager(), "error");
                    e.printStackTrace();
                }
                finally
                {
                    if(in != null)
                    {
                        in.close();
                        Log.v(TAG, "inputstream closed");
                    }

                    if(out != null)
                    {
                        out.close();
                        Log.v(TAG, "socket closed");
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return result;
        }
    }
    */

    private class SendMessageTask extends AsyncTask<Void, Void, String>
    {
        private String ip = "192.168.1.10";
        private int port = 12000;
        private byte[] msg;
        private String origMsg = "";
        private byte stx = 0x02;
        private byte etx = 0x03;
        private byte ack = 0x06;
        private OutputStream out = null;
        //private DataInputStream in = null;
        private DataInputStream in = null;

        public SendMessageTask()
        {
        }

        public void setMessage(String msg)
        {
            this.origMsg = msg;
            this.msg = origMsg.getBytes();
        }

        public void setIp(String ip)
        {
            this.ip = ip;
        }

        public void setPort(int port)
        {
            this.port = port;
        }

        @Override
        protected void onPostExecute(String result)
        {
            switch (origMsg)
            {
                case CMD_GENERIC_SWIPE:
                case CMD_MSG_SWIPE:
                case CMD_FULLY_AUTO:
                    updateCardInfo(result);
                    break;
                case CMD_UNIT_DATA:
                    updateUnitDataDisplay(result);
                    break;
            }
        }

        @Deprecated
        private void writeOnline(OutputStream out) throws IOException
        {
            write(CMD_ONLINE.getBytes(), out);
            Log.v(TAG, "writeOnline");
        }

        private void writeOffline(OutputStream out, DataInputStream in) throws IOException
        {
            write(CMD_OFFLINE.getBytes(), out);
            Log.v(TAG, "writeOffline");

            byte[] messageByte = new byte[4000];
            boolean end = false;
            while (!end)
            {
                int bytesRead = in.read(messageByte);

                if (bytesRead == 1)
                {
                    continue;
                }

                end = true;
            }

            out.write(ack);
            out.flush();
        }

        private void write(byte[] message, OutputStream out) throws IOException
        {

            byte[] forLRC = Arrays.copyOf(message, message.length + 1);
            forLRC[forLRC.length - 1] = etx;
            byte lrc = calculateLRC(forLRC);

            out.write(stx);
            out.write(forLRC);
            out.write(lrc);
        }

        private void writeThanks(OutputStream out, DataInputStream in) throws IOException, InterruptedException
        {
            write(CMD_THANKS.getBytes(), out);
            Log.v(TAG, "WRITE THANKS");
            Thread.sleep(5000);
            writeOffline(out, in);
        }

        @Override
        protected String doInBackground(Void... params)
        {
            String result = "";

            try
            {
                try
                {
                    Log.v(TAG, "IP: " + ip);
                    Log.v(TAG, "Port: " + port);
                    InetSocketAddress address = new InetSocketAddress(ip, port);
                    msocket = new Socket();
                    msocket.connect(address, 5000);
                    out = msocket.getOutputStream();
                    Log.v(TAG, "got output stream");

                    switch(origMsg)
                    {
                        case CMD_FULLY_AUTO:
                            result = doFullFlow(out);
                            break;
                        case CMD_RESET:
                        {
                            write(msg, out);
                            in = new DataInputStream(msocket.getInputStream());
                            byte[] messageByte = new byte[4000];
                            boolean end = false;
                            while (!end)
                            {
                                int bytesRead = in.read(messageByte);
                                String msgIn = new String(messageByte, 0, bytesRead);

                                if (bytesRead == 1)
                                {
                                    continue;
                                }

                                result += msgIn;
                                end = true;
                            }
                            System.out.println("MESSAGE: " + result);

                            out.write(ack);
                            out.flush();

                            break;
                        }
                        case CMD_REBOOT:
                        case CMD_THANKS:
                        case CMD_ACCEPTED:
                        case CMD_APPROVED:
                        case CMD_EACCOUNT:
                        case CMD_MSG:
                        case CMD_MSGTHICK:
                            write(msg, out);
                            Log.v(TAG, "WRITE ONE");
                            break;
                        case CMD_OFFLINE:
                        {
                            if (in == null)
                            {
                                in = new DataInputStream(msocket.getInputStream());
                            }

                            writeOffline(out, in);

                            break;
                        }
                        case CMD_MSG_SWIPE:
                        {
                            write(msg, out);
                            in = new DataInputStream(msocket.getInputStream());
                            byte[] messageByte = new byte[4000];

                            boolean readSuccess = false;
                            int readCount = 0;
                            while (!readSuccess && readCount < 4)
                            {
                                result = "";
                                boolean end = false;

                                if (readCount > 0)
                                {
                                    write(CMD_MSG_SWIPE_AGAIN.getBytes(), out);
                                }

                                while (!end)
                                {
                                    int bytesRead = in.read(messageByte);
                                    String msgIn = new String(messageByte, 0, bytesRead);

                                    if (bytesRead == 1)
                                    {
                                        continue;
                                    }

                                    result += msgIn;
                                    end = true;
                                }

                                System.out.println("MESSAGE: " + result);

                                out.write(ack);
                                out.flush();

                                readSuccess = readSuccess(result);
                                readCount++;
                            }

                            if (readSuccess)
                            {
                                writeThanks(out, in);
                            }
                            else
                            {
                                write(CMD_MSG_SWIPE_DIFFERENT.getBytes(), out);
                                result = "";
                                boolean end = false;
                                while (!end)
                                {
                                    int bytesRead = in.read(messageByte);
                                    String msgIn = new String(messageByte, 0, bytesRead);

                                    if (bytesRead == 1)
                                    {
                                        continue;
                                    }

                                    result += msgIn;
                                    end = true;
                                }

                                System.out.println("MESSAGE: " + result);

                                out.write(ack);
                                out.flush();
                                writeThanks(out, in);
                            }

                            break;
                        }
                        default:
                        {
                            write(msg, out);
                            in = new DataInputStream(msocket.getInputStream());
                            byte[] messageByte = new byte[4000];
                            boolean end = false;
                            while (!end)
                            {
                                int bytesRead = in.read(messageByte);
                                String msgIn = new String(messageByte, 0, bytesRead);

                                if (bytesRead == 1)
                                {
                                    continue;
                                }

                                result += msgIn;
                                end = true;
                            }
                            System.out.println("MESSAGE Displayed: " + result);

                            if (origMsg == CMD_UNIT_DATA || origMsg.startsWith("23."))
                            {
                                out.write(ack);
                                out.flush();
                                writeOffline(out, in);
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    Bundle args = new Bundle();
                    args.putString("message", "Error with socket");
                    errorDialog.setArguments(args);
                    errorDialog.show(getFragmentManager(), "error");
                    e.printStackTrace();
                }
                finally
                {
                    if (msocket != null && !msocket.isClosed())
                    {
                        msocket.close();
                        Log.v(TAG, "socket closed");
                    }

                    if(in != null)
                    {
                        in.close();
                        Log.v(TAG, "inputstream closed");
                    }

                    if(out != null)
                    {
                        out.close();
                        Log.v(TAG, "outpstream closed");
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return result;
        }

        private String doFullFlow(OutputStream out) throws IOException
        {
            String result = "";
            /*

            this.setMessage(CMD_MSG_SWIPE);

            write(msg, out);
            in = new DataInputStream(msocket.getInputStream());
            byte[] messageByte = new byte[4000];
            boolean end = false;
            while (!end)
            {
                System.out.println("reading");

                int bytesRead = in.read(messageByte);
                System.out.println("bytesRead: " + bytesRead);

                String msgIn = new String(messageByte, 0, bytesRead);
                System.out.println("msgIn: " + msgIn);

                if (bytesRead == 1)
                {
                    continue;
                }

                result += msgIn;
                end = true;
            }
            System.out.println("MESSAGE: " + result);
            if (origMsg == CMD_UNIT_DATA || origMsg.startsWith("23."))
            {
                Log.v(TAG, "FLOW: Sending thanks");
                setMessage(CMD_THANKS);
                Log.v(TAG, "FLOW: Thanks sent");
                write(msg, out);
                try
                {
                    Log.v(TAG, "FLOW: sleep");
                    Thread.sleep(5000);
                    Log.v(TAG, "FLOW: awake");
                }
                catch (InterruptedException e)
                {
                    Log.v(TAG, "Exception on sleep thread");
                    e.printStackTrace();
                }
            }

            if (origMsg == CMD_UNIT_DATA || origMsg.startsWith("23."))
            {
                Log.v(TAG, "FLOW: writing online");
                writeOnline(out); // Needed to session
            }
            */

            return result;
        }
    }
}

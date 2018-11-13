package by.axonim.junoforeground;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "SERVER";

    private static final String SERVICE_NAME = "Juno Device";
    private static final String SERVICE_TYPE = "_http._tcp.";
    private NsdManager mNsdManager;

    Button send;
    EditText message;
    private DataOutputStream dataOutputStream;

    NsdManager.RegistrationListener mRegistrationListener = new NsdManager.RegistrationListener() {

        @Override
        public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
            onConnected();
        }

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo,
                                         int errorCode) {
            // Registration failed! Put debugging code here to determine
            // why.
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            // Service has been unregistered. This only happens when you
            // call
            // NsdManager.unregisterService() and pass in this listener.
            Log.d(TAG, "Service Unregistered : " + serviceInfo.getServiceName());
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo,
                                           int errorCode) {
            // Unregistration failed. Put debugging code here to determine
            // why.
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        send = findViewById(R.id.buttonSend);
        message = findViewById(R.id.message);

        send.setOnClickListener(this);

        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        registerService(9000);
    }

    @Override
    protected void onDestroy() {
        if (mNsdManager != null) {
            mNsdManager.unregisterService(mRegistrationListener);
        }
        super.onDestroy();
    }

    public void registerService(int port) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(SERVICE_NAME);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(port);

        mNsdManager.registerService(serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                mRegistrationListener);
    }

    private void onConnected() {
        try {
            ServerSocket socketServer = new ServerSocket(9000);

            Socket socket = socketServer.accept();

            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(dataOutputStream != null){
                    try {
                        dataOutputStream.writeUTF(message.getText().toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
package by.axonim.junoforeground;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class ForegroundClientService extends Service {

    private static final int ONGOING_NOTIFICATION_ID = 1;

    public static final String TAG = "Client";

    private String SERVICE_NAME = "Client Device";
    private String SERVER_NAME = "Juno Device";
    private String SERVICE_TYPE = "_http._tcp.";

    private InetAddress hostAddress;
    private int hostPort;
    private NsdManager mNsdManager;

    public ForegroundClientService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle("Фоновое уведомление")
                        .setContentText("Контролируем ребенка");

        Notification notification = mBuilder.build();
        startForeground(ONGOING_NOTIFICATION_ID, notification);

        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        mNsdManager.discoverServices(SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mNsdManager != null) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
        super.onDestroy();
    }

    NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {

        // Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String regType) {
            Log.d(TAG, "Service discovery started");
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            // A service was found! Do something with it.
            Log.d(TAG, "------NEW DEVICE----");
            Log.d(TAG, "Service discovery success : " + service);
            Log.d(TAG, "Host = " + service.getServiceName());
            Log.d(TAG, "port = " + String.valueOf(service.getPort()));

            if (service.getServiceName().equals(SERVER_NAME)) {
                Log.d(TAG, "------CONNECTION----");
                mNsdManager.resolveService(service, mResolveListener);
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e(TAG, "service lost" + service);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }
    };

    NsdManager.ResolveListener mResolveListener = new NsdManager.ResolveListener() {

        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e(TAG, "Resolve failed " + errorCode);
            Log.e(TAG, "serivce = " + serviceInfo);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "Resolve Succeeded. " + serviceInfo);

            if (serviceInfo.getServiceName().equals(SERVICE_NAME)) {
                Log.d(TAG, "Same IP.");
                return;
            }

            // Obtain port and IP
            hostPort = serviceInfo.getPort();
            hostAddress = serviceInfo.getHost();

            startDataReceiving();
        }
    };

    private void startDataReceiving() {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                Socket socket = null;
                DataInputStream dataInputStream = null;
                DataOutputStream dataOutputStream = null;

                try {
                    socket = new Socket(hostAddress, hostPort);
                    dataOutputStream = new DataOutputStream(
                            socket.getOutputStream());
                    dataInputStream = new DataInputStream(socket.getInputStream());

                    while (true) {
                        String response = dataInputStream.readUTF();
                        Log.d(TAG, response);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        assert socket != null;
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        thread.start();

    }
}

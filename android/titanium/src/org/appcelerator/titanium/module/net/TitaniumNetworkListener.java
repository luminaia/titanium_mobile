/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.appcelerator.titanium.module.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Config;
import android.util.Log;

/**
 * A wrapper for a broadcast receiver that provides network connectivity
 * state information, independent of network type (mobile, Wi-Fi, etc.).
 * {@hide}
 */
public class TitaniumNetworkListener {
    private static final String LCAT = "TiNetListener";
    private static final boolean DBG = Config.LOGD;

    public static final String EXTRA_CONNECTED = "connected";
    public static final String EXTRA_NETWORK_TYPE = "networkType";
    public static final String EXTRA_NETWORK_TYPE_NAME = "networkTypeName";
    public static final String EXTRA_FAILOVER = "failover";
    public static final String EXTRA_REASON = "reason";

    private IntentFilter connectivityIntentFilter;
    private ConnectivityBroadcastReceiver receiver;

    private Handler messageHandler;
    private Context context; // null on release, might need to be softRef.
    private boolean listening;

    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                 return;
            }

            if (messageHandler == null) {
            	Log.w(LCAT, "Network receiver is active but no handler has been set.");
            	return;
            }

           boolean noConnectivity =
            	intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            NetworkInfo networkInfo = (NetworkInfo)
            	intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            NetworkInfo otherNetworkInfo = (NetworkInfo)
                intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

            String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
            boolean failover =
                intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);

            if (DBG) {
                Log.d(LCAT, "onReceive(): mNetworkInfo=" + networkInfo +  " mOtherNetworkInfo = "
                        + (otherNetworkInfo == null ? "[none]" : otherNetworkInfo +
                        " noConn=" + noConnectivity));
            }

        	Message message = Message.obtain(messageHandler);

        	Bundle b = message.getData();
        	b.putBoolean(EXTRA_CONNECTED, !noConnectivity);
        	b.putInt(EXTRA_NETWORK_TYPE, networkInfo.getType());
        	if (noConnectivity) {
        		b.putString(EXTRA_NETWORK_TYPE_NAME, "NONE");
        	} else {
           		b.putString(EXTRA_NETWORK_TYPE_NAME, networkInfo.getTypeName());
           	}
        	b.putBoolean(EXTRA_FAILOVER, failover);
        	b.putString(EXTRA_REASON, reason);

        	message.sendToTarget();
        }
    };

    /**
     * Create a new TitaniumNetworkListener.
     */
    public TitaniumNetworkListener(Handler messageHandler) {
        this.receiver = new ConnectivityBroadcastReceiver();
        this.messageHandler = messageHandler;
        this.connectivityIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    }

    public void attach(Context context) {
    	if (!listening) {
    		if (this.context == null) {
    			this.context = context;
    		} else {
    			throw new IllegalStateException("Context was not cleaned up from last release.");
    		}
    		context.registerReceiver(receiver, connectivityIntentFilter);
    		listening = true;
    	} else {
    		Log.w(LCAT, "Connectivity listener is already attached");
    	}
    }

    public void detach() {
    	if (listening) {
			context.unregisterReceiver(receiver);
			context = null;
			listening = false;
    	}
    }
}
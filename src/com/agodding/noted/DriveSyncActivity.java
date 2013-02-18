package com.agodding.noted;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.agodding.noted.sync.DriveSyncService;
import com.google.android.gms.common.AccountPicker;

@Deprecated
public class DriveSyncActivity extends Activity {
	
	private static final int ACCOUNT_REQUEST_ID = 0;
	public static final int AUTH_TOKEN_REQUEST_ID = 1;
	public static final String AUTH_APP = "do_auth";
	public static final String EXTRA_AUTH_APP_INTENT = "auth_intent";
	
	private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
           if (AUTH_APP.equals(action)) {
                Intent authAppIntent = intent.getParcelableExtra(EXTRA_AUTH_APP_INTENT);
                startActivityForResult(authAppIntent, AUTH_TOKEN_REQUEST_ID);
            }
        }
    };
	private String accountName;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		chooseAccount();
	}

	private void chooseAccount() {
		startActivityForResult(AccountPicker.newChooseAccountIntent(null, null, new String[] {"com.google"}, false, null, null, null, null), ACCOUNT_REQUEST_ID);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case ACCOUNT_REQUEST_ID: {
				if (resultCode == RESULT_OK && data != null) {
					accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
					Intent intent = new Intent(this, DriveSyncService.class);
					intent.putExtra(DriveSyncService.EXTRA_ACCOUNT_NAME, accountName);
					startService(intent);
				}
				break;
			}
			case AUTH_TOKEN_REQUEST_ID: {
                if (resultCode == RESULT_OK) {
                	Intent intent = new Intent(this, DriveSyncService.class);
					intent.putExtra(DriveSyncService.EXTRA_ACCOUNT_NAME, accountName);
                    startService(intent);
                }
                break;
			}
		}
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
                new IntentFilter(AUTH_APP));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onPause();
    }
}

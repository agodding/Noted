package com.agodding.noted;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;

import com.agodding.noted.sync.DriveSyncService;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class SettingsActivity extends PreferenceActivity {

	private static final int ACCOUNT_REQUEST_ID = 0;
	public static final int AUTH_TOKEN_REQUEST_ID = 1;
	public static final String AUTH_APP = "do_auth";
	public static final String SYNC_SUCCESS = "sync_successful";
	public static final String EXTRA_AUTH_APP_INTENT = "auth_intent";

	private String accountName;

	private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (AUTH_APP.equals(action)) {
				Intent authAppIntent = intent.getParcelableExtra(EXTRA_AUTH_APP_INTENT);
				startActivityForResult(authAppIntent, AUTH_TOKEN_REQUEST_ID);
			} else if (SYNC_SUCCESS.equals(action)) {
				if (syncPref != null) {
					dialog.cancel();
					syncPref.setChecked(true);
				}
			}

		}
	};
	private CheckBoxPreference syncPref;
	private ProgressDialog dialog;
	public static final String PREFERENCES_NAME = "MyNotes";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onResume() {
		super.onResume();

		LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
				new IntentFilter(AUTH_APP));
		LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
				new IntentFilter(SYNC_SUCCESS));

		// Every time this activity becomes visible, let's check if the user
		// should be shown the backup option
		PreferenceCategory backupCategory = (PreferenceCategory) findPreference("backup");
		int responseCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		Dialog downloadPlayServicesDialog =
				GooglePlayServicesUtil.getErrorDialog(responseCode, this, 0);
		if (downloadPlayServicesDialog != null) {
			backupCategory.setEnabled(false);
			downloadPlayServicesDialog.show();
		}

		syncPref = (CheckBoxPreference) findPreference(getResources().getString(R.string.syncPref));
		syncPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (((Boolean) newValue) == true) {
					chooseAccount();
					return false;
				}
				return true;
			}
		});
	}

	@Override
	protected void onPause() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case (android.R.id.home):
				overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
				this.finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void chooseAccount() {
		startActivityForResult(AccountPicker.newChooseAccountIntent(null, null,
				new String[] { "com.google" }, false, null, null, null, null), ACCOUNT_REQUEST_ID);
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
				dialog = ProgressDialog.show(this, "Preparing to sync", null, true);
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
}

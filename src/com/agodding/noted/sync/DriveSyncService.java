package com.agodding.noted.sync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.agodding.noted.SettingsActivity;
import com.agodding.noted.model.Note;
import com.agodding.noted.persistence.DBManager;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

public class DriveSyncService extends Service {

	private static final String MIME_TYPE_TEXT_PLAIN = "text/plain";
	private static final String PREFERENCE_MYNOTES_FOLDER_ID = "mynotes_folder_id";
	private static final String PREFERENCE_AUTH_TOKEN= "auth_token";
	private static final String DRIVE_FOLDER_NAME = "Noted";
	public static String EXTRA_ACCOUNT_NAME = "account_name";
	private static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive";
	private static final String DRIVE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

	private String authToken;
	private String accountName;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		accountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME);
		getAuthToken();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public void getAuthToken() {
		authToken = getSharedPreferences(SettingsActivity.PREFERENCES_NAME, MODE_PRIVATE).getString(PREFERENCE_AUTH_TOKEN, null);
		if (authToken != null) {
			handleAuthToken();
			return;
		}
		
		new Thread(new Runnable() {
			public void run() {
				try {
					authToken =
							GoogleAuthUtil.getToken(DriveSyncService.this, accountName, "oauth2:"
									+ DRIVE_SCOPE);
					getSharedPreferences(SettingsActivity.PREFERENCES_NAME, MODE_PRIVATE).edit().putString(PREFERENCE_AUTH_TOKEN, authToken).commit();
					handleAuthToken();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (UserRecoverableAuthException e) {
					Intent authRequiredIntent = new Intent(SettingsActivity.AUTH_APP);
					authRequiredIntent.putExtra(SettingsActivity.EXTRA_AUTH_APP_INTENT,
							e.getIntent());
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
							authRequiredIntent);
					e.printStackTrace();
				} catch (GoogleAuthException e) {
					e.printStackTrace();
				} finally {
					stopSelf();
				}
			}
		}).start();
	}

	public void invalidateToken() {
		GoogleAuthUtil.invalidateToken(this, authToken);
		getAuthToken();
	}

	private void handleAuthToken() {
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(SettingsActivity.SYNC_SUCCESS));
		syncNotes();
	}
	
	private Drive getDriveService() {
		return new Drive.Builder(
				AndroidHttp.newCompatibleTransport(),
				new JacksonFactory(),
				new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest httpRequest) throws IOException {
						httpRequest.setInterceptor(
								new HttpExecuteInterceptor() {
									@Override
									public void intercept(HttpRequest request) throws IOException {
										request.getHeaders().setAuthorization("Bearer " + authToken);
									}
								});
					}
				}).build();
	}

	private void syncNotes() {
		new Thread(new Runnable() {
			public void run() {
				Drive driveService = getDriveService();
				try {
					String folderID = getSharedPreferences(SettingsActivity.PREFERENCES_NAME, MODE_PRIVATE).getString(PREFERENCE_MYNOTES_FOLDER_ID, null);
					if (folderID == null) {
						File driveFolder = findDriveFolderByName(driveService);
						if (driveFolder == null) {
							createMyNotesFolder(driveService);
							addAllLocalFilesToDriveFolder(driveService);
						} else {
							//TODO: reinstalling, so download all remote files and persist them locally
						}
					} else {
						File driveFolder = driveService.files().get(folderID).execute();
						reconcileFiles(driveFolder, driveService);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				stopSelf();
			}
		}).start();
	}
	
	private File findDriveFolderByName(Drive driveService) throws IOException {
		FileList fileList = driveService.files().list().setQ("title = '" + DRIVE_FOLDER_NAME + "'").execute();
		if (fileList.getItems().size() == 0){
			return null;
		} else {
			File driveFolder = fileList.getItems().get(0);
			return driveFolder;
		}
	}
	
	private void createMyNotesFolder(Drive driveService) throws IOException {
		File aFile = new File();
		aFile.setMimeType(DRIVE_FOLDER_MIME_TYPE);
		aFile.setTitle(DRIVE_FOLDER_NAME);
		File driveFolder = driveService.files().insert(aFile).execute();
		SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFERENCES_NAME, MODE_PRIVATE);
		prefs.edit().putString(PREFERENCE_MYNOTES_FOLDER_ID, driveFolder.getId()).commit();
	}
	
	private void addAllLocalFilesToDriveFolder(Drive driveService) throws IOException {
		DBManager db = DBManager.getInstance(this);
		ArrayList<Note> notes = db.getNotes();
		for (Note aNote : notes) {
			addNoteToDriveFolder(driveService, aNote);
		}
	}
	
	private void addNoteToDriveFolder(Drive driveService, Note aNote) throws IOException {
		File newFile = new File();
		newFile.setTitle(aNote.getTitle());
		newFile.setMimeType(MIME_TYPE_TEXT_PLAIN);
		newFile.setDescription(Long.toString(aNote.getId()));
		
		String folderID = getSharedPreferences(SettingsActivity.PREFERENCES_NAME, MODE_PRIVATE).getString(PREFERENCE_MYNOTES_FOLDER_ID, null);
		newFile.setParents(Arrays.asList(new ParentReference().setId(folderID)));
		driveService.files().insert(newFile, ByteArrayContent.fromString(MIME_TYPE_TEXT_PLAIN, aNote.getBody())).execute();
	}
	
	private void reconcileFiles(File driveFolder, Drive driveService) throws IOException {
		String folderID = getSharedPreferences(SettingsActivity.PREFERENCES_NAME, MODE_PRIVATE).getString(PREFERENCE_MYNOTES_FOLDER_ID, null);
		FileList fileList = driveService.files().list().setQ("'" + folderID + "' in parents").execute();
		List<File> remoteFiles = fileList.getItems();
		HashMap<Long, File> remoteFileHash = new HashMap<Long, File>();
		for (File aRemoteFile : remoteFiles) {
			try {
				remoteFileHash.put(Long.valueOf(aRemoteFile.getDescription()), aRemoteFile);
			} catch (NumberFormatException e) {
				//There was no ID associated with the remote file. Move on to the next one
			}
		}
		
		DBManager db = DBManager.getInstance(this);
		ArrayList<Note> localNotes = db.getNotes();
		for (Note localNote : localNotes) {
			File remoteFile = remoteFileHash.remove(Long.valueOf(localNote.getId()));
			if (remoteFile != null) {
				this.compareAndUpdate(localNote, remoteFile, driveService);
			} else {
				this.addNoteToDriveFolder(driveService, localNote);
			}
		}
	}

	private void compareAndUpdate(Note localNote, File remoteFile, Drive driveService) throws IOException {
		if (localNote.getTimestamp() > remoteFile.getModifiedDate().getValue()) {
			driveService.files().update(remoteFile.getId(), remoteFile, ByteArrayContent.fromString(MIME_TYPE_TEXT_PLAIN, localNote.getBody())).execute();
		}
	}
}

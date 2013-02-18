package com.agodding.noted;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import com.agodding.noted.model.Note;
import com.agodding.noted.persistence.DBManager;

public class NoteActivity extends Activity {

	public static final String EXTRA_NOTE_ID = "note_id";
	private MenuItem saveItem;
	private Note note;
	private boolean saveButtonVisible = false;
	private EditText titleField;
	private EditText contentField;
	private MenuItem passwordItem;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.note);

		if (getIntent().hasExtra(EXTRA_NOTE_ID)) {
			Long noteID = (Long) getIntent().getExtras().get(EXTRA_NOTE_ID);
			note = DBManager.getInstance(this).retreiveNote(noteID.longValue());
		} else {
			note = new Note();
		}

		init();
	}

	private void init() {
		// Text Fields
		titleField = (EditText) findViewById(R.id.noteTitle);
		titleField.setText(note.getTitle());
		contentField = (EditText) findViewById(R.id.noteContent);
		contentField.setText(note.getBody());

		TextWatcher watcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (saveItem != null) {
					saveItem.setVisible(true);
					saveButtonVisible = true;
					invalidateOptionsMenu();
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		};
		titleField.addTextChangedListener(watcher);
		contentField.addTextChangedListener(watcher);

		// Action Bar
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		if (note.getTitle() == null) {
			actionBar.setTitle("New Note");
			titleField.requestFocus();
		} else {
			actionBar.setTitle(note.getTitle());
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.note_menu, menu);

		// Item configuration
		saveItem = menu.findItem(R.id.save_item);
		saveItem.setVisible(saveButtonVisible);
		menu.findItem(R.id.delete_item).setVisible(note.getId() != -1);

		passwordItem = menu.findItem(R.id.lock_item);
		configurePasswordMenuItem();
		return true;
	}

	private void configurePasswordMenuItem() {
		if (note.getPassword() != null) {
			passwordItem.setIcon(R.drawable.menu_unlock);
			passwordItem.setTitle("Unlock");
		} else {
			passwordItem.setIcon(R.drawable.menu_lock);
			passwordItem.setTitle("Lock");
		}
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.save_item:
			doSave();
			finish();
			break;
		case R.id.delete_item:
			DBManager.getInstance(this).deleteNote(note);
			finish();
			break;
		case R.id.tags_item:
			Intent tagsIntent = new Intent(this, TagsActivity.class);
			tagsIntent.putExtra(EXTRA_NOTE_ID, note.getId());
			startActivity(tagsIntent);
			break;
		case R.id.lock_item:
			handlePasswordConfiguration();
			break;
		case android.R.id.home:
			finish();
			overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
		}
		return true;
	}

	private void handlePasswordConfiguration() {
		if (note.getPassword() != null) {
			note.setPassword(null);
			doAsyncSave();
			configurePasswordMenuItem();
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final EditText passwordTextView = new EditText(this);
			passwordTextView.setTransformationMethod(new PasswordTransformationMethod());
			builder.setMessage("Set a password for this note")
					.setCancelable(false)
					.setView(passwordTextView)
					.setPositiveButton("Done", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
							note.setPassword(passwordTextView.getText().toString());
							configurePasswordMenuItem();
							doAsyncSave();
						}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			AlertDialog alert = builder.create();
			alert.show();
		}
	}

	private void doSave() {
		note.setBody(contentField.getText().toString());
		note.setTitle(titleField.getText().toString());
		if (note.getId() > 0) {
			DBManager.getInstance(this).updateNote(note);
		} else {
			DBManager.getInstance(this).createNote(note);
		}
	}

	private void doAsyncSave() {
		new Thread(new Runnable() {
			public void run() {
				doSave();
			}
		}).run();
	}
}

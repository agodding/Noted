package com.agodding.noted;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.agodding.noted.model.Note;
import com.agodding.noted.persistence.DBManager;
import com.agodding.noted.view.NoteListAdapter;

public class NotesListActivity extends Activity {

	private String searchQuery = null;
	private ListView listView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notes_list);
		handleIntent(getIntent());
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		MenuInflater inflater = getMenuInflater();

		if (searchQuery != null) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(searchQuery);
		} else {
			inflater.inflate(R.menu.list_menu, menu);
			SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
			SearchView searchView = (SearchView) menu.findItem(R.id.searchItem).getActionView();
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		}
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.newItem:
			startActivity(new Intent(this, NoteActivity.class));
			break;
		case android.R.id.home:
			finish();
			break;
		case R.id.settings:
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		listView = (ListView) findViewById(R.id.notesList);
		final ArrayList<Note> notes;
		if (searchQuery == null) {
			notes = DBManager.getInstance(this).getNotes();
		} else {
			notes = DBManager.getInstance(this).queryNotes(searchQuery);
		}
		NoteListAdapter adapter = new NoteListAdapter(notes, this);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index, long arg3) {
				ArrayList<Note> notes = DBManager.getInstance(NotesListActivity.this).getNotes();
				checkPassword(notes.get(index));
			}
		});

		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(new ChoiceListener(adapter));
	}

	private void checkPassword(final Note note) {
		if (note.getPassword() == null) {
			openNoteForEditing(note);
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final EditText passwordTextView = new EditText(this);
		passwordTextView.setTransformationMethod(new PasswordTransformationMethod());
		builder.setMessage("This note is locked. Enter password to continue")
				.setView(passwordTextView)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if (passwordTextView.getText().toString().equals(note.getPassword())) {
							openNoteForEditing(note);
						} else {
							Toast.makeText(NotesListActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
						}
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
	
	private void openNoteForEditing(Note note) {
		Intent intent = new Intent(NotesListActivity.this, NoteActivity.class);
		intent.putExtra(NoteActivity.EXTRA_NOTE_ID, note.getId());
		startActivity(intent);
	}

	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			searchQuery = intent.getStringExtra(SearchManager.QUERY);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		DBManager.getInstance(this).onClose();
	}

	/*
	 * ListView multiple choice listener
	 */
	private class ChoiceListener implements MultiChoiceModeListener {

		private NoteListAdapter adapter;

		public ChoiceListener(NoteListAdapter adapter) {
			this.adapter = adapter;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case (R.id.context_delete_item):
				deleteSelectedItems();
				mode.finish();
				return true;
			}
			return false;
		}

		private void deleteSelectedItems() {
			ArrayList<Boolean> states = adapter.getCheckedStates();
			ArrayList<Note> notes = DBManager.getInstance(NotesListActivity.this).getNotes();
			if (notes.size() != states.size()) {
				return;
			}
			for (int i = 0; i < notes.size(); i++) {
				if (states.get(i).booleanValue()) {
					DBManager.getInstance(NotesListActivity.this).deleteNote(notes.get(i));
				}
			}
			adapter.setNotes(DBManager.getInstance(NotesListActivity.this).getNotes());
			adapter.notifyDataSetChanged();
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.list_context_menu, menu);
			adapter.setInActionMode();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			adapter.resetCheckedStates();
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
				boolean checked) {
			adapter.setItemChecked(position);
		}
	}
}
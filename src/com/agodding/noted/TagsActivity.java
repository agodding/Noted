package com.agodding.noted;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.agodding.noted.model.Note;
import com.agodding.noted.persistence.DBManager;

public class TagsActivity extends Activity {

	private Note note;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tags);
		Long noteID = (Long) getIntent().getExtras().get(NoteActivity.EXTRA_NOTE_ID);
		note = DBManager.getInstance(this).retreiveNote(noteID.longValue());
		
		//Action bar
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle("Tags");
		
		initViews();
	}

	private void initViews() {
		final EditText tagText = (EditText) findViewById(R.id.tagEntryText);
		Button addButton = (Button) findViewById(R.id.addButton);
		final ListView tagList = (ListView) findViewById(R.id.tagList);
		tagList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, note.getTags()));
		
		addButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String tag = tagText.getText().toString();
				if (tag.length() > 0) {
					if (!note.getTags().contains(tag)) {
						note.addTag(tag);
						tagList.setAdapter(new ArrayAdapter<String>(TagsActivity.this, android.R.layout.simple_list_item_1, note.getTags()));
						tagText.setText(null);
						DBManager.getInstance(TagsActivity.this).updateNote(note);
					}
				}
			}
		});
	}
	
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
		}
		return true;
	}
	
}

package com.agodding.noteds.view;

import java.util.ArrayList;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.agodding.noted.R;
import com.agodding.noted.model.Note;

public class NoteListAdapter extends BaseAdapter {

	private ArrayList<Note> notes;
	private ArrayList<Boolean> checkedStates;
	private Activity activity;
	private boolean inActionMode = false;

	public NoteListAdapter(ArrayList<Note> noteList, Activity activity) {
		notes = noteList;
		this.activity = activity;
		resetCheckedStates();
	}

	@Override
	public int getCount() {
		return notes.size();
	}

	@Override
	public Object getItem(int position) {
		return notes.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public void setNotes(ArrayList<Note> notes) {
		this.notes = notes;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		Note aNote = notes.get(position);
		ViewGroup returnViewLayout = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.note_row, null);
		TextView noteNameText = (TextView) returnViewLayout.findViewById(R.id.noteNameText);
		TextView modDateText = (TextView) returnViewLayout.findViewById(R.id.modDateText);

		noteNameText.setText(aNote.getTitle());
		modDateText.setText(aNote.getFormattedModDate());

		if (inActionMode) {
			CheckBox checkbox = new CheckBox(activity);
			Boolean checkedState = checkedStates.get(position);
			checkbox.setChecked(checkedState != null && checkedState.booleanValue());
			checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					checkedStates.set(position, Boolean.valueOf(isChecked));
				}
			});
			checkbox.setAlpha(0);
			returnViewLayout.addView(checkbox, 0);
			checkbox.animate().alpha(1);
			
			noteNameText.animate().x(noteNameText.getX() + 65);
			noteNameText.animate().start();
		}
		if (position == getCount()-1) {
			inActionMode = false;
		}
		
		return returnViewLayout;
	}

	public void setInActionMode() {
		inActionMode = true;
	}
	
	public void setItemChecked(int index) {
		checkedStates.set(index, Boolean.valueOf(true));
	}
	
	public ArrayList<Boolean> getCheckedStates() {
		return checkedStates;
	}
	
	public void resetCheckedStates() {
		checkedStates = new ArrayList<Boolean>(notes.size());
		for (int i=0; i<notes.size(); i++) {
			checkedStates.add(Boolean.valueOf(false));
		}
	}
}

/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.example.android.notepad;

import com.example.android.notepad.R;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Button;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import android.app.DatePickerDialog;
import android.widget.DatePicker;
import android.app.Dialog;
import android.view.View;

/**
 * This Activity handles all four states of editing Notes:
 * <ul>
 * <li> Creating a new note (empty note)
 * <li> Creating a new note with pre-filled title and content (from external intent)
 * <li> Editing an existing note
 * <li> Viewing an existing note (read-only mode)
 * </ul>
 * 
 * It's based on the standard Android Notepad example but with added to-do list functionality.
 */
public class NoteEditor extends Activity {
    private static final String TAG = "NoteEditor";

    /**
     * The columns we are interested in from the database
     */
    private static final String[] PROJECTION =
        new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_IS_TODO,
            NotePad.Notes.COLUMN_NAME_IS_COMPLETED,
            NotePad.Notes.COLUMN_NAME_DUE_DATE
        };

    /**
     * The key to use when passing original content to saved state.
     */
    private static final String ORIGINAL_CONTENT = "origContent";

    /**
     * The state of doing an edit.
     */
    private static final int STATE_EDIT = 0;
    
    /**
     * The state of doing an insert.
     */
    private static final int STATE_INSERT = 1;

    /**
     * Current state of the activity, either STATE_EDIT or STATE_INSERT.
     */
    private int mState;
    
    /**
     * The URI for the note being edited.
     */
    private Uri mUri;
    
    /**
     * The content resolver to use.
     */
    private ContentResolver mContentResolver;
    
    /**
     * The cursor for the current note.
     */
    private Cursor mCursor;
    
    /**
     * The EditText that holds the note's content.
     */
    private EditText mText;
    
    /**
     * The original content of the note, for comparison when autosaving.
     */
    private String mOriginalContent;
    
    // 待办事项相关变量
    private LinearLayout mTodoSection;
    private CheckBox mTodoCheckbox;
    private TextView mDueDateText;
    private Switch mTodoSwitch;
    private Button mSetDueDateButton;
    private LinearLayout mBottomControlArea;
    
    private boolean mIsTodo = false;
    private boolean mIsCompleted = false;
    private long mDueDate = -1;
    private static final int DATE_DIALOG_ID = 0;
    private Calendar mDueDateCalendar = Calendar.getInstance();

    /**
     * This is a custom EditText that draws lines between each line of text that is displayed.
     */
    public static class LinedEditText extends EditText {
        private Rect mRect;
        private Paint mPaint;

        /**
         * This constructor is used by LayoutInflater
         */
        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);

            // Creates a Rect and a Paint object, and sets the style and color of the Paint object.
            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }

        /**
         * This is called to draw the LinedEditText object
         * @param canvas The canvas on which the background is drawn.
         */
        @Override
        protected void onDraw(Canvas canvas) {
            // Gets the number of lines of text in the View.
            int count = getLineCount();
            // Gets the global Rect and Paint objects
            Rect r = mRect;
            Paint paint = mPaint;

            /* Draws one line in the rectangle for every line of text. */
            for (int i = 0; i < count; i++) {
                // Gets the baseline coordinates for the current line of text
                int baseline = getLineBounds(i, r);

                /* Draws a line at the baseline of the current line of text. */
                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }

            // Finishes up by calling the parent method
            super.onDraw(canvas);
        }
    }

    private long currentCategoryId = 1; // 默认分类

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContentResolver = getContentResolver();

        // Get the intent that started this activity
        Intent intent = getIntent();

        // Get the intent action
        String action = intent.getAction();

        // Handle intents for editing or inserting
        if (Intent.ACTION_EDIT.equals(action)) {
            // Requested to edit: set that state, and get the URI of the note to edit
            mState = STATE_EDIT;
            mUri = intent.getData();
        } else if (Intent.ACTION_INSERT.equals(action)) {
            // Requested to insert: set that state, and create a new entry
            mState = STATE_INSERT;
            mUri = mContentResolver.insert(intent.getData(), null);

            // If we couldn't create a new note, finish with an error message
            if (mUri == null) {
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());
                finish();
                return;
            }

            // The new entry was created, so assume all goes well and set the data to the new URI
            setResult(RESULT_OK, (new Intent()).setData(mUri));
        } else {
            // If the activity was started with neither EDIT nor INSERT, then we can't do anything
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        // Sets the layout for this activity.
        setContentView(R.layout.note_editor);

        // Gets the EditText widget for entering/editing notes
        mText = (EditText) findViewById(R.id.note);

        // 初始化待办事项相关控件
        mTodoSection = (LinearLayout) findViewById(R.id.todo_section);
        mTodoCheckbox = (CheckBox) findViewById(R.id.todo_checkbox);
        mDueDateText = (TextView) findViewById(R.id.due_date_text);
        mTodoSwitch = (Switch) findViewById(R.id.todo_switch);
        mSetDueDateButton = (Button) findViewById(R.id.set_due_date_button);
        // 获取底部控制区域引用
        mBottomControlArea = (LinearLayout) findViewById(R.id.bottom_control_area);
        // 确保底部控制区域默认可见
        if (mBottomControlArea != null) {
            mBottomControlArea.setVisibility(View.VISIBLE);
        }
        
        // 设置待办事项开关的监听器
        mTodoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 保存当前的笔记内容，避免在切换待办事项状态时丢失
                String currentText = mText.getText().toString();
                mIsTodo = isChecked;
                updateTodoUI();
                // 先恢复内容，再保存
                mText.setText(currentText);
                saveNoteContent(); // 保存待办事项状态到数据库
            }
        });

        // 设置待办事项复选框的监听器
        mTodoCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mIsCompleted = isChecked;
                saveNoteContent();
                updateDueDateText(); // 更新截止日期显示
            }
        });

        // 设置截止日期按钮的监听器
        mSetDueDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });

        // For Edit only, retrieve the note's data
        mCursor = mContentResolver.query(mUri, PROJECTION, null, null, null);
        if (mCursor == null) {
            finish();
            return;
        }

        // Get the note's content and title from the cursor
        if (mCursor.moveToFirst()) {
            // Get the note content
            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            mOriginalContent = mCursor.getString(colNoteIndex);
            mText.setTextKeepState(mOriginalContent);
            
            // Get to-do list related data
            int colIsTodoIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_IS_TODO);
            mIsTodo = mCursor.getInt(colIsTodoIndex) == 1;
            
            int colIsCompletedIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_IS_COMPLETED);
            mIsCompleted = mCursor.getInt(colIsCompletedIndex) == 1;
            
            int colDueDateIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_DUE_DATE);
            mDueDate = mCursor.getLong(colDueDateIndex);
            
            // Update UI based on to-do list data
            mTodoSwitch.setChecked(mIsTodo);
            mTodoCheckbox.setChecked(mIsCompleted);
            updateTodoUI();
            
            if (mDueDate > 0) {
                mDueDateCalendar.setTimeInMillis(mDueDate);
                updateDueDateText();
            }
        }

        // If we are in INSERT mode, set the title from the incoming intent if it has one
        if (mState == STATE_INSERT) {
            // Set title of the activity
            setTitle(getResources().getString(R.string.title_create));
            
            // Handle pre-filled content from external intent
            Intent intentThatStartedThisActivity = getIntent();
            String initialContent = intentThatStartedThisActivity.getStringExtra(Intent.EXTRA_TEXT);
            String initialTitle = intentThatStartedThisActivity.getStringExtra(Intent.EXTRA_SUBJECT);
            
            if (initialContent != null) {
                mText.setText(initialContent);
            }
            
            // We don't actually use the title here since this app puts everything in the note body
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * mCursor is initialized, since onCreate() always precedes onResume for any running
         * process. This tests that it's not null, since it should always contain data.
         */
        if (mCursor != null) {
            // Requery in case something changed while paused (such as the title)
            // 重新查询数据库而不是使用已废弃的requery()方法
            mCursor.close();
            mCursor = getContentResolver().query(mUri, PROJECTION, null, null, null);

            /* Moves to the first record. Always call moveToFirst() before accessing data in
             * a Cursor for the first time. The semantics of using a Cursor are that when it is
             * created, its internal index is pointing to a "place" immediately before the first
             * record. */
            if (!mCursor.moveToFirst()) {
                Log.e(TAG, "Cursor is empty when trying to load note");
                setTitle(getResources().getString(R.string.error_title));
                mText.setText(getResources().getString(R.string.error_message));
                return;
            }

            // Modifies the window title for the Activity according to the current Activity state.
            if (mState == STATE_EDIT) {
                // Set the title of the Activity to include the note title
                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                Resources res = getResources();
                String text = String.format(res.getString(R.string.title_edit), title);
                setTitle(text);
            // Sets the title to "create" for inserts
            } else if (mState == STATE_INSERT) {
                setTitle(getResources().getString(R.string.title_create));
            }

            // 恢复待办事项相关数据
            int colIsTodoIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_IS_TODO);
            mIsTodo = mCursor.getInt(colIsTodoIndex) == 1;
            
            int colIsCompletedIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_IS_COMPLETED);
            mIsCompleted = mCursor.getInt(colIsCompletedIndex) == 1;
            
            int colDueDateIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_DUE_DATE);
            mDueDate = mCursor.getLong(colDueDateIndex);
            
            // 更新UI
            mTodoSwitch.setChecked(mIsTodo);
            mTodoCheckbox.setChecked(mIsCompleted);
            updateTodoUI();
            
            // 恢复截止日期
            if (mDueDate > 0) {
                mDueDateCalendar.setTimeInMillis(mDueDate);
                updateDueDateText();
            }
            
            // 恢复笔记原始内容，用于检测是否有变化
            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            mOriginalContent = mCursor.getString(colNoteIndex);
            mText.setTextKeepState(mOriginalContent);
        } else {
            // This should not happen. The cursor was valid in onCreate().
            Log.e(TAG, "cursor is null in onResume()");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save the original text, to check if we really need to do a save in onPause().
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        if (mCursor != null) {
            // Save the changes if needed
            saveNoteContent();
            
            // Close the cursor
            mCursor.close();
            mCursor = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.note_editor_menu, menu);
        
        // Copy/paste options are already included in the XML menu
        
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Hide/show menu items based on the state
        if (mState == STATE_INSERT) {
            menu.findItem(R.id.menu_delete).setVisible(false);
        }
        
        // Check if clipboard has text for paste menu item
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        boolean hasText = clipboard.hasPrimaryClip() && 
                         clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
        menu.findItem(R.id.menu_paste).setVisible(hasText);
        
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection using if-else instead of switch to avoid constant expression requirement
        int id = item.getItemId();
        
        if (id == R.id.menu_save) {
            saveNoteContent();
            finish();
            return true;
        } else if (id == R.id.menu_delete) {
            deleteNote();
            return true;
        } else if (id == R.id.menu_cancel) {
            cancelNote();
            return true;
        } else if (id == R.id.menu_copy) {
            // Copy text to clipboard
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            String textToCopy = mText.getText().toString();
            ClipData clip = ClipData.newPlainText("Note", textToCopy);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.copy_success, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.menu_paste) {
            performPaste();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Displays a category selection dialog.
     */
    private void showCategoryDialog() {
        // Category selection implementation would go here
        Toast.makeText(this, "Category selection not implemented yet", Toast.LENGTH_SHORT).show();
    }

    /**
     * Paste text from clipboard into the editor.
     */
    private final void performPaste() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            CharSequence text = clip.getItemAt(0).getText();
            if (text != null) {
                mText.getText().insert(mText.getSelectionStart(), text);
                saveNoteContent();
            }
        }
    }

    /**
     * Updates the note in the database.
     */
    private final void updateNote(String text, String title) {
        // Create a values map for the update
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);
        values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        values.put(NotePad.Notes.COLUMN_NAME_IS_TODO, mIsTodo ? 1 : 0);
        values.put(NotePad.Notes.COLUMN_NAME_IS_COMPLETED, mIsCompleted ? 1 : 0);
        values.put(NotePad.Notes.COLUMN_NAME_DUE_DATE, mDueDate);
        
        // Update the note
        mContentResolver.update(mUri, values, null, null);
    }

    /**
     * Cancels editing and returns to the previous activity.
     */
    private final void cancelNote() {
        // If we are editing an existing note, then we don't delete it.
        // If we are inserting a new note, then we delete it, because the user
        // cancelled the edit.
        if (mState == STATE_INSERT) {
            deleteNote();
        }
        finish();
    }

    /**
     * Deletes the current note.
     */
    private final void deleteNote() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            mContentResolver.delete(mUri, null, null);
            mText.setText("");
        }
        finish();
    }

    /**
     * Updates the UI based on the to-do list status.
     */
    private void updateTodoUI() {
        if (mIsTodo) {
            // 显示待办事项相关控件
            mTodoSection.setVisibility(View.VISIBLE);
            // 确保设置截止日期按钮可见
            if (mSetDueDateButton != null) {
                mSetDueDateButton.setVisibility(View.VISIBLE);
            }
        } else {
            // 隐藏待办事项相关控件
            mTodoSection.setVisibility(View.GONE);
            if (mSetDueDateButton != null) {
                mSetDueDateButton.setVisibility(View.GONE);
            }
            // 重置待办事项状态
            mIsCompleted = false;
            mDueDate = -1;
        }
    }

    /**
     * Updates the due date text display.
     */
    private void updateDueDateText() {
        if (mDueDate > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateString = sdf.format(mDueDateCalendar.getTime());
            
            // 根据完成状态设置文本样式
            if (mIsCompleted) {
                mDueDateText.setText("完成 - " + dateString);
                mDueDateText.setTextColor(getResources().getColor(R.color.completed_task_color));
            } else {
                // 检查是否过期
                if (mDueDateCalendar.before(Calendar.getInstance())) {
                    mDueDateText.setText("过期! " + dateString);
                    mDueDateText.setTextColor(getResources().getColor(R.color.overdue_task_color));
                } else {
                    mDueDateText.setText("截止: " + dateString);
                    mDueDateText.setTextColor(getResources().getColor(R.color.normal_task_color));
                }
            }
        } else {
            mDueDateText.setText("未设置截止日期");
            mDueDateText.setTextColor(getResources().getColor(R.color.normal_task_color));
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DATE_DIALOG_ID:
                return new DatePickerDialog(this,
                    mDateSetListener,
                    mDueDateCalendar.get(Calendar.YEAR),
                    mDueDateCalendar.get(Calendar.MONTH),
                    mDueDateCalendar.get(Calendar.DAY_OF_MONTH));
        }
        return null;
    }

    /**
     * Listener for date picker dialog
     */
    private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            mDueDateCalendar.set(Calendar.YEAR, year);
            mDueDateCalendar.set(Calendar.MONTH, monthOfYear);
            mDueDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            
            // 保存截止日期
            mDueDate = mDueDateCalendar.getTimeInMillis();
            saveNoteContent();
            updateDueDateText();
        }
    };

    /**
     * Saves the note content if it has changed.
     */
    private void saveNoteContent() {
        String text = mText.getText().toString();
        
        // 自动生成标题
        String title;
        int newlines = text.indexOf('\n');
        if (newlines > 0) {
            title = text.substring(0, newlines).trim();
        } else {
            // 没有换行，取前20个字符作为标题
            title = text.substring(0, Math.min(20, text.length())).trim();
        }
        
        // 如果标题为空，使用默认标题
        if (title.isEmpty()) {
            title = getResources().getString(R.string.untitled);
        }
        
        // 总是更新数据库，确保待办事项状态能够被保存
        // 不仅保存文本内容，还保存待办事项相关状态
        updateNote(text, title);
        mOriginalContent = text;
    }
}
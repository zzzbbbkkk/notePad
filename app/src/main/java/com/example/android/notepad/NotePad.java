package com.example.android.notepad;

import android.net.Uri;
import android.provider.BaseColumns;

public final class NotePad {
    public static final String AUTHORITY = "com.google.provider.NotePad";

    private static final String SCHEME = "content://";
    private static final String PATH_NOTES = "/notes";
    private static final String PATH_NOTE_ID = "/notes/";
    private static final String PATH_LIVE_FOLDER = "/live_folders/notes";
    private static final String PATH_CATEGORIES = "/categories";

    private NotePad() {
    }

    public static final class Notes implements BaseColumns {

        private Notes() {}

        public static final String TABLE_NAME = "notes";

        public static final int NOTE_ID_PATH_POSITION = 1;

        public static final Uri CONTENT_URI =  Uri.parse(SCHEME + AUTHORITY + PATH_NOTES);
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID);
        public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID + "/#");
        public static final Uri LIVE_FOLDER_URI = Uri.parse(SCHEME + AUTHORITY + PATH_LIVE_FOLDER);

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.note";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.note";
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_NOTE = "note";
        public static final String COLUMN_NAME_CREATE_DATE = "created";
        public static final String COLUMN_NAME_MODIFICATION_DATE = "modified";
        public static final String COLUMN_NAME_CATEGORY_ID = "category_id";
        
        // 待办事项相关字段
        public static final String COLUMN_NAME_IS_TODO = "is_todo"; // 是否为待办事项
        public static final String COLUMN_NAME_IS_COMPLETED = "is_completed"; // 是否已完成
        public static final String COLUMN_NAME_DUE_DATE = "due_date"; // 截止日期（可选）
    }

    public static final class Categories implements BaseColumns {

        private Categories() {}

        public static final String TABLE_NAME = "categories";

        public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_CATEGORIES);

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.google.category";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.google.category";

        public static final String COLUMN_NAME_CATEGORY_NAME = "name";
        public static final String COLUMN_NAME_CATEGORY_COLOR = "color";
        public static final String COLUMN_NAME_CREATE_DATE = "created";

        public static final String DEFAULT_CATEGORY_NAME = "默认分类";
        public static final int DEFAULT_CATEGORY_COLOR = 0xFF2196F3;
    }
}
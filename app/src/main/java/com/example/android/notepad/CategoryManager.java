package com.example.android.notepad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class CategoryManager extends Activity {

    private ListView categoriesList;
    private EditText etCategoryName;
    private Button btnAddCategory;
    private Button btnClose;

    private CategoryAdapter adapter;
    private List<Category> categories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_categories);

        initViews();
        loadCategories();
    }

    private void initViews() {
        categoriesList = (ListView) findViewById(R.id.categories_list);
        etCategoryName = (EditText) findViewById(R.id.et_category_name);
        btnAddCategory = (Button) findViewById(R.id.btn_add_category);
        btnClose = (Button) findViewById(R.id.btn_close);

        adapter = new CategoryAdapter();
        categoriesList.setAdapter(adapter);

        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCategory();
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        categoriesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showCategoryOptions(position);
            }
        });
    }

    private void loadCategories() {
        categories.clear();
        Cursor cursor = getContentResolver().query(
                NotePad.Categories.CONTENT_URI,
                new String[]{
                        NotePad.Categories._ID,
                        NotePad.Categories.COLUMN_NAME_CATEGORY_NAME,
                        NotePad.Categories.COLUMN_NAME_CATEGORY_COLOR
                },
                null, null, NotePad.Categories.COLUMN_NAME_CREATE_DATE + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Category category = new Category();
                category.id = cursor.getLong(0);
                category.name = cursor.getString(1);
                category.color = cursor.getInt(2);
                categories.add(category);
            }
            cursor.close();
        }

        adapter.notifyDataSetChanged();
    }

    private void addCategory() {
        String categoryName = etCategoryName.getText().toString().trim();
        if (categoryName.isEmpty()) {
            Toast.makeText(this, "请输入分类名称", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(NotePad.Categories.COLUMN_NAME_CATEGORY_NAME, categoryName);
        values.put(NotePad.Categories.COLUMN_NAME_CATEGORY_COLOR, getRandomColor());
        values.put(NotePad.Categories.COLUMN_NAME_CREATE_DATE, System.currentTimeMillis());

        try {
            getContentResolver().insert(NotePad.Categories.CONTENT_URI, values);
            etCategoryName.setText("");
            loadCategories();
            Toast.makeText(this, "分类添加成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "分类名称已存在", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCategoryOptions(final int position) {
        final Category category = categories.get(position);

        boolean isDefaultCategory = category.id == 1;

        String[] options = isDefaultCategory ?
                new String[]{"重命名"} : new String[]{"重命名", "删除"};

        new AlertDialog.Builder(this)
                .setTitle("分类操作")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (options[which].equals("重命名")) {
                            renameCategory(category);
                        } else if (options[which].equals("删除")) {
                            deleteCategory(category);
                        }
                    }
                })
                .show();
    }

    private void renameCategory(final Category category) {
        final EditText input = new EditText(this);
        input.setText(category.name);

        new AlertDialog.Builder(this)
                .setTitle("重命名分类")
                .setView(input)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty() && !newName.equals(category.name)) {
                            updateCategoryName(category.id, newName);
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateCategoryName(long categoryId, String newName) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Categories.COLUMN_NAME_CATEGORY_NAME, newName);

        getContentResolver().update(
                NotePad.Categories.CONTENT_URI,
                values,
                NotePad.Categories._ID + "=?",
                new String[]{String.valueOf(categoryId)}
        );

        loadCategories();
    }

    private void deleteCategory(Category category) {
        Cursor cursor = getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                new String[]{NotePad.Notes._ID},
                NotePad.Notes.COLUMN_NAME_CATEGORY_ID + "=?",
                new String[]{String.valueOf(category.id)},
                null
        );

        if (cursor != null && cursor.getCount() > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("删除分类")
                    .setMessage("该分类下有 " + cursor.getCount() + " 篇笔记，删除后将转移到默认分类")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ContentValues values = new ContentValues();
                            values.put(NotePad.Notes.COLUMN_NAME_CATEGORY_ID, 1);

                            getContentResolver().update(
                                    NotePad.Notes.CONTENT_URI,
                                    values,
                                    NotePad.Notes.COLUMN_NAME_CATEGORY_ID + "=?",
                                    new String[]{String.valueOf(category.id)}
                            );

                            deleteCategoryFromDb(category.id);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            cursor.close();
        } else {
            deleteCategoryFromDb(category.id);
        }
    }

    private void deleteCategoryFromDb(long categoryId) {
        getContentResolver().delete(
                NotePad.Categories.CONTENT_URI,
                NotePad.Categories._ID + "=?",
                new String[]{String.valueOf(categoryId)}
        );

        loadCategories();
        Toast.makeText(this, "分类删除成功", Toast.LENGTH_SHORT).show();
    }

    private int getRandomColor() {
        int[] colors = {
                0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7,
                0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4,
                0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39,
                0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722
        };
        return colors[(int) (Math.random() * colors.length)];
    }

    private static class Category {
        long id;
        String name;
        int color;
    }

    private class CategoryAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return categories.size();
        }

        @Override
        public Object getItem(int position) {
            return categories.get(position);
        }

        @Override
        public long getItemId(int position) {
            return categories.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CategoryManager.this)
                        .inflate(android.R.layout.simple_list_item_1, parent, false);
            }

            Category category = categories.get(position);
            TextView textView = (TextView) convertView;
            textView.setText(category.name);

            textView.setCompoundDrawablesWithIntrinsicBounds(
                    createColorCircle(category.color), null, null, null);
            textView.setCompoundDrawablePadding(16);

            return convertView;
        }

        private ShapeDrawable createColorCircle(int color) {
            ShapeDrawable shape = new ShapeDrawable(new OvalShape());
            shape.getPaint().setColor(color);
            shape.setIntrinsicWidth(24);
            shape.setIntrinsicHeight(24);
            return shape;
        }
    }
}
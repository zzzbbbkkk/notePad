package com.example.android.notepad;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

public class CategorySelectDialog extends Dialog {

    private Context context;
    private OnCategorySelectedListener listener;
    private Spinner categorySpinner;
    private Button btnCancel, btnConfirm;

    private List<CategoryItem> categories = new ArrayList<>();
    private long selectedCategoryId = 1; // 默认选择第一个分类

    public interface OnCategorySelectedListener {
        void onCategorySelected(long categoryId, String categoryName);
    }

    public CategorySelectDialog(Context context, OnCategorySelectedListener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category_dialog);

        setTitle("选择分类");
        initViews();
        loadCategories();
    }

    private void initViews() {
        categorySpinner = (Spinner) findViewById(R.id.category_spinner);
        btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnConfirm = (Button) findViewById(R.id.btn_confirm);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null && !categories.isEmpty()) {
                    CategoryItem selected = categories.get(categorySpinner.getSelectedItemPosition());
                    listener.onCategorySelected(selected.id, selected.name);
                }
                dismiss();
            }
        });

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!categories.isEmpty()) {
                    selectedCategoryId = categories.get(position).id;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadCategories() {
        categories.clear();
        Cursor cursor = context.getContentResolver().query(
                NotePad.Categories.CONTENT_URI,
                new String[]{
                        NotePad.Categories._ID,
                        NotePad.Categories.COLUMN_NAME_CATEGORY_NAME
                },
                null, null, NotePad.Categories.COLUMN_NAME_CREATE_DATE + " ASC"
        );

        if (cursor != null) {
            List<String> categoryNames = new ArrayList<>();
            while (cursor.moveToNext()) {
                CategoryItem category = new CategoryItem();
                category.id = cursor.getLong(0);
                category.name = cursor.getString(1);
                categories.add(category);
                categoryNames.add(category.name);
            }
            cursor.close();

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    context, android.R.layout.simple_spinner_item, categoryNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            categorySpinner.setAdapter(adapter);
        }
    }

    public void setSelectedCategory(long categoryId) {
        this.selectedCategoryId = categoryId;
        // 设置Spinner选中项
        if (categorySpinner != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).id == categoryId) {
                    categorySpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private static class CategoryItem {
        long id;
        String name;
    }
}
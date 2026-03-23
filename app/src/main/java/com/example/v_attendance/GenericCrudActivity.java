package com.example.v_attendance;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;

public class GenericCrudActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "crud_type";
    public static final String TYPE_YEAR = "Year";
    public static final String TYPE_COURSE = "Course";
    public static final String TYPE_SUBJECT = "Subject";

    private String type;
    private DatabaseHelper dbHelper;
    private ListView lvItems;
    private ArrayList<String> itemList;
    private ArrayAdapter<String> adapter;
    private ExtendedFloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_crud);

        type = getIntent().getStringExtra(EXTRA_TYPE);
        setTitle("Manage " + type + "s");

        dbHelper = new DatabaseHelper(this);
        lvItems = findViewById(R.id.lvItems);
        fabAdd = findViewById(R.id.fabAddItem);

        itemList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemList);
        lvItems.setAdapter(adapter);

        loadItems();

        fabAdd.setOnClickListener(v -> showAddItemDialog());

        lvItems.setOnItemClickListener((parent, view, position, id) -> {
            String selected = itemList.get(position);
            String itemId = selected.split(" - ")[0];
            showStudentsInItem(itemId);
        });

        lvItems.setOnItemLongClickListener((parent, view, position, id) -> {
            String selected = itemList.get(position);
            String itemId = selected.split(" - ")[0];
            new AlertDialog.Builder(this)
                    .setTitle("Delete " + type)
                    .setMessage("Are you sure you want to delete " + selected + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (type.equals(TYPE_YEAR)) {
                            dbHelper.deleteYear(Integer.parseInt(itemId));
                        } else if (type.equals(TYPE_COURSE)) {
                            dbHelper.deleteCourse(itemId);
                        } else if (type.equals(TYPE_SUBJECT)) {
                            dbHelper.deleteSubject(itemId);
                        }
                        loadItems();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });
    }

    private void showAddItemDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_generic_add, null);
        EditText etId = dialogView.findViewById(R.id.etId);
        EditText etName = dialogView.findViewById(R.id.etName);

        if (type.equals(TYPE_YEAR)) {
            etId.setHint("Year ID (Integer)");
            etName.setHint("Year Level (e.g. 1st Year)");
        } else {
            etId.setHint(type + " ID (Code)");
            etName.setHint(type + " Name");
        }

        new AlertDialog.Builder(this)
                .setTitle("Add " + type)
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String id = etId.getText().toString();
                    String name = etName.getText().toString();
                    if (id.isEmpty() || name.isEmpty()) {
                        Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        if (type.equals(TYPE_YEAR)) {
                            dbHelper.addYear(Integer.parseInt(id), name);
                        } else if (type.equals(TYPE_COURSE)) {
                            dbHelper.addCourse(id, name);
                        } else if (type.equals(TYPE_SUBJECT)) {
                            dbHelper.addSubject(id, name);
                        }
                        loadItems();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error adding item", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStudentsInItem(String itemId) {
        Cursor cursor = null;
        if (type.equals(TYPE_YEAR)) {
            cursor = dbHelper.getStudentsByYear(Integer.parseInt(itemId));
        } else if (type.equals(TYPE_COURSE)) {
            cursor = dbHelper.getStudentsByCourse(itemId);
        } else if (type.equals(TYPE_SUBJECT)) {
            cursor = dbHelper.getStudentsBySubject(itemId);
        }

        ArrayList<String> studentNames = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                studentNames.add(cursor.getString(2) + " " + cursor.getString(1) + " (" + cursor.getString(0) + ")");
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (studentNames.isEmpty()) {
            Toast.makeText(this, "No students found in this " + type, Toast.LENGTH_SHORT).show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Students in " + type + ": " + itemId)
                    .setItems(studentNames.toArray(new String[0]), null)
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void loadItems() {
        itemList.clear();
        Cursor cursor = null;
        if (type.equals(TYPE_YEAR)) cursor = dbHelper.getAllYears();
        else if (type.equals(TYPE_COURSE)) cursor = dbHelper.getAllCourses();
        else if (type.equals(TYPE_SUBJECT)) cursor = dbHelper.getAllSubjects();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                itemList.add(cursor.getString(0) + " - " + cursor.getString(1));
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }
}

package com.example.v_attendance;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

public class GenericCrudActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static final String EXTRA_TYPE = "crud_type";
    public static final String TYPE_YEAR = "Year";
    public static final String TYPE_COURSE = "Course";
    public static final String TYPE_SUBJECT = "Subject";

    private String type;
    private DatabaseHelper dbHelper;
    private ListView lvItems;
    private ArrayList<Object> itemList;
    private ModernListAdapter adapter;
    private ExtendedFloatingActionButton fabAdd;
    private DrawerLayout drawerLayout;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_crud);

        type = getIntent().getStringExtra(EXTRA_TYPE);
        
        dbHelper = new DatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbarGeneric);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(type + "s");
        }

        drawerLayout = findViewById(R.id.drawer_layout_generic);
        NavigationView navigationView = findViewById(R.id.nav_view_generic);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        lvItems = findViewById(R.id.lvItems);
        fabAdd = findViewById(R.id.fabAddItem);
        etSearch = findViewById(R.id.etSearchGeneric);

        itemList = new ArrayList<>();
        adapter = new ModernListAdapter(this, itemList);
        lvItems.setAdapter(adapter);

        loadItems();

        fabAdd.setOnClickListener(v -> showAddItemDialog());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        lvItems.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) adapter.getItem(position);
            String itemId = selected.split(" - ")[0];
            String itemName = selected.split(" - ")[1];
            showItemDetails(itemId, itemName);
        });

        lvItems.setOnItemLongClickListener((parent, view, position, id) -> {
            String selected = (String) adapter.getItem(position);
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;

        if (id == R.id.nav_dashboard) {
            intent = new Intent(this, MainActivity.class);
        } else if (id == R.id.nav_students) {
            intent = new Intent(this, StudentActivity.class);
        } else if (id == R.id.nav_years) {
            if (!TYPE_YEAR.equals(type)) {
                intent = new Intent(this, GenericCrudActivity.class);
                intent.putExtra(GenericCrudActivity.EXTRA_TYPE, GenericCrudActivity.TYPE_YEAR);
            }
        } else if (id == R.id.nav_courses) {
            if (!TYPE_COURSE.equals(type)) {
                intent = new Intent(this, GenericCrudActivity.class);
                intent.putExtra(GenericCrudActivity.EXTRA_TYPE, GenericCrudActivity.TYPE_COURSE);
            }
        } else if (id == R.id.nav_subjects) {
            if (!TYPE_SUBJECT.equals(type)) {
                intent = new Intent(this, GenericCrudActivity.class);
                intent.putExtra(GenericCrudActivity.EXTRA_TYPE, GenericCrudActivity.TYPE_SUBJECT);
            }
        } else if (id == R.id.nav_mark_attendance) {
            intent = new Intent(this, EventListActivity.class);
            intent.putExtra(EventListActivity.EXTRA_TARGET, "List");
        } else if (id == R.id.nav_scan_attendance) {
            intent = new Intent(this, EventListActivity.class);
            intent.putExtra(EventListActivity.EXTRA_TARGET, "Scan");
        }

        if (intent != null) {
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawers();
        return true;
    }

    private void showAddItemDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_generic_add, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvGenericTitle);
        TextInputLayout tilId = dialogView.findViewById(R.id.tilId);
        TextInputLayout tilName = dialogView.findViewById(R.id.tilName);
        EditText etId = dialogView.findViewById(R.id.etId);
        EditText etName = dialogView.findViewById(R.id.etName);

        tvTitle.setText(type + " Information");
        if (type.equals(TYPE_YEAR)) {
            tilId.setHint("Year ID (Integer)");
            tilName.setHint("Year Level (e.g. 1st Year)");
        } else {
            tilId.setHint(type + " ID (Code)");
            tilName.setHint(type + " Name");
        }

        new AlertDialog.Builder(this)
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

    private void showItemDetails(String itemId, String itemName) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_generic_details, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvGenericDetailTitle);
        TextView tvIdLabel = dialogView.findViewById(R.id.tvGenericDetailIdLabel);
        TextView tvId = dialogView.findViewById(R.id.tvGenericDetailId);
        TextView tvNameLabel = dialogView.findViewById(R.id.tvGenericDetailNameLabel);
        TextView tvName = dialogView.findViewById(R.id.tvGenericDetailName);
        TextView tvListLabel = dialogView.findViewById(R.id.tvGenericDetailListLabel);
        TextView tvList = dialogView.findViewById(R.id.tvGenericDetailList);

        tvTitle.setText(type + " Details");
        tvIdLabel.setText(type + " ID");
        tvId.setText(itemId);
        tvNameLabel.setText(type + " Name");
        tvName.setText(itemName);
        tvListLabel.setText("Students in this " + type);

        Cursor cursor = null;
        if (type.equals(TYPE_YEAR)) {
            cursor = dbHelper.getStudentsByYear(Integer.parseInt(itemId));
        } else if (type.equals(TYPE_COURSE)) {
            cursor = dbHelper.getStudentsByCourse(itemId);
        } else if (type.equals(TYPE_SUBJECT)) {
            cursor = dbHelper.getStudentsBySubject(itemId);
        }

        StringBuilder sb = new StringBuilder();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                if (sb.length() > 0) sb.append("\n");
                sb.append("• ").append(cursor.getString(2)).append(" ").append(cursor.getString(1))
                        .append(" (").append(cursor.getString(0)).append(")");
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (sb.length() == 0) {
            tvList.setText("No students found.");
        } else {
            tvList.setText(sb.toString());
        }

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }

    private void loadItems() {
        loadItems("");
    }

    private void loadItems(String query) {
        itemList.clear();
        Cursor cursor = null;
        if (type.equals(TYPE_YEAR)) cursor = dbHelper.getAllYears();
        else if (type.equals(TYPE_COURSE)) cursor = dbHelper.getAllCourses();
        else if (type.equals(TYPE_SUBJECT)) cursor = dbHelper.getAllSubjects();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String id = cursor.getString(0);
                String name = cursor.getString(1);
                String full = id + " - " + name;
                if (query.isEmpty() || full.toLowerCase().contains(query.toLowerCase())) {
                    itemList.add(full);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }
}

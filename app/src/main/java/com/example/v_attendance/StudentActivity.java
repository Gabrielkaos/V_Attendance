package com.example.v_attendance;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

public class StudentActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DatabaseHelper dbHelper;
    private ListView lvStudents;
    private ArrayList<Student> studentList;
    private ArrayAdapter<Student> adapter;
    private ExtendedFloatingActionButton fabAdd;
    private EditText etSearch;
    private DrawerLayout drawerLayout;

    private ArrayList<String> yearLevels, courseIds;
    private String selectedSubjects = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        dbHelper = new DatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbarStudent);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout_student);
        NavigationView navigationView = findViewById(R.id.nav_view_student);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        lvStudents = findViewById(R.id.lvStudents);
        fabAdd = findViewById(R.id.fabAddStudent);
        etSearch = findViewById(R.id.etSearchStudent);

        studentList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, studentList);
        lvStudents.setAdapter(adapter);
        
        loadStudents();

        fabAdd.setOnClickListener(v -> showAddStudentDialog());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        lvStudents.setOnItemClickListener((parent, view, position, id) -> {
            Student s = adapter.getItem(position);
            showStudentDetails(s);
        });

        lvStudents.setOnItemLongClickListener((parent, view, position, id) -> {
            final Student student = adapter.getItem(position);
            new AlertDialog.Builder(this)
                    .setTitle("Delete Student")
                    .setMessage("Delete " + student.getName() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dbHelper.deleteStudent(student.getId());
                        loadStudents();
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
            // Already here
        } else if (id == R.id.nav_years) {
            intent = new Intent(this, GenericCrudActivity.class);
            intent.putExtra(GenericCrudActivity.EXTRA_TYPE, GenericCrudActivity.TYPE_YEAR);
        } else if (id == R.id.nav_courses) {
            intent = new Intent(this, GenericCrudActivity.class);
            intent.putExtra(GenericCrudActivity.EXTRA_TYPE, GenericCrudActivity.TYPE_COURSE);
        } else if (id == R.id.nav_subjects) {
            intent = new Intent(this, GenericCrudActivity.class);
            intent.putExtra(GenericCrudActivity.EXTRA_TYPE, GenericCrudActivity.TYPE_SUBJECT);
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

    private void showAddStudentDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_student_add, null);
        EditText etStudentId = dialogView.findViewById(R.id.etStudentId);
        EditText etFirstName = dialogView.findViewById(R.id.etFirstName);
        EditText etLastName = dialogView.findViewById(R.id.etLastName);
        EditText etMiddleName = dialogView.findViewById(R.id.etMiddleName);
        AutoCompleteTextView spnYear = dialogView.findViewById(R.id.spnYear);
        AutoCompleteTextView spnCourse = dialogView.findViewById(R.id.spnCourse);
        EditText etSubjects = dialogView.findViewById(R.id.etSubjects);

        selectedSubjects = "";
        
        // Load Year Level
        yearLevels = new ArrayList<>();
        Cursor cYear = dbHelper.getAllYears();
        while (cYear.moveToNext()) yearLevels.add(cYear.getString(1));
        cYear.close();
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, yearLevels);
        spnYear.setAdapter(yearAdapter);

        // Load Course
        courseIds = new ArrayList<>();
        ArrayList<String> courseNames = new ArrayList<>();
        Cursor cCourse = dbHelper.getAllCourses();
        while (cCourse.moveToNext()) {
            courseIds.add(cCourse.getString(0));
            courseNames.add(cCourse.getString(1));
        }
        cCourse.close();
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, courseNames);
        spnCourse.setAdapter(courseAdapter);

        etSubjects.setOnClickListener(v -> showMultiSelectSubjectsDialog(etSubjects));

        new AlertDialog.Builder(this)
                .setTitle("Add New Student")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String sid = etStudentId.getText().toString();
                    String first = etFirstName.getText().toString();
                    String last = etLastName.getText().toString();
                    String middle = etMiddleName.getText().toString();
                    
                    String yearStr = spnYear.getText().toString();
                    String courseStr = spnCourse.getText().toString();

                    if (sid.isEmpty() || first.isEmpty() || last.isEmpty() || yearStr.isEmpty() || courseStr.isEmpty()) {
                        Toast.makeText(this, "All fields except middle name are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int yearId = yearLevels.indexOf(yearStr) + 1;
                    int courseIndex = courseNames.indexOf(courseStr);
                    String courseId = courseIndex != -1 ? courseIds.get(courseIndex) : "";

                    if (yearId == 0 || courseId.isEmpty()) {
                        Toast.makeText(this, "Invalid selection", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    dbHelper.addStudent(sid, last, first, middle, yearId, courseId, selectedSubjects);
                    loadStudents();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStudentDetails(Student s) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_student_details, null);
        TextView tvId = dialogView.findViewById(R.id.tvDetailId);
        TextView tvName = dialogView.findViewById(R.id.tvDetailName);
        TextView tvYear = dialogView.findViewById(R.id.tvDetailYear);
        TextView tvCourse = dialogView.findViewById(R.id.tvDetailCourse);
        TextView tvSubjects = dialogView.findViewById(R.id.tvDetailSubjects);

        tvId.setText(s.studentId);
        tvName.setText(s.getFullName());
        tvYear.setText(dbHelper.getYearName(s.yearId));
        tvCourse.setText(dbHelper.getCourseName(s.courseId));
        
        StringBuilder sb = new StringBuilder();
        if (s.subjectIds != null && !s.subjectIds.isEmpty()) {
            String[] ids = s.subjectIds.split(",");
            for (String id : ids) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("• ").append(dbHelper.getSubjectName(id)).append(" (").append(id).append(")");
            }
        } else {
            sb.append("No subjects enrolled.");
        }
        tvSubjects.setText(sb.toString());

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showMultiSelectSubjectsDialog(EditText etSubjects) {
        ArrayList<String> subjectItems = new ArrayList<>();
        ArrayList<String> subjectIdList = new ArrayList<>();
        Cursor cursor = dbHelper.getAllSubjects();
        while (cursor.moveToNext()) {
            subjectIdList.add(cursor.getString(0));
            subjectItems.add(cursor.getString(1) + " (" + cursor.getString(0) + ")");
        }
        cursor.close();
        String[] itemsArray = subjectItems.toArray(new String[0]);
        boolean[] checkedItems = new boolean[itemsArray.length];
        new AlertDialog.Builder(this)
                .setTitle("Select Subjects")
                .setMultiChoiceItems(itemsArray, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                .setPositiveButton("OK", (dialog, which) -> {
                    ArrayList<String> selectedIds = new ArrayList<>();
                    ArrayList<String> selectedNames = new ArrayList<>();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selectedIds.add(subjectIdList.get(i));
                            selectedNames.add(itemsArray[i]);
                        }
                    }
                    selectedSubjects = TextUtils.join(",", selectedIds);
                    etSubjects.setText(TextUtils.join(", ", selectedNames));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadStudents() {
        studentList.clear();
        Cursor cursor = dbHelper.getAllStudents();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                studentList.add(new Student(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getInt(4),
                        cursor.getString(5),
                        cursor.getString(6)
                ));
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.notifyDataSetChanged();
        if (!etSearch.getText().toString().isEmpty()) {
            adapter.getFilter().filter(etSearch.getText().toString());
        }
    }
}

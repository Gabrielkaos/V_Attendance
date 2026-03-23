package com.example.v_attendance;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class EventListActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET = "target_activity";
    private DatabaseHelper dbHelper;
    private ListView lvEvents;
    private ExtendedFloatingActionButton fabAdd;
    private EditText etSearch;
    private ArrayList<String> eventNames, eventIds;
    private String targetActivity;
    private ArrayAdapter<String> adapter;

    private String selectedYears = "";
    private String selectedCourses = "";
    private String selectedSubjects = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);
        
        targetActivity = getIntent().getStringExtra(EXTRA_TARGET);
        setTitle("Select Event for " + targetActivity);

        dbHelper = new DatabaseHelper(this);
        lvEvents = findViewById(R.id.lvEvents);
        fabAdd = findViewById(R.id.fabAddEvent);
        etSearch = findViewById(R.id.etSearchEvent);

        eventNames = new ArrayList<>();
        eventIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, eventNames);
        lvEvents.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddEventDialog());

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

        lvEvents.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = adapter.getItem(position);
            // Extract ID from "Name (ID)" format
            String eventId = selectedItem.substring(selectedItem.lastIndexOf("(") + 1, selectedItem.lastIndexOf(")"));
            showEventDetailsOrSelect(eventId);
        });

        lvEvents.setOnItemLongClickListener((parent, view, position, id) -> {
            String selectedItem = adapter.getItem(position);
            String eventId = selectedItem.substring(selectedItem.lastIndexOf("(") + 1, selectedItem.lastIndexOf(")"));
            
            new AlertDialog.Builder(this)
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete: " + selectedItem + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        dbHelper.deleteEvent(eventId);
                        Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                        loadEvents();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    private void showAddEventDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.activity_event_add, null);
        EditText etId = dialogView.findViewById(R.id.etEventId);
        EditText etName = dialogView.findViewById(R.id.etEventName);
        EditText etStart = dialogView.findViewById(R.id.etStartDate);
        EditText etEnd = dialogView.findViewById(R.id.etEndDate);
        EditText etYears = dialogView.findViewById(R.id.etAllowedYears);
        EditText etCourses = dialogView.findViewById(R.id.etAllowedCourses);
        EditText etSubjects = dialogView.findViewById(R.id.etAllowedSubjects);
        dialogView.findViewById(R.id.btnSaveEvent).setVisibility(View.GONE);

        selectedYears = "";
        selectedCourses = "";
        selectedSubjects = "";

        etStart.setOnClickListener(v -> showDateTimePicker(etStart));
        etEnd.setOnClickListener(v -> showDateTimePicker(etEnd));
        etYears.setOnClickListener(v -> showMultiSelectDialog("Years", etYears));
        etCourses.setOnClickListener(v -> showMultiSelectDialog("Courses", etCourses));
        etSubjects.setOnClickListener(v -> showMultiSelectDialog("Subjects", etSubjects));

        new AlertDialog.Builder(this)
                .setTitle("Create New Event")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String id = etId.getText().toString();
                    String name = etName.getText().toString();
                    String start = etStart.getText().toString();
                    String end = etEnd.getText().toString();

                    if (id.isEmpty() || name.isEmpty() || start.isEmpty() || end.isEmpty()) {
                        Toast.makeText(this, "ID, Name, and Dates are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long result = dbHelper.addEvent(id, name, start, end, selectedYears, selectedCourses, selectedSubjects);
                    if (result != -1) {
                        Toast.makeText(this, "Event Created", Toast.LENGTH_SHORT).show();
                        loadEvents();
                    } else {
                        Toast.makeText(this, "Error creating event (ID may already exist)", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDateTimePicker(EditText target) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    int hour = c.get(Calendar.HOUR_OF_DAY);
                    int minute = c.get(Calendar.MINUTE);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                            (view1, hourOfDay, minute1) -> 
                                    target.setText(String.format(Locale.getDefault(), "%d-%02d-%02d %02d:%02d", year1, (monthOfYear + 1), dayOfMonth, hourOfDay, minute1)),
                            hour, minute, true);
                    timePickerDialog.show();
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void showMultiSelectDialog(String type, EditText target) {
        ArrayList<String> items = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        
        Cursor cursor;
        if (type.equals("Years")) cursor = dbHelper.getAllYears();
        else if (type.equals("Courses")) cursor = dbHelper.getAllCourses();
        else cursor = dbHelper.getAllSubjects();

        while (cursor.moveToNext()) {
            ids.add(cursor.getString(0));
            items.add(cursor.getString(1) + " (" + cursor.getString(0) + ")");
        }
        cursor.close();

        String[] itemsArray = items.toArray(new String[0]);
        boolean[] checkedItems = new boolean[itemsArray.length];
        
        new AlertDialog.Builder(this)
                .setTitle("Select Allowed " + type)
                .setMultiChoiceItems(itemsArray, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                .setPositiveButton("OK", (dialog, which) -> {
                    ArrayList<String> selectedIds = new ArrayList<>();
                    ArrayList<String> selectedNames = new ArrayList<>();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selectedIds.add(ids.get(i));
                            selectedNames.add(itemsArray[i]);
                        }
                    }
                    String resultIds = TextUtils.join(",", selectedIds);
                    String resultNames = TextUtils.join(", ", selectedNames);
                    
                    target.setText(resultNames);
                    if (type.equals("Years")) selectedYears = resultIds;
                    else if (type.equals("Courses")) selectedCourses = resultIds;
                    else selectedSubjects = resultIds;
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEventDetailsOrSelect(String eventId) {
        Cursor cursor = dbHelper.getEventById(eventId);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("event_name"));
            String start = cursor.getString(cursor.getColumnIndexOrThrow("start_date"));
            String end = cursor.getString(cursor.getColumnIndexOrThrow("end_date"));
            String years = cursor.getString(cursor.getColumnIndexOrThrow("allowed_year_ids"));
            String courses = cursor.getString(cursor.getColumnIndexOrThrow("allowed_course_ids"));
            String subjects = cursor.getString(cursor.getColumnIndexOrThrow("allowed_subject_ids"));

            String info = "Name: " + name + "\n" +
                          "Start: " + start + "\n" +
                          "End: " + end + "\n" +
                          "Allowed Years: " + years + "\n" +
                          "Allowed Courses: " + courses + "\n" +
                          "Allowed Subjects: " + subjects;

            new AlertDialog.Builder(this)
                    .setTitle("Event Information")
                    .setMessage(info)
                    .setPositiveButton("Select for " + targetActivity, (dialog, which) -> {
                        Intent intent;
                        if ("Scan".equals(targetActivity)) {
                            intent = new Intent(this, MarkAttendanceActivity.class);
                        } else {
                            intent = new Intent(this, AttendanceActivity.class);
                        }
                        intent.putExtra("event_id", eventId);
                        startActivity(intent);
                    })
                    .setNegativeButton("Close", null)
                    .show();
            cursor.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    private void loadEvents() {
        eventNames.clear();
        eventIds.clear();
        Cursor cursor = dbHelper.getAllEvents();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                eventIds.add(cursor.getString(0));
                eventNames.add(cursor.getString(1) + " (" + cursor.getString(0) + ")");
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.notifyDataSetChanged();
        if (!etSearch.getText().toString().isEmpty()) {
            adapter.getFilter().filter(etSearch.getText().toString());
        }
    }
}

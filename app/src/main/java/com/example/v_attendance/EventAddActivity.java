package com.example.v_attendance;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class EventAddActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etId, etName, etStart, etEnd, etYears, etCourses, etSubjects;
    private Button btnSave;

    private String selectedYears = "";
    private String selectedCourses = "";
    private String selectedSubjects = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_add);
        setTitle("Create New Event");

        dbHelper = new DatabaseHelper(this);
        etId = findViewById(R.id.etEventId);
        etName = findViewById(R.id.etEventName);
        etStart = findViewById(R.id.etStartDate);
        etEnd = findViewById(R.id.etEndDate);
        etYears = findViewById(R.id.etAllowedYears);
        etCourses = findViewById(R.id.etAllowedCourses);
        etSubjects = findViewById(R.id.etAllowedSubjects);
        btnSave = findViewById(R.id.btnSaveEvent);

        etStart.setOnClickListener(v -> showDateTimePicker(etStart));
        etEnd.setOnClickListener(v -> showDateTimePicker(etEnd));

        etYears.setOnClickListener(v -> showMultiSelectDialog("Years", etYears));
        etCourses.setOnClickListener(v -> showMultiSelectDialog("Courses", etCourses));
        etSubjects.setOnClickListener(v -> showMultiSelectDialog("Subjects", etSubjects));

        btnSave.setOnClickListener(v -> {
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
                finish();
            } else {
                Toast.makeText(this, "Error creating event (ID may already exist)", Toast.LENGTH_SHORT).show();
            }
        });
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
}

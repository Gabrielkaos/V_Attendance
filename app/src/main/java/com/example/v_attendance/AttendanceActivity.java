package com.example.v_attendance;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;

public class AttendanceActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView lvAttendance;
    private TextView tvEventInfo;
    private Button btnSave;
    private ArrayList<AttendanceRecord> records;
    private AttendanceAdapter adapter;
    private String currentEventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        currentEventId = getIntent().getStringExtra("event_id");
        if (currentEventId == null) {
            Toast.makeText(this, "No Event Selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);
        
        Toolbar toolbar = findViewById(R.id.toolbarAttendance);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        lvAttendance = findViewById(R.id.lvAttendance);
        tvEventInfo = findViewById(R.id.tvDate); // Reusing the same ID for event info
        btnSave = findViewById(R.id.btnSaveAttendance);

        loadEventDetails();
        loadAttendance();

        btnSave.setOnClickListener(v -> {
            for (AttendanceRecord record : records) {
                dbHelper.markAttendance(record.studentId, currentEventId, record.isPresent ? 1 : 0);
            }
            Toast.makeText(AttendanceActivity.this, "Attendance Saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void loadEventDetails() {
        Cursor cursor = dbHelper.getEventById(currentEventId);
        if (cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("event_name"));
            tvEventInfo.setText("Event: " + name);
        }
        cursor.close();
    }

    private void loadAttendance() {
        records = new ArrayList<>();
        Cursor cursor = dbHelper.getAttendanceByEvent(currentEventId);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String id = cursor.getString(0);
                String name = cursor.getString(1);
                int status = cursor.getInt(3); 
                records.add(new AttendanceRecord(id, name, status == 1));
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter = new AttendanceAdapter();
        lvAttendance.setAdapter(adapter);
    }

    private static class AttendanceRecord {
        String studentId;
        String studentName;
        boolean isPresent;
        boolean initiallyPresent;

        AttendanceRecord(String id, String name, boolean present) {
            this.studentId = id;
            this.studentName = name;
            this.isPresent = present;
            this.initiallyPresent = present;
        }
    }

    private class AttendanceAdapter extends BaseAdapter {
        @Override
        public int getCount() { return records.size(); }
        @Override
        public Object getItem(int position) { return records.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(AttendanceActivity.this).inflate(R.layout.item_attendance, parent, false);
            }
            AttendanceRecord record = records.get(position);
            TextView tvName = convertView.findViewById(R.id.tvStudentName);
            CheckBox cbPresent = convertView.findViewById(R.id.cbPresent);

            tvName.setText(record.studentName);
            cbPresent.setOnCheckedChangeListener(null); 
            cbPresent.setChecked(record.isPresent);
            
            cbPresent.setEnabled(record.initiallyPresent);

            cbPresent.setOnCheckedChangeListener((buttonView, isChecked) -> {
                record.isPresent = isChecked;
            });

            return convertView;
        }
    }
}

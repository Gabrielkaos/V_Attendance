package com.example.v_attendance;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DatabaseHelper dbHelper;
    private DrawerLayout drawerLayout;
    private TextView tvStudentCount, tvEventCount, tvActiveEventCount;
    private LinearLayout containerUpcoming;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        tvStudentCount = findViewById(R.id.tvStudentCount);
        tvEventCount = findViewById(R.id.tvEventCount);
        tvActiveEventCount = findViewById(R.id.tvActiveEventCount);
        containerUpcoming = findViewById(R.id.containerUpcoming);

        refreshStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStats();
    }

    private void refreshStats() {
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        
        tvStudentCount.setText(String.valueOf(dbHelper.getStudentCount()));
        tvEventCount.setText(String.valueOf(dbHelper.getEventCount()));
        tvActiveEventCount.setText(String.valueOf(dbHelper.getActiveEventCount(now)));

        containerUpcoming.removeAllViews();
        Cursor cursor = dbHelper.getUpcomingEvents(now);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                TextView tv = new TextView(this);
                String name = cursor.getString(cursor.getColumnIndexOrThrow("event_name"));
                String end = cursor.getString(cursor.getColumnIndexOrThrow("end_date"));
                tv.setText("• " + name + "\n  Ends: " + end);
                tv.setPadding(0, 8, 0, 8);
                tv.setTextSize(16);
                containerUpcoming.addView(tv);
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            TextView tv = new TextView(this);
            tv.setText("No upcoming deadlines.");
            containerUpcoming.addView(tv);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;

        if (id == R.id.nav_dashboard) {
            // Already here
        } else if (id == R.id.nav_students) {
            intent = new Intent(this, StudentActivity.class);
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
        }

        drawerLayout.closeDrawers();
        return true;
    }
}

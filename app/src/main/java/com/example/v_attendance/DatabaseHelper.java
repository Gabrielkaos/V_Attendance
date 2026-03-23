package com.example.v_attendance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "V_Attendance_Pro.db";
    private static final int DATABASE_VERSION = 5;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE years (year_id INTEGER PRIMARY KEY, year_level TEXT)");
        db.execSQL("CREATE TABLE courses (course_id TEXT PRIMARY KEY, course_name TEXT)");
        db.execSQL("CREATE TABLE subjects (subject_id TEXT PRIMARY KEY, subject_name TEXT)");

        db.execSQL("CREATE TABLE students (" +
                "student_id TEXT PRIMARY KEY, " +
                "last_name TEXT, first_name TEXT, middle_name TEXT, " +
                "year_id INTEGER, course_id TEXT, subject_ids TEXT, " +
                "FOREIGN KEY(year_id) REFERENCES years(year_id), " +
                "FOREIGN KEY(course_id) REFERENCES courses(course_id))");

        db.execSQL("CREATE TABLE events (" +
                "event_id TEXT PRIMARY KEY, " +
                "event_name TEXT, " +
                "start_date TEXT, " +
                "end_date TEXT, " +
                "allowed_year_ids TEXT, " +
                "allowed_course_ids TEXT, " +
                "allowed_subject_ids TEXT)");

        db.execSQL("CREATE TABLE attendance (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id TEXT, event_id TEXT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "status INTEGER DEFAULT 0, " +
                "FOREIGN KEY(student_id) REFERENCES students(student_id), " +
                "FOREIGN KEY(event_id) REFERENCES events(event_id))");
        
        db.execSQL("INSERT INTO years VALUES (1, '1st Year'), (2, '2nd Year'), (3, '3rd Year'), (4, '4th Year')");
        db.execSQL("INSERT INTO courses VALUES ('C-BSIT', 'BS in Information Technology')");
        db.execSQL("INSERT INTO subjects VALUES ('S-MOBILE', 'Mobile Dev'), ('S-WEB', 'Web Dev')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS attendance");
        db.execSQL("DROP TABLE IF EXISTS events");
        db.execSQL("DROP TABLE IF EXISTS students");
        db.execSQL("DROP TABLE IF EXISTS subjects");
        db.execSQL("DROP TABLE IF EXISTS courses");
        db.execSQL("DROP TABLE IF EXISTS years");
        onCreate(db);
    }

    private String removeFromCSV(String csv, String itemToRemove) {
        if (csv == null || csv.isEmpty()) return "";
        String[] items = csv.split(",");
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            String trimmed = item.trim();
            if (!trimmed.equals(itemToRemove)) {
                if (sb.length() > 0) sb.append(",");
                sb.append(trimmed);
            }
        }
        return sb.toString();
    }

    // --- Statistics ---
    public int getStudentCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM students", null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public int getEventCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM events", null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public int getActiveEventCount(String now) {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM events WHERE end_date >= ?", new String[]{now});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public Cursor getUpcomingEvents(String now) {
        return getReadableDatabase().rawQuery("SELECT * FROM events WHERE end_date >= ? ORDER BY end_date ASC LIMIT 3", new String[]{now});
    }

    // --- CRUD for Years ---
    public long addYear(int id, String level) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("year_id", id);
        v.put("year_level", level);
        return db.insert("years", null, v);
    }
    public Cursor getAllYears() {
        return getReadableDatabase().rawQuery("SELECT * FROM years", null);
    }
    public String getYearName(int id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT year_level FROM years WHERE year_id = ?", new String[]{String.valueOf(id)});
        if (c.moveToFirst()) {
            String name = c.getString(0);
            c.close();
            return name;
        }
        c.close();
        return "Unknown";
    }

    public void deleteYear(int id) {
        SQLiteDatabase db = getWritableDatabase();
        String yearIdStr = String.valueOf(id);

        // 1. Delete Students in this year
        db.delete("attendance", "student_id IN (SELECT student_id FROM students WHERE year_id = ?)", new String[]{yearIdStr});
        db.delete("students", "year_id = ?", new String[]{yearIdStr});

        // 2. Update Events allowed years
        Cursor cursor = db.rawQuery("SELECT event_id, allowed_year_ids FROM events", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String eventId = cursor.getString(0);
                String allowedYears = cursor.getString(1);
                if (allowedYears != null && !allowedYears.isEmpty()) {
                    String updated = removeFromCSV(allowedYears, yearIdStr);
                    if (updated.isEmpty()) {
                        db.delete("attendance", "event_id = ?", new String[]{eventId});
                        db.delete("events", "event_id = ?", new String[]{eventId});
                    } else if (!updated.equals(allowedYears)) {
                        ContentValues cv = new ContentValues();
                        cv.put("allowed_year_ids", updated);
                        db.update("events", cv, "event_id = ?", new String[]{eventId});
                    }
                }
            }
            cursor.close();
        }

        // 3. Delete Year itself
        db.delete("years", "year_id = ?", new String[]{yearIdStr});
    }

    // --- CRUD for Courses ---
    public long addCourse(String id, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("course_id", id);
        v.put("course_name", name);
        return db.insert("courses", null, v);
    }
    public Cursor getAllCourses() {
        return getReadableDatabase().rawQuery("SELECT * FROM courses", null);
    }
    public String getCourseName(String id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT course_name FROM courses WHERE course_id = ?", new String[]{id});
        if (c.moveToFirst()) {
            String name = c.getString(0);
            c.close();
            return name;
        }
        c.close();
        return "Unknown";
    }

    public void deleteCourse(String id) {
        SQLiteDatabase db = getWritableDatabase();

        // 1. Delete Students in this course
        db.delete("attendance", "student_id IN (SELECT student_id FROM students WHERE course_id = ?)", new String[]{id});
        db.delete("students", "course_id = ?", new String[]{id});

        // 2. Update Events allowed courses
        Cursor cursor = db.rawQuery("SELECT event_id, allowed_course_ids FROM events", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String eventId = cursor.getString(0);
                String allowedCourses = cursor.getString(1);
                if (allowedCourses != null && !allowedCourses.isEmpty()) {
                    String updated = removeFromCSV(allowedCourses, id);
                    if (updated.isEmpty()) {
                        db.delete("attendance", "event_id = ?", new String[]{eventId});
                        db.delete("events", "event_id = ?", new String[]{eventId});
                    } else if (!updated.equals(allowedCourses)) {
                        ContentValues cv = new ContentValues();
                        cv.put("allowed_course_ids", updated);
                        db.update("events", cv, "event_id = ?", new String[]{eventId});
                    }
                }
            }
            cursor.close();
        }

        // 3. Delete Course itself
        db.delete("courses", "course_id = ?", new String[]{id});
    }

    // --- CRUD for Subjects ---
    public long addSubject(String id, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("subject_id", id);
        v.put("subject_name", name);
        return db.insert("subjects", null, v);
    }
    public Cursor getAllSubjects() {
        return getReadableDatabase().rawQuery("SELECT * FROM subjects", null);
    }
    public String getSubjectName(String id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT subject_name FROM subjects WHERE subject_id = ?", new String[]{id});
        if (c.moveToFirst()) {
            String name = c.getString(0);
            c.close();
            return name;
        }
        c.close();
        return "Unknown";
    }

    public void deleteSubject(String id) {
        SQLiteDatabase db = getWritableDatabase();

        // 1. Update Students and delete if subject was the only one
        Cursor sCursor = db.rawQuery("SELECT student_id, subject_ids FROM students", null);
        if (sCursor != null) {
            while (sCursor.moveToNext()) {
                String sid = sCursor.getString(0);
                String sids = sCursor.getString(1);
                if (sids != null && !sids.isEmpty()) {
                    String updated = removeFromCSV(sids, id);
                    if (updated.isEmpty()) {
                        db.delete("attendance", "student_id = ?", new String[]{sid});
                        db.delete("students", "student_id = ?", new String[]{sid});
                    } else if (!updated.equals(sids)) {
                        ContentValues cv = new ContentValues();
                        cv.put("subject_ids", updated);
                        db.update("students", cv, "student_id = ?", new String[]{sid});
                    }
                }
            }
            sCursor.close();
        }

        // 2. Update Events allowed subjects
        Cursor eCursor = db.rawQuery("SELECT event_id, allowed_subject_ids FROM events", null);
        if (eCursor != null) {
            while (eCursor.moveToNext()) {
                String eid = eCursor.getString(0);
                String sids = eCursor.getString(1);
                if (sids != null && !sids.isEmpty()) {
                    String updated = removeFromCSV(sids, id);
                    if (updated.isEmpty()) {
                        db.delete("attendance", "event_id = ?", new String[]{eid});
                        db.delete("events", "event_id = ?", new String[]{eid});
                    } else if (!updated.equals(sids)) {
                        ContentValues cv = new ContentValues();
                        cv.put("allowed_subject_ids", updated);
                        db.update("events", cv, "event_id = ?", new String[]{eid});
                    }
                }
            }
            eCursor.close();
        }

        // 3. Delete Subject itself
        db.delete("subjects", "subject_id = ?", new String[]{id});
    }

    // --- CRUD for Events ---
    public long addEvent(String id, String name, String start, String end, String years, String courses, String subjects) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("event_id", id);
        v.put("event_name", name);
        v.put("start_date", start);
        v.put("end_date", end);
        v.put("allowed_year_ids", years);
        v.put("allowed_course_ids", courses);
        v.put("allowed_subject_ids", subjects);
        return db.insert("events", null, v);
    }
    public Cursor getAllEvents() {
        return getReadableDatabase().rawQuery("SELECT * FROM events", null);
    }
    public Cursor getEventById(String eventId) {
        return getReadableDatabase().rawQuery("SELECT * FROM events WHERE event_id = ?", new String[]{eventId});
    }
    public void deleteEvent(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("attendance", "event_id = ?", new String[]{id});
        db.delete("events", "event_id = ?", new String[]{id});
    }

    // --- Student Operations ---
    public long addStudent(String id, String last, String first, String middle, int yearId, String courseId, String subjectIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("student_id", id);
        v.put("last_name", last);
        v.put("first_name", first);
        v.put("middle_name", middle);
        v.put("year_id", yearId);
        v.put("course_id", courseId);
        v.put("subject_ids", subjectIds);
        return db.insert("students", null, v);
    }

    public Cursor getAllStudents() {
        return getReadableDatabase().rawQuery("SELECT * FROM students", null);
    }

    public Cursor getStudentsByYear(int yearId) {
        return getReadableDatabase().rawQuery("SELECT * FROM students WHERE year_id = ?", new String[]{String.valueOf(yearId)});
    }

    public Cursor getStudentsByCourse(String courseId) {
        return getReadableDatabase().rawQuery("SELECT * FROM students WHERE course_id = ?", new String[]{courseId});
    }

    public Cursor getStudentsBySubject(String subjectId) {
        return getReadableDatabase().rawQuery("SELECT * FROM students WHERE subject_ids LIKE ?", new String[]{"%" + subjectId + "%"});
    }

    public void deleteStudent(String studentId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("attendance", "student_id = ?", new String[]{studentId});
        db.delete("students", "student_id = ?", new String[]{studentId});
    }

    public Cursor getStudentById(String studentId) {
        return getReadableDatabase().rawQuery("SELECT * FROM students WHERE student_id = ?", new String[]{studentId});
    }

    // --- Attendance Operations ---
    public long recordAttendance(String studentId, String eventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM attendance WHERE student_id = ? AND event_id = ?", new String[]{studentId, eventId});
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            int status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
            if (status == 1) {
                cursor.close();
                return -2;
            } else {
                ContentValues values = new ContentValues();
                values.put("status", 1);
                db.update("attendance", values, "student_id = ? AND event_id = ?", new String[]{studentId, eventId});
                cursor.close();
                return 1;
            }
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put("student_id", studentId);
        values.put("event_id", eventId);
        values.put("status", 1);
        return db.insert("attendance", null, values);
    }

    public void markAttendance(String studentId, String eventId, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (status == 0) {
            db.delete("attendance", "student_id = ? AND event_id = ?", new String[]{studentId, eventId});
        } else {
            ContentValues values = new ContentValues();
            values.put("status", 1);
            db.update("attendance", values, "student_id = ? AND event_id = ?", new String[]{studentId, eventId});
        }
    }

    public Cursor getAttendanceByEvent(String eventId) {
        String query = "SELECT s.student_id, s.first_name || ' ' || s.last_name as full_name, a.timestamp, IFNULL(a.status, 0) as status " +
                       "FROM students s LEFT JOIN attendance a ON s.student_id = a.student_id " +
                       "AND a.event_id = ? ";
        return getReadableDatabase().rawQuery(query, new String[]{eventId});
    }
}

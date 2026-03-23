package com.example.v_attendance;

public class Event {
    public String event_id;
    public String event_name;
    public String deadline;
    public String allowed_subject_ids;
    public int year;
    public String course_id;
    public String instructor_name;

    public Event(String id, String name, String deadline, String allowed_subject_ids, int year, String course_id, String instructor_name) {
        this.event_id = id;
        this.event_name = name;
        this.deadline = deadline;
        this.allowed_subject_ids = allowed_subject_ids;
        this.year = year;
        this.course_id = course_id;
        this.instructor_name = instructor_name;
    }
}

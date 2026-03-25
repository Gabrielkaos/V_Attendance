package com.example.v_attendance;

import androidx.annotation.NonNull;

public class Student implements ModernListAdapter.ModernItem {
    public String studentId;
    public String lastName;
    public String firstName;
    public String middleName;
    public int yearId;
    public String courseId;
    public String subjectIds;

    public Student(String studentId, String lastName, String firstName, String middleName, int yearId, String courseId, String subjectIds) {
        this.studentId = studentId;
        this.lastName = lastName;
        this.firstName = firstName;
        this.middleName = middleName;
        this.yearId = yearId;
        this.courseId = courseId;
        this.subjectIds = subjectIds;
    }

    public String getId() {
        return studentId;
    }

    public String getName() {
        return firstName + " " + lastName;
    }

    public String getFullName() {
        return lastName + ", " + firstName + " " + middleName;
    }

    @Override
    public String getMainTitle() {
        return firstName + " " + lastName;
    }

    @Override
    public String getSubTitle() {
        return studentId;
    }

    @Override
    public String getIconText() {
        return firstName.substring(0, 1).toUpperCase();
    }

    @NonNull
    @Override
    public String toString() {
        return getFullName() + " (" + studentId + ")";
    }
}

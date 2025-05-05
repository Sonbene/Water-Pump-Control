package com.example.waterpumpcontrol;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Schedule {
    private Integer id;
    private String pumpMode;
    private String recurrence;
    private String date;
    private List<Integer> daysOfWeek = new ArrayList<>();
    private List<Integer> daysOfMonth = new ArrayList<>();
    private String startTime;
    private String endTime;
    private boolean active;

    public Schedule() {
        // Ensure lists are always initialized
        this.daysOfWeek = new ArrayList<>();
        this.daysOfMonth = new ArrayList<>();
    }

    // Getter / Setter
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public String getPumpMode() {
        return pumpMode;
    }
    public void setPumpMode(String pumpMode) {
        this.pumpMode = pumpMode;
    }

    public String getRecurrence() {
        return recurrence;
    }
    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
    }

    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }

    public List<Integer> getDaysOfWeek() {
        return daysOfWeek;
    }
    public void setDaysOfWeek(List<Integer> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public List<Integer> getDaysOfMonth() {
        return daysOfMonth;
    }
    public void setDaysOfMonth(List<Integer> daysOfMonth) {
        this.daysOfMonth = daysOfMonth;
    }

    public String getStartTime() {
        return startTime;
    }
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }

    // You can add conflict checking or nextRun() logic here
}

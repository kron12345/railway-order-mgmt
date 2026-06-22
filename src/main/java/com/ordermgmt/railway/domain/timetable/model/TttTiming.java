package com.ordermgmt.railway.domain.timetable.model;

/** One TTT TimingAtLocation/Timing entry. */
public record TttTiming(String qualifierCode, String time, int offset) {}

package com.gt30pro.launcher;
public class CalendarSwipeTest {
    public static void main(String[] args) {
        if (CalendarSwipe.direction(-60, 3, 48) != 1 || CalendarSwipe.direction(60, 3, 48) != -1 ||
            CalendarSwipe.direction(12, 0, 48) != 0 || CalendarSwipe.direction(60, 55, 48) != 0 ||
            CalendarSwipe.direction(-48, 0, 48) != 1) throw new AssertionError("Calendar swipe direction");
        System.out.println("Calendar swipe: direction, threshold and vertical rejection: OK");
    }
}

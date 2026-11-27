package com.gt30pro.launcher;
import java.time.LocalDate;
public class CalendarRecurrenceTest {
    static LocalDate d(String s) { return LocalDate.parse(s); }
    static void expect(String start,String from,String unit,int n,String expected) {
        LocalDate result=CalendarRecurrence.next(d(start),d(from),unit,n);
        if (!(expected==null ? result==null : d(expected).equals(result))) throw new AssertionError(start+" "+from+" "+unit+" got "+result);
    }
    public static void main(String[] args) {
        expect("2026-12-31","2027-01-01","none",1,null);
        expect("2026-12-31","2027-01-01","day",2,"2027-01-02");
        expect("2026-12-31","2027-01-01","week",2,"2027-01-14");
        expect("2024-01-31","2024-02-01","month",1,"2024-02-29");
        expect("2024-01-31","2024-03-01","month",1,"2024-03-31");
        expect("2024-02-29","2025-01-01","year",1,"2025-02-28");
        expect("2024-02-29","2028-01-01","year",1,"2028-02-29");
        expect("2100-12-31","2101-01-01","day",1,null);
        expect("2026-01-01","2026-02-01","month",0,null);
        System.out.println("Calendar recurrence: daily, custom intervals, leap dates, clamp and boundaries OK");
    }
}

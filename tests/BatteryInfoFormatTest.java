package com.gt30pro.launcher;

public final class BatteryInfoFormatTest {
    static void eq(Object expected,Object value) { if(!expected.equals(value)) throw new AssertionError("expected="+expected+" actual="+value); }
    public static void main(String[] args) {
        String n=BatteryInfoFormat.UNKNOWN;
        eq("85%",BatteryInfoFormat.level(85,100)); eq("100%",BatteryInfoFormat.level(200,200));
        eq(n,BatteryInfoFormat.level(-1,100)); eq(n,BatteryInfoFormat.level(101,100)); eq(n,BatteryInfoFormat.level(1,0));
        eq("Mengisi",BatteryInfoFormat.status(2)); eq("Digunakan",BatteryInfoFormat.status(3)); eq("Tidak mengisi",BatteryInfoFormat.status(4)); eq("Penuh",BatteryInfoFormat.status(5)); eq(n,BatteryInfoFormat.status(1));
        eq("Tidak terhubung",BatteryInfoFormat.source(0)); eq("AC",BatteryInfoFormat.source(1)); eq("USB",BatteryInfoFormat.source(2)); eq("Nirkabel",BatteryInfoFormat.source(4)); eq("Dock",BatteryInfoFormat.source(8)); eq("AC + USB",BatteryInfoFormat.source(3)); eq(n,BatteryInfoFormat.source(-1)); eq(n,BatteryInfoFormat.source(16));
        eq("Baik",BatteryInfoFormat.health(2)); eq("Terlalu panas",BatteryInfoFormat.health(3)); eq("Rusak",BatteryInfoFormat.health(4)); eq("Tegangan tinggi",BatteryInfoFormat.health(5)); eq("Gangguan",BatteryInfoFormat.health(6)); eq("Terlalu dingin",BatteryInfoFormat.health(7)); eq(n,BatteryInfoFormat.health(1));
        eq("31,2 °C",BatteryInfoFormat.temperature(312)); eq("-5,0 °C",BatteryInfoFormat.temperature(-50)); eq(n,BatteryInfoFormat.temperature(Integer.MIN_VALUE)); eq(n,BatteryInfoFormat.temperature(2000));
        eq("3,950 V",BatteryInfoFormat.voltage(3950)); eq(n,BatteryInfoFormat.voltage(0));
        eq("+1250 mA",BatteryInfoFormat.current(1250000)); eq("-420 mA",BatteryInfoFormat.current(-420000)); eq(n,BatteryInfoFormat.current(Integer.MIN_VALUE));
        eq("3150 mAh",BatteryInfoFormat.charge(3150000)); eq("0 mAh",BatteryInfoFormat.charge(0)); eq(n,BatteryInfoFormat.charge(Integer.MIN_VALUE));
        eq("12,50 Wh",BatteryInfoFormat.energy(12500000000L)); eq(n,BatteryInfoFormat.energy(Long.MIN_VALUE));
        eq("1 menit",BatteryInfoFormat.timeToFull(1000)); eq("1 jam 2 menit",BatteryInfoFormat.timeToFull(3700000)); eq(n,BatteryInfoFormat.timeToFull(-1));
        eq("0",BatteryInfoFormat.cycles(0)); eq(n,BatteryInfoFormat.cycles(-1));
        eq("Li-ion",BatteryInfoFormat.technology(" Li-ion ")); eq(n,BatteryInfoFormat.technology(null)); eq("Ya",BatteryInfoFormat.present(1)); eq("Tidak",BatteryInfoFormat.low(0)); eq(n,BatteryInfoFormat.present(-1));
        System.out.println("Battery info: units, status, charging sources, supported/unknown values and sensor signs OK");
    }
}

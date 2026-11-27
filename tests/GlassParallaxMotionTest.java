package com.gt30pro.launcher;
public final class GlassParallaxMotionTest {
    static void ok(boolean value,String message) { if(!value) throw new AssertionError(message); }
    static void close(float a,float b,String message) { ok(Math.abs(a-b)<.0001f,message+" expected="+b+" actual="+a); }
    public static void main(String[] args) {
        GlassParallaxMotion m=new GlassParallaxMotion();
        ok(m.update(0,9,0),"first sample"); close(m.x(),0,"initial x"); close(m.y(),0,"initial y");
        m.update(4,7,0); close(m.x(),.2f,"smooth x"); close(m.y(),-.1f,"smooth y");
        for(int i=0;i<100;i++) m.update(19,-10,0);
        close(m.x(),1,"positive clamp"); close(m.y(),-1,"negative clamp");
        ok(!m.update(Float.NaN,0,0),"reject NaN"); ok(!m.update(Float.POSITIVE_INFINITY,0,0),"reject infinity");
        ok(!m.update(21,0,0),"reject implausible acceleration"); ok(!m.update(0,0,4),"reject invalid rotation");
        m.update(2,4,1); close(m.x(),0,"rotation recenters x"); close(m.y(),0,"rotation recenters y");
        m.update(2,8,1); close(m.x(),-.2f,"90 degree screen axis"); close(m.y(),0,"90 degree other axis");
        m.reset(); m.update(2,4,2); m.update(6,4,2); close(m.x(),-.2f,"180 degree screen axis");
        m.reset(); m.update(2,4,3); m.update(2,8,3); close(m.x(),.2f,"270 degree screen axis");
        m.reset(); m.update(7,7,0); close(m.x(),0,"reset origin"); close(m.y(),0,"reset origin y");
        System.out.println("Glass parallax motion: calibration, filtering, bounds, rotation and invalid samples OK");
    }
}

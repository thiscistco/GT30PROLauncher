package com.gt30pro.launcher;

public class HomeGestureTest {
    private static void eq(int actual,int expected,String message) { if(actual!=expected) throw new AssertionError(message+" got "+actual); }
    public static void main(String[] args) {
        for (float density : new float[]{1f,2f,3f}) {
            float width=400*density,height=800*density;
            for (float x : new float[]{1,100,200,399}) for(float y : new float[]{90,400,750}) {
                eq(HomeGesture.action(x*density,y*density,x*density,(y-50)*density,width,height,density),HomeGesture.SUPER,"up from anywhere");
            }
            eq(HomeGesture.action(100*density,200*density,100*density,260*density,width,height,density),HomeGesture.NOTIFICATIONS,"left down");
            eq(HomeGesture.action(200*density,200*density,200*density,260*density,width,height,density),HomeGesture.CONTROLS,"center goes right");
            eq(HomeGesture.action(390*density,200*density,390*density,260*density,width,height,density),HomeGesture.CONTROLS,"right down");
            eq(HomeGesture.action(100*density,200*density,100*density,220*density,width,height,density),HomeGesture.NONE,"short drag");
            eq(HomeGesture.action(100*density,200*density,200*density,260*density,width,height,density),HomeGesture.NONE,"diagonal");
        }
        eq(HomeGesture.action(-1,2,0,100,400,800,1),HomeGesture.NONE,"outside");
        eq(HomeGesture.action(1,2,0,100,0,800,1),HomeGesture.NONE,"unmeasured");
        if (HomeGesture.vertical(20,5,12) || HomeGesture.vertical(0,5,12) || !HomeGesture.vertical(1,-12,12)) throw new AssertionError("intercept thresholds");
        System.out.println("Home vertical gestures: Super App anywhere, left/right panels and cancellation thresholds OK");
    }
}

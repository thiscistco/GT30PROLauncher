package com.gt30pro.launcher;

public class BottomSwipeTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        for (float height : new float[] {600, 1600, 2720}) {
            check(BottomSwipe.startsAtBottom(height * .85f, height), "bottom start");
            check(!BottomSwipe.startsAtBottom(height * .79f, height), "start outside bottom zone");
            check(!BottomSwipe.startsAtBottom(height + 1, height), "start outside screen");
            check(BottomSwipe.completes(100, height * .90f, 110, height * .5f, height), "slow bottom to middle");
            check(!BottomSwipe.completes(100, height * .7f, 110, height * .3f, height), "not from edge");
            check(!BottomSwipe.completes(100, height * .9f, 100, height * .7f, height), "short swipe");
            check(!BottomSwipe.completes(100, height * .9f, 100 + height, height * .5f, height), "diagonal swipe");
            check(!BottomSwipe.completes(100, height * .9f, 100, height * .95f, height), "downward swipe");
        }
        check(BottomSwipe.shouldIntercept(10,100,10,80,1), "take over upward drag");
        check(!BottomSwipe.shouldIntercept(10,100,70,98,1), "preserve sideways back gesture");
        check(!BottomSwipe.shouldIntercept(10,100,10,98,1), "preserve tap and short motion");
        check(!BottomSwipe.startsAtBottom(0,0), "unmeasured window");
        System.out.println("Bottom swipe geometry and direction: OK");
    }
}

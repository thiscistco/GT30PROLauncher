package android.widget;
import android.content.Context;
public class Toast {
    public static final int LENGTH_LONG=1; public static int count;
    public static Toast makeText(Context c,CharSequence text,int duration) { count++; return new Toast(); }
    public void show() { }
}

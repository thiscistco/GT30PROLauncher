package android.os;
import java.util.ArrayList;
public class Handler {
    public final ArrayList<Runnable> actions=new ArrayList<>();
    public final ArrayList<Long> delays=new ArrayList<>();
    public boolean post(Runnable r) { r.run(); return true; }
    public boolean postDelayed(Runnable r,long delay) { actions.add(r); delays.add(delay); return true; }
    public void removeCallbacks(Runnable r) { for(int i=actions.size()-1;i>=0;i--) if(actions.get(i)==r) { actions.remove(i); delays.remove(i); } }
    public void runDelay(long delay) { for(int i=0;i<actions.size();i++) if(delays.get(i)==delay) { Runnable r=actions.remove(i); delays.remove(i); r.run(); return; } throw new AssertionError("missing delay " + delay); }
}

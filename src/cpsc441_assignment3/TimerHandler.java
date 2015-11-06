package cpsc441_assignment3;

import java.util.TimerTask;

/**
 *
 * @author brad
 */
public class TimerHandler extends TimerTask {
    FastFtp parent;
    
    public TimerHandler(FastFtp parent)     {
        this.parent = parent;
    }
    
    @Override
    public void run() {
        parent.processTimeout();
    }
    
}

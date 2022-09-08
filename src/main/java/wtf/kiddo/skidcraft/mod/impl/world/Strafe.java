package wtf.kiddo.skidcraft.mod.impl.world;

import me.bush.eventbus.annotation.EventListener;
import wtf.kiddo.skidcraft.event.MotionEvent;
import wtf.kiddo.skidcraft.mod.Category;
import wtf.kiddo.skidcraft.mod.Mod;
import wtf.kiddo.skidcraft.util.MoveUtil;

public class Strafe extends Mod {
    public Strafe() {
        super("Strafe", Category.WORLD);
    }

    @EventListener
    public void onMoveEvent(final MotionEvent event) {
        MoveUtil.strafe();
    }
}

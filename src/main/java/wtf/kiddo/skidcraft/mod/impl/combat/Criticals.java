package wtf.kiddo.skidcraft.mod.impl.combat;

import me.bush.eventbus.annotation.EventListener;
import net.minecraft.src.Packet11PlayerPosition;
import net.minecraft.src.Packet18Animation;
import wtf.kiddo.skidcraft.event.PacketEvent;
import wtf.kiddo.skidcraft.event.UpdateEvent;
import wtf.kiddo.skidcraft.mod.Category;
import wtf.kiddo.skidcraft.mod.Mod;
import wtf.kiddo.skidcraft.util.TimerUtil;
import wtf.kiddo.skidcraft.value.impl.NumberValue;

public final class Criticals extends Mod {
    public NumberValue<Integer> delay = new NumberValue<>("Delay", 100, 0, 1000, 1);
    private int groundTicks;

    private final TimerUtil timer = new TimerUtil();

    private final double[] offsets = new double[]{0.051, 0.011511, 0.001, 0.001};
    public Criticals() {
        super("Criticals", Category.COMBAT);
    }

    @EventListener
    public void onUpdateEvent(final UpdateEvent event) {
        if (event.isPre())
            if (getMc().thePlayer.onGround)
                groundTicks++;
            else
                groundTicks = 0;
    }

    @EventListener
    public void onPacketEvent(final PacketEvent event) {
        if (event.getPacket() instanceof Packet18Animation) {
            final Packet18Animation animation = (Packet18Animation) event.getPacket();
            if(animation.entityId == getMc().thePlayer.entityId) {
                final boolean canCrit = getMc().thePlayer.onGround &&
                        !getMc().gameSettings.keyBindJump.isPressed() && !getMc().thePlayer.isInWater() &&
                        !getMc().thePlayer.isOnLadder();
                if (canCrit)
                    if (getMc().objectMouseOver != null && getMc().objectMouseOver.entityHit != null)
                        this.crit();
            }
        }
    }

    private void crit() {
        double x = getMc().thePlayer.posX;
        double y = getMc().thePlayer.posY;
        double z = getMc().thePlayer.posZ;
        if (timer.reach(delay.getValue()) && groundTicks > 1) {
            getMc().ingameGUI.getChatGUI().printChatMessage("Criticals");
            /*
            for (double offset : offsets)

                getMc().thePlayer.sendQueue.addToSendQueue(new Packet11PlayerPosition(getMc().thePlayer.posX, getMc().thePlayer.posY + offset, getMc().thePlayer.posY, getMc().thePlayer.posZ, false));
             */
            //getMc().thePlayer.motionY = 0.1;
            //getMc().thePlayer.fallDistance = 0.1f;
            //getMc().thePlayer.onGround = false;
            getMc().thePlayer.sendQueue.addToSendQueue(new Packet11PlayerPosition(x, y, y + 0.05250000001304, z, true));
            getMc().thePlayer.sendQueue.addToSendQueue(new Packet11PlayerPosition(x, y, y + 0.00150000001304, z, false));
            getMc().thePlayer.sendQueue.addToSendQueue(new Packet11PlayerPosition(x, y, y + 0.01400000001304, z, false));
            getMc().thePlayer.sendQueue.addToSendQueue(new Packet11PlayerPosition(x, y, y + 0.00150000001304, z, false));
            timer.reset();

        }
    }
}

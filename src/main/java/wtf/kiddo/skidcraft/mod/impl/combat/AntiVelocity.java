package wtf.kiddo.skidcraft.mod.impl.combat;

import me.bush.eventbus.annotation.EventListener;
import net.minecraft.src.Packet;
import net.minecraft.src.Packet28EntityVelocity;
import net.minecraft.src.Packet60Explosion;
import wtf.kiddo.skidcraft.event.PacketEvent;
import wtf.kiddo.skidcraft.mod.Category;
import wtf.kiddo.skidcraft.mod.Mod;
import wtf.kiddo.skidcraft.value.impl.NumberValue;

public final class AntiVelocity extends Mod {
    private NumberValue<Double> horizontal = new NumberValue<>("Horizontal", 0.0D, -3.00, 3.00, 1D);
    private NumberValue<Double> vertical = new NumberValue<>("Vertical", 0.0D, -3.00, 3.00, 1D);
    public AntiVelocity() {
        super("AntiVelocity", Category.COMBAT);
    }

    @EventListener
    public void onPacketEvent(final PacketEvent event) {
        final Packet packet = event.getPacket();
        if(packet instanceof Packet28EntityVelocity) {
            final Packet28EntityVelocity velocity = (Packet28EntityVelocity) packet;
            if(velocity.entityId == getMc().thePlayer.entityId) {
                velocity.motionX = (int) (velocity.motionX * horizontal.getValue());
                velocity.motionY = (int) (velocity.motionY * vertical.getValue());
                velocity.motionZ = (int) (velocity.motionZ * horizontal.getValue());
            }
        }else if (packet instanceof Packet60Explosion) {
            event.setCancelled(true);
        }
    }
}

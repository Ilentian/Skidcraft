package wtf.kiddo.skidcraft.mod.impl.combat;

import me.bush.eventbus.annotation.EventListener;
import net.minecraft.src.Packet;
import net.minecraft.src.Packet28EntityVelocity;
import net.minecraft.src.Packet60Explosion;
import wtf.kiddo.skidcraft.event.PacketEvent;
import wtf.kiddo.skidcraft.mod.Category;
import wtf.kiddo.skidcraft.mod.Mod;

public final class AntiVelocity extends Mod {
    public AntiVelocity() {
        super("AntiVelocity", Category.COMBAT);
    }

    @EventListener
    public void onPacketEvent(final PacketEvent event) {
        final Packet packet = event.getPacket();
        if(packet instanceof Packet28EntityVelocity || packet instanceof Packet60Explosion) {
            event.setCancelled(true);
        }
    }
}

package wtf.kiddo.skidcraft.mod.impl.world;

import me.bush.eventbus.annotation.EventListener;
import org.lwjgl.input.Keyboard;
import wtf.kiddo.skidcraft.event.MotionEvent;
import wtf.kiddo.skidcraft.mod.Category;
import wtf.kiddo.skidcraft.mod.Mod;
import wtf.kiddo.skidcraft.value.impl.NumberValue;

public final class Fly extends Mod {
    private NumberValue<Double> flyspeed = new NumberValue("FlySpeed", 2.0, 0.8, 3.5, 0.1);
    public Fly() {
        super("Fly", Category.WORLD);
        setKey(Keyboard.KEY_F);
    }

    @EventListener
    public void onMoveEvent(final MotionEvent event) {
        if (getMc().gameSettings.keyBindForward.pressed || getMc().gameSettings.keyBindBack.pressed || getMc().gameSettings.keyBindRight.pressed || getMc().gameSettings.keyBindLeft.pressed) {
            setMoveSpeed(event, flyspeed.getValue());
        }
        getMc().thePlayer.capabilities.isFlying = false;
        getMc().thePlayer.motionY = 0.085;
        getMc().thePlayer.jumpMovementFactor = 2;

        if (getMc().gameSettings.keyBindJump.pressed) {
            getMc().thePlayer.motionY += 1;
        }
        if (getMc().gameSettings.keyBindSneak.pressed) {
            getMc().thePlayer.motionY -= 1;
        }
    }

    private void setMoveSpeed(MotionEvent event, double speed) {
        double forward = getMc().thePlayer.movementInput.moveForward;
        double strafe = getMc().thePlayer.movementInput.moveStrafe;
        float yaw = getMc().thePlayer.rotationYaw;
        if (forward == 0.0D && strafe == 0.0D) {
            event.setX(0);
            event.setZ(0);
        } else {
            if (forward != 0.0D) {
                if (strafe > 0.0D) {
                    yaw += forward > 0.0D ? -45 : 45;
                } else if (strafe < 0.0D) {
                    yaw += forward > 0.0D ? 45 : -45;
                }

                strafe = 0.0D;
                if (forward > 0.0D) {
                    forward = 1.0D;
                } else if (forward < 0.0D) {
                    forward = -1.0D;
                }
            }

            event.setX(forward * speed * -Math.sin(Math.toRadians(yaw)) + strafe * speed * Math.cos(Math.toRadians(yaw)));
            event.setZ(forward * speed * Math.cos(Math.toRadians(yaw)) - strafe * speed * -Math.sin(Math.toRadians(yaw)));
        }
    }
}

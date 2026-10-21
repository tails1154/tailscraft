package net.lax1dude.eaglercraft;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiCheatMenu;
import net.lax1dude.eaglercraft.sp.SingleplayerServerController;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;

/** Local-only cheat toggles. Never active on external multiplayer servers. */
public final class CheatMenuState {
    private static boolean playerEsp;
    private static boolean flight;
    private static boolean nightVision;
    private static boolean noFall;
    private static boolean speedBoost;
    private static boolean flightOverride;
    private static boolean previousAllowFlying;
    private static boolean previousFlying;
    private static boolean ctrlKArmed;
    private static long ctrlKDeadline;

    private CheatMenuState() { }

    public static boolean isAllowed() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc != null && mc.world != null && SingleplayerServerController.isClientInEaglerSingleplayer();
    }

    public static boolean isPlayerEsp() { return isAllowed() && playerEsp; }
    public static void setPlayerEsp(boolean enabled) { playerEsp = enabled && isAllowed(); }
    public static boolean togglePlayerEsp() {
        setPlayerEsp(!playerEsp);
        return playerEsp;
    }

    public static boolean isFlight() { return isAllowed() && flight; }
    public static boolean isNightVision() { return isAllowed() && nightVision; }
    public static boolean isNoFall() { return isAllowed() && noFall; }
    public static boolean isSpeedBoost() { return isAllowed() && speedBoost; }

    public static boolean toggleFlight() {
        if (flight) setFlight(false); else setFlight(true);
        return flight;
    }
    public static boolean toggleNightVision() {
        nightVision = !nightVision;
        if (!nightVision && isAllowed()) Minecraft.getMinecraft().player.removePotionEffect(MobEffects.NIGHT_VISION);
        return nightVision;
    }
    public static boolean toggleNoFall() { noFall = !noFall; return noFall; }
    public static boolean toggleSpeedBoost() { speedBoost = !speedBoost; return speedBoost; }

    private static void setFlight(boolean enabled) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (enabled && isAllowed() && player != null) {
            if (!flightOverride) {
                previousAllowFlying = player.capabilities.allowFlying;
                previousFlying = player.capabilities.isFlying;
                flightOverride = true;
            }
            flight = true;
            player.capabilities.allowFlying = true;
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
        } else {
            flight = false;
            if (flightOverride && player != null) {
                player.capabilities.allowFlying = previousAllowFlying;
                player.capabilities.isFlying = previousFlying;
                player.sendPlayerAbilities();
            }
            flightOverride = false;
        }
    }

    public static void heal() {
        if (!isAllowed()) return;
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null) player.setHealth(player.getMaxHealth());
    }
    public static void feed() {
        if (!isAllowed()) return;
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null) {
            player.getFoodStats().setFoodLevel(20);
            player.getFoodStats().setFoodSaturationLevel(20.0F);
        }
    }
    public static void clearEffects() {
        if (!isAllowed()) return;
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null) for (PotionEffect effect : new ArrayList<PotionEffect>(player.getActivePotionEffects())) player.removePotionEffect(effect.getPotion());
    }

    /** Applies local-only toggles each client tick while the player is in singleplayer. */
    public static void tick() {
        if (!isAllowed()) {
            if (flightOverride) setFlight(false);
            flight = nightVision = noFall = speedBoost = false;
            return;
        }
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) return;
        if (flight) {
            player.capabilities.allowFlying = true;
            if (!player.capabilities.isFlying) player.capabilities.isFlying = true;
        }
        if (noFall) player.fallDistance = 0.0F;
        if (nightVision && (!player.isPotionActive(MobEffects.NIGHT_VISION)
                || player.getActivePotionEffect(MobEffects.NIGHT_VISION).getDuration() < 40))
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 220, 0, false, false));
        if (speedBoost && (!player.isPotionActive(MobEffects.SPEED)
                || player.getActivePotionEffect(MobEffects.SPEED).getDuration() < 40))
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 220, 1, false, false));
    }

    public static boolean handleKey(int keyCode) {
        if (!isAllowed()) {
            ctrlKArmed = false;
            return false;
        }
        boolean keyK = keyCode == KeyboardConstants.KEY_K || keyCode == 75;
        boolean keyC = keyCode == KeyboardConstants.KEY_C || keyCode == 67;
        if (keyK && (Keyboard.isKeyDown(KeyboardConstants.KEY_LCONTROL)
                || Keyboard.isKeyDown(KeyboardConstants.KEY_RCONTROL)
                || Keyboard.isKeyDown(17))) {
            ctrlKArmed = true;
            ctrlKDeadline = Minecraft.getSystemTime() + 1500L;
            return true;
        }
        if (keyC && ctrlKArmed && Minecraft.getSystemTime() <= ctrlKDeadline) {
            ctrlKArmed = false;
            Minecraft.getMinecraft().displayGuiScreen(new GuiCheatMenu());
            return true;
        }
        if (Minecraft.getSystemTime() > ctrlKDeadline) ctrlKArmed = false;
        return false;
    }
}

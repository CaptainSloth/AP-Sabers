package com.adventureparkmc.robawesome.apsabers.abilities;

import com.adventureparkmc.robawesome.apsabers.APSabers;
import com.adventureparkmc.robawesome.apsabers.SaberType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle; // Ensure this is org.bukkit.Particle
import org.bukkit.SoundCategory;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.Color; // This is org.bukkit.Color

public class SaberThrowRunnable extends BukkitRunnable {

    private final APSabers plugin;
    private final Player owner;
    private final ArmorStand saberStand;
    private final SaberType saberType;
    private final AbilityManager abilityManager;
    private Vector direction;
    private int ticksLived = 0;
    private final int maxFlightTicks = 70;
    private final int returnStartTicks = 30;
    private boolean returning = false;
    private final double speed = 0.9;
    private boolean hitTarget = false;

    public SaberThrowRunnable(APSabers plugin, Player owner, ArmorStand saberStand, SaberType saberType, AbilityManager abilityManager) {
        this.plugin = plugin;
        this.owner = owner;
        this.saberStand = saberStand;
        this.saberType = saberType;
        this.abilityManager = abilityManager;
        this.direction = owner.getEyeLocation().getDirection().normalize().multiply(speed);
    }

    @Override
    public void run() {
        if (!owner.isOnline() || owner.isDead() || saberStand.isDead()) {
            cleanupAndNotifyManager(hitTarget);
            return;
        }

        ticksLived++;

        if (ticksLived > maxFlightTicks) {
            cleanupAndNotifyManager(hitTarget);
            return;
        }

        Location currentLocation = saberStand.getLocation();

        if (!returning && ticksLived >= returnStartTicks) {
            returning = true;
            owner.sendActionBar(Component.text("Saber returning...").color(NamedTextColor.GRAY));
        }

        if (returning) {
            if (owner.getLocation().distanceSquared(currentLocation) < 2.0 * 2.0) {
                cleanupAndNotifyManager(hitTarget);
                return;
            }
            direction = owner.getEyeLocation().add(0, 0.5, 0).toVector().subtract(currentLocation.toVector()).normalize().multiply(speed * 1.3);
        } else {
            Location nextLocation = currentLocation.clone().add(direction);
            if (!nextLocation.getBlock().isPassable() && !nextLocation.getBlock().isLiquid()) {
                currentLocation.getWorld().playSound(currentLocation, saberType.getClashSound(), SoundCategory.BLOCKS, 0.7f, 1.2f);
                owner.sendActionBar(Component.text("Saber hit a wall! Returning...").color(NamedTextColor.GRAY));
                // Corrected Particle Usage for block hit (line 81 context)
                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(180, 180, 180), 1.0F); // Grey sparks
                currentLocation.getWorld().spawnParticle(Particle.DUST, currentLocation, 5, 0.2, 0.2, 0.2, dustOptions); // Changed to DUST for DustOptions
                currentLocation.getWorld().spawnParticle(Particle.CRIT, currentLocation, 3, 0.2,0.2,0.2,0);
                returning = true;
                hitTarget = true;
            }

            if (!returning) {
                for (Entity entity : saberStand.getWorld().getNearbyEntities(currentLocation, 1.0, 1.0, 1.0)) {
                    if (entity instanceof LivingEntity && entity != owner && !(entity instanceof ArmorStand)) {
                        LivingEntity target = (LivingEntity) entity;
                        target.damage(saberType.getDamage(), owner);
                        target.getWorld().playSound(target.getLocation(), saberType.getClashSound(), SoundCategory.NEUTRAL, 1f, 1.1f);
                        owner.sendActionBar(Component.text("Saber hit " + target.getName() + "! Returning...").color(NamedTextColor.YELLOW));

                        // Corrected Particle Usage for entity hit
                        Particle.DustOptions hitDust = new Particle.DustOptions(Color.RED, 1.2F); // Red "blood" or impact
                        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, target.getHeight() / 2, 0), 10, 0.3, 0.3, 0.3, hitDust); // Changed to DUST
                        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, target.getHeight() / 2, 0), 3, 0.3, 0.3, 0.3, 0);

                        returning = true;
                        hitTarget = true;
                        break;
                    }
                }
            }
        }

        saberStand.teleport(currentLocation.add(direction));
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
        saberStand.setRotation(yaw, 0);
        saberStand.setHeadPose(saberStand.getHeadPose().add(Math.toRadians(45), 0, 0));

        // Corrected Particle Usage for trail (line 123 context)
        Color trailColor = Color.LIME; // Default, can be customized per saber later
        // Example customization based on internal name (you'd make this more robust)
        if (saberType.getInternalName().toLowerCase().contains("red")) trailColor = Color.RED;
        else if (saberType.getInternalName().toLowerCase().contains("blue")) trailColor = Color.BLUE;
        else if (saberType.getInternalName().toLowerCase().contains("green")) trailColor = Color.GREEN;
        // ... add other colors

        Particle.DustOptions trailDustOptions = new Particle.DustOptions(trailColor, 0.8F);
        currentLocation.getWorld().spawnParticle(Particle.DUST, currentLocation, 1, 0, 0, 0, trailDustOptions); // Changed to DUST
    }

    private void cleanupAndNotifyManager(boolean didHitSomething) {
        if (saberStand != null && !saberStand.isDead()) {
            saberStand.remove();
        }
        abilityManager.completeSaberThrow(owner.getUniqueId(), didHitSomething);
        if (!this.isCancelled()) {
            this.cancel();
        }
    }

    public void forceCleanup() {
        if (saberStand != null && !saberStand.isDead()) {
            saberStand.remove();
        }
        if (!this.isCancelled()) {
            this.cancel();
        }
    }
}
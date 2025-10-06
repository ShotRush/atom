package org.shotrush.atom.handler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.shotrush.atom.Atom;

public class SpearHandler implements Listener {
    private final Atom plugin;
    
    public SpearHandler(Atom plugin) {
        this.plugin = plugin;
    }
    
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onPlayerThrow(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item.getType() != Material.STICK) {
            return;
        }
        
        if (!player.isSneaking()) {
            return;
        }
        
        event.setCancelled(true);
        
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
        
        Snowball spear = player.launchProjectile(Snowball.class);
        Vector velocity = player.getLocation().getDirection().multiply(2.5);
        spear.setVelocity(velocity);
        spear.setMetadata("spear", new FixedMetadataValue(plugin, true));
        spear.setMetadata("thrower", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        
        player.sendMessage("§aThrew a spear!");
    }
    
    @EventHandler
    public void onSpearHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }
        
        if (!snowball.hasMetadata("spear")) {
            return;
        }
        
        if (event.getHitEntity() != null && event.getHitEntity() instanceof LivingEntity target) {
            target.damage(6.0);
            
            if (snowball.hasMetadata("thrower")) {
                String throwerId = snowball.getMetadata("thrower").get(0).asString();
                Player thrower = Bukkit.getPlayer(java.util.UUID.fromString(throwerId));
                if (thrower != null) {
                    thrower.sendMessage("§aSpear hit for 6 damage!");
                }
            }
        }
        
        snowball.getWorld().dropItemNaturally(snowball.getLocation(), new ItemStack(Material.STICK));
    }
}

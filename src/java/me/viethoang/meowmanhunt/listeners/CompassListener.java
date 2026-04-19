package me.viethoang.meowmanhunt.listeners;

import me.viethoang.meowmanhunt.MeowManhunt;
import me.viethoang.meowmanhunt.managers.GameManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class CompassListener implements Listener {

    private final MeowManhunt plugin;

    public CompassListener(MeowManhunt plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        PlayerInteractEvent.Action action = event.getAction();
        if (action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR
                && action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.COMPASS) return;

        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        if (!gm.isHunter(player.getUniqueId())) return;

        gm.updateCompassForHunter(player);
    }
}

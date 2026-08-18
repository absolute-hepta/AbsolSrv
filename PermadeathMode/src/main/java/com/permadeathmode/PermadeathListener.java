package com.permadeathmode;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;

public class PermadeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!PermadeathMode.getInstance().isPermadeathEnabled()) {
            return;
        }

        Player player = event.getEntity();
        Player killer = player.getKiller();
        String cause = event.getDeathMessage() == null ? "desconocida" : event.getDeathMessage();

        PermadeathMode.getInstance().handleDeath(player, cause, killer);
        event.setDeathMessage("");
        event.getDrops().clear();  // Los items deben ir al cofre, no al suelo
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!PermadeathMode.getInstance().isPermadeathEnabled()) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        if (PermadeathMode.getInstance().isReviveBook(item)) {
            event.setCancelled(true);
            PermadeathMode.getInstance().revivePlayer(event.getPlayer(), event.getPlayer());
            
            // Eliminar el libro del inventario
            item.setAmount(item.getAmount() - 1);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Mostrar aviso si permadeath está activado
        if (PermadeathMode.getInstance().isPermadeathEnabled()) {
            player.sendTitle("§c§l¡ATENCIÓN!", "§7El modo permadeath está ACTIVO en este servidor", 10, 40, 10);
            player.sendMessage("§6[Permadeath] §7El modo permadeath está activado. Si mueres, serás expulsado del servidor.");
        }
        
        if (PermadeathMode.getInstance().hasPendingRevive(player.getUniqueId())) {
            // Mantener en espectador hasta que haga clic en el libro
            player.setGameMode(GameMode.SPECTATOR);
            
            // Mostrar las coordenadas donde murió (seleccionables)
            Location deathLoc = PermadeathMode.getInstance().getDeathLocation(player.getUniqueId());
            if (deathLoc != null) {
                String coordText = Math.round(deathLoc.getX()) + " " + Math.round(deathLoc.getY()) + " " + Math.round(deathLoc.getZ());
                player.sendMessage("§6[Permadeath] §7Moriste en: §e" + coordText + " §7(copiar para teleportarse)");
            }
            
            player.sendTitle("§c§lEstás muerto", "§7Recoge el libro dorado de la caja y haz clic derecho", 10, 120, 10);
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            PermadeathMode.getInstance().setModeByWeather(true);
        } else {
            PermadeathMode.getInstance().setModeByWeather(false);
        }
    }
}

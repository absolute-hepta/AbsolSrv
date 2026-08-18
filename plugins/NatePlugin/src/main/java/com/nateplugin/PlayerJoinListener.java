package com.nateplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.ChatColor;

public class PlayerJoinListener implements Listener {
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Verificar si es un jugador nuevo (no tiene memoria en Nate)
        if (!MemoryManager.getInstance().getAllMemories().containsKey(player.getUniqueId())) {
            // Mensaje de bienvenida con instrucciones
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6=== Bienvenido al servidor con Nate ==="));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Nate es un admin artificial con personalidad tímida y empática."));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eComandos disponibles:"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7/chat <mensaje> &f- Habla en privado con Nate"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Menciona 'Nate' en el chat &f- Nate responderá públicamente"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7/nate on/off &f- Activar/desactivar a Nate"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7/nate status &f- Ver estado de Nate"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7/nate model list &f- Ver modelos disponibles"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Para usar Nate, primero configura una API key gratuita:"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7https://openrouter.ai/"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Luego usa: /nate apikey <tu_key>"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7*Nate responde con timidez y empatía...*"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        }
    }
}

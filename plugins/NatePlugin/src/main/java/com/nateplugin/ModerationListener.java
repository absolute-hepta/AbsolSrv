package com.nateplugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class ModerationListener implements Listener {
    
    private static final Pattern LINK_PATTERN = Pattern.compile(
        "(https?://(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&/=]*)|" +
        "(www\\.[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}(?:/\\S*)?)|" +
        "([a-zA-Z0-9-]+\\.(com|net|org|io|gg|xyz|tk|ml)(?:/\\S*)?)",
        Pattern.CASE_INSENSITIVE
    );
    
    private final Map<UUID, Integer> messageCounts = new HashMap<>();
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!NatePlugin.getInstance().isNateEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String message = event.getMessage();
        
        if (LINK_PATTERN.matcher(message).find()) {
            event.setCancelled(true);
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&', "&cHas sido baneado por compartir links."));
            NatePlugin.getInstance().getServer().getBanList(org.bukkit.BanList.Type.NAME).addBan(player.getName(), "Compartir links en el chat", null, "Nate");
            NatePlugin.getInstance().getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&c" + player.getName() + " ha sido baneado por compartir links."));
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastMessage = currentTime - lastMessageTime.getOrDefault(playerUUID, 0L);
        
        if (timeSinceLastMessage < 1200L) {
            int count = messageCounts.getOrDefault(playerUUID, 0) + 1;
            messageCounts.put(playerUUID, count);
            
            if (count >= 5) {
                event.setCancelled(true);
                player.kickPlayer(ChatColor.translateAlternateColorCodes('&', "&cHas sido baneado por spam."));
                NatePlugin.getInstance().getServer().getBanList(org.bukkit.BanList.Type.NAME).addBan(player.getName(), "Spam en el chat", null, "Nate");
                NatePlugin.getInstance().getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&c" + player.getName() + " ha sido baneado por spam."));
                messageCounts.remove(playerUUID);
                lastMessageTime.remove(playerUUID);
                return;
            }
        } else {
            messageCounts.put(playerUUID, 1);
        }
        
        lastMessageTime.put(playerUUID, currentTime);
    }
}

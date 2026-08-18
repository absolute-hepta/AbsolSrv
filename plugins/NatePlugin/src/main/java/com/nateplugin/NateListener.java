package com.nateplugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NateListener implements Listener {
    
    private static final Pattern[] HALAGO_PATTERNS = {
        Pattern.compile("(?i)\\b(bueno|buena|genial|increíble|amazing|awesome|cool|gracias|thanks|perfecto|perfecta|excelente|me gusta|te quiero|te amo|buen trabajo|good job)\\b"),
        Pattern.compile("(?i)\\b(eres el mejor|eres la mejor|crack|pro|maestro|master)\\b"),
        Pattern.compile("(?i)\\b(nate eres|nate es)\\s*(bueno|buena|genial|increíble|amazing|awesome|cool)\\b")
    };
    
    private static final Pattern[] INSULTO_PATTERNS = {
        Pattern.compile("(?i)\\b(estúpido|estúpida|idiota|tonto|tonta|imbécil|inútil|bob|boba|loser|basura|mierda|shit|fuck|stupid|dumb)\\b"),
        Pattern.compile("(?i)\\b(nate eres|nate es)\\s*(estúpido|estúpida|idiota|tonto|tonta|imbécil|inútil)\\b")
    };
    
    private static final Pattern[] TRATO_AMABLE_PATTERNS = {
        Pattern.compile("(?i)\\b(por favor|please|gracias|thanks|disculpa|perdón|sorry)\\b"),
        Pattern.compile("(?i)\\b(nate ¿puedes|nate puedes|nate podrías)\\b")
    };
    
    private static final Pattern[] TRATO_RUDO_PATTERNS = {
        Pattern.compile("(?i)\\b(callate|cállate|shut up)\\b"),
        Pattern.compile("(?i)\\b(nate haz|nate hazlo|nate haz)\\s*(ahora|ya|rápido)\\b")
    };
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!NatePlugin.getInstance().isNateEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String message = event.getMessage();

        if (message.toLowerCase().startsWith("/nate") || message.toLowerCase().startsWith("/chat")) {
            return;
        }

        if (NatePlugin.getInstance().processAdminBanOrder(player, message)) {
            event.setCancelled(true);
            return;
        }

        event.setMessage(NatePlugin.getInstance().colorNateMentions(message));

        String normalizedMessage = event.getMessage();
        MemoryManager.getInstance().recordPlayerMessage(playerUUID, player.getName(), normalizedMessage);
        analyzeAndRecordInteraction(playerUUID, player.getName(), normalizedMessage);
        SceneManager.getInstance().trackConversation(player, normalizedMessage);

        boolean mentionedNate = NatePlugin.getInstance().isNateEnabledForPlayer(player) && NatePlugin.getInstance().isNateMentioned(normalizedMessage);

        if (mentionedNate) {
            String fullMessage = "El jugador " + player.getName() + " te menciona en el chat: " + normalizedMessage;
            NatePlugin.getInstance().broadcastThinkingAlert();
            NatePlugin.getInstance().showThinking(player);

            OpenAIManager.getInstance().generateResponse(playerUUID, player.getName(), fullMessage)
                .thenAccept(response -> {
                    NatePlugin.getInstance().getServer().getScheduler().runTask(
                        NatePlugin.getInstance(),
                        () -> {
                            NatePlugin.getInstance().stopThinking(player);
                            String formattedResponse = ChatColor.translateAlternateColorCodes('&', "&7[Nate] &f" + response);
                            NatePlugin.getInstance().getServer().broadcastMessage(formattedResponse);
                        }
                    );
                })
                .exceptionally(ex -> {
                    NatePlugin.getInstance().getServer().getScheduler().runTask(
                        NatePlugin.getInstance(),
                        () -> {
                            NatePlugin.getInstance().stopThinking(player);
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cLo siento... tuve un problema al responder..."));
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Error: " + ex.getMessage()));
                        }
                    );
                    return null;
                });
        }

        int onlinePlayers = NatePlugin.getInstance().getServer().getOnlinePlayers().size();
        if (onlinePlayers == 1 && !mentionedNate) {
            String fullMessage = "Eres el único jugador en el servidor. El jugador " + player.getName() + " dice: " + normalizedMessage;
            NatePlugin.getInstance().broadcastThinkingAlert();
            NatePlugin.getInstance().showThinking(player);

            OpenAIManager.getInstance().generateResponse(playerUUID, player.getName(), fullMessage)
                .thenAccept(response -> {
                    NatePlugin.getInstance().getServer().getScheduler().runTask(
                        NatePlugin.getInstance(),
                        () -> {
                            NatePlugin.getInstance().stopThinking(player);
                            String formattedResponse = ChatColor.translateAlternateColorCodes('&', "&7[Nate] &f" + response);
                            NatePlugin.getInstance().getServer().broadcastMessage(formattedResponse);
                        }
                    );
                })
                .exceptionally(ex -> {
                    NatePlugin.getInstance().getServer().getScheduler().runTask(
                        NatePlugin.getInstance(),
                        () -> {
                            NatePlugin.getInstance().stopThinking(player);
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cLo siento... tuve un problema al responder..."));
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Error: " + ex.getMessage()));
                        }
                    );
                    return null;
                });
        }
    }
    
    private void analyzeAndRecordInteraction(UUID playerUUID, String playerName, String message) {
        analyzeAndRecordInteractionStatic(playerUUID, playerName, message);
    }
    
    public static void analyzeAndRecordInteractionStatic(UUID playerUUID, String playerName, String message) {
        String lowerMessage = message.toLowerCase();
        
        if (containsPatternStatic(lowerMessage, HALAGO_PATTERNS)) {
            MemoryManager.getInstance().recordInteraction(playerUUID, playerName, "halagos");
        } else if (containsPatternStatic(lowerMessage, INSULTO_PATTERNS)) {
            MemoryManager.getInstance().recordInteraction(playerUUID, playerName, "insultos");
        } else if (containsPatternStatic(lowerMessage, TRATO_AMABLE_PATTERNS)) {
            MemoryManager.getInstance().recordInteraction(playerUUID, playerName, "tratos_amables");
        } else if (containsPatternStatic(lowerMessage, TRATO_RUDO_PATTERNS)) {
            MemoryManager.getInstance().recordInteraction(playerUUID, playerName, "tratos_rudos");
        }
    }
    
    private static boolean containsPatternStatic(String message, Pattern[] patterns) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!NatePlugin.getInstance().isNateEnabled()) {
            return;
        }
        
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();
        String deathMessage = event.getDeathMessage();
        
        String message = "El jugador " + player.getName() + " ha muerto. " + deathMessage;
        
        OpenAIManager.getInstance().generateResponse(playerUUID, player.getName(), message)
            .thenAccept(response -> {
                NatePlugin.getInstance().getServer().getScheduler().runTaskLater(
                    NatePlugin.getInstance(),
                    () -> {
                        String formattedResponse = ChatColor.translateAlternateColorCodes('&', "&7[Nate] &f" + response);
                        NatePlugin.getInstance().getServer().broadcastMessage(formattedResponse);
                    },
                    20L
                );
            })
            .exceptionally(ex -> null);
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!NatePlugin.getInstance().isNateEnabled()) {
            return;
        }
        if (event.getEntity().getKiller() == null) {
            return;
        }
        
        Player killer = event.getEntity().getKiller();
        UUID killerUUID = killer.getUniqueId();
        Entity victim = event.getEntity();
        
        String victimName = victim.getName();
        String message = "El jugador " + killer.getName() + " ha matado a " + victimName;
        
        OpenAIManager.getInstance().generateResponse(killerUUID, killer.getName(), message)
            .thenAccept(response -> {
                NatePlugin.getInstance().getServer().getScheduler().runTaskLater(
                    NatePlugin.getInstance(),
                    () -> {
                        String formattedResponse = ChatColor.translateAlternateColorCodes('&', "&7[Nate] &f" + response);
                        NatePlugin.getInstance().getServer().broadcastMessage(formattedResponse);
                    },
                    20L
                );
            })
            .exceptionally(ex -> null);
    }
    
    @EventHandler
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        if (!NatePlugin.getInstance().isNateEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        String advancementName = event.getAdvancement().getKey().getKey();
        String message = "El jugador " + player.getName() + " ha conseguido el logro: " + advancementName;
        
        OpenAIManager.getInstance().generateResponse(playerUUID, player.getName(), message)
            .thenAccept(response -> {
                NatePlugin.getInstance().getServer().getScheduler().runTaskLater(
                    NatePlugin.getInstance(),
                    () -> {
                        String formattedResponse = ChatColor.translateAlternateColorCodes('&', "&7[Nate] &f" + response);
                        NatePlugin.getInstance().getServer().broadcastMessage(formattedResponse);
                    },
                    20L
                );
            })
            .exceptionally(ex -> null);
    }
}

package com.nateplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ChatCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!NatePlugin.getInstance().isNateEnabled()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNate no está activado. Usa /nate on para activarlo."));
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cEste comando solo puede ser usado por jugadores."));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUso: /chat <mensaje>"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Ejemplo: /chat Hola Nate, ¿cómo estás?"));
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();
        String message = String.join(" ", args);
        
        if (NatePlugin.getInstance().getApiKey().isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPrimero configura la API key con /nate apikey <tu_key>"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Obtén tu API key gratuita en https://openrouter.ai/"));
            return true;
        }
        
        NateListener.analyzeAndRecordInteractionStatic(playerUUID, player.getName(), message);
        MemoryManager.getInstance().recordPlayerMessage(playerUUID, player.getName(), message);
        NatePlugin.getInstance().broadcastThinkingAlert();
        NatePlugin.getInstance().showThinking(player);
        
        String fullMessage = "El jugador " + player.getName() + " te habla en privado: " + message;
        
        OpenAIManager.getInstance().generateResponse(playerUUID, player.getName(), fullMessage)
            .thenAccept(response -> {
                NatePlugin.getInstance().getServer().getScheduler().runTask(
                    NatePlugin.getInstance(),
                    () -> {
                        NatePlugin.getInstance().stopThinking(player);
                        String formattedResponse = ChatColor.translateAlternateColorCodes('&', "&7[Nate-Privado] &f" + response);
                        player.sendMessage(formattedResponse);
                    }
                );
            })
            .exceptionally(ex -> {
                NatePlugin.getInstance().getServer().getScheduler().runTask(
                    NatePlugin.getInstance(),
                    () -> {
                        NatePlugin.getInstance().stopThinking(player);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cLo siento... tuve un problema al procesar tu mensaje privado..."));
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Error: " + ex.getMessage()));
                    }
                );
                return null;
            });
        
        return true;
    }
}

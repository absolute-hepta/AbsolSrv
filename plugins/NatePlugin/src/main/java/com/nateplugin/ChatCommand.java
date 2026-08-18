package com.nateplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.UUID;
import java.util.regex.Pattern;

public class ChatCommand implements CommandExecutor {
    
    private static final Pattern[] PROFANITY_PATTERNS = {
        Pattern.compile("(?i)\\b(estúpido|estúpida|idiota|tonto|tonta|imbécil|inútil|bob|boba|loser|basura|mierda|shit|fuck|stupid|dumb|puto|puta|zorra|cerdo|perra|concha|verga|culo|pendejo|hijoputa|malparido)\\b"),
        Pattern.compile("(?i)\\b(nate eres|nate es)\\s*(estúpido|estúpida|idiota|tonto|tonta|imbécil|inútil)\\b")
    };
    
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
        
        // Unir todos los argumentos en un solo mensaje
        String message = String.join(" ", args);
        
        // Verificar si contiene groserías
        if (containsProfanity(message)) {
            NateListener.analyzeAndRecordInteractionStatic(playerUUID, player.getName(), message);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[Nate] &cLo siento... no puedo responder a mensajes con lenguaje ofensivo..."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Por favor, sé amable y respetuoso..."));
            return true;
        }
        
        // Verificar API key
        if (NatePlugin.getInstance().getApiKey().isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPrimero configura la API key con /nate apikey <tu_key>"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Obtén tu API key gratuita en https://openrouter.ai/"));
            return true;
        }
        
        // Analizar y registrar interacción
        NateListener.analyzeAndRecordInteractionStatic(playerUUID, player.getName(), message);
        
        // Agregar mensaje a la memoria reciente
        PlayerMemory memory = MemoryManager.getInstance().getPlayerMemory(playerUUID, player.getName());
        memory.addRecentMessage(message);
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Enviando mensaje a Nate..."));
        
        String fullMessage = "El jugador " + player.getName() + " te habla en privado: " + message;
        
        OpenAIManager.getInstance().generateResponse(playerUUID, player.getName(), fullMessage)
            .thenAccept(response -> {
                NatePlugin.getInstance().getServer().getScheduler().runTask(
                    NatePlugin.getInstance(),
                    () -> {
                        String formattedResponse = ChatColor.translateAlternateColorCodes('&', "&7[Nate-Privado] &f" + response);
                        player.sendMessage(formattedResponse);
                    }
                );
            })
            .exceptionally(ex -> {
                NatePlugin.getInstance().getServer().getScheduler().runTask(
                    NatePlugin.getInstance(),
                    () -> {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cLo siento... tuve un problema al procesar tu mensaje privado..."));
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Error: " + ex.getMessage()));
                    }
                );
                return null;
            });
        
        return true;
    }
    
    private boolean containsProfanity(String message) {
        String lowerMessage = message.toLowerCase();
        for (Pattern pattern : PROFANITY_PATTERNS) {
            java.util.regex.Matcher matcher = pattern.matcher(lowerMessage);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }
}

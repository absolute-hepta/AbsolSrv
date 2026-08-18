package com.nateplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class NateCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUso: /nate <on|off|apikey|status|model|suggest>"));
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "on":
                if (!sender.hasPermission("nate.use")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo tienes permiso para usar este comando."));
                    return true;
                }
                
                if (NatePlugin.getInstance().getApiKey().isEmpty()) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPrimero debes configurar la API key con /nate apikey <tu_key>"));
                    return true;
                }
                
                NatePlugin.getInstance().setNateEnabled(true);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aNate ha sido activado. &7*sonríe timidamente* H-hola..."));
                return true;
                
            case "off":
                if (!sender.hasPermission("nate.use")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo tienes permiso para usar este comando."));
                    return true;
                }
                
                NatePlugin.getInstance().setNateEnabled(false);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNate ha sido desactivado. &7Adiós..."));
                return true;
                
            case "apikey":
                if (!sender.hasPermission("nate.use")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo tienes permiso para usar este comando."));
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUso: /nate apikey <tu_api_key>"));
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Obtén tu API key gratuita en https://openrouter.ai/"));
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Regístrate gratis y obtén tu key en Settings > API Keys"));
                    return true;
                }
                
                String key = args[1];
                NatePlugin.getInstance().setApiKey(key);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aAPI key de OpenRouter configurada correctamente. &7Gracias..."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Ahora puedes activar a Nate con /nate on"));
                return true;
                
            case "status":
                boolean enabled = NatePlugin.getInstance().isNateEnabled();
                boolean hasKey = !NatePlugin.getInstance().getApiKey().isEmpty();
                String currentModel = NatePlugin.getInstance().getModel();
                
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6=== Estado de Nate ==="));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Estado: " + (enabled ? "&aActivado" : "&cDesactivado")));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7API Key: " + (hasKey ? "&aConfigurada" : "&cNo configurada")));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Modelo actual: &f" + currentModel));
                return true;
                
            case "model":
                if (!sender.hasPermission("nate.use")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo tienes permiso para usar este comando."));
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUso: /nate model <modelo>"));
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Usa /nate model list para ver los modelos disponibles"));
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Obtén tu API key gratuita en https://openrouter.ai/"));
                    return true;
                }
                
                if (args[1].equalsIgnoreCase("list")) {
                    if (NatePlugin.getInstance().getApiKey().isEmpty()) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPrimero configura la API key con /nate apikey <tu_key>"));
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Obtén una API key gratuita en https://openrouter.ai/"));
                        return true;
                    }
                    
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Obteniendo modelos disponibles de OpenRouter..."));
                    
                    OpenAIManager.getInstance().getAvailableModels()
                        .thenAccept(models -> {
                            NatePlugin.getInstance().getServer().getScheduler().runTask(
                                NatePlugin.getInstance(),
                                () -> {
                                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6=== Modelos disponibles (OpenRouter) ==="));
                                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a✓ = Gratis | &c✗ = Pago"));
                                    
                                    // Mostrar solo los primeros 15 modelos para no saturar el chat
                                    int limit = Math.min(models.size(), 15);
                                    for (int i = 0; i < limit; i++) {
                                        String model = models.get(i);
                                        boolean isFree = isFreeModel(model);
                                        String indicator = isFree ? "&a✓" : "&c✗";
                                        String modelDisplay = indicator + " &f" + model;
                                        
                                        if (model.equals(NatePlugin.getInstance().getModel())) {
                                            modelDisplay += " &7(Actual)";
                                        }
                                        
                                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', modelDisplay));
                                    }
                                    
                                    if (models.size() > 15) {
                                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7... y " + (models.size() - 15) + " modelos más"));
                                    }
                                }
                            );
                        })
                        .exceptionally(ex -> {
                            NatePlugin.getInstance().getServer().getScheduler().runTask(
                                NatePlugin.getInstance(),
                                () -> {
                                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cError al obtener modelos: " + ex.getMessage()));
                                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Verifica que tu API key de OpenRouter sea correcta"));
                                }
                            );
                            return null;
                        });
                    return true;
                }
                
                String model = args[1];
                NatePlugin.getInstance().setModel(model);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aModelo cambiado a: &f" + model + " &7... espero que funcione bien..."));
                return true;
                
            case "suggest":
                if (!sender.hasPermission("nate.use")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo tienes permiso para usar este comando."));
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUso: /nate suggest <tu_sugerencia>"));
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Ejemplo: /nate suggest Nate debería responder más rápido"));
                    return true;
                }
                
                // Unir todos los argumentos después de "suggest"
                String suggestionText = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                String playerName = sender.getName();
                
                SuggestionManager.getInstance().addSuggestion(playerName, suggestionText);
                
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aSugerencia enviada: &f" + suggestionText));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Gracias por tu sugerencia... la tendré en cuenta..."));
                return true;
                
            default:
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUso: /nate <on|off|apikey|status|model|suggest>"));
                return true;
        }
    }
    
    private boolean isFreeModel(String model) {
        // En OpenRouter, los modelos gratuitos tienen ":free" al final
        return model.contains(":free");
    }
}

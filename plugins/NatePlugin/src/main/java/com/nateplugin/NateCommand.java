package com.nateplugin;

import org.bukkit.BanList;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class NateCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUso: /nate <on|off|self|apikey|status|model|banlist|pardon|suggest>"));
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "self":
            case "me":
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cEste comando solo puede usarse por un jugador."));
                    return true;
                }
                if (args.length < 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off") && !args[1].equalsIgnoreCase("toggle"))) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Uso: /nate self <on|off|toggle>"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("toggle")) {
                    NatePlugin.getInstance().toggleNateForPlayer(player);
                    boolean enabled = NatePlugin.getInstance().isNateEnabledForPlayer(player);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', enabled ? "&aNate está habilitado para ti." : "&cNate está deshabilitado para ti. Ya no contarás como mención."));
                    return true;
                }
                boolean enabled = args[1].equalsIgnoreCase("on");
                NatePlugin.getInstance().setNateEnabledForPlayer(player, enabled);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', enabled ? "&aNate ha sido habilitado para ti." : "&cNate ha sido deshabilitado para ti. Ya no contarás como mención."));
                return true;
            case "on":
                if (!NatePlugin.getInstance().isAdmin(sender)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cSolo los OP pueden configurar a Nate. Puedes hablar con él por /chat o mencionándole en el chat."));
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
                if (!NatePlugin.getInstance().isAdmin(sender)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cSolo los OP pueden configurar a Nate."));
                    return true;
                }
                
                NatePlugin.getInstance().setNateEnabled(false);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNate ha sido desactivado. &7Adiós..."));
                return true;
                
            case "apikey":
                if (!NatePlugin.getInstance().isAdmin(sender)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cSolo los OP pueden configurar la API de Nate."));
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
                boolean statusEnabled = NatePlugin.getInstance().isNateEnabled();
                boolean hasKey = !NatePlugin.getInstance().getApiKey().isEmpty();
                String currentModel = NatePlugin.getInstance().getModel();
                
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6=== Estado de Nate ==="));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Estado: " + (statusEnabled ? "&aActivado" : "&cDesactivado")));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7API Key: " + (hasKey ? "&aConfigurada" : "&cNo configurada")));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Modelo actual: &f" + currentModel));
                return true;
                
            case "model":
                if (!NatePlugin.getInstance().isAdmin(sender)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cSolo los OP pueden cambiar el modelo de Nate."));
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
                
            case "banlist":
                if (!NatePlugin.getInstance().isAdmin(sender)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cSolo los OP pueden revisar la lista de bans."));
                    return true;
                }
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6=== Ban list ==="));
                var entries = NatePlugin.getInstance().getServer().getBanList(BanList.Type.NAME).getBanEntries();
                if (entries.isEmpty()) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7No hay baneos activos en este momento."));
                    return true;
                }
                for (var entry : entries) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7- &f" + entry.getTarget() + " &8| &7" + entry.getReason()));
                }
                return true;
                
            case "pardon":
                if (!NatePlugin.getInstance().isAdmin(sender)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cSolo los OP pueden perdonar baneos."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUso: /nate pardon <jugador>"));
                    return true;
                }
                String target = args[1];
                var banList = NatePlugin.getInstance().getServer().getBanList(BanList.Type.NAME);
                if (banList.isBanned(target)) {
                    banList.pardon(target);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEl jugador &f" + target + " &aha sido perdonado."));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7El jugador &f" + target + " &7no tiene un baneo activo."));
                }
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
                
                String suggestionText = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                String playerName = sender.getName();
                
                SuggestionManager.getInstance().addSuggestion(playerName, suggestionText);
                
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aSugerencia enviada: &f" + suggestionText));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Gracias por tu sugerencia... la tendré en cuenta..."));
                return true;
                
            default:
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUso: /nate <on|off|apikey|status|model|banlist|pardon|suggest>"));
                return true;
        }
    }
    
    private boolean isFreeModel(String model) {
        return model.contains(":free");
    }
}

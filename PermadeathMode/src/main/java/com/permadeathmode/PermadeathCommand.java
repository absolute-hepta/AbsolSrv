package com.permadeathmode;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PermadeathCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUso: /permadeath <on|off|auto> [on|off]"));
            return true;
        }

        String subcommand = args[0].toLowerCase();
        
        if (subcommand.equals("on") || subcommand.equals("off")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUso: /permadeath <on|off>"));
                return true;
            }
            
            boolean enabled = subcommand.equals("on");
            PermadeathMode.getInstance().handleToggle(sender, enabled);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', enabled ? "&aPermadeath activado." : "&cPermadeath desactivado."));
            return true;
        }
        
        if (subcommand.equals("auto")) {
            if (args.length != 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUso: /permadeath auto <on|off>"));
                return true;
            }
            
            boolean autoEnabled = args[1].equalsIgnoreCase("on");
            PermadeathMode.getInstance().setAutoWeatherEnabled(autoEnabled);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', autoEnabled ? "&aActivación automática por tormenta habilitada." : "&cActivación automática por tormenta deshabilitada."));
            return true;
        }
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUso: /permadeath <on|off|auto> [on|off]"));
        return true;
    }
}

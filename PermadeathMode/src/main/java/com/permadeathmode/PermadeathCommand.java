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
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /permadeath <on|off|auto|lang> [on|off|es|en]"));
            return true;
        }

        String subcommand = args[0].toLowerCase();
        
        if (subcommand.equals("on") || subcommand.equals("off")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /permadeath <on|off>"));
                return true;
            }
            
            boolean enabled = subcommand.equals("on");
            PermadeathMode.getInstance().handleToggle(sender, enabled);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', enabled ? "&aPermadeath activated." : "&cPermadeath deactivated."));
            return true;
        }
        
        if (subcommand.equals("auto")) {
            if (args.length != 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /permadeath auto <on|off>"));
                return true;
            }
            
            boolean autoEnabled = args[1].equalsIgnoreCase("on");
            PermadeathMode.getInstance().setAutoWeatherEnabled(autoEnabled);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', autoEnabled ? "&aAutomatic storm activation enabled." : "&cAutomatic storm activation disabled."));
            return true;
        }
        
        if (subcommand.equals("lang")) {
            if (args.length != 2 || (!args[1].equalsIgnoreCase("es") && !args[1].equalsIgnoreCase("en"))) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /permadeath lang <es|en>"));
                return true;
            }
            
            String lang = args[1].toLowerCase();
            PermadeathMode.getInstance().setLanguage(lang);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', lang.equals("es") ? "&aIdioma cambiado a español." : "&aLanguage changed to English."));
            return true;
        }
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: /permadeath <on|off|auto|lang> [on|off|es|en]"));
        return true;
    }
}

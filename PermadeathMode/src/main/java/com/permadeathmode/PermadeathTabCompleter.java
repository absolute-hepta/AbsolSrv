package com.permadeathmode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class PermadeathTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("on");
            completions.add("off");
            completions.add("auto");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("auto")) {
            completions.add("on");
            completions.add("off");
        }

        return completions;
    }
}

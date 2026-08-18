package com.nateplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NateTabCompleter implements TabCompleter {
    
    private static final String[] SUBCOMMANDS = {"on", "off", "apikey", "status", "model", "suggest"};
    private static final String[] MODEL_SUBCOMMANDS = {"list"};
    private static final String[] POPULAR_FREE_MODELS = {
        "meta-llama/llama-3.3-70b-instruct:free",
        "deepseek/deepseek-chat-v3-0324:free", 
        "google/gemma-3-27b-it:free",
        "meta-llama/llama-4-scout:free",
        "openai/gpt-oss-20b:free"
    };
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("nate")) {
            if (args.length == 1) {
                // Sugerir subcomandos principales
                String partial = args[0].toLowerCase();
                completions = StringUtil.copyPartialMatches(partial, Arrays.asList(SUBCOMMANDS), new ArrayList<>());
            } else if (args.length == 2 && args[0].equalsIgnoreCase("model")) {
                // Sugerir subcomandos de model y modelos populares
                String partial = args[1].toLowerCase();
                
                List<String> allOptions = new ArrayList<>();
                allOptions.addAll(Arrays.asList(MODEL_SUBCOMMANDS));
                allOptions.addAll(Arrays.asList(POPULAR_FREE_MODELS));
                
                completions = StringUtil.copyPartialMatches(partial, allOptions, new ArrayList<>());
            }
        } else if (command.getName().equalsIgnoreCase("chat")) {
            // Para el comando /chat, no hay sugerencias específicas (es libre)
            // Pero podemos dar algunos ejemplos
            if (args.length == 1) {
                String[] examples = {
                    "Hola Nate, ¿cómo estás?",
                    "¿Qué puedo construir?",
                    "Necesito ayuda con...",
                    "Cuéntame un chiste"
                };
                String partial = args[0].toLowerCase();
                completions = StringUtil.copyPartialMatches(partial, Arrays.asList(examples), new ArrayList<>());
            }
        }
        
        return completions;
    }
}

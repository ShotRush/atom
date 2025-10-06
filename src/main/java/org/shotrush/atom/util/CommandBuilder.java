package org.shotrush.atom.util;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CommandBuilder {
    
    public static void addSimpleCommand(BaseCommand commandClass, String name, String description, Consumer<Player> action) {
    }
    
    public static void addAdminCommand(BaseCommand commandClass, String name, String description, Consumer<CommandSender> action) {
    }
    
    public static String formatSuccess(String message) {
        return "§a" + message;
    }
    
    public static String formatError(String message) {
        return "§c" + message;
    }
    
    public static String formatInfo(String message) {
        return "§e" + message;
    }
    
    public static String formatHighlight(String message) {
        return "§6§l" + message;
    }
}

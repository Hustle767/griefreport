package com.jamplifier.griefreport.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public class MessageUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    public static void send(CommandSender sender, String miniMessageString) {
        Component component = MINI.deserialize(miniMessageString);
        sender.sendMessage(component);
    }
}

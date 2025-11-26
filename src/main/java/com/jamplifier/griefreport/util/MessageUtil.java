package com.jamplifier.griefreport.util;

import com.jamplifier.griefreport.GriefReportPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class MessageUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private MessageUtil() {
    }

    public static void send(CommandSender sender, String key, TagResolver... extraResolvers) {
        GriefReportPlugin plugin = GriefReportPlugin.getInstance();
        String path = "messages." + key;
        String raw = plugin.getConfig().getString(path);

        if (raw == null) {
            // Fallback to key name if missing
            raw = "<red>Missing message key:</red> " + key;
        }

        // Prefix support for MiniMessage messages
        String prefix = plugin.getConfig().getString("messages.prefix",
                "<gray>[<red>GriefReport</red>]</gray> ");
        TagResolver prefixResolver = Placeholder.parsed("prefix", prefix);

        TagResolver resolver = TagResolver.builder()
                .resolver(prefixResolver)
                .resolvers(extraResolvers)
                .build();

        Component component;

        if (raw.contains("<")) {
            // Treat as MiniMessage
            component = MINI.deserialize(raw, resolver);
        } else if (raw.contains("&")) {
            // Treat as legacy & color codes (no MiniMessage placeholders here)
            component = LEGACY.deserialize(raw);
        } else {
            // Plain text as MiniMessage (so we can still use placeholders)
            component = MINI.deserialize(raw, resolver);
        }

        sender.sendMessage(component);
    }
}

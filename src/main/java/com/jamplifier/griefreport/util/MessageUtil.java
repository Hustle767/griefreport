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
        sender.sendMessage(buildComponent(key, extraResolvers));
    }

    public static Component buildComponent(String key, TagResolver... extraResolvers) {
        GriefReportPlugin plugin = GriefReportPlugin.getInstance();
        String path = "messages." + key;
        String raw = plugin.getConfig().getString(path);

        if (raw == null) {
            raw = "<red>Missing message key:</red> " + key;
        }

        String prefix = plugin.getConfig().getString(
                "messages.prefix",
                "<gray>[<red>GriefReport</red>]</gray> "
        );
        TagResolver prefixResolver = Placeholder.parsed("prefix", prefix);

        TagResolver resolver = TagResolver.builder()
                .resolver(prefixResolver)
                .resolvers(extraResolvers)
                .build();

        if (raw.contains("<")) {
            // Treat as MiniMessage
            return MINI.deserialize(raw, resolver);
        } else if (raw.contains("&")) {
            // Treat as legacy & codes (no MiniMessage placeholders)
            return LEGACY.deserialize(raw);
        } else {
            // Plain text but still run through MiniMessage for placeholders
            return MINI.deserialize(raw, resolver);
        }
    }
}

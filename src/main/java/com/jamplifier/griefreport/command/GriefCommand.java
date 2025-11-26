package com.jamplifier.griefreport.command;

import com.jamplifier.griefreport.GriefReportPlugin;
import com.jamplifier.griefreport.manager.GriefReportManager;
import com.jamplifier.griefreport.model.GriefReport;
import com.jamplifier.griefreport.model.GriefReportStatus;
import com.jamplifier.griefreport.util.MessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GriefCommand implements CommandExecutor, TabCompleter {

    private final GriefReportPlugin plugin;
    private final GriefReportManager reportManager;
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withLocale(Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());

    public GriefCommand(GriefReportPlugin plugin) {
        this.plugin = plugin;
        this.reportManager = plugin.getReportManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            MessageUtil.send(sender, "usage-main");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "report" -> handleReport(sender, args);
            case "status" -> handleStatus(sender, args);
            case "close" -> handleClose(sender, args);
            case "teleport" -> handleTeleport(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            default -> MessageUtil.send(sender, "usage-main");
        }

        return true;
    }

    // /grief report [optional message]
    private void handleReport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can report griefs.");
            return;
        }

        if (!player.hasPermission("griefreport.use")) {
            MessageUtil.send(player, "no-permission");
            return;
        }

        String message = "";
        if (args.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) sb.append(" ");
                sb.append(args[i]);
            }
            message = sb.toString();
        }

        Location loc = player.getLocation();
        GriefReport report = reportManager.createReport(player.getUniqueId(), loc, message);

        TagResolver[] placeholders = new TagResolver[]{
                Placeholder.unparsed("id", String.valueOf(report.getId())),
                Placeholder.unparsed("message", message.isEmpty() ? "none" : message)
        };

        if (message.isEmpty()) {
            MessageUtil.send(player, "report-created", placeholders);
        } else {
            MessageUtil.send(player, "report-created-with-message", placeholders);
        }

        // In-game staff alert
        if (plugin.getConfig().getBoolean("in-game-staff-alerts", true)) {
            TagResolver[] staffResolvers = new TagResolver[]{
                    Placeholder.unparsed("id", String.valueOf(report.getId())),
                    Placeholder.unparsed("player", player.getName()),
                    Placeholder.unparsed("world", report.getWorldName()),
                    Placeholder.unparsed("x", String.valueOf(loc.getBlockX())),
                    Placeholder.unparsed("y", String.valueOf(loc.getBlockY())),
                    Placeholder.unparsed("z", String.valueOf(loc.getBlockZ())),
                    Placeholder.unparsed("message", message.isEmpty() ? "no message" : message)
            };

            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("griefreport.staff"))
                    .forEach(p -> MessageUtil.send(p, "staff-report-alert", staffResolvers));
        }

        // Discord
        plugin.getDiscordNotifier().sendNewReport(report);
    }

    // /grief status [id]
    private void handleStatus(CommandSender sender, String[] args) {

        // If ID is provided: show that specific report
        if (args.length >= 2) {
            Integer id = parseIdOrError(sender, args[1]);
            if (id == null) return;

            GriefReport report = reportManager.getReport(id);
            if (report == null) {
                MessageUtil.send(sender, "report-not-found",
                        Placeholder.unparsed("id", String.valueOf(id)));
                return;
            }

            sendStatus(sender, report);
            return;
        }

        // No ID -> if player: use their last report
        if (sender instanceof Player player) {
            var opt = reportManager.getLastReportFor(player.getUniqueId());
            if (opt.isEmpty()) {
                MessageUtil.send(sender, "status-no-self");
                return;
            }
            sendStatus(sender, opt.get());
        } else {
            // Console must specify an ID
            MessageUtil.send(sender, "status-console-requires-id");
        }
    }

    private void sendStatus(CommandSender sender, GriefReport report) {
        Location loc = report.toLocation();
        int x = loc != null ? loc.getBlockX() : 0;
        int y = loc != null ? loc.getBlockY() : 0;
        int z = loc != null ? loc.getBlockZ() : 0;

        String statusKey;
        TagResolver statusResolver;

        if (report.getStatus() == GriefReportStatus.OPEN) {
            statusKey = "status-open";
            statusResolver = Placeholder.parsed("status", "<green>OPEN</green>");
        } else if (report.getStatus() == GriefReportStatus.IN_PROGRESS) {
            statusKey = "status-in-progress";
            statusResolver = Placeholder.parsed("status", "<gold>IN PROGRESS</gold>");
        } else {
            statusKey = "status-closed";
            String closedByName = report.getClosedBy() != null
                    ? Bukkit.getOfflinePlayer(report.getClosedBy()).getName()
                    : "unknown";
            String closedAt = report.getClosedAt() != null
                    ? dateFormatter.format(report.getClosedAt())
                    : "unknown";

            TagResolver closedDetailResolver = TagResolver.builder()
                    .resolver(Placeholder.unparsed("world", report.getWorldName()))
                    .resolver(Placeholder.unparsed("x", String.valueOf(x)))
                    .resolver(Placeholder.unparsed("y", String.valueOf(y)))
                    .resolver(Placeholder.unparsed("z", String.valueOf(z)))
                    .resolver(Placeholder.unparsed("closed_by", closedByName == null ? "unknown" : closedByName))
                    .resolver(Placeholder.unparsed("closed_at", closedAt))
                    .build();

            // header line
            MessageUtil.send(sender, "status-header",
                    Placeholder.unparsed("id", String.valueOf(report.getId())),
                    Placeholder.parsed("status", "<red>CLOSED</red>"));
            // details line
            MessageUtil.send(sender, statusKey, closedDetailResolver);
            return;
        }

        // OPEN or IN_PROGRESS
        TagResolver commonResolvers = TagResolver.builder()
                .resolver(Placeholder.unparsed("id", String.valueOf(report.getId())))
                .resolver(statusResolver)
                .build();

        MessageUtil.send(sender, "status-header", commonResolvers);

        MessageUtil.send(sender, statusKey,
                Placeholder.unparsed("world", report.getWorldName()),
                Placeholder.unparsed("x", String.valueOf(x)),
                Placeholder.unparsed("y", String.valueOf(y)),
                Placeholder.unparsed("z", String.valueOf(z)));
    }

    // /grief close <id>
    private void handleClose(CommandSender sender, String[] args) {
        if (!sender.hasPermission("griefreport.staff")) {
            MessageUtil.send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(sender, "usage-main");
            return;
        }

        Integer id = parseIdOrError(sender, args[1]);
        if (id == null) return;

        GriefReport report = reportManager.getReport(id);
        if (report == null) {
            MessageUtil.send(sender, "report-not-found",
                    Placeholder.unparsed("id", String.valueOf(id)));
            return;
        }

        if (report.getStatus() == GriefReportStatus.CLOSED) {
            MessageUtil.send(sender, "close-already-closed",
                    Placeholder.unparsed("id", String.valueOf(id)));
            return;
        }

        // mark closed
        if (sender instanceof Player p) {
            report.close(p.getUniqueId());
        } else {
            report.close(null);
        }

        reportManager.save(report);

        MessageUtil.send(sender, "close-success",
                Placeholder.unparsed("id", String.valueOf(id)));

        // Broadcast to staff
        String staffName = sender.getName();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("griefreport.staff")) continue;
            MessageUtil.send(online, "close-broadcast",
                    Placeholder.unparsed("id", String.valueOf(id)),
                    Placeholder.unparsed("staff", staffName));
        }

        // Discord
        plugin.getDiscordNotifier().sendReportClosed(report);
    }

    // /grief teleport <id>
    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "teleport-only-player");
            return;
        }

        if (!player.hasPermission("griefreport.teleport")) {
            MessageUtil.send(player, "no-permission");
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "usage-main");
            return;
        }

        Integer id = parseIdOrError(player, args[1]);
        if (id == null) return;

        GriefReport report = reportManager.getReport(id);
        if (report == null) {
            MessageUtil.send(player, "report-not-found",
                    Placeholder.unparsed("id", String.valueOf(id)));
            return;
        }

        Location loc = report.toLocation();
        if (loc == null) {
            MessageUtil.send(player, "teleport-world-missing",
                    Placeholder.unparsed("world", report.getWorldName()));
            return;
        }

        player.teleport(loc);
        MessageUtil.send(player, "teleported",
                Placeholder.unparsed("id", String.valueOf(id)));
    }

    // /grief list
    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("griefreport.staff")) {
            MessageUtil.send(sender, "no-permission");
            return;
        }

        var open = reportManager.getOpenReports();
        if (open.isEmpty()) {
            MessageUtil.send(sender, "list-empty");
            return;
        }

        MessageUtil.send(sender, "list-header");

        for (GriefReport report : open) {
            Location loc = report.toLocation();
            int x = loc != null ? loc.getBlockX() : 0;
            int y = loc != null ? loc.getBlockY() : 0;
            int z = loc != null ? loc.getBlockZ() : 0;

            String statusStr = switch (report.getStatus()) {
                case OPEN -> "OPEN";
                case IN_PROGRESS -> "IN_PROGRESS";
                case CLOSED -> "CLOSED";
            };

            String playerName = Bukkit.getOfflinePlayer(report.getReporter()).getName();
            if (playerName == null) playerName = "unknown";

            String message = report.getMessage();
            if (message.length() > 40) {
                message = message.substring(0, 37) + "...";
            }
            if (message.isEmpty()) message = "no message";

            MessageUtil.send(sender, "list-entry",
                    Placeholder.unparsed("id", String.valueOf(report.getId())),
                    Placeholder.unparsed("status", statusStr),
                    Placeholder.unparsed("world", report.getWorldName()),
                    Placeholder.unparsed("x", String.valueOf(x)),
                    Placeholder.unparsed("y", String.valueOf(y)),
                    Placeholder.unparsed("z", String.valueOf(z)),
                    Placeholder.unparsed("player", playerName),
                    Placeholder.unparsed("message", message));
        }
    }

    private Integer parseIdOrError(CommandSender sender, String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            MessageUtil.send(sender, "not-a-number",
                    Placeholder.unparsed("input", input));
            return null;
        }
    }

    
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("griefreport.reload")) {
            MessageUtil.send(sender, "no-permission");
            return;
        }

        MessageUtil.send(sender, "reload-start");
        plugin.reloadPluginConfig();
        MessageUtil.send(sender, "reload-done");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("report");
            completions.add("status");
            if (sender.hasPermission("griefreport.staff")) {
                completions.add("close");
                completions.add("teleport");
                completions.add("list");
            }
            if (sender.hasPermission("griefreport.reload")) {
                completions.add("reload");
            }
            return completions;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("close")
                || args[0].equalsIgnoreCase("teleport")
                || args[0].equalsIgnoreCase("status"))) {
            // Suggest open report IDs
            for (GriefReport report : reportManager.getOpenReports()) {
                completions.add(String.valueOf(report.getId()));
            }
        }

        return completions;
    }
}

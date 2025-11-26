package com.jamplifier.griefreport.command;

import com.jamplifier.griefreport.GriefReportPlugin;
import com.jamplifier.griefreport.manager.GriefReportManager;
import com.jamplifier.griefreport.model.GriefReport;
import com.jamplifier.griefreport.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GriefCommand implements CommandExecutor, TabCompleter {

    private final GriefReportPlugin plugin;
    private final GriefReportManager reportManager;

    public GriefCommand(GriefReportPlugin plugin) {
        this.plugin = plugin;
        this.reportManager = plugin.getReportManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            // later: send help
            sender.sendMessage("§eUsage: /grief <report|status|close|teleport|list>");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "report":
                handleReport(sender, args);
                break;
            case "status":
                handleStatus(sender, args);
                break;
            case "close":
                handleClose(sender, args);
                break;
            case "teleport":
                handleTeleport(sender, args);
                break;
            case "list":
                handleList(sender, args);
                break;
            default:
                sender.sendMessage("§eUsage: /grief <report|status|close|teleport|list>");
                break;
        }

        return true;
    }

    private void handleReport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can report griefs.");
            return;
        }

        // join optional message after /grief report
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

        // In-game confirm
        player.sendMessage("§aGrief reported! ID: §e#" + report.getId());

        // TODO: send staff alert + Discord webhook
        plugin.getDiscordNotifier().sendNewReport(report);
    }

    private void handleStatus(CommandSender sender, String[] args) {
        // later: show last report or take an ID
        sender.sendMessage("§7/grief status not fully implemented yet.");
    }

    private void handleClose(CommandSender sender, String[] args) {
        sender.sendMessage("§7/grief close not fully implemented yet.");
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        sender.sendMessage("§7/grief teleport not fully implemented yet.");
    }

    private void handleList(CommandSender sender, String[] args) {
        sender.sendMessage("§7/grief list not fully implemented yet.");
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
        }

        return completions;
    }
}

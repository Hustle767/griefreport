package com.jamplifier.griefreport.storage;

import com.jamplifier.griefreport.GriefReportPlugin;
import com.jamplifier.griefreport.model.GriefReport;
import com.jamplifier.griefreport.model.GriefReportStatus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class YamlReportStorage implements ReportStorage {

    private final GriefReportPlugin plugin;
    private final File file;
    private final YamlConfiguration config;

    public YamlReportStorage(GriefReportPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "reports.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void loadAll(Map<Integer, GriefReport> target) {
        for (String key : config.getKeys(false)) {
            int id = Integer.parseInt(key);
            String reporterStr = config.getString(key + ".reporter");
            String world = config.getString(key + ".world");
            double x = config.getDouble(key + ".x");
            double y = config.getDouble(key + ".y");
            double z = config.getDouble(key + ".z");
            String message = config.getString(key + ".message", "");

            org.bukkit.World bWorld = Bukkit.getWorld(world);
            if (bWorld == null) continue;

            Location loc = new Location(bWorld, x, y, z);
            GriefReport report = new GriefReport(id, UUID.fromString(reporterStr), loc, message);

            String statusStr = config.getString(key + ".status", "OPEN");
            report.setStatus(GriefReportStatus.valueOf(statusStr));

            // you can later re-add createdAt / closedAt / closedBy etc.

            target.put(id, report);
        }
    }

    @Override
    public void save(GriefReport report) {
        String key = String.valueOf(report.getId());
        config.set(key + ".reporter", report.getReporter().toString());
        config.set(key + ".world", report.getWorldName());
        if (report.toLocation() != null) {
            config.set(key + ".x", report.toLocation().getX());
            config.set(key + ".y", report.toLocation().getY());
            config.set(key + ".z", report.toLocation().getZ());
        }
        config.set(key + ".message", report.getMessage());
        config.set(key + ".status", report.getStatus().name());

        saveFile();
    }

    @Override
    public void saveAll(Collection<GriefReport> reports) {
        config.getKeys(false).forEach(k -> config.set(k, null));
        for (GriefReport report : reports) {
            save(report);
        }
        saveFile();
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

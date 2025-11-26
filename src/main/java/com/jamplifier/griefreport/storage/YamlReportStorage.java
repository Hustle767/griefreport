package com.jamplifier.griefreport.storage;

import com.jamplifier.griefreport.GriefReportPlugin;
import com.jamplifier.griefreport.model.GriefReport;
import com.jamplifier.griefreport.model.GriefReportStatus;
import org.bukkit.Bukkit;
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
            try {
                int id = Integer.parseInt(key);

                String reporterStr = config.getString(key + ".reporter");
                String reporterName = config.getString(key + ".reporterName");
                String worldName = config.getString(key + ".world");
                double x = config.getDouble(key + ".x");
                double y = config.getDouble(key + ".y");
                double z = config.getDouble(key + ".z");
                float yaw = (float) config.getDouble(key + ".yaw", 0.0D);
                float pitch = (float) config.getDouble(key + ".pitch", 0.0D);
                String message = config.getString(key + ".message", "");

                if (reporterStr == null || worldName == null) {
                    continue;
                }

                if (reporterName == null) {
                    // Backwards compat: resolve once on main thread
                    reporterName = Bukkit.getOfflinePlayer(UUID.fromString(reporterStr)).getName();
                }

                GriefReport report = new GriefReport(
                        id,
                        UUID.fromString(reporterStr),
                        reporterName,
                        worldName,
                        x,
                        y,
                        z,
                        yaw,
                        pitch,
                        message
                );

                String statusStr = config.getString(key + ".status", "OPEN");
                report.setStatus(GriefReportStatus.valueOf(statusStr));

                String createdAtStr = config.getString(key + ".createdAt");
                if (createdAtStr != null) {
                    report.setCreatedAt(Instant.parse(createdAtStr));
                }

                String closedByStr = config.getString(key + ".closedBy");
                if (closedByStr != null) {
                    report.setClosedBy(UUID.fromString(closedByStr));
                }

                String closedByName = config.getString(key + ".closedByName");
                report.setClosedByName(closedByName);

                String closedAtStr = config.getString(key + ".closedAt");
                if (closedAtStr != null) {
                    report.setClosedAt(Instant.parse(closedAtStr));
                }

                target.put(id, report);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load grief report " + key + ": " + ex.getMessage());
            }
        }
    }

    @Override
    public void save(GriefReport report) {
        String key = String.valueOf(report.getId());

        config.set(key + ".reporter", report.getReporter().toString());
        config.set(key + ".reporterName", report.getReporterName());
        config.set(key + ".world", report.getWorldName());
        config.set(key + ".x", report.getX());
        config.set(key + ".y", report.getY());
        config.set(key + ".z", report.getZ());
        config.set(key + ".yaw", report.getYaw());
        config.set(key + ".pitch", report.getPitch());

        config.set(key + ".message", report.getMessage());
        config.set(key + ".status", report.getStatus().name());

        if (report.getCreatedAt() != null) {
            config.set(key + ".createdAt", report.getCreatedAt().toString());
        }
        if (report.getClosedBy() != null) {
            config.set(key + ".closedBy", report.getClosedBy().toString());
        }
        if (report.getClosedByName() != null) {
            config.set(key + ".closedByName", report.getClosedByName());
        }
        if (report.getClosedAt() != null) {
            config.set(key + ".closedAt", report.getClosedAt().toString());
        }

        saveFile();
    }

    @Override
    public void saveAll(Collection<GriefReport> reports) {
        for (GriefReport report : reports) {
            save(report);
        }
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.jamplifier.griefreport.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.time.Instant;
import java.util.UUID;

public class GriefReport {

    private final int id;
    private final UUID reporter;
    private final String reporterName;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final String message;

    private GriefReportStatus status;
    private Instant createdAt;
    private UUID closedBy;
    private String closedByName;
    private Instant closedAt;

    // Used when creating a fresh report from a Location
    public GriefReport(int id, UUID reporter, String reporterName, Location location, String message) {
        this(
                id,
                reporter,
                reporterName,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                message
        );
    }

    // Used when loading from storage
    public GriefReport(
            int id,
            UUID reporter,
            String reporterName,
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            String message
    ) {
        this.id = id;
        this.reporter = reporter;
        this.reporterName = reporterName == null ? "Unknown" : reporterName;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.message = message == null ? "" : message;
        this.status = GriefReportStatus.OPEN;
        this.createdAt = Instant.now();
    }

    public int getId() {
        return id;
    }

    public UUID getReporter() {
        return reporter;
    }

    public String getReporterName() {
        return reporterName;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    public String getMessage() {
        return message;
    }

    public GriefReportStatus getStatus() {
        return status;
    }

    public void setStatus(GriefReportStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(UUID closedBy) {
        this.closedBy = closedBy;
    }

    public String getClosedByName() {
        return closedByName;
    }

    public void setClosedByName(String closedByName) {
        this.closedByName = closedByName;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public void close(UUID staffUuid, String staffName) {
        this.status = GriefReportStatus.CLOSED;
        this.closedBy = staffUuid;
        this.closedByName = staffName == null ? "Unknown" : staffName;
        this.closedAt = Instant.now();
    }
}

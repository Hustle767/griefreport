package com.jamplifier.griefreport.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.time.Instant;
import java.util.UUID;

public class GriefReport {

    private final int id;
    private final UUID reporter;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final String message;

    private GriefReportStatus status;
    private Instant createdAt;
    private UUID closedBy;
    private Instant closedAt;

    public GriefReport(int id, UUID reporter, Location location, String message) {
        this.id = id;
        this.reporter = reporter;
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.message = message;
        this.status = GriefReportStatus.OPEN;
        this.createdAt = Instant.now();
    }

    public int getId() {
        return id;
    }

    public UUID getReporter() {
        return reporter;
    }

    public String getWorldName() {
        return worldName;
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z);
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

    public UUID getClosedBy() {
        return closedBy;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void close(UUID staffUuid) {
        this.status = GriefReportStatus.CLOSED;
        this.closedBy = staffUuid;
        this.closedAt = Instant.now();
    }
}

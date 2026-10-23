package com.denizenscript.denizen.utilities.entity;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.utilities.packets.NetworkInterceptHelper;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class EntityAttachmentHelper {

    public static volatile HashMap<UUID, PlayerAttachMap> attachedEntityToData = new HashMap<>();
    public static volatile HashMap<UUID, EntityAttachedToMap> toEntityToData = new HashMap<>();

    private static final Object writeLock = new Object();

    static <V> HashMap<UUID, V> mapWith(HashMap<UUID, V> source, UUID key, V value) {
        HashMap<UUID, V> replacement = new HashMap<>(source);
        replacement.put(key, value);
        return replacement;
    }

    static <V> HashMap<UUID, V> mapWithout(HashMap<UUID, V> source, UUID key) {
        HashMap<UUID, V> replacement = new HashMap<>(source);
        replacement.remove(key);
        return replacement;
    }

    public static class AttachmentData {

        public EntityTag attached, to;

        public boolean offsetRelative;

        public float yawAngleOffset, pitchAngleOffset;

        public Location positionalOffset;

        public HashMap<UUID, Vector> visiblePositions = new HashMap<>();

        public boolean syncServer;

        public boolean noRotate, noPitch;

        public BukkitTask checkTask;

        public UUID forPlayer;

        public Vector fixedForOffset(Vector offset, float yaw, float pitch) {
            if (offsetRelative) {
                return offset.clone().add(EntityAttachmentHelper.fixOffset(positionalOffset.toVector(), -yaw + yawAngleOffset, pitch + pitchAngleOffset));
            }
            else {
                return offset.clone().add(positionalOffset.toVector());
            }
        }

        public void doServerSync() {
            Location goal = to.getLocation();
            if (positionalOffset != null) {
                goal = fixedForOffset(goal.toVector(), goal.getYaw(), goal.getPitch()).toLocation(goal.getWorld());
            }
            if (noRotate) {
                Location attachLoc = attached.getLocation();
                goal.setYaw(attachLoc.getYaw());
                goal.setPitch(attachLoc.getPitch());
            }
            else if (noPitch) {
                goal.setPitch(attached.getLocation().getPitch());
            }
            attached.teleport(goal);
        }

        public void startTask() {
            if (checkTask != null) {
                checkTask.cancel();
            }
            BukkitRunnable runnable = new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (!attached.isValid() || !to.isValid()) {
                        cancelAndRemove();
                        return;
                    }
                    if (ticks++ >= 20 * 10) { // Run a forcetele every 10 seconds to guarantee sync
                        visiblePositions.clear();
                        ticks = 0;
                    }
                    if (syncServer) {
                        doServerSync();
                    }
                }
            };
            runnable.run();
            checkTask = runnable.runTaskTimer(Denizen.getInstance(), 1, 1);
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Run a forcetele one second later just to guarantee sync for lagging clients
                    visiblePositions.clear();
                }
            }.runTaskLater(Denizen.getInstance(), 20);
        }

        public void removeFrom(PlayerAttachMap map) {
            if (map != null) {
                if (forPlayer == null) {
                    map.everyoneAttachment = null;
                }
                else {
                    if (map.playerToAttachment != null) {
                        map.playerToAttachment = mapWithout(map.playerToAttachment, forPlayer);
                    }
                    map.autoNull();
                }
            }
        }

        public void cancelAndRemove() {
            if (checkTask != null) {
                checkTask.cancel();
            }
            checkTask = null;
            synchronized (writeLock) {
                EntityAttachedToMap map = toEntityToData.get(to.getUUID());
                if (map != null) {
                    PlayerAttachMap subMap = map.attachedToMap.get(attached.getUUID());
                    if (subMap != null) {
                        removeFrom(subMap);
                        if (subMap.everyoneAttachment == null && subMap.playerToAttachment == null) {
                            map.attachedToMap = mapWithout(map.attachedToMap, attached.getUUID());
                            if (map.attachedToMap.isEmpty()) {
                                toEntityToData = mapWithout(toEntityToData, to.getUUID());
                            }
                        }
                    }
                }
                PlayerAttachMap attachMap = attachedEntityToData.get(attached.getUUID());
                if (attachMap != null) {
                    removeFrom(attachMap);
                    if (attachMap.everyoneAttachment == null && attachMap.playerToAttachment == null) {
                        attachedEntityToData = mapWithout(attachedEntityToData, attached.getUUID());
                    }
                }
            }
        }
    }

    public static class PlayerAttachMap {

        public EntityTag attached;

        public volatile AttachmentData everyoneAttachment;

        public volatile HashMap<UUID, AttachmentData> playerToAttachment;

        public AttachmentData getAttachment(UUID player) {
            if (playerToAttachment == null) {
                return everyoneAttachment;
            }
            AttachmentData data = playerToAttachment.get(player);
            if (data != null) {
                return data;
            }
            return everyoneAttachment;
        }

        public void cancelAll() {
            if (everyoneAttachment != null) {
                everyoneAttachment.cancelAndRemove();
            }
            if (playerToAttachment != null) {
                for (AttachmentData attachment : new HashSet<>(playerToAttachment.values())) {
                    attachment.cancelAndRemove();
                }
            }
        }

        public void autoNull() {
            if (playerToAttachment != null && playerToAttachment.isEmpty()) {
                playerToAttachment = null;
            }
        }
    }

    public static class EntityAttachedToMap {

        public volatile HashMap<UUID, PlayerAttachMap> attachedToMap = new HashMap<>();
    }

    public static byte adaptedCompressedAngle(byte angle, float offset) {
        float angleF = ((float) angle) * (360F / 256F);
        return compressAngle(angleF + offset);
    }

    public static float normalizeAngle(float angle) {
        angle %= 360;
        if (angle > 180) {
            angle -= 360;
        }
        if (angle < -180) {
            angle += 360;
        }
        return angle;
    }

    public static byte compressAngle(float angle) {
        return (byte)((int)(normalizeAngle(angle) * (256F / 360F)));
    }

    public static Vector fixOffset(Vector offset, double yaw, double pitch) {
        yaw = Math.toRadians(yaw);
        pitch = Math.toRadians(pitch);
        Vector offsetPatched = offset.clone();
        // x rotation
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double y1 = (offsetPatched.getY() * cosPitch) - (offsetPatched.getZ() * sinPitch);
        double z1 = (offsetPatched.getY() * sinPitch) + (offsetPatched.getZ() * cosPitch);
        offsetPatched.setY(y1);
        offsetPatched.setZ(z1);
        // y rotation
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double x2 = (offsetPatched.getX() * cosYaw) + (offsetPatched.getZ() * sinYaw);
        double z2 = (offsetPatched.getX() * -sinYaw) + (offsetPatched.getZ() * cosYaw);
        offsetPatched.setX(x2);
        offsetPatched.setZ(z2);
        return offsetPatched;
    }

    public static void removeAttachment(UUID attachedId, UUID forPlayer) {
        synchronized (writeLock) {
            PlayerAttachMap map = attachedEntityToData.get(attachedId);
            if (map == null) {
                return;
            }
            if (forPlayer == null) {
                attachedEntityToData = mapWithout(attachedEntityToData, attachedId);
                map.cancelAll();
            }
            else {
                if (map.playerToAttachment != null) {
                    AttachmentData data = map.playerToAttachment.get(forPlayer);
                    if (data != null) {
                        data.cancelAndRemove();
                    }
                }
            }
        }
    }

    public static HashMap<UUID, PlayerAttachMap> addPlayerAttachMap(HashMap<UUID, PlayerAttachMap> target, AttachmentData attachment) {
        PlayerAttachMap map = target.get(attachment.attached.getUUID());
        boolean freshMap = map == null;
        if (freshMap) {
            map = new PlayerAttachMap();
            map.attached = attachment.attached;
        }
        if (attachment.forPlayer == null) {
            map.everyoneAttachment = attachment;
        }
        else {
            map.playerToAttachment = mapWith(map.playerToAttachment == null ? new HashMap<>() : map.playerToAttachment, attachment.forPlayer, attachment);
        }
        if (freshMap) {
            target = mapWith(target, attachment.attached.getUUID(), map);
        }
        return target;
    }

    public static void registerAttachment(AttachmentData attachment) {
        NetworkInterceptHelper.enable();
        removeAttachment(attachment.attached.getUUID(), attachment.forPlayer);
        attachment.startTask();
        synchronized (writeLock) {
            EntityAttachedToMap toMap = toEntityToData.get(attachment.to.getUUID());
            boolean freshToMap = toMap == null;
            if (freshToMap) {
                toMap = new EntityAttachedToMap();
            }
            attachedEntityToData = addPlayerAttachMap(attachedEntityToData, attachment);
            toMap.attachedToMap = addPlayerAttachMap(toMap.attachedToMap, attachment);
            if (freshToMap) {
                toEntityToData = mapWith(toEntityToData, attachment.to.getUUID(), toMap);
            }
        }
        if (attachment.syncServer) {
            attachment.doServerSync();
        }
    }

    public static void forceAttachMove(EntityTag attached, EntityTag to, Vector offset, boolean matchRotation) {
        removeAttachment(attached.getUUID(), null);
        if (to == null) {
            return;
        }
        AttachmentData data = new AttachmentData();
        data.attached = attached;
        data.to = to;
        data.positionalOffset = offset == null ? null : offset.toLocation(null);
        data.offsetRelative = matchRotation;
        registerAttachment(data);
    }

    public static boolean denyOriginalPacketSend(UUID player, UUID entity) {
        PlayerAttachMap attached = EntityAttachmentHelper.attachedEntityToData.get(entity);
        if (attached == null) {
            return false;
        }
        AttachmentData data = attached.getAttachment(player);
        if (data == null) {
            return false;
        }
        if (data.to.getUUID().equals(player)) {
            return false;
        }
        return true;
    }
}

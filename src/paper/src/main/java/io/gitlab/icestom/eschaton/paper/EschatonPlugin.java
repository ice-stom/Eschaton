package io.gitlab.icestom.eschaton.paper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import io.github.openboatutils.protocol.OBUPacket;
import io.github.openboatutils.protocol.channels.OBUSettingsPacket;
import io.github.openboatutils.protocol.impl.DataOutputStreamWriter;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.gitlab.icestom.eschaton.core.BruteForceReverseSolver;
import io.gitlab.icestom.eschaton.core.StreamMovementValidator;
import io.gitlab.icestom.eschaton.kinematics.D0;
import io.gitlab.icestom.eschaton.kinematics.WorldLike;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EschatonPlugin extends JavaPlugin implements PacketListener {

    public static final String NAMESPACE = "eschaton";
    public static EschatonPlugin instance;

    private static boolean VERBOSE = false;

    private static final int HEAT_TRIGGER_THRESHOLD = 2;

    private static final StreamMovementValidator.Builder validatorBuilder = StreamMovementValidator.builder()
            .regardSlime(StreamMovementValidator.RegardSlime.SKIP)
            .regardGravity(StreamMovementValidator.RegardGravity.SKIP)
            .regardSpeed(StreamMovementValidator.RegardSpeed.IGNORE_WALL);

    private final Map<UUID, StreamMovementValidator> validators = new HashMap<>();
    private final Map<UUID, Long> lasts = new HashMap<>();
    private final Map<UUID, HeatTracker> heat = new HashMap<>();

    private Map<World, WorldLike> worldWrappers = new HashMap<>();

    private volatile boolean eschatonEnabled = false;
    private final Set<UUID> ignored = ConcurrentHashMap.newKeySet();
    private final Set<UUID> subscribers = ConcurrentHashMap.newKeySet();

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.HIGH);
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        try {
            event.getUser().sendPacket(writeOBUPacket(new OBUSettingsPacket.Compound(new OBUSettingsPacket.CompoundPayload(List.of(
                    new OBUSettingsPacket.Reset(),
                    new OBUSettingsPacket.AirStepping(true),
                    new OBUSettingsPacket.AirControl(true),
                    new OBUSettingsPacket.BlockSlipperiness(0.989f, List.of("minecraft:air", "minecraft:petrified_oak_slab")),
                    new OBUSettingsPacket.BlockSlipperiness(0.98f, List.of("minecraft:oxidized_cut_copper_slab")),
//                    new OBUSettingsPacket.BlockSlipperiness(0.98901f, List.of("minecraft:blue_ice")),
                    new OBUSettingsPacket.StepSize(0.5f),
                    new OBUSettingsPacket.WaterElevation(true)
            )))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onHeatTriggered(Player player, long heatLevel) {
        Component alert = Component.text(
                String.format("[Eschaton] %s tripped boat movement heat (level %d)", player.getName(), heatLevel),
                NamedTextColor.GOLD
        );

        Set<Player> targets = new HashSet<>();

        for (Player online : player.getServer().getOnlinePlayers()) {
            if (subscribers.contains(online.getUniqueId())) {
                targets.add(online);
            }
        }

        for (Player target : targets) {
            target.sendMessage(alert);
        }
    }

    @Override
    public void onPacketReceive(@NonNull PacketReceiveEvent event) {
        if (!eschatonEnabled) {
            return;
        }

        final User user = event.getUser();
        final Player player = event.getPlayer();

        final UUID uuid = user.getUUID();

        if (ignored.contains(uuid)) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
            WrapperPlayClientVehicleMove packet = new WrapperPlayClientVehicleMove(event);

            Vector3d position = packet.getPosition();
            float yaw = packet.getYaw();

            if (player.getVehicle() instanceof Boat boat) {
                D0 now = new D0.D0Record(position.x, position.y, position.z, yaw);

                long gameTime = player.getWorld().getGameTime();

                Long last = lasts.get(uuid);

                if (last != null) {
                    if (gameTime - last > 20) {
                        validators.remove(uuid);
                        heat.remove(uuid);
                    }
                }

                StreamMovementValidator validator = validators.get(uuid);

                lasts.put(uuid, player.getWorld().getGameTime());

                if (validator == null) {
                    WorldLike world = worldWrappers.computeIfAbsent(boat.getWorld(), WorldWrapper::new);

                    validators.put(uuid, validatorBuilder.build(now, world));

                    return;
                }

                BruteForceReverseSolver.Result result = validator.update(now);

                if (result == null) {
                    if (VERBOSE) player.sendActionBar(Component.text("SKIPPED", NamedTextColor.BLUE));
                    return;
                }

                if (VERBOSE) player.sendActionBar(Component.text(String.format("%s", result.input()), result.exact() ? NamedTextColor.GREEN : NamedTextColor.RED));

                if (!result.exact()) {
                    if (VERBOSE) player.sendMessage(Component.text(String.format("Fail %s %s: %.10f, %.10f, %.10f", result.input(), result.error(), result.ex(), result.ey(), result.ez()), NamedTextColor.RED));

                    HeatTracker tracker = heat.computeIfAbsent(uuid, k -> new HeatTracker());
                    long current = tracker.gain(gameTime);

                    if (current >= HEAT_TRIGGER_THRESHOLD) {
                        onHeatTriggered(player, current);
                    }
                }

                heat.computeIfPresent(uuid, (k, t) -> t.isExpired(gameTime) ? null : t);
            }
        }
    }

    public static WrapperPlayServerPluginMessage writeOBUPacket(OBUPacket packet) throws IOException {
        DataOutputStreamWriter writer = new DataOutputStreamWriter();

        packet.write(writer);

        return new WrapperPlayServerPluginMessage(packet.getChannel().getChannel(), writer.toBytes());
    }

    public void setEschatonActive(boolean enabled) {
        this.eschatonEnabled = enabled;
    }

    public void ignore(UUID uuid) {
        ignored.add(uuid);
    }

    public void unignore(UUID uuid) {
        ignored.remove(uuid);
    }

    public boolean isIgnored(UUID uuid) {
        return ignored.contains(uuid);
    }

    public void subscribe(UUID uuid) {
        subscribers.add(uuid);
    }

    public void unsubscribe(UUID uuid) {
        subscribers.remove(uuid);
    }

    public boolean isSubscribed(UUID uuid) {
        return subscribers.contains(uuid);
    }

    public static class WorldWrapper implements WorldLike {

        private final World world;

        WorldWrapper(World world) {
            this.world = world;
        }

        @Override
        public Float getBlockSlipperiness(int x, int y, int z) {
            Block block = world.getBlockAt(x, y, z);

            if (block.getType().isAir()) return null;
            if (!block.getType().isSolid()) return null;

            if (block.getType() == Material.PETRIFIED_OAK_SLAB) {
                return 0.989f;
            }
            if (block.getType() == Material.OXIDIZED_CUT_COPPER_SLAB) {
                return 0.98f;
            }

            return block.getType().getSlipperiness();
        }

        @Override
        public boolean isWater(int x, int y, int z) {
            return world.getBlockAt(x, y, z).getType() == Material.WATER;
        }

        @Override
        public boolean isSlime(int x, int y, int z) {
            return world.getBlockAt(x, y, z).getType() == Material.SLIME_BLOCK;
        }

        @Override
        public float getAirSlipperiness() {
            return 0.989f;
        }
    }
}
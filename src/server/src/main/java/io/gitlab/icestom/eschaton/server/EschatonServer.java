package io.gitlab.icestom.eschaton.server;

import io.github.openboatutils.protocol.channels.OBUSettingsPacket;
import io.gitlab.icestom.eschaton.core.BruteForceReverseSolver;
import io.gitlab.icestom.eschaton.core.StreamMovementValidator;
import io.gitlab.icestom.eschaton.kinematics.D0;
import io.gitlab.icestom.eschaton.kinematics.WorldLike;
import io.gitlab.icestom.eschaton.server.command.SlipperinessCommand;
import io.gitlab.icestom.eschaton.server.entity.Boat;
import io.gitlab.icestom.eschaton.server.entity.BoatMarker;
import io.gitlab.icestom.eschaton.server.entity.LightningRod;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.client.play.ClientVehicleMovePacket;
import net.minestom.server.network.packet.server.play.EntityVelocityPacket;
import net.minestom.server.network.packet.server.play.VehicleMovePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.gitlab.icestom.eschaton.server.command.SlipperinessCommand.writePacket;

public class EschatonServer {

    private static final Logger log = LoggerFactory.getLogger(EschatonServer.class);

    private static final Map<Player, StreamMovementValidator> models = new HashMap<>();
    private static final Map<Player, LightningRod> rods = new HashMap<>();

    private static final StreamMovementValidator.Builder validator = StreamMovementValidator.builder()
            .regardSlime(StreamMovementValidator.RegardSlime.SKIP)
            .regardSpeed(StreamMovementValidator.RegardSpeed.IGNORE_WALL);

    static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();

        instanceContainer.setGenerator(unit -> {
            int x = unit.absoluteStart().chunkX();
            int z = unit.absoluteStart().chunkZ();

            if ((x + z) % 2 == 0) {
                unit.modifier().fillHeight(0, 40, Block.PACKED_ICE);
            } else {
                unit.modifier().fillHeight(0, 40, Block.BLUE_ICE);
            }

            if (x == 2 && z == 2) {
                unit.modifier().fillHeight(0, 40, Block.WHITE_CONCRETE);
            }

            if (x == 4 && z == 4) {
                unit.modifier().fillHeight(0, 40, Block.SLIME_BLOCK);
            }

            if (x == -2 && z == 2) {
                unit.modifier().fillHeight(0, 42, Block.WHITE_CONCRETE);
            }

            if (x == -3 && z == 1) {
                unit.modifier().fillHeight(0, 42, Block.WHITE_CONCRETE);
            }

            if (x == 3 && z == 3) {
                unit.modifier().fillHeight(0, 41, Block.PACKED_ICE);
            }

            if (x == 6 && z == 6) {
                unit.modifier().fillHeight(39, 40, Block.WATER);
            }
        });

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(new Pos(0, 42, 0));
        });

        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            final Player player = event.getPlayer();

            if (!event.isFirstSpawn()) return;

            Boat boat = new Boat();
            boat.setInstance(event.getInstance(), new Pos(0, 42, 0, 270, 0));
            boat.addPassenger(player);

            try {
                player.sendPacket(writePacket(new OBUSettingsPacket.StepSize(1f)));
                player.sendPacket(writePacket(new OBUSettingsPacket.JumpForce(0.5f)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            models.put(player, validator.build(new D0.D0Record(
                    0, 42, 0, 0
            ), new WorldLike() {
                @Override
                public Float getBlockSlipperiness(int x, int y, int z) {
                    Block block = instanceContainer.getBlock(x, y, z);

                    if (block.isAir()) {
                        return null;
                    }

                    return block.registry().friction();
                }

                @Override
                public boolean isWater(int x, int y, int z) {
                    return instanceContainer.getBlock(x, y, z).isLiquid();
                }

                @Override
                public boolean isSlime(int x, int y, int z) {
                    return instanceContainer.getBlock(x, y, z) == Block.SLIME_BLOCK;
                }
            }));

            LightningRod lightningRod = new LightningRod();

            lightningRod.setInstance(instanceContainer);

            rods.put(player, lightningRod);
        });

        globalEventHandler.addListener(PlayerDisconnectEvent.class, event -> {
            final Player player = event.getPlayer();

            models.remove(player);
            rods.remove(player).remove();
        });

        globalEventHandler.addListener(PlayerPacketEvent.class, event -> {
            final Player player = event.getPlayer();
            final StreamMovementValidator validator = models.get(player);
            final LightningRod rod = rods.get(player);

            if (event.getPacket() instanceof ClientVehicleMovePacket(Pos position, boolean onGround)) {
                rod.teleport(position);

                BruteForceReverseSolver.Result result = validator.update(new D0.D0Record(
                        position.x(),
                        position.y(),
                        position.z(),
                        position.yaw()
                ));

                if (result == null) {
                    player.sendActionBar(Component.text("SKIPPED", NamedTextColor.BLUE));
                    return;
                }

                player.sendActionBar(Component.text(String.format("%s", result.input()), result.exact() ? NamedTextColor.GREEN : NamedTextColor.RED));

                if (!result.exact()) {
                    player.sendMessage(Component.text(String.format("Fail %s %s: %.10f, %.10f, %.10f", result.input(), result.error(), result.ex(), result.ey(), result.ez()), NamedTextColor.RED));

                    BoatMarker marker = new BoatMarker();
                    marker.setInstance(instanceContainer, position.asVec().asPos());
                }
            }
        });

        globalEventHandler.addListener(PlayerPacketOutEvent.class, playerPacketOutEvent -> {
            Player player = playerPacketOutEvent.getPlayer();

            if (playerPacketOutEvent.getPacket() instanceof EntityVelocityPacket(int entityId, Vec _)) {
                Instance container = player.getInstance();

                if (container == null) return; // we can get unlucky when people leave

                Entity entity = container.getEntityById(entityId);

                if (entity instanceof Boat) {
                    playerPacketOutEvent.setCancelled(true);
                }
            }

            if (playerPacketOutEvent.getPacket() instanceof VehicleMovePacket) {
                playerPacketOutEvent.setCancelled(true);
            }
        });

        MinecraftServer.getCommandManager().register(new SlipperinessCommand());

        server.start("0.0.0.0", 25565);
    }
}

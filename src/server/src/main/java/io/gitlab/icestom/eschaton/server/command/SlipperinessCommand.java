package io.gitlab.icestom.eschaton.server.command;

import io.github.openboatutils.protocol.OBUPacket;
import io.github.openboatutils.protocol.channels.OBUSettingsPacket;
import io.github.openboatutils.protocol.impl.DataOutputStreamWriter;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;

import java.io.IOException;
import java.util.List;

public class SlipperinessCommand extends Command {
    public SlipperinessCommand() {
        super("slipperiness");

        var numberArgument = ArgumentType.Float("slipperiness");

        addSyntax((sender, context) -> {
            final float number = context.get(numberArgument);

            if (sender instanceof Player player) {
                try {
                    player.sendPacket(writePacket(new OBUSettingsPacket.BlockSlipperiness(number, List.of("minecraft:packed_ice"))));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }, numberArgument);
    }

    public static PluginMessagePacket writePacket(OBUPacket packet) throws IOException {
        DataOutputStreamWriter writer = new DataOutputStreamWriter();

        try {
            packet.write(writer);
        } catch (IOException e) {
            throw new IOException(e);
        }

        return new PluginMessagePacket(packet.getChannel().getChannel(), writer.toBytes());
    }
}

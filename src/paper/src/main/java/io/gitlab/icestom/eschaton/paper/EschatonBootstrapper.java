package io.gitlab.icestom.eschaton.paper;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.util.List;

public class EschatonBootstrapper implements PluginBootstrap {
    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(
                    EschatonCommand.create().build(),
                    EschatonPlugin.NAMESPACE,
                    List.of()
            );
        });
    }
}

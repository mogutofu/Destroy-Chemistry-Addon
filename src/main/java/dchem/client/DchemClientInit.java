package dchem.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * Client-only registration: runs only on the physical client (Dist.CLIENT), never on a
 * dedicated server. Wires the chemistry datapack loader into the client's resource reload
 * pipeline (fires at startup and on F3+T), and schedules a one-shot JEI refresh after JEI
 * has finished initialising.
 */
@EventBusSubscriber(modid = "dchem", value = Dist.CLIENT)
public class DchemClientInit {

    public static void register() {
        // No-op; kept for symmetry. Actual work happens in the subscribers below.
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new DchemChemistryReloadListener());
    }

    @SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        DchemChemistryReloadListener.onClientTick();
    }
}

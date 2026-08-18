package dchem;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;

@Mod(DchemMod.MOD_ID)
public class DchemMod {

    public static final String MOD_ID = "dchem";

    public DchemMod() {
        // Client-only display support (JEI / tooltips / names). The server side is pure
        // datapack — Destroy loads it via its own reload listeners; the client needs the
        // local loader below because client-side datapack reloads do not fire server-side
        // events in multiplayer.
        if (FMLLoader.getDist().isClient()) {
            dchem.client.DchemClientInit.register();
        }
    }
}

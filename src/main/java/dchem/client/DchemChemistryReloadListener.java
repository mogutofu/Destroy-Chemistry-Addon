package dchem.client;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import petrolpark.mc.destroy.chemistry.legacy.LegacyReaction;
import petrolpark.mc.destroy.chemistry.legacy.LegacySpecies;
import petrolpark.mc.destroy.core.chemistry.data.MoleculeDefinition;
import petrolpark.mc.destroy.core.chemistry.data.ReactionDefinition;

/**
 * Client-only reload listener that feeds dchem's datapack molecules and reactions into
 * Destroy's client-side registries ({@link LegacySpecies#MOLECULES} /
 * {@link LegacyReaction#REACTIONS}).
 *
 * <p>Why this exists: Destroy loads datapack chemistry through server-side reload listeners
 * (AddReloadListenerEvent), which never fire on a multiplayer client. The fork's S2C sync
 * packets are supposed to cover multiplayer, but to be independent of them (and of the
 * client's Destroy version) this listener loads the same JSON files locally. JEI's Reaction
 * category builds its recipe map from the reactions registry at class-load / refresh time,
 * so populating the registries here makes the new molecules and reactions visible and
 * searchable in JEI without any network round-trip.</p>
 *
 * <p>The client's resource manager only serves asset paths, so the JSONs are mirrored under
 * {@code assets/dchem/destroy/...} in this jar (the server continues to read them from
 * {@code data/dchem/destroy/...}).</p>
 */
public class DchemChemistryReloadListener implements PreparableReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("dchem");
    private static final Gson GSON = new Gson();
    private static final String NAMESPACE = "dchem";
    private static final String MOLECULE_FOLDER = "destroy/molecules";
    private static final String REACTION_FOLDER = "destroy/reactions";

    /** One-shot delayed JEI refresh; JEI may finish initialising after the first reload. */
    private static int ticksUntilJeiRefresh = 100;
    private static boolean jeiRefreshDone = false;

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
            ProfilerFiller prepProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor,
            Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> load(manager), backgroundExecutor)
            .thenCompose(barrier::wait)
            .thenAcceptAsync(this::apply, gameExecutor);
    }

    private Map<String, Object> load(ResourceManager manager) {
        Map<ResourceLocation, MoleculeDefinition> molecules = new LinkedHashMap<>();
        Map<ResourceLocation, ReactionDefinition> reactions = new LinkedHashMap<>();
        try {
            for (Map.Entry<ResourceLocation, Resource> entry
                    : manager.listResources(MOLECULE_FOLDER, p -> p.getPath().endsWith(".json")).entrySet()) {
                ResourceLocation id = entry.getKey();
                if (!NAMESPACE.equals(id.getNamespace())) continue;
                try (InputStream in = entry.getValue().open()) {
                    JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
                    MoleculeDefinition def = MoleculeDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                        .getOrThrow(msg -> new RuntimeException("Molecule decode error: " + msg));
                    molecules.put(id, def);
                } catch (Throwable t) {
                    LOGGER.warn("Failed to load client molecule {}: {}", id, t.getMessage());
                }
            }
            for (Map.Entry<ResourceLocation, Resource> entry
                    : manager.listResources(REACTION_FOLDER, p -> p.getPath().endsWith(".json")).entrySet()) {
                ResourceLocation id = entry.getKey();
                if (!NAMESPACE.equals(id.getNamespace())) continue;
                try (InputStream in = entry.getValue().open()) {
                    JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
                    ReactionDefinition def = ReactionDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                        .getOrThrow(msg -> new RuntimeException("Reaction decode error: " + msg));
                    reactions.put(id, def);
                } catch (Throwable t) {
                    LOGGER.warn("Failed to load client reaction {}: {}", id, t.getMessage());
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("dchem client datapack scan failed: {}", t.getMessage());
        }
        return Map.of("molecules", molecules, "reactions", reactions);
    }

    @SuppressWarnings("unchecked")
    private void apply(Map<String, Object> data) {
        Map<ResourceLocation, MoleculeDefinition> molecules =
            (Map<ResourceLocation, MoleculeDefinition>) data.get("molecules");
        Map<ResourceLocation, ReactionDefinition> reactions =
            (Map<ResourceLocation, ReactionDefinition>) data.get("reactions");

        // Molecules must be registered before reactions (reactions reference molecules).
        LegacySpecies.clearDatapackMolecules();
        LegacyReaction.clearDatapackReactions();

        int okMolecules = 0;
        for (Map.Entry<ResourceLocation, MoleculeDefinition> entry : molecules.entrySet()) {
            try {
                if (entry.getValue().apply(entry.getKey())) okMolecules++;
            } catch (Throwable t) {
                LOGGER.warn("Failed to apply client molecule {}: {}", entry.getKey(), t.getMessage());
            }
        }
        int okReactions = 0;
        for (Map.Entry<ResourceLocation, ReactionDefinition> entry : reactions.entrySet()) {
            try {
                if (entry.getValue().apply(entry.getKey())) okReactions++;
            } catch (Throwable t) {
                LOGGER.warn("Failed to apply client reaction {}: {}", entry.getKey(), t.getMessage());
            }
        }
        LOGGER.info("dchem client chemistry loaded: {} molecule(s), {} reaction(s).", okMolecules, okReactions);

        refreshDestroyJei();
    }

    /** Called from a client tick subscriber: one-shot, slightly delayed JEI refresh. */
    public static void onClientTick() {
        if (jeiRefreshDone) return;
        if (--ticksUntilJeiRefresh > 0) return;
        jeiRefreshDone = true;
        refreshDestroyJei();
    }

    /**
     * Ask Destroy's JEI integration to rebuild the datapack reaction entries and push them
     * into JEI's live recipe manager. Called reflectively to avoid a compile-time dependency
     * on JEI / Destroy's JEI package.
     */
    private static void refreshDestroyJei() {
        try {
            Class<?> jei = Class.forName("petrolpark.mc.destroy.compat.jei.DestroyJEI");
            jei.getMethod("refreshDatapackReactionsClientSide").invoke(null);
            LOGGER.info("dchem: JEI reaction list refreshed.");
        } catch (Throwable t) {
            // JEI not installed, or JEI runtime not ready yet — the ReactionCategory static
            // map is rebuilt on class-load and the delayed tick refresh retries later.
        }
    }
}

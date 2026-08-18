package dchem.client.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * JEI plugin for dchem: registers a recipe-manager plugin that makes Destroy's datapack
 * reactions reachable from item focus lookups (U = uses, R = recipes) — clicking
 * {@code northstar:salt} shows the crystallization reaction that precipitates it, clicking
 * raw meat shows the dissolution reaction, etc.
 *
 * <p>JEI discovers classes annotated with {@code @JeiPlugin} in every mod jar at client
 * startup; on a dedicated server JEI is absent, so this class is never loaded there.</p>
 */
@JeiPlugin
@OnlyIn(Dist.CLIENT)
public class DchemJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("dchem", "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addRecipeManagerPlugin(new DchemReactionFocusPlugin());
    }
}

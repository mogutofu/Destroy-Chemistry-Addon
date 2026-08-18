package dchem.client.jei;

import java.util.ArrayList;
import java.util.List;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import petrolpark.mc.destroy.chemistry.legacy.LegacyReaction;
import petrolpark.mc.destroy.compat.jei.category.ReactionCategory;
import petrolpark.mc.destroy.core.chemistry.recipe.ReactionRecipe;

/**
 * Fills the JEI focus-lookup gap for Destroy's datapack reactions.
 *
 * <p>Destroy's own {@code ItemReverseReactionRecipeManagerPlugin} deliberately relies on
 * JEI's static per-slot ingredient matcher for irreversible reactions (item reactants,
 * precipitates, catalysts). That matcher only indexes recipes JEI knew about at
 * registration/reload time — recipes pushed into the live runtime later (our datapack
 * reactions) are never indexed, so clicking e.g. {@code northstar:salt} finds nothing.
 * This plugin queries {@link LegacyReaction#REACTIONS} directly instead, so it works
 * regardless of JEI's runtime state:</p>
 *
 * <ul>
 *   <li>R (recipes) / OUTPUT focus: reactions whose precipitate is the focused item.</li>
 *   <li>U (uses) / INPUT or CATALYST focus: reactions whose item reactant or catalyst is
 *       the focused item.</li>
 * </ul>
 *
 * <p>Only datapack reactions are surfaced, avoiding duplicates with built-in reactions that
 * JEI's static matcher handles natively.</p>
 */
public class DchemReactionFocusPlugin implements IRecipeManagerPlugin {

    @Override
    public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        if (focus.getTypedValue().getType() == VanillaTypes.ITEM_STACK) {
            return List.of(ReactionCategory.TYPE);
        }
        return List.of();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T, V> List<T> getRecipes(IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
        if (!(recipeCategory instanceof ReactionCategory)) return List.of();

        ItemStack stack = focus.checkedCast(VanillaTypes.ITEM_STACK)
            .map(f -> f.getTypedValue().getIngredient())
            .orElse(null);
        if (stack == null || stack.isEmpty()) return List.of();

        RecipeIngredientRole role = focus.getRole();
        List<T> recipes = new ArrayList<>();
        int[] counter = { 0 };

        for (LegacyReaction reaction : LegacyReaction.REACTIONS.values()) {
            if (!reaction.isDatapack() || !reaction.includeInJei()) continue;

            boolean match;
            if (role == RecipeIngredientRole.OUTPUT) {
                // "What makes X" — X is a precipitate (or other item result) of the reaction.
                match = reaction.hasResult() && reaction.getResult().getAllPrecipitates().stream()
                    .anyMatch(p -> ItemStack.isSameItemSameComponents(p.getPrecipitate(), stack));
            } else {
                // INPUT / CATALYST — "What uses X" — X is an item reactant or catalyst.
                match = reaction.getItemReactants().stream().anyMatch(ir -> ir.isItemValid(stack));
            }
            if (!match) continue;

            ReactionRecipe recipe = ReactionCategory.RECIPES.get(reaction);
            if (recipe == null) recipe = ReactionRecipe.create(reaction);
            recipes.add((T) new RecipeHolder<>(
                ResourceLocation.fromNamespaceAndPath("dchem", "focus_reaction_" + counter[0]++), recipe));
        }
        return recipes;
    }

    @Override
    public <T> List<T> getRecipes(IRecipeCategory<T> recipeCategory) {
        return List.of();
    }
}

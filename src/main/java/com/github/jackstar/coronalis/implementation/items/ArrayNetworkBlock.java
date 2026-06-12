package com.github.jackstar.coronalis.implementation.items;

import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

/**
 * Bloque simple de infraestructura Coronalis.
 *
 * <p>Se usa para cable coaxial y núcleos SU: al colocarse o romperse pide una
 * reconstrucción acotada de redes cercanas, sin escaneos profundos de mundo.</p>
 */
public class ArrayNetworkBlock extends SlimefunItem {

    public ArrayNetworkBlock(@Nonnull ItemGroup itemGroup,
                             @Nonnull SlimefunItemStack item,
                             @Nonnull RecipeType recipeType,
                             @Nonnull ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

}

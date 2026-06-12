package com.github.jackstar.coronalis.implementation.items;

import com.github.drakescraft_labs.slimefun4.api.items.ItemGroup;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItemStack;
import com.github.drakescraft_labs.slimefun4.api.recipes.RecipeType;
import com.github.jackstar.coronalis.Coronalis;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

/**
 * Abre una copia segura del manual sin modificar el ítem que sostiene el jugador.
 */
public final class FieldGuideItem extends SlimefunItem implements Listener {

    public FieldGuideItem(
        @Nonnull ItemGroup itemGroup,
        @Nonnull SlimefunItemStack item,
        @Nonnull RecipeType recipeType,
        @Nonnull ItemStack[] recipe
    ) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void preRegister() {
        Coronalis.instance().getServer().getPluginManager().registerEvents(this, Coronalis.instance());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void openGuide(@Nonnull PlayerInteractEvent event) {
        if (!event.getAction().isRightClick() || event.getItem() == null) {
            return;
        }
        SlimefunItem item = SlimefunItem.getByItem(event.getItem());
        if (item == null || !getId().equals(item.getId())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().openBook(getItem().clone());
    }
}

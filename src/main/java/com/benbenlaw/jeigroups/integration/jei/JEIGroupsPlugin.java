package com.benbenlaw.jeigroups.integration.jei;

import com.benbenlaw.jeigroups.JEIGroups;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.runtime.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static com.mojang.text2speech.Narrator.LOGGER;

@JeiPlugin
public class JEIGroupsPlugin implements IModPlugin {

    public static final IIngredientType<StackGroup> GROUP_TYPE = () -> StackGroup.class;

    private final Map<String, StackGroup> groupCache = new HashMap<>();
    private IJeiRuntime jeiRuntime;

    @Override
    public Identifier getPluginUid() {
        return JEIGroups.identifier("jei_plugin");
    }

    private List<ItemStack> getWoolChildren() {
        return List.of(
                new ItemStack(Items.RED_WOOL), new ItemStack(Items.BLUE_WOOL),
                new ItemStack(Items.GREEN_WOOL), new ItemStack(Items.YELLOW_WOOL),
                new ItemStack(Items.BLACK_WOOL), new ItemStack(Items.WHITE_WOOL),
                new ItemStack(Items.ORANGE_WOOL), new ItemStack(Items.PURPLE_WOOL),
                new ItemStack(Items.BROWN_WOOL), new ItemStack(Items.CYAN_WOOL),
                new ItemStack(Items.LIGHT_GRAY_WOOL), new ItemStack(Items.LIGHT_BLUE_WOOL),
                new ItemStack(Items.MAGENTA_WOOL), new ItemStack(Items.GRAY_WOOL)
        );
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {

        groupCache.clear();

        GroupDataLoader.RAW_DATA.forEach((name, data) -> {
            var iconItem = BuiltInRegistries.ITEM.getValue(data.icon());
            var children = data.items().stream()
                    .map(itemId -> new ItemStack(BuiltInRegistries.ITEM.getValue(itemId)))
                    .toList();

            StackGroup group = new StackGroup(name, new ItemStack(iconItem), children, false);
            groupCache.put(name, group);
        });

        if (groupCache.isEmpty()) {
            LOGGER.warn("No JEI groups found in GroupDataLoader!");
        }

        registration.register(GROUP_TYPE, new ArrayList<>(groupCache.values()), new GroupHelper(), new GroupRenderer(), StackGroup.CODEC);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.jeiRuntime = jeiRuntime;
        IIngredientManager manager = jeiRuntime.getIngredientManager();

        for (StackGroup group : groupCache.values()) {
            manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, group.children());
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory factory, double mouseX, double mouseY) {

                return jeiRuntime.getIngredientListOverlay().getIngredientUnderMouse()
                        .flatMap(clickable -> {
                            if (clickable.getIngredient() instanceof StackGroup group) {

                                long handle = Minecraft.getInstance().getWindow().handle();
                                boolean isLeftPressed = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

                                if (isLeftPressed) {
                                    toggleGroup(group);
                                }

                                return factory.createBuilder(GROUP_TYPE, group)
                                        .buildWithArea((int)mouseX - 10, (int)mouseY - 10, 20, 20);
                            }
                            return Optional.empty();
                        });
            }
        });
    }

    private void toggleGroup(StackGroup group) {
        IIngredientManager manager = jeiRuntime.getIngredientManager();
        boolean nowExpanded = !group.expanded();
        StackGroup toggledGroup = group.withExpanded(nowExpanded);
        groupCache.put(group.name(), toggledGroup);

        manager.removeIngredientsAtRuntime(GROUP_TYPE, List.of(group));
        manager.addIngredientsAtRuntime(GROUP_TYPE, List.of(toggledGroup));

        if (nowExpanded) {
            new Thread(() -> {
                try {
                    Thread.sleep(150);
                    Minecraft.getInstance().execute(() -> {
                        manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toggledGroup.children());
                        jeiRuntime.getIngredientFilter().setFilterText(jeiRuntime.getIngredientFilter().getFilterText());
                    });
                } catch (InterruptedException ignored) {}
            }).start();
        } else {
            manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toggledGroup.children());
        }
        jeiRuntime.getIngredientFilter().setFilterText(jeiRuntime.getIngredientFilter().getFilterText());
    }
}
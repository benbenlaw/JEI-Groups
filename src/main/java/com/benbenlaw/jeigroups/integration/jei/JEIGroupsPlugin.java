package com.benbenlaw.jeigroups.integration.jei;

import com.benbenlaw.jeigroups.JEIGroups;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.gui.ingredients.IngredientFilter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static com.mojang.text2speech.Narrator.LOGGER;

@JeiPlugin
public class JEIGroupsPlugin implements IModPlugin {

    private final Map<Item, StackGroup> itemToGroupMap = new HashMap<>();
    public final Map<String, StackGroup> groupCache = new HashMap<>();

    private final Map<Item, List<ItemStack>> jeiVariantCache = new HashMap<>();

    public final Map<StackGroup, Integer> currentFilteredCounts = new IdentityHashMap<>();

    public static JEIGroupsPlugin instance;
    public IJeiRuntime jeiRuntime;

    @Override
    public Identifier getPluginUid() {
        return JEIGroups.identifier("jei_plugin");
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        instance = this;
        rebuildGroups();
    }

    public void rebuildGroups() {
        groupCache.clear();
        itemToGroupMap.clear();

        Set<String> savedExpanded = ExpandedGroupsStorage.load();

        GroupDataLoader.RAW_DATA.forEach((name, data) -> {
            var iconItem = data.icon().create();
            var children = resolveItemStacks(data.items());

            if (children.isEmpty()) {
                LOGGER.warn("JEI Group '{}' resolved to zero items - check your item/tag/mod-id references", name);
            }

            StackGroup group = new StackGroup(
                    name, iconItem, children, savedExpanded.contains(name),
                    data.borderColor(), data.overlayTint(), data.plusIconColor(), data.borderThickness(),
                    data.backgroundColor()
            );
            groupCache.put(name, group);

            for (ItemStack child : children) {
                Item item = child.getItem();
                StackGroup existing = itemToGroupMap.get(item);

                if (existing != null) {

                    LOGGER.warn(
                            "Item '{}' is listed in both JEI Groups '{}' and '{}' - keeping it in '{}' since that group loaded first",
                            BuiltInRegistries.ITEM.getKey(item), existing.name(), name, existing.name()
                    );
                    continue;
                }

                itemToGroupMap.put(item, group);
            }
        });
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        instance = this;
        this.jeiRuntime = jeiRuntime;

        IIngredientManager manager = jeiRuntime.getIngredientManager();
        Collection<ItemStack> allStacks = manager.getAllIngredients(VanillaTypes.ITEM_STACK);

        for (StackGroup group : groupCache.values()) {
            if (group.children().isEmpty()) continue;

            if (group.children().size() == 1) {
                Item targetItem = group.children().getFirst().getItem();
                List<ItemStack> variants = allStacks.stream().filter(stack -> stack.getItem() == targetItem).toList();
                jeiVariantCache.put(targetItem, variants);
            }
        }
    }

    public void toggleGroup(StackGroup group) {
        boolean nowExpanded = !group.expanded();
        StackGroup toggledGroup = group.withExpanded(nowExpanded);

        groupCache.put(group.name(), toggledGroup);

        for (ItemStack child : group.children()) {
            itemToGroupMap.put(child.getItem(), toggledGroup);
        }

        persistExpandedState();
        refreshFilter();
    }

    private void persistExpandedState() {
        Set<String> expandedNames = groupCache.values().stream()
                .filter(StackGroup::expanded)
                .map(StackGroup::name)
                .collect(Collectors.toSet());
        ExpandedGroupsStorage.save(expandedNames);
    }

    public StackGroup getGroupForItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return itemToGroupMap.get(stack.getItem());
    }

    private void refreshFilter() {
        var filter = jeiRuntime.getIngredientFilter();

        if (filter instanceof com.benbenlaw.jeigroups.mixin.IngredientFilterApiAccessor accessor) {
            IngredientFilter concreteFilter = accessor.jeigroups$getIngredientFilter();
            concreteFilter.invalidateCache();

            if (concreteFilter instanceof com.benbenlaw.jeigroups.mixin.IngredientFilterInvoker invoker) {
                invoker.jeigroups$notifyListenersOfChange();
            }
        } else {
            String currentText = filter.getFilterText();
            filter.setFilterText(currentText + " ");
            filter.setFilterText(currentText);
        }
    }

    public int getGroupMemberCount(StackGroup group) {
        if (group.children().size() >= 2) {
            return group.children().size();
        }
        Item targetItem = group.children().getFirst().getItem();
        List<ItemStack> variants = jeiVariantCache.get(targetItem);
        return variants != null ? variants.size() : 1;
    }

    private List<ItemStack> resolveItemStacks(List<String> references) {
        LinkedHashSet<Item> resolved = new LinkedHashSet<>();

        for (String reference : references) {
            if (reference.startsWith("#")) {
                resolveTag(reference.substring(1), resolved);
            } else if (reference.startsWith("@")) {
                resolveModId(reference.substring(1), resolved);
            } else if (reference.contains("*")) {
                resolveWildcard(reference, resolved);
            } else {
                resolveSingleItem(reference, resolved);
            }
        }

        return resolved.stream().map(ItemStack::new).toList();
    }

    private void resolveWildcard(String pattern, Set<Item> out) {
        Pattern regex;
        try {
            regex = globToPattern(pattern);
        } catch (PatternSyntaxException e) {
            LOGGER.warn("Invalid wildcard pattern '{}' in JEI Groups data - skipping", pattern);
            return;
        }

        boolean matchedAny = false;
        for (var holder : BuiltInRegistries.ITEM.listElements().toList()) {
            Optional<net.minecraft.resources.ResourceKey<Item>> key = holder.unwrapKey();
            if (key.isEmpty()) continue;

            String fullId = key.get().identifier().toString();
            if (regex.matcher(fullId).matches()) {
                out.add(holder.value());
                matchedAny = true;
            }
        }

        if (!matchedAny) {
            LOGGER.warn("Wildcard pattern '{}' in JEI Groups data matched no items", pattern);
        }
    }

    private static Pattern globToPattern(String glob) {
        StringBuilder regex = new StringBuilder();
        for (char c : glob.toCharArray()) {
            if (c == '*') {
                regex.append(".*");
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(regex.toString());
    }

    private void resolveTag(String rawId, Set<Item> out) {
        Identifier id = Identifier.tryParse(rawId);
        if (id == null) {
            LOGGER.warn("Invalid tag reference '#{}' in JEI Groups data - skipping", rawId);
            return;
        }

        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, id);
        Optional<HolderSet.Named<Item>> tag = BuiltInRegistries.ITEM.get(tagKey);

        if (tag.isEmpty()) {
            LOGGER.warn("Tag '#{}' resolved to no items (unknown tag, or not loaded yet - try again after joining a world)", rawId);
            return;
        }

        tag.get().forEach(holder -> out.add(holder.value()));
    }

    private void resolveModId(String modId, Set<Item> out) {
        BuiltInRegistries.ITEM.listElements().forEach(holder ->
                holder.unwrapKey().ifPresent(key -> {
                    if (key.identifier().getNamespace().equals(modId)) {
                        out.add(holder.value());
                    }
                })
        );
    }

    private void resolveSingleItem(String rawId, Set<Item> out) {
        Identifier id = Identifier.tryParse(rawId);
        if (id == null) {
            LOGGER.warn("Invalid item reference '{}' in JEI Groups data - skipping", rawId);
            return;
        }

        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            LOGGER.warn("Unknown item '{}' in JEI Groups data - skipping", rawId);
            return;
        }
        out.add(item);
    }


}
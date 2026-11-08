package com.argusrazor.friends.adapter;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;

public class ItemModelAdapter {

    private static Method setItemModelMethod = null;

    static {
        try {
            setItemModelMethod = ItemMeta.class.getMethod("setItemModel", NamespacedKey.class);
        } catch (NoSuchMethodException ignored) {
        }
    }

    public static void applyCustomModel(ItemStack item, Object modelValue) {
        if (item == null || modelValue == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (modelValue instanceof Integer) {
            meta.setCustomModelData((Integer) modelValue);
            item.setItemMeta(meta);
            return;
        }

        if (modelValue instanceof String str) {
            try {
                int intVal = Integer.parseInt(str);
                meta.setCustomModelData(intVal);
                item.setItemMeta(meta);
                return;
            } catch (NumberFormatException ignored) {
            }

            if (setItemModelMethod != null) {
                try {
                    NamespacedKey key = NamespacedKey.fromString(str);
                    if (key != null) {
                        setItemModelMethod.invoke(meta, key);
                        item.setItemMeta(meta);
                        return;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }
}

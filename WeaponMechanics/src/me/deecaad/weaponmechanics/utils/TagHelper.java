package me.deecaad.weaponmechanics.utils;

import me.deecaad.core.utils.TagUtils;
import org.bukkit.inventory.ItemStack;

public class TagHelper {

    /**
     * Don't let anyone instantiate this class
     */
    private TagHelper() { }

    /**
     * @param itemStack the item stack from which to get tag
     * @param tag the tag name
     * @return the value of the tag, if not found null
     */
    public static String getStringTag(ItemStack itemStack, CustomTag tag) {
        return TagUtils.getStringTag(itemStack, tag.getId());
    }

    /**
     * Set or change tag of item stack with given value.
     *
     * @param itemStack the item stack to modify
     * @param tag the tag name
     * @param value the value for tag
     * @return the item stack with new or modified tag value
     */
    public static ItemStack setStringTag(ItemStack itemStack, CustomTag tag, String value) {
        return TagUtils.setStringTag(itemStack, tag.getId(), value);
    }

    /**
     * @param itemStack the item stack from which to get tag
     * @param tag the tag name
     * @return the value of the tag, if not found null
     */
    public static Integer getIntegerTag(ItemStack itemStack, CustomTag tag) {
        return TagUtils.getIntegerTag(itemStack, tag.getId());
    }

    /**
     * Set or change tag of item stack with given value.
     *
     * @param itemStack the item stack to modify
     * @param tag the tag name
     * @param value the value for tag
     * @return the item stack with new or modified tag value
     */
    public static ItemStack setIntegerTag(ItemStack itemStack, CustomTag tag, int value) {
        return TagUtils.setIntegerTag(itemStack, tag.getId(), value);
    }
}
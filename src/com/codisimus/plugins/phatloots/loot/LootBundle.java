package com.codisimus.plugins.phatloots.loot;

import java.util.LinkedList;
import java.util.List;
import org.bukkit.inventory.ItemStack;

/**
 * A LootBundle is a collection of items, commands, money, and experience that has been rolled for
 *
 * @author Cody
 */
public class LootBundle {
    private List<ItemStack> itemList;
    private List<CommandLoot> commandList = new LinkedList<CommandLoot>();
    private double money = 0;
    private int exp = 0;

    /**
     * Creates a new LootBundle with the given list of ItemStacks
     *
     * @param itemList The preexisting list of items
     */
    public LootBundle(List<ItemStack> itemList) {
        this.itemList = itemList;
    }

    /**
     * Creates a new, empty LootBundle
     */
    public LootBundle() {
        this.itemList = new LinkedList<ItemStack>();
    }

    /**
     * Returns the list of items in the bundle
     *
     * @return The List of ItemStacks in the bundle
     */
    public List<ItemStack> getItemList() {
        return itemList;
    }

    /**
     * Adds the given item to the list of looted items
     *
     * @param item The given ItemStack to add as loot
     */
    public void addItem(ItemStack item) {
        itemList.add(item);
    }

    /**
     * Returns the list of commands to be executed
     *
     * @return The list of CommandLoots
     */
    public List<CommandLoot> getCommandList() {
        return commandList;
    }

    /**
     * Adds the given command to the list of looted commands
     *
     * @param command The given CommandLoot to add as loot
     */
    public void addCommand(CommandLoot command) {
        commandList.add(command);
    }

    /**
     * Returns the amount of money in the bundle of loot
     *
     * @return The amount of money that was looted
     */
    public double getMoney() {
        return money;
    }

    /**
     * Sets the amount of money in the bundle of loot
     *
     * @param money The new amount of money to be looted
     */
    public void setMoney(double money) {
        this.money = money;
    }

    /**
     * Returns the amount of experience in the bundle of loot
     *
     * @return The amount of experience that was looted
     */
    public int getExp() {
        return exp;
    }

    /**
     * Sets the amount of experience in the bundle of loot
     *
     * @param money The new amount of experience to be looted
     */
    public void setExp(int exp) {
        this.exp = exp;
    }
}

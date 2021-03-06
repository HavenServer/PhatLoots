package com.codisimus.plugins.phatloots;

import com.codisimus.plugins.chestlock.ChestLock;
import com.codisimus.plugins.chestlock.Safe;
import com.codisimus.plugins.phatloots.listeners.PhatLootInfoListener;
import com.codisimus.plugins.phatloots.loot.CommandLoot;
import com.codisimus.plugins.phatloots.loot.Item;
import com.codisimus.plugins.phatloots.loot.Loot;
import com.codisimus.plugins.phatloots.loot.LootCollection;
import com.codisimus.plugins.regionown.Region;
import com.codisimus.plugins.regionown.RegionSelector;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Executes Sender Commands
 *
 * @author Cody
 */
public class PhatLootsCommand implements CommandExecutor {
    private static enum Action { //Loot Commands
        HELP, MAKE, DELETE, LINK, UNLINK, TIME, GLOBAL, AUTOLOOT, BREAK, ROUND,
        ADD, REMOVE, COST, MONEY, EXP, LIST, INFO, GIVE, RESET, CLEAN, RL
    }
    private static enum Help { CREATE, SETUP, LOOT } //Help Pages
    private static final HashSet<Byte> TRANSPARENT = Sets.newHashSet(
            (byte)0,   (byte)6,   (byte)8,   (byte)9,   (byte)10,  (byte)11,
            (byte)26,  (byte)27,  (byte)28,  (byte)30,  (byte)31,  (byte)32,
            (byte)37,  (byte)38,  (byte)39,  (byte)40,  (byte)44,  (byte)50,
            (byte)51,  (byte)53,  (byte)55,  (byte)59,  (byte)64,  (byte)63,
            (byte)65,  (byte)66,  (byte)67,  (byte)68,  (byte)69,  (byte)70,
            (byte)71,  (byte)72,  (byte)75,  (byte)76,  (byte)77,  (byte)78,
            (byte)85,  (byte)90,  (byte)92,  (byte)96,  (byte)101, (byte)102,
            (byte)104, (byte)105, (byte)106, (byte)107, (byte)108, (byte)109,
            (byte)111, (byte)113, (byte)114, (byte)115, (byte)117, (byte)126,
            (byte)127, (byte)131, (byte)132, (byte)139, (byte)140, (byte)141,
            (byte)142, (byte)144, (byte)145);
    static String command; //Main Command
    static boolean setUnlockable; //True if linked Chests should be set as unlockable by ChestLock

    /**
     * Listens for PhatLoots commands to execute them
     *
     * @param sender The CommandSender who may not be a Player
     * @param command The command that was executed
     * @param alias The alias that the sender used
     * @param args The arguments for the command
     * @return true always
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        //Display the help page if the sender did not add any arguments
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        Action action;
        try {
            action = Action.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException notEnum) { //Command Loot
            //Cancel if the first argument is not a valid PhatLoot
            PhatLoot phatLoot = PhatLoots.getPhatLoot(args[0]);
            if (phatLoot == null) {
                sender.sendMessage("§4PhatLoot §6" + args[0] + "§4 does not exist");
                return true;
            }

            //Cancel if the sender does not have the needed permission
            if (sender instanceof Player) {
                Player player = (Player) sender;
            	if (!player.hasPermission("phatloots.commandloot")
                        || !(PhatLoots.canLoot(player, phatLoot))) {
                    player.sendMessage(PhatLootsConfig.permission);
            	    return true;
            	}
            	phatLoot.rollForLoot(player);
            }
            return true;
        }

        //Cancel if the sender does not have permission to use the command
        if (!sender.hasPermission("phatloots." + action.toString().toLowerCase())
                && action != Action.HELP) {
            sender.sendMessage(PhatLootsConfig.permission);
            return true;
        }

        //Execute the correct command
        switch (action) {
        case MAKE: //Make a new PhatLoot
            if (args.length == 2) {
                make(sender, args[1]);
            } else {
                sendCreateHelp(sender);
            }

            return true;

        case DELETE: //Delete an existing PhatLoot
            if (args.length == 2) {
                PhatLoot delete = PhatLoots.getPhatLoot(args[1]);

                if (delete == null) {
                    sender.sendMessage("§4PhatLoot §6" + args[1] + "§4 does not exist");
                } else {
                    PhatLoots.removePhatLoot(delete);
                    sender.sendMessage("§5PhatLoot §6" + delete.name + "§5 was deleted!");
                }
            } else {
                sendCreateHelp(sender);
            }

            return true;

        case LINK: //Link the target Block to a PhatLoot
            if (args.length == 2) {
                link(sender, args[1]);
            } else {
                sendCreateHelp(sender);
            }

            return true;

        case UNLINK: //Unlink the target Block from a PhatLoot
            switch (args.length) {
            case 1: //All PhatLoots
                unlink(sender, null);
                break;
            case 2: //Specific PhatLoot
                unlink(sender, args[1]);
                break;
            default:
                sendCreateHelp(sender);
                break;
            }

            return true;

        case TIME: //Set the reset (cooldown) time of a PhatLoot
            switch (args.length) {
            case 2:  //Name is not provided
                if (!args[1].equals("never")) {
                    break;
                }

                time(sender, null, -1, -1, -1, -1);
                return true;

            case 3: //Name is provided
                if (!args[2].equals("never")) {
                    break;
                }

                time(sender, args[1], -1, -1, -1, -1);
                return true;

            case 5: //Name is not provided
                try {
                    time(sender, null, Integer.parseInt(args[1]), Integer.parseInt(args[2]),
                            Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                    return true;
                } catch (Exception notInt) {
                    break;
                }

            case 6: //Name is provided
                try {
                    time(sender, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]),
                            Integer.parseInt(args[4]), Integer.parseInt(args[5]));
                    return true;
                } catch (Exception notInt) {
                    break;
                }

            default: break;
            }

            sendSetupHelp(sender);
            return true;

        case GLOBAL: //Set a PhatLoot to global or individual looting
            switch (args.length) {
            case 2: //Name is not provided
                try {
                    global(sender, null, Boolean.parseBoolean(args[1]));
                    return true;
                } catch (Exception notBool) {
                    break;
                }

            case 3: //Name is provided
                try {
                    global(sender, args[1], Boolean.parseBoolean(args[2]));
                    return true;
                } catch (Exception notBool) {
                    break;
                }

            default: break;
            }

            sendSetupHelp(sender);
            return true;

        case AUTOLOOT: //Set a PhatLoot to automatically add loot to the player's inventory
            switch (args.length) {
            case 2: //Name is not provided
                try {
                    autoLoot(sender, null, Boolean.parseBoolean(args[1]));
                    return true;
                } catch (Exception notBool) {
                    break;
                }

            case 3: //Name is provided
                try {
                    autoLoot(sender, args[1], Boolean.parseBoolean(args[2]));
                    return true;
                } catch (Exception notBool) {
                    break;
                }

            default: break;
            }

            sendSetupHelp(sender);
            return true;

        case BREAK: //Set a PhatLootChest to automatically break and respawn after being looted
            switch (args.length) {
            case 2: //Name is not provided
                try {
                    breakAndRespawn(sender, null, Boolean.parseBoolean(args[1]));
                    return true;
                } catch (Exception notBool) {
                    break;
                }

            case 3: //Name is provided
                try {
                    breakAndRespawn(sender, args[1], Boolean.parseBoolean(args[2]));
                    return true;
                } catch (Exception notBool) {
                    break;
                }

            default: break;
            }

            sendSetupHelp(sender);
            return true;

        case ROUND: //Set a PhatLoot to round down reset times
            switch (args.length) {
            case 2: //Name is not provided
                try {
                    round(sender, null, Boolean.parseBoolean(args[1]));
                    return true;
                } catch (Exception notBool) {
                    break;
                }

            case 3: //Name is provided
                try {
                    round(sender, args[1], Boolean.parseBoolean(args[2]));
                    return true;
                } catch (Exception notBool) {
                    break;
                }

            default: break;
            }

            sendSetupHelp(sender);
            return true;

        case REMOVE: //Fall through
        case ADD: //Manage Loot of a PhatLoot
            if (args.length < 2) {
                sendSetupHelp(sender);
                return true;
            }

            boolean add = action.equals(Action.ADD);
            ItemStack item = null; //The ItemStack to be added/removed
            String collName = null; //The name of the Loot Collection to be added/removed
            String cmd = null; //The command to be added/removed

            String phatLoot = null; //The name of the PhatLoot
            double percent = 100; //The chance of receiving the Loot (defaulted to 100)
            String coll = null; //The Collection to add the Loot to
            int lowerBound = 1; //Stack size of the Loot item (defaulted to 1)
            int upperBound = 1; //Amount to possibly increase the Stack size of the Loot item (defaulted to 1)
            boolean autoEnchant = false; //Whether or not the Loot Item should be automatically enchanted at time of Looting
            boolean tiered = false; //Whether or not the Loot Item should be Tiered
            boolean generateName = false; //Whether or not the Loot Item should have a generated name

            int i = 2;
            if (args[1].equals("coll")) { //LootCollection
                if (args.length < 3) {
                    sendSetupHelp(sender);
                    return true;
                }
                collName = args[i];
                lowerBound = PhatLootsConfig.defaultLowerNumberOfLoots;
                upperBound = PhatLootsConfig.defaultUpperNumberOfLoots;
                i++;
            } else if (!args[1].equals("cmd")) { //Item
                item = getItemStack(sender, args[1]);
                if (item == null) {
                    return true;
                }
            }

            //Check each parameter
            while (i < args.length) {
                char c = args[i].charAt(0);
                String s = args[i].substring(1);
                switch (c) {
                case 'p': //PhatLoot Name
                    phatLoot = s;
                    break;

                case '%': //Probability
                    percent = getPercent(sender, s);
                    if (percent == -1) {
                        sender.sendMessage("§6" + s + "§4 is not a percent");
                        return true;
                    }
                    break;

                case 'c': //Collection Name
                    coll = s;
                    break;

                case '#': //Amount
                    lowerBound = getLowerBound(s);
                    upperBound = getUpperBound(s);
                    if (lowerBound == -1 || upperBound == -1) {
                        sender.sendMessage("§6" + s + "§4 is not a valid number or range");
                        return true;
                    }
                    if (item != null) {
                        item.setAmount(lowerBound);
                    }
                    break;

                case 'e': //Enchantment
                    if (s.equalsIgnoreCase("auto")) {
                        autoEnchant = true;
                    } else {
                        Map<Enchantment, Integer> enchantments = getEnchantments(s);
                        if (enchantments == null) {
                            sender.sendMessage("§6" + s + "§4 is not a valid enchantment");
                            return true;
                        }
                        item.addUnsafeEnchantments(enchantments);
                    }
                    break;

                case 'd': //Durability
                    short data = getData(s);
                    if (data == -1) {
                        sender.sendMessage("§6" + s + "§4 is not a valid data/durability value");
                        return true;
                    }
                    item.setDurability(data);
                    break;

                case 't': //Tiered
                    tiered = true;
                    break;

                case 'l': //Automatic Lore
                    generateName = true;
                    break;

                case '/': //Command
                    cmd = args[i];
                    i++;
                    while (i < args.length) {
                        cmd += " " + args[i];
                        i++;
                    }
                    break;

                default: //Invalid Parameter
                    sender.sendMessage("§6" + c + "§4 is not a valid parameter ID");
                    return true;
                }

                i++;
            }

            //Construct the Loot
            Loot loot;
            if (item != null) { //Item
                loot = new Item(item, upperBound - lowerBound);
                if (autoEnchant) {
                    ((Item) loot).autoEnchant = true;
                }
                if (tiered) {
                    ((Item) loot).tieredName = true;
                }
                if (generateName) {
                    ((Item) loot).generateName = true;
                }
            } else if (collName != null) { //LootCollection
                loot = new LootCollection(collName, lowerBound, upperBound);
            } else { //CommandLoot
                loot = new CommandLoot(cmd);
            }
            loot.setProbability(percent);

            setLoot(sender, phatLoot, add, coll, loot);
            return true;

        case COST: //Set the cost to loot a PhatLoot
            switch (args.length) {
            case 2: //Name is not provided
                try {
                    setCost(sender, null, Integer.parseInt(args[1]));
                    return true;
                } catch (Exception notInt) {
                    sendSetupHelp(sender);
                    break;
                }

            case 3: //Name is provided
                try {
                    setCost(sender, args[1], Integer.parseInt(args[2]));
                    return true;
                } catch (Exception notInt) {
                    sendSetupHelp(sender);
                    break;
                }

            default: break;
            }

            sendSetupHelp(sender);
            return true;

        case MONEY: //Set the amount of money to be looted from a PhatLoot
            switch (args.length) {
            case 2: //Name is not provided
                try {
                    setMoney(sender, null, args[1]);
                    return true;
                } catch (Exception notInt) {
                    sendSetupHelp(sender);
                    break;
                }

            case 3: //Name is provided
                try {
                    setMoney(sender, args[1], args[2]);
                    return true;
                } catch (Exception notInt) {
                    sendSetupHelp(sender);
                    break;
                }

            default: break;
            }

            sendSetupHelp(sender);
            return true;

        case EXP: //Set the amount of experience to be looted from a PhatLoot
            switch (args.length) {
            case 2: //Name is not provided
                setExp(sender, null, args[1]);
                return true;

            case 3: //Name is provided
                setExp(sender, args[1], args[2]);
                return true;

            default:
                sendSetupHelp(sender);
                return true;
            }

        case LIST: //List all PhatLoots
            if (args.length == 1) {
                list(sender);
            } else {
                sendHelp(sender);
            }
            return true;

        case INFO: //View information of a PhatLoot
            switch (args.length) {
            case 1: //Name is not provided
                info(sender, null);
                return true;
            case 2: //Name is provided
                info(sender, args[1]);
                return true;
            default:
                sendHelp(sender);
                return true;
            }

        case RESET: //Reset the loot times of a PhatLoot
            switch (args.length) {
            case 1: //Name is not provided
                reset(sender, null);
                return true;
            case 2: //Name is provided
                reset(sender, args[1]);
                return true;
            default:
                sendHelp(sender);
                return true;
            }

        case CLEAN: //Clean up loot times of a PhatLoot
            switch (args.length) {
            case 1: //Name is not provided
                clean(sender, null);
                return true;
            case 2: //Name is provided
                clean(sender, args[1]);
                return true;
            default:
                sendHelp(sender);
                return true;
            }

        case GIVE: //Force a Player to open a PhatLoot
            if (args.length < 3) {
                if (sender instanceof Player) {
                    sendHelp(sender);
                    return true;
                } else {
                    return false;
                }
            }

            Player player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage("§6" + args[1] + " §4is not online");
                return true;
            }

            PhatLoot pLoot = PhatLoots.getPhatLoot(args[2]);
            if (pLoot == null) {
                sender.sendMessage("§4PhatLoot §6" + args[2] + "§4 does not exist");
                return true;
            }

            //Set the custom name of the PhatLoot Inventory
            String name = args.length == 3
                          ? pLoot.name
                          : concatArgs(args, 3);
            name = ChatColor.translateAlternateColorCodes('&', name);
            pLoot.rollForLoot(player, name);
            return true;

        case RL: //Reload plugin data and settings
            if (args.length == 1) {
                PhatLoots.rl(sender);
            } else {
                sendHelp(sender);
            }
            return true;

        case HELP: //Display the PhatLoots help page
            if (args.length == 2) {
                Help help;

                try {
                    help = Help.valueOf(args[1].toUpperCase());
                } catch (Exception notEnum) {
                    sendHelp(sender);
                    return true;
                }

                switch (help) {
                case CREATE:
                    sendCreateHelp(sender);
                    break;
                case SETUP:
                    sendSetupHelp(sender);
                    break;
                case LOOT:
                    sendLootHelp(sender);
                    break;
                }
            } else {
                sendHelp(sender);
            }

            return true;

        default:
            sendHelp(sender);
            return true;
        }
    }

    /**
     * Creates a new PhatLoot of the given name
     *
     * @param sender The CommandSender creating the PhatLoot
     * @param name The name of the PhatLoot being created (must not already exist)
     */
    public static void make(CommandSender sender, String name) {
        //Cancel if the PhatLoot already exists
        if (PhatLoots.hasPhatLoot(name)) {
            sender.sendMessage("§4A PhatLoot named §6" + name + "§4 already exists.");
            return;
        }

        PhatLoots.addPhatLoot(new PhatLoot(name));
        sender.sendMessage("§5PhatLoot §6" + name + "§5 made!");
    }

    /**
     * Links the target Block to the specified PhatLoot
     *
     * @param sender The CommandSender linking the Block they are targeting
     * @param name The name of the PhatLoot the Block will be linked to
     */
    public static void link(CommandSender sender, String name) {
    	//Cancel if the sender is console
    	if (!(sender instanceof Player)) {
            sender.sendMessage("§4You cannot do this from the console!");
            return;
    	}

        //Cancel if the sender is not targeting a correct Block
        Block block  = ((Player) sender).getTargetBlock(TRANSPARENT, 10);
        String blockName = block.getType().toString();
        if (!PhatLoots.isLinkableType(block)) {
            sender.sendMessage("§6" + blockName + "§4 is not a linkable type.");
            return;
        }

        switch (block.getType()) {
        case CHEST:
            Chest chest = (Chest) block.getState();
            Inventory inventory = chest.getInventory();

            //Linked the left side if it is a DoubleChest
            if (inventory instanceof DoubleChestInventory) {
                chest = (Chest) ((DoubleChestInventory) inventory).getLeftSide().getHolder();
                block = chest.getBlock();
            }
            //Fall through
        case ENDER_CHEST:
            //Make the Chest unlockable if ChestLock is enabled
            if (setUnlockable && Bukkit.getPluginManager().isPluginEnabled("ChestLock")) {
                Safe safe = ChestLock.findSafe(block);
                if (safe == null) {
                    safe = new Safe(sender.getName(), block);
                    safe.lockable = false;
                    safe.locked = false;

                    ChestLock.addSafe(safe);
                }
            }
            break;

        default:
            break;
        }

        //Cancel if the PhatLoot with the given name does not exist
        if (!PhatLoots.hasPhatLoot(name)) {
            sender.sendMessage("§4PhatLoot §6" + name + "§4 does not exist.");
            return;
        }

        PhatLoot phatLoot = PhatLoots.getPhatLoot(name);

        phatLoot.addChest(block);
        sender.sendMessage("§5Target " + blockName + " has been linked to PhatLoot §6" + name);
        phatLoot.saveChests();
    }

    /**
     * Finds all Chests within the selected RegionOwn Region and links them to the specified PhatLoot
     *
     * @param player The Player who has a Region selected
     * @param name The name of the PhatLoot the Chests will be linked to
     */
    public static void regionLink(Player player, String name) {
        if (!Bukkit.getPluginManager().isPluginEnabled("RegionOwn")) {
            player.sendMessage("You must install RegionOwn to use that command");
            return;
        }

        //Cancel if the PhatLoot with the given name does not exist
        if (!PhatLoots.hasPhatLoot(name)) {
            player.sendMessage("§4PhatLoot §6" + name + "§4 does not exist.");
            return;
        }

        PhatLoot phatLoot = PhatLoots.getPhatLoot(name);

        if (RegionSelector.isSelecting(player)) {
            RegionSelector.endSelection(player);
        }

        if (!RegionSelector.hasSelection(player)) {
            player.sendMessage("You must first select a Region");
            return;
        }

        Region region = RegionSelector.getSelection(player);
        int chests = 0;

        for (Block block : region.getBlocks()) {
            if (block.getType() == Material.CHEST) {
                Chest chest = (Chest) block.getState();
                Inventory inventory = chest.getInventory();

                //Linked the left side if it is a DoubleChest
                if (inventory instanceof DoubleChestInventory) {
                    chest = (Chest) ((DoubleChestInventory) inventory).getLeftSide().getHolder();
                    block = chest.getBlock();
                }

                phatLoot.addChest(block);
                chests++;
            }
        }

        player.sendMessage("§6" + chests + "§5 chests have been linked to PhatLoot §5" + name);
        phatLoot.saveChests();
    }

    /**
     * Unlinks the target Block from the specified PhatLoots
     * If a name is not provided, a list of PhatLoots linked to the target Block is displayed
     *
     * @param sender The CommandSender unlinking the Block they are targeting
     * @param name The name of the PhatLoot to unlink
     */
    public static void unlink(CommandSender sender, String name) {
    	//Cancel if the sender is console
    	if (!(sender instanceof Player)) {
            sender.sendMessage("§4You cannot do this from the console!");
            return;
    	}

        Block block = ((Player) sender).getTargetBlock(TRANSPARENT, 10);
        for (PhatLoot phatLoot : getPhatLoots(sender, name)) {
            phatLoot.removeChest(block);
            sender.sendMessage("§5Target " + block.getType().toString() + " has been unlinked from PhatLoot §6" + phatLoot.name);
            phatLoot.saveChests();
        }
    }

    /**
     * Modifies the reset time of the specified PhatLoot
     *
     * @param sender The CommandSender modifying the PhatLoot
     * @param name The name of the PhatLoot to be modified or null to indicate all linked PhatLoots
     * @param days The amount of days
     * @param hours The amount of hours
     * @param minutes The amount of minutes
     * @param seconds The amount of seconds
     */
    public static void time(CommandSender sender, String name, int days, int hours, int minutes, int seconds) {
        for (PhatLoot phatLoot : getPhatLoots(sender, name)) {
            phatLoot.days = days;
            phatLoot.hours = hours;
            phatLoot.minutes = minutes;
            phatLoot.seconds = seconds;
            sender.sendMessage("§5Reset time for PhatLoot §6" + phatLoot.name
                    + "§5 has been set to §6" + days + " days, "
                    + hours + " hours, " + minutes + " minutes, and "
                    + seconds + " seconds");

            phatLoot.save();
        }
    }

    /**
     * Modifies global of the specified PhatLoot
     *
     * @param sender The CommandSender modifying the PhatLoot
     * @param name The name of the PhatLoot to be modified or null to indicate all linked PhatLoots
     * @param global The new value of global
     */
    public static void global(CommandSender sender, String name, boolean global) {
        for (PhatLoot phatLoot : getPhatLoots(sender, name)) {
            if (phatLoot.global != global) {
                phatLoot.global = global;
                phatLoot.reset(null);

                sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 has been set to §6"
                        + (global ? "global" : "individual") + "§5 reset");
            }
            phatLoot.save();
        }
    }

    /**
     * Modifies autoLoot of the specified PhatLoot
     *
     * @param sender The CommandSender modifying the PhatLoot
     * @param name The name of the PhatLoot to be modified or null to indicate all linked PhatLoots
     * @param autoLoot The new value of autoLoot
     */
    public static void autoLoot(CommandSender sender, String name, boolean autoLoot) {
        for (PhatLoot phatLoot : getPhatLoots(sender, name)) {
            if (phatLoot.autoLoot != autoLoot) {
                phatLoot.autoLoot = autoLoot;

                sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 has been set to"
                        + (autoLoot ? "automatically add Loot to the looters inventory." : "open the chest inventory for the looter."));
            }
            phatLoot.save();
        }
    }

    /**
     * Modifies breakAndRespawn of the specified PhatLoot
     *
     * @param sender The CommandSender modifying the PhatLoot
     * @param name The name of the PhatLoot to be modified or null to indicate all linked PhatLoots
     * @param breakAndRespawn The new value of breakAndRespawn
     */
    public static void breakAndRespawn(CommandSender sender, String name, boolean breakAndRespawn) {
        for (PhatLoot phatLoot : getPhatLoots(sender, name)) {
            if (phatLoot.breakAndRespawn != breakAndRespawn) {
                phatLoot.breakAndRespawn = breakAndRespawn;

                sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 has been set to"
                        + (breakAndRespawn
                           ? "automatically break global chests when they are looted and have them respawn."
                           : "keep chests present after looting."));
            }
            phatLoot.save();
        }
    }

    /**
     * Modifies round of the specified PhatLoot
     *
     * @param sender The CommandSender modifying the PhatLoot
     * @param name The name of the PhatLoot to be modified or null to indicate all linked PhatLoots
     * @param round The new value of round
     */
    public static void round(CommandSender sender, String name, boolean round) {
        for (PhatLoot phatLoot : getPhatLoots(sender, name)) {
            phatLoot.round = round;

            sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 has been set to §6"
                    + (round ? "" : "not ") + "round down time");
            phatLoot.save();
        }
    }

    /**
     * Adds/Removes a Loot to the specified PhatLoot
     *
     * @param sender The CommandSender modifying the PhatLoot
     * @param phatLootName The name of the PhatLoot to be modified or null to indicate all linked PhatLoots
     * @param add True the Loot will be added, false if it will be removed
     * @param collName The id of the Loot, 0 for individual loots
     * @param loot The Loot that will be added/removed
     */
    public static void setLoot(CommandSender sender, String phatLootName, boolean add, String collName, Loot loot) {
        String lootDescription = loot.toString();

        for (PhatLoot phatLoot : getPhatLoots(sender, phatLootName)) {
            //Check if a LootCollection was specified
            LootCollection coll = null;
            if (collName != null) {
                coll = phatLoot.findCollection(collName);
                if (coll == null) {
                    sender.sendMessage("§4Collection §6" + collName + "§4 does not exist");
                    return;
                }
            }

            if (coll == null) {
                if (add) { //Add to PhatLoot
                    if (phatLoot.addLoot(loot)) { //Successful
                        sender.sendMessage("§6" + lootDescription
                                + "§5 added as Loot for PhatLoot §6"
                                + phatLoot.name);
                        phatLoot.save();
                    } else { //Unsuccessful
                        sender.sendMessage("§6" + lootDescription
                                + "§4 is already Loot for PhatLoot §6"
                                + phatLoot.name);
                    }
                } else { //Remove from PhatLoot
                    if (phatLoot.removeLoot(loot)) { //Successful
                        sender.sendMessage("§6" + lootDescription
                                + "§5 removed as Loot for PhatLoot §6"
                                + phatLoot.name);
                        phatLoot.save();
                    } else { //Unsuccessful
                        sender.sendMessage("§6" + lootDescription
                                + "§4 is not Loot for PhatLoot §6"
                                + phatLoot.name);
                    }
                }
            } else {
                if (add) { //Add to LootCollection
                    if (coll.addLoot(loot)) { //Successful
                        sender.sendMessage("§6" + lootDescription
                                + "§5 added as Loot for Collection §6"
                                + coll.name);
                        phatLoot.save();
                    } else { //Unsuccessful
                        sender.sendMessage("§6" + lootDescription
                                + "§4 is already Loot for Collection §6"
                                + coll.name);
                    }
                } else { //Remove from LootCollection
                    if (coll.removeLoot(loot)) { //Successful
                        sender.sendMessage("§6" + lootDescription
                                + "§5 removed as Loot for Collection §6"
                                + coll.name);
                        phatLoot.save();
                    } else { //Unsuccessful
                        sender.sendMessage("§6" + lootDescription
                                + "§4 is not Loot for Collection §6"
                                + coll.name);
                    }
                }
            }
        }
    }

    /**
     * Sets the cost range of the specified PhatLoot
     *
     * @param sender The CommandSender modifying the PhatLoot
     * @param name The name of the PhatLoot to be modified or null to indicate all linked PhatLoots
     * @param amount The new cost
     */
    public static void setCost(CommandSender sender, String name, int amount) {
        if (amount > 0) {
            amount = -amount;
        }

        for (PhatLoot phatLoot : getPhatLoots(sender, name)) {
            phatLoot.moneyLower = amount;
            phatLoot.moneyUpper = amount;

            sender.sendMessage("§5Players will now be charged §6"
                    + -amount + "§5 to loot §6" + phatLoot.name);
            phatLoot.save();
        }
    }

    /**
     * Sets the money range of the specified PhatLoot
     *
     * @param sender The CommandSender modifying the PhatLoot
     * @param name The name of the PhatLoot to be modified or null to indicate all linked PhatLoots
     * @param amount The String of the range of the amount
     */
    public static void setMoney(CommandSender sender, String name, String amount) {
        int lower = getLowerBound(amount);
        int upper = getUpperBound(amount);
        if (lower == -1 || upper == -1) {
            sender.sendMessage("§6" + amount + " §4is not a valid number or range");
            return;
        }

        for (PhatLoot phatLoot : getPhatLoots(sender, name)) {
            phatLoot.moneyLower = lower;
            phatLoot.moneyUpper = upper;

            sender.sendMessage("§5Money for PhatLoot §6"
                    + phatLoot.name + "§5 set to "
                    + (lower == upper
                       ? "§6"
                       : "a range from §6" + lower + "§5 to §6")
                    + upper);
            phatLoot.save();
        }
    }

    /**
     * Sets the experience range of the specified PhatLoot
     *
     * @param sender The CommandSender modifying the PhatLoot
     * @param name The name of the PhatLoot to be modified or null to indicate all linked PhatLoots
     * @param amount The String of the range of the amount
     */
    public static void setExp(CommandSender sender, String name, String amount) {
        int lower = getLowerBound(amount);
        int upper = getUpperBound(amount);
        if (lower == -1 || upper == -1) {
            sender.sendMessage("§6" + amount + " §4is not a valid number or range");
            return;
        }

        for (PhatLoot phatLoot : getPhatLoots(sender, name)) {
            phatLoot.expLower = lower;
            phatLoot.expUpper = upper;

            sender.sendMessage("§5Experience for PhatLoot §6"
                    + phatLoot.name + "§5 set to "
                    + (lower == upper
                       ? "§6"
                       : "a range from §6" + lower + "§5 to §6")
                    + upper);
            phatLoot.save();
        }
    }

    /**
     * Displays a list of current PhatLoot
     *
     * @param sender The CommandSender requesting the list
     */
    public static void list(CommandSender sender) {
        String list = "§5Current PhatLoots: §6";

        //Concat each PhatLoot
        for (PhatLoot phatLoot : PhatLoots.getPhatLoots()) {
            list += phatLoot.name + ", ";
        }

        sender.sendMessage(list.substring(0, list.length() - 2));
    }

    /**
     * Displays the info of the specified PhatLoot
     * If a name is not provided, a list of PhatLoots linked to the target Block is displayed
     *
     * @param sender The CommandSender requesting the info
     * @param name The name of the PhatLoot
     */
    public static void info(CommandSender sender, String name) {
        LinkedList<PhatLoot> phatLoots = getPhatLoots(sender, name);
        switch (phatLoots.size()) {
        case 0:
            break;

        case 1: //Display information for the one PhatLoot
            PhatLoot phatLoot = phatLoots.getFirst();
            if (sender instanceof Player) {
                PhatLootInfoListener.viewPhatLoot((Player) sender, phatLoot);
            } else {
                sender.sendMessage("§2Name:§b " + phatLoot.name
                        + " §2Global Reset:§b " + phatLoot.global
                        + " §2Round Down:§b " + phatLoot.round);
                sender.sendMessage("§2Reset Time:§b " + phatLoot.days
                        + " days, " + phatLoot.hours + " hours, "
                        + phatLoot.minutes + " minutes, and "
                        + phatLoot.seconds + " seconds.");
                sender.sendMessage("§2Money§b: " + phatLoot.moneyLower + "-"
                        + phatLoot.moneyUpper + " §2Experience§b: "
                        + phatLoot.expLower + "-" + phatLoot.expUpper);
            }
            break;

        default: //List all PhatLoots
            String list = "§5Linked PhatLoots: §6";

            //Concat each PhatLoot
            for (PhatLoot pl : phatLoots) {
                list += pl.name + ", ";
            }

            sender.sendMessage(list.substring(0, list.length() - 2));
            break;
        }
    }

    /**
     * Reset the use times of the specified PhatLoot/PhatLootChest
     * If a name is not provided, the target PhatLootChest is reset
     *
     * @param sender The CommandSender reseting the PhatLootChests
     * @param name The name of the PhatLoot
     */
    public static void reset(CommandSender sender, String name) {
        //Reset the target Chest if a name was not provided
        if (name != null) {
            //Reset all Chests in every PhatLoot if the name provided is 'all'
            if (name.equals("all")) {
                for (PhatLoot phatLoots : PhatLoots.getPhatLoots()) {
                    phatLoots.reset(null);
                }

                if (sender != null) {
                    sender.sendMessage("§5All Chests in each PhatLoot have been reset.");
                }
                return;
            }

            //Find the PhatLoot that will be reset using the given name
            PhatLoot phatLoot = PhatLoots.getPhatLoot(name);

            //Cancel if the PhatLoot does not exist
            if (!PhatLoots.hasPhatLoot(name)) {
                if (sender != null) {
                    sender.sendMessage("§4PhatLoot §6" + name + "§4 does not exsist.");
                }
                return;
            }

            //Reset all Chests linked to the PhatLoot
            phatLoot.reset(null);

            if (sender != null) {
                sender.sendMessage("§5All Chests in PhatLoot §6"
                                    + name + "§5 have been reset.");
            }
        } else if (sender != null) {
            //Cancel is the sender is console
            if (!(sender instanceof Player)) {
                sender.sendMessage("§4You cannot do this from the console!");
                return;
            }

            Block block = ((Player) sender).getTargetBlock(TRANSPARENT, 10);
            for (PhatLoot phatLoot : getPhatLoots(sender, name)) {
                phatLoot.reset(block);
                sender.sendMessage("§5Target "+ block.getType().toString() + " has been reset.");
            }
        }
    }

    /**
     * Clean the use times of the specified PhatLoot/PhatLootChest
     * If a name is not provided, the target PhatLootChest is cleaned
     *
     * @param sender The CommandSender cleaning the PhatLootChests
     * @param name The name of the PhatLoot
     */
    public static void clean(CommandSender sender, String name) {
        //Clean the target Chest if a name was not provided
        if (name != null) {
            //Clean all Chests in every PhatLoot if the name provided is 'all'
            if (name.equals("all")) {
                for (PhatLoot phatLoots : PhatLoots.getPhatLoots()) {
                    phatLoots.clean(null);
                }

                if (sender != null) {
                    sender.sendMessage("§5All Chests in each PhatLoot have been reset.");
                }
                return;
            }

            //Find the PhatLoot that will be reset using the given name
            PhatLoot phatLoot = PhatLoots.getPhatLoot(name);

            //Cancel if the PhatLoot does not exist
            if (!PhatLoots.hasPhatLoot(name)) {
                if (sender != null) {
                    sender.sendMessage("§4PhatLoot §6" + name + "§4 does not exsist.");
                }
                return;
            }

            //Clean all Chests linked to the PhatLoot
            phatLoot.clean(null);

            if (sender != null) {
                sender.sendMessage("§5All Chests in PhatLoot §6"
                                    + name + "§5 have been reset.");
            }
        } else if (sender != null) {
        	//Cancel is the sender is console
        	if (!(sender instanceof Player)) {
                    sender.sendMessage("§4You cannot do this from the console!");
                    return;
        	}

            Block block = ((Player) sender).getTargetBlock(TRANSPARENT, 10);
            for (PhatLoot phatLoot : getPhatLoots(sender, name)) {
                phatLoot.clean(block);
                sender.sendMessage("§5Target "+ block.getType().toString() + " has been reset.");
            }
        }
    }

    /**
     * Displays the PhatLoots Help Page to the given Player
     *
     * @param sender The CommandSender needing help
     */
    private static void sendHelp(CommandSender sender) {
        //If the Player has none of the below permissions then they need not see the help page
        if (!sender.hasPermission("phatloots.make") && !sender.hasPermission("phatloots.rl")
                 && !sender.hasPermission("phatloots.reset") && !sender.hasPermission("phatloots.list")
                 && !sender.hasPermission("phatloots.info") && !sender.hasPermission("phatloots.name")
                 && !sender.hasPermission("phatloots.give")) {
            return;
        }
        sender.sendMessage("§e     PhatLoots Help Page:");
        sender.sendMessage("§2/"+command+" <Name>§b Loot a virtual Chest for the given PhatLoot");
        sender.sendMessage("§2/"+command+" list§b List all PhatLoots");
        sender.sendMessage("§2/"+command+" info [Name]§b Open info GUI of PhatLoot");
        sender.sendMessage("§2/"+command+" give <Player> <PhatLoot> [Title]§b Force Player to loot a PhatLoot");
        sender.sendMessage("§2/"+command+" reset§b Reset looted times for target Block");
        sender.sendMessage("§2/"+command+" reset <Name>§b Reset looted times for PhatLoot");
        sender.sendMessage("§2/"+command+" reset all§b Reset looted times for all PhatLoots");
        sender.sendMessage("§2/"+command+" clean§b Clean looted times for target Block");
        sender.sendMessage("§2/"+command+" clean <Name>§b Clean looted times for PhatLoot");
        sender.sendMessage("§2/"+command+" clean all§b Clean looted times for all PhatLoots");
        sender.sendMessage("§2/"+command+" help create§b Display PhatLoots Create Help Page");
        sender.sendMessage("§2/"+command+" help setup§b Display PhatLoots Setup Help Page");
        sender.sendMessage("§2/"+command+" help loot§b Display PhatLoots Manage Loot Help Page");
        sender.sendMessage("§2/"+command+" rl§b Reload the PhatLoots Plugin");
    }

    /**
     * Displays the PhatLoots Create Help Page to the given Player
     *
     * @param sender The CommandSender needing help
     */
    private static void sendCreateHelp(CommandSender sender) {
        sender.sendMessage("§e     PhatLoots Create Help Page:");
        sender.sendMessage("§2/"+command+" make <Name>§b Create PhatLoot with given name");
        sender.sendMessage("§2/"+command+" delete <Name>§b Delete PhatLoot");
        sender.sendMessage("§2/"+command+" link <Name>§b Link target Chest/Dispenser with PhatLoot");
        sender.sendMessage("§2/"+command+" unlink [Name]§b Unlink target Block from PhatLoot");
    }

    /**
     * Displays the PhatLoots Setup Help Page to the given Player
     *
     * @param sender The CommandSender needing help
     */
    private static void sendSetupHelp(CommandSender sender) {
        sender.sendMessage("§e     PhatLoots Setup Help Page:");
        sender.sendMessage("§7If Name is not specified then all PhatLoots linked to the target Block will be affected");
        sender.sendMessage("§6Amount may be a number §4(100)§6 or range §4(100-500)");
        sender.sendMessage("§2/"+command+" time [Name] <Days> <Hrs> <Mins> <Secs>§b Set cooldown time for PhatLoot");
        sender.sendMessage("§2/"+command+" time [Name] never§b Set PhatLoot to only be lootable once per chest");
        sender.sendMessage("§2/"+command+" global [Name] <true|false>§b Set PhatLoot to global or individual");
        sender.sendMessage("§2/"+command+" autoloot [Name] <true|false>§b Set if Items are automatically looted");
        sender.sendMessage("§2/"+command+" autoloot [Name] <true|false>§b Set if global Chests are broken after looting");
        sender.sendMessage("§2/"+command+" round [Name] <true|false>§b Set if cooldown times should round down (ex. Daily/Hourly loots)");
        sender.sendMessage("§2/"+command+" cost [Name] <Amount>§b Set cost of looting");
        sender.sendMessage("§2/"+command+" money [Name] <Amount>§b Set money range to be looted");
        sender.sendMessage("§2/"+command+" exp [Name] <Amount>§b Set experience to be gained");
    }

    /**
     * Displays the PhatLoots Sign Help Page to the given Player
     *
     * @param sender The CommandSender needing help
     */
    private static void sendLootHelp(CommandSender sender) {
        sender.sendMessage("§e     PhatLoots Manage Loot Help Page:");
        sender.sendMessage("§5A Parameter starts with the 1 character §2id");
        sender.sendMessage("§2p§f: §5The Name of the PhatLoot ex. §6pEpic");
        sender.sendMessage("§bIf PhatLoot is not specified then all PhatLoots linked to the target Block will be affected");
        sender.sendMessage("§2%§f: §5The chance of looting ex. §6%50 §5or §6%0.1 §5(default: §6100§5)");
        sender.sendMessage("§2c§f: §5The name of the collection to add the loot to ex. §6cFood");
        sender.sendMessage("§2#§f: §5The amount to be looted ex. §6#10 §5or §6#1-64");
        sender.sendMessage("§bUse §6#0 §bif you want each Loot in a collection to be rolled for individually");
        sender.sendMessage("§2d§f: §5The data/durability value of the item ex. §6d5");
        sender.sendMessage("§2t§f: §5Tier the Item (tiers.yml) ex. §6t");
        sender.sendMessage("§2l§f: §5Generate Lore for the Item (lores.yml) ex. §l");
        sender.sendMessage("§2e§f: §5The item enchantment ex. §6earrow_fire §5or §6eauto");
        sender.sendMessage("§bEnchantment levels can be added. ex. §6arrow_fire(2)");
        sender.sendMessage("§2/"+command+" <add|remove> <Item|ID|hand> [Parameter1] [Parameter2]...");
        sender.sendMessage("§bex. §6/"+command+" add hand #1-16 %32");
        sender.sendMessage("§bex. §6/"+command+" add diamond_sword efire_aspect(2) edamage_all %75 cWeapon");
        sender.sendMessage("§2/"+command+" <add|remove> coll <Name> [Parameter1] [Parameter2]...");
        sender.sendMessage("§bex. §6/"+command+" add coll Weapon %25");
        sender.sendMessage("§2/"+command+" <add|remove> cmd [Parameter1] [Parameter2]... /<Command>");
        sender.sendMessage("§bTutorial Video (OUTDATED):");
        sender.sendMessage("§1§nwww.youtu.be/tRQuKbRTaA4");
    }

    /**
     * Returns the a LinkedList of PhatLoots
     * If a name is provided then only the PhatLoot with the given name will be in the List
     * If no name is provided then each PhatLoot that is linked to the target Block will be in the List
     *
     * @param sender The CommandSender targeting a Block
     * @param name The name of the PhatLoot to be found
     * @return The a LinkedList of PhatLoots
     */
    public static LinkedList<PhatLoot> getPhatLoots(CommandSender sender, String name) {
        LinkedList<PhatLoot> phatLoots = new LinkedList<PhatLoot>();

        if (name != null) {
            //Find the PhatLoot using the given name
            PhatLoot phatLoot = PhatLoots.getPhatLoot(name);

            //Inform the sender if the PhatLoot does not exist
            if (phatLoot != null ) {
                phatLoots.add(phatLoot);
            } else {
                sender.sendMessage("§4PhatLoot §6" + name + "§4 does not exist.");
            }
        } else {
            //Cancel is the sender is console
            if (!(sender instanceof Player)) {
                sender.sendMessage("§4You cannot do this from the console!");
                return phatLoots;
            }

            //Cancel if the sender is not targeting a correct Block
            Block block = ((Player) sender).getTargetBlock(TRANSPARENT, 10);
            String blockName = block.getType().toString();
            if (!PhatLoots.isLinkableType(block)) {
                sender.sendMessage("§6" + blockName + "§4 is not a linkable type.");
                return phatLoots;
            }

            phatLoots = PhatLoots.getPhatLoots(block);

            //Inform the sender if the Block is not linked to any PhatLoots
            if (phatLoots.isEmpty()) {
                sender.sendMessage("§4Target " + blockName + " is not linked to a PhatLoot");
            }
        }

        return phatLoots;
    }

    /**
     * Retrieves an int value from the given string
     *
     * @param sender The CommandSender that will receive error messages
     * @param string The String that contains the amount
     */
    public static int getLowerBound(String string) {
        if (string.contains("-")) {
            string = string.substring(0, string.indexOf('-'));
        }

        try {
            return Integer.parseInt(string);
        } catch (Exception notInt) {
            return -1;
        }
    }

    /**
     * Retrieves an int value from the given string
     *
     * @param sender The CommandSender that will receive error messages
     * @param string The String that contains the amount
     */
    public static int getUpperBound(String string) {
        if (string.contains("-")) {
            string = string.substring(string.indexOf('-') + 1);
        }

        try {
            return Integer.parseInt(string);
        } catch (Exception notInt) {
            return -1;
        }
    }

    /**
     * Retrieves an int value from the given string
     *
     * @param sender The CommandSender that will receive error messages
     * @param string The String that contains the item
     */
    public static ItemStack getItemStack(CommandSender sender, String string) {
        if (string.equals("hand")) {
            if (sender instanceof Player) {
                return ((Player) sender).getItemInHand().clone();
            }
        }

        Material material;
        if (string.matches("[0-9]+")) {
            int id = Integer.parseInt(string);
            material = Material.getMaterial(id);
        } else {
            //Verify that the item id is valid
            material = Material.getMaterial(string.toUpperCase());
        }

        //Verify that the item is valid
        if (material == null) {
            if (sender != null) {
                sender.sendMessage("§6" + string + "§4 is not a valid item id");
            }
            return null;
        }

        return new ItemStack(material);
    }

    /**
     * Retrieves Enchantments from the given string
     *
     * @param string The String that contains the item
     * @return The Enchantments of the item
     */
    public static Map<Enchantment, Integer> getEnchantments(String string) {
        Map<Enchantment, Integer> enchantments = new HashMap<Enchantment, Integer>();
        try {
            for (String split: string.split("&")) {
                Enchantment enchantment = null;
                int level = -1;

                if (split.contains("(")) {
                    int index = split.indexOf('(');
                    level = Integer.parseInt(split.substring(index + 1, split.length() - 1));
                    split = split.substring(0, index);
                }

                for (Enchantment enchant: Enchantment.values()) {
                    if (enchant.getName().equalsIgnoreCase(split)) {
                        enchantment = enchant;
                    }
                }

                if (level < enchantment.getStartLevel()) {
                    level = enchantment.getStartLevel();
                }

                enchantments.put(enchantment, level);
            }
        } catch (Exception notEnchantment) {
            return null;
        }
        return enchantments;
    }

    /**
     * Retrieves a short value from the given string
     *
     * @param sender The CommandSender that will receive error messages
     * @param string The String that contains the item
     */
    public static short getData(String string) {
        short data;
        try {
            data = Short.parseShort(string);
        } catch (Exception notShort) {
            return -1;
        }
        return data;
    }

    /**
     * Retrieves a double value from the given string that ends with %
     *
     * @param sender The CommandSender that will receive error messages
     * @param string The String that contains the percent
     */
    public static double getPercent(CommandSender sender, String string) {
        double percent;
        try {
            percent = Double.parseDouble(string);
            if (percent < 0) {
                if (sender != null) {
                    sender.sendMessage("§4The percent cannot be below 0");
                }
            }
            if (percent > 100) {
                if (sender != null) {
                    sender.sendMessage("§4The percent cannot be above 100");
                }
            } else {
                return percent;
            }
        } catch (Exception notDouble) {
            if (sender != null) {
                sender.sendMessage("§6" + string + "§4 is not a valid number");
            }
        }
        return -1;
    }

    /**
     * Concats arguments together to create a sentence from words
     * This also replaces & with § to add color codes
     *
     * @param args the arguments to concat
     * @param first Which argument should the sentence start with
     * @return The new String that was created
     */
    private static String concatArgs(String[] args, int first) {
        StringBuilder sb = new StringBuilder();
        if (first > args.length) {
            return "";
        }
        for (int i = first; i <= args.length - 1; i++) {
            sb.append(" ");
            sb.append(args[i]);
        }
        String string = sb.substring(1);
        return ChatColor.translateAlternateColorCodes('&', string);
    }
}

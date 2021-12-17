package de.rapha149.fogremover;

import de.rapha149.fogremover.version.VersionWrapper;
import io.netty.channel.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class FogRemover extends JavaPlugin {

    private static final String HANDLER_NAME = "FogRemover";
    private VersionWrapper WRAPPER;
    private FileConfiguration config;
    private File messageFile;
    private FileConfiguration messageConfig;

    @Override
    public void onEnable() {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1);
        try {
            WRAPPER = (VersionWrapper) Class.forName(VersionWrapper.class.getPackage().getName() + ".Wrapper" + version).newInstance();
        } catch (IllegalAccessException | InstantiationException exception) {
            throw new IllegalStateException("Failed to load support for server version " + version, exception);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("FogRemover does not support the server version \"" + version + "\"", exception);
        }

        messageFile = new File(getDataFolder(), "messages.yml");
        messageConfig = new YamlConfiguration();
        messageConfig.addDefault("prefix", "&8[&4FogRemover&8] ");
        messageConfig.addDefault("plugin.enable", "Plugin successfully enabled.");
        messageConfig.addDefault("plugin.disable", "Plugin disabled.");
        messageConfig.addDefault("plugin.invalid_distance", "The view distance set in the config is invalid. It has to be between 2 and 32.");
        messageConfig.addDefault("plugin.invalid_individual_distance", "The individual view distance for %player% " +
                                                                       "set in the config is invalid. It has to be between 2 and 32.");
        messageConfig.addDefault("syntax", "%prefix%&cSyntax error! Please use &7/%syntax%&c.");
        messageConfig.addDefault("error", "%prefix%&cAn error occured. Check the console for details.");
        messageConfig.addDefault("no_permission", "%prefix%&cYou do not have enough permissions to perform this action.");
        messageConfig.addDefault("player_not_found", "%prefix%&cThe player &7%player% &ccould not be found.");
        messageConfig.addDefault("reload", "%prefix%&7Config was reloaded.");
        messageConfig.addDefault("feature_not_enabled", "%prefix%&cThis feature is not enabled. You can enable it in the config.");
        messageConfig.addDefault("get.self.return", "%prefix%&7Your individual view distance is &6%distance%&7.");
        messageConfig.addDefault("get.self.not_set", "%prefix%&7Your individual view distance is &6not set&7.");
        messageConfig.addDefault("get.others.return", "%prefix%&7The individual view distance of &5%player% &7is &6%distance%&7.");
        messageConfig.addDefault("get.others.not_set", "%prefix%&7The individual view distance of &5%player% &7is &6not set&7.");
        messageConfig.addDefault("set.state_number", "%prefix%&cPlease state a number.");
        messageConfig.addDefault("set.out_of_bounds", "%prefix%&cPlease state a number between 2 and 32.");
        messageConfig.addDefault("set.self.already_set", "%prefix%&cYour individual view distance is already set to &7%distance%&c.");
        messageConfig.addDefault("set.self.success", "%prefix%&7Your individual view distance was set to &6%distance%&7.");
        messageConfig.addDefault("set.others.already_set", "%prefix%&cThe individual view distance of &5%player% &cis already set to &7%distance%&c.");
        messageConfig.addDefault("set.others.success", "%prefix%&7The individual view distance of &5%player% &7was set to &6%distance%&7.");
        messageConfig.addDefault("unset.self.does_not_exist", "%prefix%&cYou do not have an individual view distance.");
        messageConfig.addDefault("unset.self.success", "%prefix%&7Your individual view distance was removed.");
        messageConfig.addDefault("unset.others.does_not_exist", "%prefix%&5%player% &cdoes not have an individual view distance.");
        messageConfig.addDefault("unset.others.success", "%prefix%&7The individual view distance of &5%player% &7was removed.");

        loadMessages();
        loadConfig();

        try {
            registerHandler();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("fog").setExecutor(this);
        getLogger().info(getMessage("plugin.enable"));
    }

    @Override
    public void onDisable() {
        try {
            unregisterHandler();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        getLogger().info(getMessage("plugin.disable"));
    }

    private void loadConfig() {
        config = getConfig();
        config.addDefault("enabled", true);
        config.addDefault("view-distance", 32);
        config.addDefault("individual-distances.enabled", false);
        if (!config.isConfigurationSection("individual-distances.players"))
            config.createSection("individual-distances.players");
        config.options().copyDefaults(true);
        saveConfig();
        checkViewDistances();
    }

    private void loadMessages() {
        try {
            if (messageFile.exists())
                messageConfig.load(messageFile);
            messageConfig.options().copyDefaults(true);
            messageConfig.save(messageFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            getLogger().severe("Failed to load message config.");
            messageConfig.options().copyDefaults(true);
        }
    }

    private String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', messageConfig.getString(key, "")
                .replace("%prefix%", messageConfig.getString("prefix")));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("fogremover")) {
            sender.sendMessage(getMessage("no_permission"));
            return true;
        }
        if (args.length < 1 || !args[0].toLowerCase().matches("reload|get|set|unset")) {
            sender.sendMessage(getMessage("syntax").replace("%syntax%", alias + " <reload|get|set|unset>"));
            return true;
        }

        String arg = args[0].toLowerCase();
        switch (arg) {
            case "reload":
                if (!sender.hasPermission("fogremover.reload")) {
                    sender.sendMessage(getMessage("no_permission"));
                    break;
                }

                loadMessages();
                reloadConfig();
                loadConfig();
                config = getConfig();
                config.options().copyDefaults(true);
                saveConfig();
                checkViewDistances();
                try {
                    unregisterHandler();
                    registerHandler();
                    sender.sendMessage(getMessage("reload"));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                    sender.sendMessage(getMessage("error"));
                }
                break;
            case "get":
            case "set":
            case "unset":
                if (!config.getBoolean("individual-distances.enabled")) {
                    sender.sendMessage(getMessage("feature_not_enabled"));
                    break;
                }

                if (!sender.hasPermission("fogremover.individual")) {
                    sender.sendMessage(getMessage("no_permission"));
                    break;
                }

                boolean set = arg.equals("set");
                boolean permOthers = sender.hasPermission("fogremover.individual.others");
                boolean self;
                OfflinePlayer target;
                if (args.length >= (set ? 3 : 2) && permOthers) {
                    self = false;
                    target = Bukkit.getOfflinePlayer(args[set ? 2 : 1]);
                    if (!target.hasPlayedBefore()) {
                        sender.sendMessage(getMessage("player_not_found").replace("%player%", args[set ? 2 : 1]));
                        break;
                    }
                } else if (sender instanceof Player) {
                    self = true;
                    target = (Player) sender;
                } else {
                    sender.sendMessage(getMessage("syntax").replace("%syntax%",
                            alias + " " + arg + (set ? " <View Distance>" : "") + " <Player>"));
                    break;
                }

                String key = "individual-distances.players." + target.getUniqueId();
                String messagePrefix = arg + "." + (self ? "self" : "others") + ".";
                if (arg.equals("get")) {
                    if (config.isSet(key)) {
                        sender.sendMessage(getMessage(messagePrefix + "return")
                                .replace("%distance%", String.valueOf(checkViewDistance(config.getInt(key))))
                                .replace("%player%", target.getName()));
                    } else
                        sender.sendMessage(getMessage(messagePrefix + "not_set").replace("%player%", target.getName()));
                } else if (set) {
                    if (args.length >= 2) {
                        try {
                            int viewDistance = Integer.parseInt(args[1]);
                            if (viewDistance < 2 || viewDistance > 32) {
                                sender.sendMessage(getMessage("set.out_of_bounds"));
                                break;
                            }
                            if (config.getInt(key) == viewDistance) {
                                sender.sendMessage(getMessage(messagePrefix + "already_set")
                                        .replace("%distance%", String.valueOf(viewDistance))
                                        .replace("%player%", target.getName()));
                                break;
                            }

                            config.set(key, viewDistance);
                            saveConfig();
                            sender.sendMessage(getMessage(messagePrefix + "success")
                                    .replace("%distance%", String.valueOf(viewDistance))
                                    .replace("%player%", target.getName()));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(getMessage("set.state_number"));
                        }
                    } else
                        sender.sendMessage(getMessage("syntax").replace("%syntax%",
                                alias + " set <View Distance>" + (permOthers ? " [Player]" : "")));
                } else {
                    if (!config.isSet(key)) {
                        sender.sendMessage(getMessage(messagePrefix + "does_not_exist").replace("%player%", target.getName()));
                        break;
                    }

                    config.set(key, null);
                    saveConfig();
                    sender.sendMessage(getMessage(messagePrefix + "success").replace("%player%", target.getName()));
                }
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("fogremover.reload"))
                list.add("reload");
            if (sender.hasPermission("fogremover.individual"))
                list.addAll(Arrays.asList("get", "set", "unset"));
        }
        if (args[0].toLowerCase().matches("get|set|unset") &&
            sender.hasPermission("fogremover.individual")) {
            boolean set = args[0].equalsIgnoreCase("set");
            if (args.length == 2 && set)
                for (int i = 2; i <= 32; i++)
                    list.add(String.valueOf(i));
            if (args.length == (set ? 3 : 2) && sender.hasPermission("fogremover.individual"))
                Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).forEach(list::add);
        }

        String arg = args[args.length - 1].toLowerCase();
        List<String> completions = new ArrayList<>();
        list.stream().filter(s -> s.toLowerCase().startsWith(arg)).forEach(completions::add);
        return completions;
    }

    private int checkViewDistance(int distance) {
        return Math.max(2, Math.min(32, distance));
    }

    private void checkViewDistances() {
        if (config.getBoolean("enabled")) {
            int viewDistance = config.getInt("view-distance");
            if (viewDistance < 2 || viewDistance > 32)
                getLogger().warning(getMessage("plugin.invalid_distance"));
        }
        if (config.getBoolean("individual-distances.enabled")) {
            config.getConfigurationSection("individual-distances.players").getKeys(false).forEach(uuid -> {
                int viewDistance = config.getInt("individual-distances.players." + uuid);
                if (viewDistance < 2 || viewDistance > 32)
                    getLogger().warning(getMessage("plugin.invalid_individual_distance").replace("%player%", uuid));
            });
        }
    }

    private void registerHandler() throws NoSuchFieldException, IllegalAccessException {
        if (!config.getBoolean("enabled") && !config.getBoolean("individual-distances.enabled"))
            return;

        ChannelHandler packetInit = new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) {
                channel.eventLoop().submit(() -> {
                    ChannelPipeline pipeline = channel.pipeline();
                    if (!pipeline.names().contains(HANDLER_NAME)) {
                        pipeline.addAfter("packet_handler", HANDLER_NAME, new ChannelDuplexHandler() {

                            private UUID player;

                            @Override
                            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                try {
                                    if (msg.getClass() == WRAPPER.getLoginSuccessPacketClass())
                                        player = WRAPPER.getUUIDFromLoginPacket(msg);

                                    if (msg.getClass() == WRAPPER.getLoginPlayPacketClass()) {
                                        int viewDistance = -1;
                                        if (config.getBoolean("enabled"))
                                            viewDistance = config.getInt("view-distance");
                                        if (player != null && config.getBoolean("individual-distances.enabled") &&
                                            config.isSet("individual-distances.players." + player)) {
                                            viewDistance = config.getInt("individual-distances.players." + player);
                                        }

                                        if (viewDistance != -1)
                                            msg = WRAPPER.replaceViewDistance(msg, checkViewDistance(viewDistance));
                                        pipeline.remove(HANDLER_NAME);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                super.write(ctx, msg, promise);
                            }
                        });
                    }
                });
            }
        };
        ChannelHandler init = new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) {
                channel.pipeline().addLast(packetInit);
            }
        };
        ChannelHandler handler = new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                ((Channel) msg).pipeline().addFirst(init);
                ctx.fireChannelRead(msg);
            }
        };
        WRAPPER.getServerPipelines().forEach(pipeline -> {
            if (pipeline.names().contains(HANDLER_NAME))
                pipeline.remove(HANDLER_NAME);
            pipeline.addFirst(HANDLER_NAME, handler);
        });
    }

    private void unregisterHandler() throws NoSuchFieldException, IllegalAccessException {
        if (WRAPPER != null)
            WRAPPER.getServerPipelines().forEach(pipeline -> {
                if (pipeline.names().contains(HANDLER_NAME))
                    pipeline.remove(HANDLER_NAME);
            });
    }
}

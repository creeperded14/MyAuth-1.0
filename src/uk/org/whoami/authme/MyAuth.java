/*
 * Copyright 2013 creeperde14 [creeperdedpt@gmail.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.creeperded14.myauth;

import java.io.IOException;
import java.sql.SQLException;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import uk.org.creeperded14.myauth.cache.auth.PlayerAuth;
import uk.org.creeperded14.myauth.cache.auth.PlayerCache;
import uk.org.creeperded14.myauth.cache.limbo.LimboCache;
import uk.org.creeperded14.myauth.commands.AdminCommand;
import uk.org.creeperded14.myauth.commands.ChangePasswordCommand;
import uk.org.creeperded14.myauth.commands.LoginCommand;
import uk.org.creeperded14.myauth.commands.LogoutCommand;
import uk.org.creeperded14.myauth.commands.RegisterCommand;
import uk.org.creeperded14.myauth.commands.UnregisterCommand;
import uk.org.creeperded14.myauth.datasource.CacheDataSource;
import uk.org.creeperded14.myauth.datasource.DataSource;
import uk.org.creeperded14.myauth.datasource.FileDataSource;
import uk.org.creeperded14.myauth.datasource.MiniConnectionPoolManager.TimeoutException;
import uk.org.creeperded14.myauth.datasource.MySQLDataSource;
import uk.org.creeperded14.myauth.listener.MyAuthBlockListener;
import uk.org.creeperded14.myauth.listener.MyAuthEntityListener;
import uk.org.creeperded14.myauth.listener.MyAuthPlayerListener;
import uk.org.creeperded14.myauth.settings.Messages;
import uk.org.creeperded14.myauth.settings.Settings;
import uk.org.creeperded14.myauth.task.MessageTask;
import uk.org.creeperded14.myauth.task.TimeoutTask;

public class MyAuth extends JavaPlugin {

    private DataSource database;
    private Settings settings;
    private Messages m;

    @Override
    public void onEnable() {
        settings = Settings.getInstance();
        m = Messages.getInstance();

        switch (settings.getDataSource()) {
            case FILE:
                try {
                    database = new FileDataSource();
                } catch (IOException ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    this.getServer().getPluginManager().disablePlugin(this);
                    return;
                }
                break;
            case MYSQL:
                try {
                    database = new MySQLDataSource();
                } catch (ClassNotFoundException ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    this.getServer().getPluginManager().disablePlugin(this);
                    return;
                } catch (SQLException ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    this.getServer().getPluginManager().disablePlugin(this);
                    return;
                } catch(TimeoutException ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    this.getServer().getPluginManager().disablePlugin(this);
                    return;
                }
                break;
        }

        if (settings.isCachingEnabled()) {
            database = new CacheDataSource(database);
        }

        MyAuthPlayerListener playerListener = new MyAuthPlayerListener(this, database);
        MyAuthBlockListener blockListener = new MyAuthBlockListener(database);
        MyAuthEntityListener entityListener = new MyAuthEntityListener(database);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, playerListener,
                         Priority.Lowest, this);
        pm.registerEvent(Event.Type.PLAYER_CHAT, playerListener,
                         Priority.Lowest, this);
        pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener,
                         Priority.Lowest, this);
        pm.registerEvent(Event.Type.PLAYER_LOGIN, playerListener,
                         Priority.Lowest, this);
        pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener,
                         Priority.Lowest, this);
        pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener,
                         Priority.Lowest, this);
        pm.registerEvent(Event.Type.PLAYER_KICK, playerListener,
                         Priority.Lowest, this);
        pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener,
                         Priority.Lowest, this);
        pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener,
                         Priority.Lowest, this);
        pm.registerEvent(Event.Type.PLAYER_INTERACT_ENTITY, playerListener,
                         Priority.Low, this);
        pm.registerEvent(Event.Type.PLAYER_DROP_ITEM, playerListener,
                         Priority.Low, this);
        pm.registerEvent(Event.Type.PLAYER_BED_ENTER, playerListener,
                         Priority.Low, this);
        pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener,
                         Priority.Lowest, this);
        pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener,
                         Priority.Lowest, this);
        pm.registerEvent(Event.Type.FOOD_LEVEL_CHANGE, entityListener,
                         Priority.Low, this);
        pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener,
                         Priority.Lowest, this);
        pm.registerEvent(Event.Type.ENTITY_TARGET, entityListener,
                         Priority.Lowest, this);

        this.getCommand("myauth").setExecutor(new AdminCommand(database));
        this.getCommand("register").setExecutor(new RegisterCommand(database));
        this.getCommand("login").setExecutor(new LoginCommand(database));
        this.getCommand("changepassword").setExecutor(new ChangePasswordCommand(database));
        this.getCommand("logout").setExecutor(new LogoutCommand(this,database));
        this.getCommand("unregister").setExecutor(new UnregisterCommand(this, database));

        onReload(this.getServer().getOnlinePlayers());
        ConsoleLogger.info("Myauth " + this.getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        ConsoleLogger.info("Myauth " + this.getDescription().getVersion() + " disabled");
    }

    private void onReload(Player[] players) {
        for (Player player : players) {
            String name = player.getName().toLowerCase();
            String ip = player.getAddress().getAddress().getHostAddress();

            boolean authAvail = database.isAuthAvailable(name);

            if (authAvail) {
                if (settings.isSessionsEnabled()) {
                    PlayerAuth auth = database.getAuth(name);
                    if (auth.getNickname().equals(name) && auth.getIp().equals(ip)) {
                        PlayerCache.getInstance().addPlayer(auth);
                        player.sendMessage(m._("valid_session"));
                        break;
                    }
                }
            } else if (!settings.isForcedRegistrationEnabled()) {
                break;
            } else if (settings.isKickNonRegisteredEnabled()) {
                player.kickPlayer(m._("reg_only"));
                break;
            }

            LimboCache.getInstance().addLimboPlayer(player);
            player.getInventory().setArmorContents(new ItemStack[0]);
            player.getInventory().setContents(new ItemStack[36]);

            if (settings.isTeleportToSpawnEnabled()) {
                player.teleport(player.getWorld().getSpawnLocation());
            }

            String msg = authAvail ? m._("login_msg") : m._("reg_msg");
            int time = settings.getRegistrationTimeout() * 20;
            int msgInterval = settings.getWarnMessageInterval();
            BukkitScheduler sched = this.getServer().getScheduler();
            if (time != 0) {
                int id = sched.scheduleSyncDelayedTask(this, new TimeoutTask(this, name), time);
                LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
            }
            sched.scheduleSyncDelayedTask(this, new MessageTask(this, name, msg, msgInterval));
        }
    }
}

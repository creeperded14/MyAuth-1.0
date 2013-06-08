/*
 *Copyright 2013 creeperde14 [creeperdedpt@gmail.com]
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

package uk.org.creeperded14.myauth.commands;

import java.security.NoSuchAlgorithmException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import uk.org.creeperded14.myauth.ConsoleLogger;
import uk.org.creeperded14.myauth.cache.auth.PlayerAuth;
import uk.org.creeperded14.myauth.cache.auth.PlayerCache;
import uk.org.creeperded14.myauth.datasource.DataSource;
import uk.org.creeperded14.myauth.security.PasswordSecurity;
import uk.org.creeperded14.myauth.settings.Messages;
import uk.org.creeperded14.myauth.settings.Settings;

public class ChangePasswordCommand implements CommandExecutor {

    private Messages m = Messages.getInstance();
    private Settings settings = Settings.getInstance();
    private DataSource database;

    public ChangePasswordCommand(DataSource database) {
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        if (!sender.hasPermission("myauth." + label.toLowerCase())) {
            sender.sendMessage(m._("no_perm"));
            return true;
        }

        Player player = (Player) sender;
        String name = player.getName().toLowerCase();
        String ip = player.getAddress().getAddress().getHostAddress();

        if (!PlayerCache.getInstance().isAuthenticated(name)) {
            player.sendMessage(m._("not_logged_in"));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(m._("Usage: /changepassword oldPassword newPassword"));
            return true;
        }

        try {
            String hashnew = PasswordSecurity.getHash(settings.getPasswordHash(), args[1]);

            if (PasswordSecurity.comparePasswordWithHash(args[0], PlayerCache.getInstance().getAuth(name).getHash())) {
                PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
                auth.setHash(hashnew);
                if (!database.updatePassword(auth)) {
                    player.sendMessage(m._("error"));
                    return true;
                }
                PlayerCache.getInstance().updatePlayer(auth);
                player.sendMessage(m._("pwd_changed"));
                ConsoleLogger.info(player.getDisplayName() + " changed his password");
            } else {
                player.sendMessage(m._("wrong_pwd"));
            }
        } catch (NoSuchAlgorithmException ex) {
            ConsoleLogger.showError(ex.getMessage());
            sender.sendMessage(m._("error"));
        }
        return true;
    }
}

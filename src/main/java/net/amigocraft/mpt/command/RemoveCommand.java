/*
 * MPT (Map Packaging Tool)
 *
 * Copyright (c) 2014 Maxim Roncacé <https://github.com/mproncace>
 *
 * The MIT License (MIT)
 *
 *     Permission is hereby granted, free of charge, to any person obtaining a copy
 *     of this software and associated documentation files (the "Software"), to deal
 *     in the Software without restriction, including without limitation the rights
 *     to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the Software is
 *     furnished to do so, subject to the following conditions:
 *
 *     The above copyright notice and this permission notice shall be included in all
 *     copies or substantial portions of the Software.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *     IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *     FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *     AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *     LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *     OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *     SOFTWARE.
 */
package net.amigocraft.mpt.command;

import static net.amigocraft.mpt.util.Config.*;
import static net.amigocraft.mpt.util.MiscUtil.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.istack.internal.NotNull;
import net.amigocraft.mpt.Main;
import net.amigocraft.mpt.util.MPTException;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;

public class RemoveCommand extends SubcommandManager {

	public RemoveCommand(CommandSender sender, String[] args){
		super(sender, args);
	}

	@Override
	public void handle(){
		if (sender.hasPermission("mpt.install")){
			if (args.length > 1){
				Bukkit.getScheduler().runTaskAsynchronously(Main.plugin, new Runnable() {
					public void run(){
						try {
							for (int i = 1; i < args.length; i++){
								String id = args[i];
								threadSafeSendMessage(sender, INFO_COLOR + "Removing " + ID_COLOR + id +
										INFO_COLOR + "...");
								removePackage(id);
								threadSafeSendMessage(sender, "Successfully removed " + ID_COLOR + id);
							}
						}
						catch (MPTException ex){
							sender.sendMessage(ERROR_COLOR + ex.getMessage());
						}
					}
				});
			}
			else
				sender.sendMessage(ERROR_COLOR + "[MPT] Too few arguments! Type " + COMMAND_COLOR + "/mpt help" +
						ERROR_COLOR + " for help.");
		}
		else
			sender.sendMessage(ERROR_COLOR + "[MPT] You do not have permission to use this command!");
	}

	public static void removePackage(String id) throws MPTException {
		lockStores();
		if (Main.packageStore.getAsJsonObject("packages").has(id)){
			JsonObject pack = Main.packageStore.getAsJsonObject("packages").getAsJsonObject(id);
			String name = pack.get("name").getAsString() + " v" +
					pack.get("version").getAsString();
			if (pack.has("installed")){
				if (pack.has("files")){
					for (JsonElement e : pack.getAsJsonArray("files")){
						File f = new File(Bukkit.getWorldContainer(), e.getAsString());
						if (f.exists()){
							if (!f.delete()){
								if (VERBOSE){
									Main.log.warning("Failed to delete file " + f.getName());
								}
							}
							else {
								checkParent(f);
							}
						}
					}
					pack.remove("files");
				}
				else
					Main.log.warning("No file listing for package " + id + "!");
				File archive = new File(Main.plugin.getDataFolder(), "cache" + File.separator +
						id + ".zip");
				if (archive.exists()){
					if (!archive.delete() && VERBOSE){
						Main.log.warning("Failed to delete archive from cache");
					}
				}
				pack.remove("installed");
				try {
					writePackageStore();
				}
				catch (IOException ex){
					throw new MPTException(ERROR_COLOR + "Failed to save changes to disk!");
				}
			}
			else
				throw new MPTException(ERROR_COLOR + "Package " + ID_COLOR + id + ERROR_COLOR + " is not installed!");
		}
		else
			throw new MPTException(ERROR_COLOR + "Cannot find package with id " + ID_COLOR + id);
		unlockStores();
	}

	public static void checkParent(@NotNull File file){
		if (file != null){
			if (file.getParentFile().listFiles().length == 0){
				file.getParentFile().delete();
				checkParent(file.getParentFile());
			}
		}
		else
			throw new IllegalArgumentException("File cannot be null!");
	}
}
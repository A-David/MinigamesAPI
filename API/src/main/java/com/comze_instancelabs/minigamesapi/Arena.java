package com.comze_instancelabs.minigamesapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.comze_instancelabs.minigamesapi.ArenaState;
import com.comze_instancelabs.minigamesapi.ArenaType;
import com.comze_instancelabs.minigamesapi.MinigamesAPI;
import com.comze_instancelabs.minigamesapi.PluginInstance;
import com.comze_instancelabs.minigamesapi.SmartReset;
import com.comze_instancelabs.minigamesapi.arcade.ArcadeInstance;
import com.comze_instancelabs.minigamesapi.util.BungeeUtil;
import com.comze_instancelabs.minigamesapi.util.Cuboid;
import com.comze_instancelabs.minigamesapi.util.IconMenu;
import com.comze_instancelabs.minigamesapi.util.Util;
import com.comze_instancelabs.minigamesapi.util.Validator;

public class Arena {

	// Plugin the arena belongs to
	JavaPlugin plugin;
	PluginInstance pli;
	private ArcadeInstance ai;

	private ArrayList<Location> spawns = new ArrayList<Location>();
	HashMap<String, ItemStack[]> pinv = new HashMap<String, ItemStack[]>();
	HashMap<String, ItemStack[]> pinv_armor = new HashMap<String, ItemStack[]>();
	private HashMap<String, GameMode> pgamemode = new HashMap<String, GameMode>();
	HashMap<String, Location> pspawnloc = new HashMap<String, Location>();

	/**
	 * Used when players leave with command, they shouldn't get rewards!
	 */
	private ArrayList<String> pnoreward = new ArrayList<String>();

	HashMap<String, String> lastdamager = new HashMap<String, String>();

	private Location mainlobby;
	private Location waitinglobby;
	private Location signloc;

	private int max_players;
	private int min_players;

	private boolean viparena;
	private String permission_node;

	private ArrayList<String> players = new ArrayList<String>();

	private ArenaType type = ArenaType.DEFAULT;
	private ArenaState currentstate = ArenaState.JOIN;
	String name = "mainarena";

	private boolean shouldClearInventoryOnJoin = true;
	private Arena currentarena;
	boolean started = false;
	boolean startedIngameCountdown = false;
	private boolean showArenascoreboard = true;
	private boolean alwaysPvP = false;

	SmartReset sr = null;

	Cuboid boundaries;

	boolean temp_countdown = true;
	boolean skip_join_lobby = false;

	int currentspawn = 0;

	/**
	 * Creates a normal singlespawn arena
	 * 
	 * @param plugin
	 *            JavaPlugin the arena belongs to
	 * @param name
	 *            name of the arena
	 */
	public Arena(JavaPlugin plugin, String name) {
		currentarena = this;
		this.plugin = plugin;
		this.name = name;
		sr = new SmartReset(this);
		this.pli = MinigamesAPI.getAPI().pinstances.get(plugin);
	}

	/**
	 * Creates an arena of given arenatype
	 * 
	 * @param name
	 *            name of the arena
	 * @param type
	 *            arena type
	 */
	public Arena(JavaPlugin plugin, String name, ArenaType type) {
		this(plugin, name);
		this.type = type;
	}

	// This is for loading existing arenas
	public void init(Location signloc, ArrayList<Location> spawns, Location mainlobby, Location waitinglobby, int max_players, int min_players, boolean viparena) {
		this.signloc = signloc;
		this.spawns = spawns;
		this.mainlobby = mainlobby;
		this.waitinglobby = waitinglobby;
		this.viparena = viparena;
		this.min_players = min_players;
		this.max_players = max_players;
		this.showArenascoreboard = pli.arenaSetup.getShowScoreboard(plugin, this.getName());
		// if (this.getArenaType() == ArenaType.REGENERATION) {
		if (Util.isComponentForArenaValid(plugin, this.getName(), "bounds.low") && Util.isComponentForArenaValid(plugin, this.getName(), "bounds.high")) {
			try {
				this.boundaries = new Cuboid(Util.getComponentForArena(plugin, this.getName(), "bounds.low"), Util.getComponentForArena(plugin, this.getName(), "bounds.high"));
			} catch (Exception e) {
				plugin.getServer().getConsoleSender().sendMessage(ChatColor.RED + "Failed to save arenas as you forgot to set boundaries or they could not be found. This will lead to major error flows later, please fix your setup.");
			}
		}
		// }
	}

	// This is for loading existing arenas
	public Arena initArena(Location signloc, ArrayList<Location> spawn, Location mainlobby, Location waitinglobby, int max_players, int min_players, boolean viparena) {
		this.init(signloc, spawn, mainlobby, waitinglobby, max_players, min_players, viparena);
		return this;
	}

	public Arena getArena() {
		return this;
	}

	public SmartReset getSmartReset() {
		return this.sr;
	}

	public boolean getShowScoreboard() {
		return this.showArenascoreboard;
	}

	public boolean getAlwaysPvP() {
		return this.alwaysPvP;
	}

	public void setAlwaysPvP(boolean t) {
		this.alwaysPvP = t;
	}

	public Location getSignLocation() {
		return this.signloc;
	}

	public void setSignLocation(Location l) {
		this.signloc = l;
	}

	public ArrayList<Location> getSpawns() {
		return this.spawns;
	}

	public Cuboid getBoundaries() {
		return this.boundaries;
	}

	public String getName() {
		return name;
	}

	public int getMaxPlayers() {
		return this.max_players;
	}

	public int getMinPlayers() {
		return this.min_players;
	}

	public void setMinPlayers(int i) {
		this.min_players = i;
	}

	public void setMaxPlayers(int i) {
		this.max_players = i;
	}

	public boolean isVIPArena() {
		return this.viparena;
	}

	public void setVIPArena(boolean t) {
		this.viparena = t;
	}

	public ArrayList<String> getAllPlayers() {
		return this.players;
	}

	public boolean containsPlayer(String playername) {
		return players.contains(playername);
	}

	/**
	 * Please do not use this function to add players
	 * 
	 * @param playername
	 * @return
	 */
	@Deprecated
	public boolean addPlayer(String playername) {
		return players.add(playername);
	}

	public ArenaState getArenaState() {
		return this.currentstate;
	}

	public void setArenaState(ArenaState s) {
		this.currentstate = s;
	}

	public ArenaType getArenaType() {
		return this.type;
	}

	/**
	 * Joins the waiting lobby of an arena
	 * 
	 * @param playername
	 *            the playername
	 */
	public void joinPlayerLobby(String playername) {
		if (this.getArenaState() != ArenaState.JOIN && this.getArenaState() != ArenaState.STARTING) {
			// arena ingame or restarting
			return;
		}
		if (!pli.arenaSetup.getArenaEnabled(plugin, this.getName())) {
			Util.sendMessage(plugin, Bukkit.getPlayer(playername), pli.getMessagesConfig().arena_disabled);
			return;
		}
		if (ai == null && this.isVIPArena()) {
			if (Validator.isPlayerOnline(playername)) {
				if (!Bukkit.getPlayer(playername).hasPermission("arenas." + this.getName()) && !Bukkit.getPlayer(playername).hasPermission("arenas.*")) {
					Util.sendMessage(plugin, Bukkit.getPlayer(playername), pli.getMessagesConfig().no_perm_to_join_arena.replaceAll("<arena>", this.getName()));
					return;
				}
			}
		}
		if (ai == null && this.getAllPlayers().size() > this.max_players - 1) {
			// arena full

			// if player vip -> kick someone and continue
			System.out.println(playername + " is vip: " + Bukkit.getPlayer(playername).hasPermission("arenas.*"));
			if (!Bukkit.getPlayer(playername).hasPermission("arenas." + this.getName()) && !Bukkit.getPlayer(playername).hasPermission("arenas.*")) {
				return;
			} else {
				// player has vip
				boolean noone_found = true;
				ArrayList<String> temp = new ArrayList<String>(this.getAllPlayers());
				for (String p_ : temp) {
					if (Validator.isPlayerOnline(p_)) {
						if (!Bukkit.getPlayer(p_).hasPermission("arenas." + this.getName()) && !Bukkit.getPlayer(p_).hasPermission("arenas.*")) {
							this.leavePlayer(p_, false, true);
							Bukkit.getPlayer(p_).sendMessage(pli.getMessagesConfig().you_got_kicked_because_vip_joined);
							noone_found = false;
							break;
						}
					}
				}
				if (noone_found) {
					// apparently everyone is vip, can't join
					return;
				}
			}
		}

		if (MinigamesAPI.getAPI().global_party.containsKey(playername)) {
			Party party = MinigamesAPI.getAPI().global_party.get(playername);
			int playersize = party.getPlayers().size() + 1;
			if (this.getAllPlayers().size() + playersize > this.max_players) {
				Bukkit.getPlayer(playername).sendMessage(MinigamesAPI.getAPI().partymessages.party_too_big_to_join);
				return;
			} else {
				for (String p_ : party.getPlayers()) {
					if (Validator.isPlayerOnline(p_)) {
						boolean cont = true;
						for (PluginInstance pli_ : MinigamesAPI.getAPI().pinstances.values()) {
							// if (!pli_.getPlugin().getName().equalsIgnoreCase("MGArcade") && pli_.global_players.containsKey(p_)) {
							if (pli_.global_players.containsKey(p_)) {
								cont = false;
							}
						}
						if (cont) {
							this.joinPlayerLobby(p_);
						}
					}
				}
			}
		}

		if (this.getAllPlayers().size() == this.max_players - 1) {
			if (currentlobbycount > 16 && this.getArenaState() == ArenaState.STARTING) {
				currentlobbycount = 16;
			}
		}
		pli.global_players.put(playername, this);
		this.players.add(playername);

		if (Validator.isPlayerValid(plugin, playername, this)) {
			final Player p = Bukkit.getPlayer(playername);
			Util.sendMessage(plugin, p, pli.getMessagesConfig().you_joined_arena.replaceAll("<arena>", this.getName()));
			if (pli.getArenasConfig().getConfig().isSet("arenas." + this.getName() + ".author")) {
				Util.sendMessage(plugin, p, pli.getMessagesConfig().author_of_the_map.replaceAll("<arena>", this.getName()).replaceAll("<author>", pli.getArenasConfig().getConfig().getString("arenas." + this.getName() + ".author")));
			}
			for (String p_ : this.getAllPlayers()) {
				if (Validator.isPlayerOnline(p_) && !p_.equalsIgnoreCase(p.getName())) {
					Player p__ = Bukkit.getPlayer(p_);
					int count = this.getAllPlayers().size();
					int maxcount = this.getMaxPlayers();
					Util.sendMessage(plugin, p__, pli.getMessagesConfig().broadcast_player_joined.replaceAll("<player>", p.getName()).replace("<count>", Integer.toString(count)).replace("<maxcount>", Integer.toString(maxcount)));
				}
			}
			Util.updateSign(plugin, this);
			if (shouldClearInventoryOnJoin) {
				pinv.put(playername, p.getInventory().getContents());
				pinv_armor.put(playername, p.getInventory().getArmorContents());
				if (this.getArenaType() == ArenaType.JUMPNRUN) {
					Util.teleportPlayerFixed(p, this.spawns.get(currentspawn));
					if (currentspawn < this.spawns.size() - 1) {
						currentspawn++;
					}
					Util.clearInv(p);
					pgamemode.put(p.getName(), p.getGameMode());
					p.setGameMode(GameMode.SURVIVAL);
					p.setHealth(20D);
					return;
				} else {
					if (startedIngameCountdown) {
						p.setWalkSpeed(0.0F);
						p.setFoodLevel(5);
						p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 9999999, -7)); // -5
						Util.teleportAllPlayers(currentarena.getArena().getAllPlayers(), currentarena.getArena().spawns);
						Bukkit.getScheduler().runTaskLater(MinigamesAPI.getAPI(), new Runnable() {
							public void run() {
								Util.clearInv(p);
								Util.giveLobbyItems(plugin, p);
								pgamemode.put(p.getName(), p.getGameMode());
								p.setGameMode(GameMode.SURVIVAL);
							}
						}, 10L);
						pli.scoreboardManager.updateScoreboard(plugin, this);
						return;
					} else {
						Util.teleportPlayerFixed(p, this.waitinglobby);
					}
				}
				Bukkit.getScheduler().runTaskLater(MinigamesAPI.getAPI(), new Runnable() {
					public void run() {
						Util.clearInv(p);
						Util.giveLobbyItems(plugin, p);
						pgamemode.put(p.getName(), p.getGameMode());
						p.setGameMode(GameMode.SURVIVAL);
						p.setHealth(20D);
					}
				}, 10L);
				if (!skip_join_lobby) {
					if (ai == null && this.getAllPlayers().size() > this.min_players - 1) {
						this.startLobby(temp_countdown);
					} else if (ai != null) {
						this.startLobby(temp_countdown);
					}
				}
			}
		}
	}

	/**
	 * Primarily used for ArcadeInstance to join a waiting lobby without countdown
	 * 
	 * @param playername
	 * @param countdown
	 */
	public void joinPlayerLobby(String playername, boolean countdown) {
		temp_countdown = countdown;
		joinPlayerLobby(playername);
	}

	/**
	 * Joins the waiting lobby of an arena
	 * 
	 * @param playername
	 *            the playername
	 * @param ai
	 *            the ArcadeInstance
	 */
	public void joinPlayerLobby(String playername, ArcadeInstance ai, boolean countdown, boolean skip_lobby) {
		this.skip_join_lobby = skip_lobby;
		this.ai = ai;
		joinPlayerLobby(playername, countdown); // join playerlobby without lobby countdown
	}

	/**
	 * Leaves the current arena, won't do anything if not present in any arena
	 * 
	 * @param playername
	 * @param fullLeave
	 *            Determines if player left only minigame or the server
	 */
	@Deprecated
	public void leavePlayer(final String playername, boolean fullLeave) {
		this.leavePlayerRaw(playername, fullLeave);
	}

	public void leavePlayer(final String playername, boolean fullLeave, boolean endofGame) {
		if (!endofGame) {
			pnoreward.add(playername);
		}

		this.leavePlayer(playername, fullLeave);

		if (!endofGame) {
			if (this.getAllPlayers().size() < 2) {
				this.stop();
			}
		}
	}

	public void leavePlayerRaw(final String playername, boolean fullLeave) {
		if (!this.containsPlayer(playername)) {
			return;
		}
		this.players.remove(playername);
		pli.global_players.remove(playername);
		if (fullLeave) {
			plugin.getConfig().set("temp.left_players." + playername + ".name", playername);
			plugin.getConfig().set("temp.left_players." + playername + ".plugin", plugin.getName());
			for (ItemStack i : pinv.get(playername)) {
				if (i != null) {
					plugin.getConfig().set("temp.left_players." + playername + ".items." + Integer.toString((int) Math.round(Math.random() * 10000)) + i.getType().toString(), i);
				}
			}
			plugin.saveConfig();

			try {
				Player p = Bukkit.getPlayer(playername);
				if (p != null) {
					p.removePotionEffect(PotionEffectType.JUMP);
					Util.teleportPlayerFixed(p, this.mainlobby);
					p.setFireTicks(0);
					p.setFlying(false);
					if (!p.isOp()) {
						p.setAllowFlight(false);
					}
					if (pgamemode.containsKey(p.getName())) {
						p.setGameMode(pgamemode.get(p.getName()));
					}
					p.getInventory().setContents(pinv.get(playername));
					p.getInventory().setArmorContents(pinv_armor.get(playername));
					p.updateInventory();

					p.setWalkSpeed(0.2F);
					p.setFoodLevel(20);
					p.setHealth(20D);
					p.removePotionEffect(PotionEffectType.JUMP);
					pli.getSpectatorManager().setSpectate(p, false);

				}
			} catch (Exception e) {
				System.out.println("Failed to log player out of arena. " + e.getMessage());
			}

			return;
		}
		final Player p = Bukkit.getPlayer(playername);
		Util.clearInv(p);
		p.setWalkSpeed(0.2F);
		p.setFoodLevel(20);
		p.setHealth(20D);
		p.setFireTicks(0);
		p.removePotionEffect(PotionEffectType.JUMP);
		pli.getSpectatorManager().setSpectate(p, false);

		for (PotionEffect effect : p.getActivePotionEffects()) {
			if (effect != null) {
				p.removePotionEffect(effect.getType());
			}
		}

		for (Entity e : p.getNearbyEntities(50D, 50D, 50D)) {
			if (e.getType() == EntityType.DROPPED_ITEM || e.getType() == EntityType.SLIME || e.getType() == EntityType.ZOMBIE || e.getType() == EntityType.SKELETON || e.getType() == EntityType.SPIDER || e.getType() == EntityType.CREEPER) {
				e.remove();
			}
		}

		if (started) {
			if (!pnoreward.contains(playername)) {
				pli.getRewardsInstance().giveWinReward(playername, this);
			} else {
				pnoreward.remove(playername);
			}
		}

		pli.global_players.remove(playername);
		if (pli.global_lost.containsKey(playername)) {
			pli.global_lost.remove(playername);
		}
		if (pli.global_arcade_spectator.containsKey(playername)) {
			pli.global_arcade_spectator.remove(playername);
		}

		Util.updateSign(plugin, this);

		final String arenaname = this.getName();
		final Arena a = this;
		Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			public void run() {
				if (p != null) {
					Util.teleportPlayerFixed(p, a.mainlobby);
					p.setFireTicks(0);
					p.setFlying(false);
					if (!p.isOp()) {
						p.setAllowFlight(false);
					}
					if (pgamemode.containsKey(p.getName())) {
						p.setGameMode(pgamemode.get(p.getName()));
					}
					p.getInventory().setContents(pinv.get(playername));
					p.getInventory().setArmorContents(pinv_armor.get(playername));
					p.updateInventory();
					try {
						pli.scoreboardManager.removeScoreboard(arenaname, p);
					} catch (Exception e) {
						//
					}
				}
			}
		}, 5L);
	}

	/**
	 * Spectates the game
	 * 
	 * @param playername
	 *            the playername
	 */
	public void spectate(String playername) {
		if (Validator.isPlayerValid(plugin, playername, this)) {
			this.onEliminated(playername);
			Player p = Bukkit.getPlayer(playername);
			pli.getSpectatorManager().setSpectate(p, true);
			if (!plugin.getConfig().getBoolean("config.spectator_after_fall_or_death")) {
				pli.global_lost.put(playername, this);
				this.leavePlayer(playername, false, false);
				return;
			}
			Util.clearInv(p);
			Util.giveSpectatorItems(plugin, p);
			pli.global_lost.put(playername, this);
			p.setAllowFlight(true);
			p.setFlying(true);
			pli.scoreboardManager.updateScoreboard(plugin, this);
			if (!plugin.getConfig().getBoolean("config.last_man_standing_wins")) {
				if (this.getPlayerAlive() < 1) {
					final Arena a = this;
					Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
						public void run() {
							a.stop();
						}
					}, 20L);
				} else {
					Location temp = this.spawns.get(0);
					Util.teleportPlayerFixed(p, temp.clone().add(0D, 30D, 0D));
				}
			} else {
				if (this.getPlayerAlive() < 2) {
					final Arena a = this;
					Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
						public void run() {
							a.stop();
						}
					}, 20L);
				} else {
					Location temp = this.spawns.get(0);
					Util.teleportPlayerFixed(p, temp.clone().add(0D, 30D, 0D));
				}
			}
		}
	}

	public void spectateArcade(String playername) {
		Player p = Bukkit.getPlayer(playername);
		pli.global_players.put(playername, currentarena);
		pli.global_arcade_spectator.put(playername, currentarena);
		Util.teleportPlayerFixed(p, currentarena.getSpawns().get(0).clone().add(0D, 30D, 0D));
		p.setAllowFlight(true);
		p.setFlying(true);
		pli.getSpectatorManager().setSpectate(p, true);
	}

	int currentlobbycount = 10;
	int currentingamecount = 10;
	int currenttaskid = 0;

	public void setTaskId(int id) {
		this.currenttaskid = id;
	}

	public int getTaskId() {
		return this.currenttaskid;
	}

	/**
	 * Starts the lobby countdown and the arena afterwards
	 * 
	 * You can insta-start an arena by using Arena.start();
	 */
	public void startLobby() {
		startLobby(true);
	}

	public void startLobby(final boolean countdown) {
		if (currentstate != ArenaState.JOIN) {
			return;
		}
		this.setArenaState(ArenaState.STARTING);
		Util.updateSign(plugin, this);
		currentlobbycount = pli.lobby_countdown;
		final Arena a = this;

		// skip countdown
		if (!countdown) {
			Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
				public void run() {
					currentarena.getArena().start(true);
				}
			}, 10L);
		}

		currenttaskid = Bukkit.getScheduler().runTaskTimer(MinigamesAPI.getAPI(), new Runnable() {
			public void run() {
				currentlobbycount--;
				if (currentlobbycount == 60 || currentlobbycount == 30 || currentlobbycount == 15 || currentlobbycount == 10 || currentlobbycount < 6) {
					for (String p_ : a.getAllPlayers()) {
						if (Validator.isPlayerOnline(p_)) {
							Player p = Bukkit.getPlayer(p_);
							if (countdown) {
								Util.sendMessage(plugin, p, pli.getMessagesConfig().teleporting_to_arena_in.replaceAll("<count>", Integer.toString(currentlobbycount)));
							}
						}
					}
				}
				for (String p_ : a.getAllPlayers()) {
					if (Validator.isPlayerOnline(p_)) {
						Player p = Bukkit.getPlayer(p_);
						p.setExp(1F * ((1F * currentlobbycount) / (1F * pli.lobby_countdown)));
					}
				}
				if (currentlobbycount < 1) {
					Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
						public void run() {
							currentarena.getArena().start(true);
						}
					}, 10L);
					try {
						Bukkit.getScheduler().cancelTask(currenttaskid);
					} catch (Exception e) {
					}
				}
			}
		}, 5L, 20).getTaskId();
	}

	/**
	 * Instantly starts the arena, teleports players and udpates the arena
	 */
	public void start(boolean tp) {
		try {
			Bukkit.getScheduler().cancelTask(currenttaskid);
		} catch (Exception e) {
		}
		currentingamecount = pli.ingame_countdown;
		if (tp) {
			pspawnloc = Util.teleportAllPlayers(currentarena.getArena().getAllPlayers(), currentarena.getArena().spawns);
		}
		for (String p_ : currentarena.getArena().getAllPlayers()) {
			Player p = Bukkit.getPlayer(p_);
			p.setWalkSpeed(0.0F);
			p.setFoodLevel(5);
			p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 9999999, -7)); // -5
		}
		final Arena a = this;
		pli.scoreboardManager.updateScoreboard(plugin, a);
		currenttaskid = Bukkit.getScheduler().runTaskTimer(MinigamesAPI.getAPI(), new Runnable() {
			public void run() {
				startedIngameCountdown = true;
				currentingamecount--;
				if (currentingamecount == 60 || currentingamecount == 30 || currentingamecount == 15 || currentingamecount == 10 || currentingamecount < 6) {
					for (String p_ : a.getAllPlayers()) {
						if (Validator.isPlayerOnline(p_)) {
							Player p = Bukkit.getPlayer(p_);
							Util.sendMessage(plugin, p, pli.getMessagesConfig().starting_in.replaceAll("<count>", Integer.toString(currentingamecount)));
						}
					}
				}
				for (String p_ : a.getAllPlayers()) {
					if (Validator.isPlayerOnline(p_)) {
						Player p = Bukkit.getPlayer(p_);
						p.setExp(1F * ((1F * currentingamecount) / (1F * pli.ingame_countdown)));
					}
				}
				if (currentingamecount < 1) {
					currentarena.getArena().setArenaState(ArenaState.INGAME);
					startedIngameCountdown = false;
					Util.updateSign(plugin, a);
					boolean send_game_started_msg = plugin.getConfig().getBoolean("config.send_game_started_msg");
					for (String p_ : a.getAllPlayers()) {
						try {
							if (!pli.global_lost.containsKey(p_)) {
								if (plugin.getConfig().getBoolean("config.auto_add_default_kit")) {
									if (!pli.getClassesHandler().hasClass(p_)) {
										pli.getClassesHandler().setClass("default", p_);
									}
									pli.getClassesHandler().getClass(p_);
								} else {
									Util.clearInv(Bukkit.getPlayer(p_));
								}
								Bukkit.getPlayer(p_).setFlying(false);
								Bukkit.getPlayer(p_).setAllowFlight(false);
							}
						} catch (Exception e) {
							System.out.println("Failed to set class: " + e.getMessage());
						}
						Player p = Bukkit.getPlayer(p_);
						p.setWalkSpeed(0.2F);
						p.setFoodLevel(20);
						p.removePotionEffect(PotionEffectType.JUMP);
						if (send_game_started_msg) {
							p.sendMessage(pli.getMessagesConfig().game_started);
						}
					}
					if (plugin.getConfig().getBoolean("config.bungee.whitelist_while_game_running")) {
						Bukkit.setWhitelist(true);
					}
					started = true;
					started();
					try {
						Bukkit.getScheduler().cancelTask(currenttaskid);
					} catch (Exception e) {
					}
				}
			}
		}, 5L, 20).getTaskId();
	}

	/**
	 * Gets executed after an arena started (after lobby countdown)
	 */
	public void started() {
		System.out.println(this.getName() + " started.");
	}

	boolean temp_delay_stopped = false;

	/**
	 * Stops the arena and teleports all players to the mainlobby
	 */
	public void stop() {
		final Arena a = this;
		if (!temp_delay_stopped) {
			if (plugin.getConfig().getBoolean("config.delay.enabled")) {
				Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
					public void run() {
						temp_delay_stopped = true;
						a.stop();
					}
				}, plugin.getConfig().getInt("config.delay.amount_seconds") * 20L);
				this.setArenaState(ArenaState.RESTARTING);
				Util.updateSign(plugin, this);
				if (plugin.getConfig().getBoolean("config.spawn_fireworks_for_winners")) {
					if (this.getAllPlayers().size() > 0) {
						Util.spawnFirework(Bukkit.getPlayer(this.getAllPlayers().get(0)));
					}
				}
				return;
			}
		}
		temp_delay_stopped = false;

		try {
			Bukkit.getScheduler().cancelTask(currenttaskid);
		} catch (Exception e) {

		}

		this.setArenaState(ArenaState.RESTARTING);

		final ArrayList<String> temp = new ArrayList<String>(this.getAllPlayers());
		for (final String p_ : temp) {
			try {
				Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
					public void run() {
						if (Validator.isPlayerOnline(p_)) {
							for (Entity e : Bukkit.getPlayer(p_).getNearbyEntities(50, 50, 50)) {
								if (e.getType() == EntityType.DROPPED_ITEM || e.getType() == EntityType.SLIME || e.getType() == EntityType.ZOMBIE || e.getType() == EntityType.SKELETON || e.getType() == EntityType.SPIDER || e.getType() == EntityType.CREEPER) {
									e.remove();
								}
							}
						}
					}
				}, 10L);
			} catch (Exception e) {
				System.out.println("Failed clearing entities.");
			}
			leavePlayer(p_, false, true);
		}

		if (a.getArenaType() == ArenaType.REGENERATION) {
			reset();
		} else {
			a.setArenaState(ArenaState.JOIN);
			Util.updateSign(plugin, a);
		}

		Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			public void run() {
				players.clear();
				pinv.clear();
				pinv_armor.clear();
				pnoreward.clear();
				for (IconMenu im : pli.getClassesHandler().lasticonm.values()) {
					im.destroy();
				}
				pli.getClassesHandler().lasticonm.clear();
			}
		}, 10L);

		started = false;
		startedIngameCountdown = false;

		temp_countdown = true;
		skip_join_lobby = false;
		currentspawn = 0;

		pli.scoreboardManager.clearScoreboard(this.getName());

		/*
		 * try { pli.getStatsInstance().updateSkulls(); } catch (Exception e) {
		 * 
		 * }
		 */

		if (plugin.getConfig().getBoolean("config.execute_cmds_on_stop")) {
			String[] cmds = plugin.getConfig().getString("config.cmds").split(";");
			for (String cmd : cmds) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
			}
		}

		if (plugin.getConfig().getBoolean("config.bungee.teleport_all_to_server_on_stop.tp")) {
			final String server = plugin.getConfig().getString("config.bungee.teleport_all_to_server_on_stop.server");
			Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
				public void run() {
					for (Player p : Bukkit.getOnlinePlayers()) {
						BungeeUtil.connectToServer(MinigamesAPI.getAPI(), p.getName(), server);
					}
				}
			}, 30L);
			return;
		}
		if (plugin.getConfig().getBoolean("config.bungee.whitelist_while_game_running")) {
			Bukkit.setWhitelist(false);
		}

		if (ai != null) {
			Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
				public void run() {
					if (ai != null) {
						ai.nextMinigame();
						ai = null;
					}
				}
			}, 10L);
		} else {
			// Map rotation only works without Arcade
			// check if there is only one player or none left
			if (temp.size() < 2) {
				return;
			}
			if (plugin.getConfig().getBoolean("config.map_rotation")) {
				Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
					public void run() {
						a.nextArenaOnMapRotation(temp);
					}
				}, 35L);
			}
		}

	}

	/**
	 * Rebuilds an arena from file (only for arenas of REGENERATION type)
	 */
	public void reset() {
		/*
		 * Runnable r = new Runnable() { public void run() { Util.loadArenaFromFileSYNC(plugin, currentarena); } }; new Thread(r).start();
		 */
		sr.reset();
		/*
		 * Bukkit.getScheduler().runTask(plugin, new Runnable() { public void run() { // Util.loadArenaFromFileSYNC(plugin, currentarena); sr.reset();
		 * } });
		 */
	}

	/***
	 * Use this when someone got killed/pushed down/eliminated in some way by a player
	 * 
	 * @param playername
	 *            The player that got eliminated
	 */
	public void onEliminated(String playername) {
		if (lastdamager.containsKey(playername)) {
			Player killer = Bukkit.getPlayer(lastdamager.get(playername));
			if (killer != null) {
				pli.getRewardsInstance().giveKillReward(killer.getName(), 2);
				Util.sendMessage(plugin, killer, MinigamesAPI.getAPI().pinstances.get(plugin).getMessagesConfig().you_got_a_kill.replaceAll("<player>", playername));
			}
			lastdamager.remove(playername);
		}
	}

	/**
	 * Will shuffle all arenas and join the next available arena
	 * 
	 * @param players
	 */
	public void nextArenaOnMapRotation(ArrayList<String> players) {
		ArrayList<Arena> arenas = pli.getArenas();
		Collections.shuffle(arenas);
		for (Arena a : arenas) {
			if (a.getArenaState() == ArenaState.JOIN && a != this) {
				System.out.println(plugin.getName() + ": Next arena on map rotation: " + a.getName());
				for (String p_ : players) {
					if (!a.containsPlayer(p_)) {
						a.joinPlayerLobby(p_, false);
					}
				}
			}
		}
	}

	public String getPlayerCount() {
		int alive = 0;
		for (String p_ : getAllPlayers()) {
			if (pli.global_lost.containsKey(p_)) {
				continue;
			} else {
				alive++;
			}
		}
		return Integer.toString(alive) + "/" + Integer.toString(getAllPlayers().size());
	}

	public int getPlayerAlive() {
		int alive = 0;
		for (String p_ : getAllPlayers()) {
			if (pli.global_lost.containsKey(p_)) {
				continue;
			} else {
				alive++;
			}
		}
		return alive;
	}

	public Location getWaitingLobbyTemp() {
		return this.waitinglobby;
	}

	public Location getMainLobbyTemp() {
		return this.mainlobby;
	}

	public ArcadeInstance getArcadeInstance() {
		return ai;
	}

	public HashMap<String, Location> getPSpawnLocs() {
		return pspawnloc;
	}

}

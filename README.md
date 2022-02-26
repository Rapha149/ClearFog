# ClearFog

Are you also unnerved by the new 1.18 fog system? And you tried to increase the server view distance?  
**You don't have to!** This Spigot plugin "tells" the player that the view distance is higher than it actually is.
The server view distance won't change.  
You can even change the default view distance or set individual view distances for different players!

## URLs

- [Spigot](https://www.spigotmc.org/resources/clearfog.98448)
- [bStats](https://bstats.org/plugin/bukkit/ClearFog/13628)

## Why?

As of 1.18 the fog in Minecraft is not done by the client anymore but by the server.
This raises the problem that the server view distance may not be that high and the player will have very unnerving fog.  
If you can't increase it for some reason or you don't want to in order to save performance this was getting even more annoying.

## How this plugin works

The plugin modifies the login packet that is sent to the client when the client joins and changes the view distance.  
How that looks:  
#### Before
![2021-12-17_17 54 32](https://user-images.githubusercontent.com/49787110/146580689-1eab2fab-446b-4d83-a49d-2d79984fd01f.png)
#### After
![2021-12-17_17 54 14](https://user-images.githubusercontent.com/49787110/146580691-a13337b8-a76a-4f0c-916d-bcd688c57a6a.png)

## Config

The default `config.yml` looks like this:
```yml
check-for-updates: true
direct-view-distance-updates: false
default:
  enabled: true
  view-distance: 32
individual:
  enabled: false
  players: {}
world:
  enabled: false
  worlds: {}
```
You can change all values using commands, but you can edit the config, too.  
Here's what the values do:

- `check-for-updates` - Whether to check for plugin updates on startup.
- `direct-view-distance-updates` - Whether view distances should be updated directly when they are being changed by commands or by reloading the config. Please note that this is not recommended because it does not look good and the method for doing so is messy.
- `default`
  - `enabled` - Changes whether the default view distance is applied.  
  - `view-distance` - The default view distance.  
- `distances`  
  - `enabled` - Changes whether player specific view distances are applied.
  - `players` - Here are player specific view distances saved.
- `world`
  - `enabled` - Changes whether world specific view distances are applied.
  - `worlds` - Here are world specific view distances saved.

### Messages

You can also change all messages in the `messages.yml` file!

## Commands

The plugin contains one basic command: `/fog`  
Sub commands are:
- `/fog reload` - Reloads the config.
- `/fog directupdates status` - Checks whether direct updates are enabled.
- `/fog directupdates <enable|disable>` - Enable or disable direct updates.
- `/fog default status` - Checks whether the default view distance is enabled.
- `/fog default <enable/disable>` - Enables or disables the default view distance.
- `/fog default get` - Returns the default view distance.
- `/fog default set <View Distance>` - Sets the default view distance.
- `/fog world status` - Checks whether world specific view distances are enabled.
- `/fog world <enable|disable>` - Enables or disables world specific view distances.
- `/fog world list` - Lists world specific view distances.
- `/fog world get [World]` - Returns the world specific view distance for a specific world or the world where you are currently in.
- `/fog world set <View Distance> [World]` - Sets the world specific view distance for a specific world or the world where you are currently in.
- `/fog world unset [World]` - Removes the world specific view distance for a specific world or the world where you are currently in.
- `/fog individual status` - Checks whether player specific view distances are enabled.
- `/fog individual <enable|disable>` - Enables or disables player specific view distances.
- `/fog individual list` - Lists player specific view distances.
- `/fog individual get [Player]` - Returns the player specific view distance for yourself or another player.
- `/fog individual set <View Distance> [Player]` - Sets the player specific view distance for yourself or another player.
- `/fog individual unset [Player]` - Removes the player specific view distance for yourself or another player.

You can also use the alias `/worldfog` for all `/fog world` commands and the alias `/myfog` for all `/fog individual` commmands.  
For example: You can use `/myfog set 16` instead of `/fog individual set 16`.

Please also note that if direct updates are distabled, fog is only applied on join, so you have to rejoin for the changes to take effect.

## Permissions

- `clearfog.reload` - Permission for `/fog reload`
- `clearfog.directupdates` - Permission for `/fog directupdates`
- `clearfog.default.status` - Permission for `/fog default status` and `/fog default <enable|disable>`
- `clearfog.default.values` - Permission for `/fog default get` and `/fog default set`
- `clearfog.world.status` - Permission for `/fog world status` and `/fog world <enable|disable>`
- `clearfog.world.list` - Permission for `/fog world list`
- `clearfog.world.values` - Permission for `/fog world get`, `/fog world set` and `/fog world unset`
- `clearfog.individual.status` - Permission for `/fog individual status` and `/fog individual <enable|disable>`
- `clearfog.individual.list` - Permission for `/fog individual list`
- `clearfog.individual.values` - Permission for `/fog individual get`, `/fog individual set` and `/fog individual unset`
- `clearfog.individual.values.others` - Allows players to change the player specific view distances for other players.

## Additional information

This plugin collects anonymous server stats with [bStats](https://bstats.org), an open-source statistics service for Minecraft software. If you don't want this, you can deactivate it in `plugins/bStats/config.yml`.

## Credits

Credits go to [dmulloy2](https://github.com/dmulloy2) and aadnk for [TinyProtocol](https://github.com/dmulloy2/ProtocolLib/blob/master/TinyProtocol/src/main/java/com/comphenix/tinyprotocol/TinyProtocol.java) (a part of [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997)) from which I used some code. 

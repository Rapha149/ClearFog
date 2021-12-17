# ClearFog

Are you also unnerved by the new 1.18 fog system? Did you try to increase the server view distance?  
**You don't have to!** This Spigot plugin "tells" the player that the view distance is higher than it actually is.
The server view distance won't change.  
You can even change the default view distance or set individual view distances for different players!

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
default:
  enabled: true
  view-distance: 32
distances:
  enabled: false
  players: {}
```
You can change all values using commands, but you can edit the config, too.  
Here's what the values do:

- `default`
  - `enabled` - Changes whether the default view distance is applied.  
  - `view-distance` - The default view distance.  
- `distances`  
  - `enabled` - Changes whether player specific view distances are applied.
  - `players` - Here are player specific view distances saved.

### Messages

You can also change all messages in the `messages.yml` file!

## Commands

The plugin contains one basic command: `/fog`  
Sub commands are:
- `/fog reload` - Reloads the config.
- `/fog default <enable/disable>` - Enables or disables the default view distance.
- `/fog default get` - Returns the default view distance.
- `/fog default set <View Distance>` - Sets the default view distance.
- `/fog individual <enable|disable>` - Enables or disables player specific view distances.
- `/fog individual get [Player]` - Returns the player specific view distance for yourself or another player.
- `/fog individual set <View Distance> [Player]` - Sets the player specific view distance for yourself or another player.
- `/fog individual unset [Player]` - Removes the player specific view distance for yourself or another player.

You can also use the alias `/myfog` for all `/fog individual` commmands.  
For example: You can use `/myfog set 16` instead of `/fog individual set 16`.

Please also note that fog is only applied on join, so you have to rejoin for the changes to take effect.

## Permissions

- `clearfog.reload` - Permission for `/fog reload`
- `clearfog.default.toggle` - Permission for `/fog default <enable|disable>`
- `clearfog.default.values` - Permission for `/fog default get` and `/fog default set`
- `clearfog.individual.toggle` - Permission for `/fog individual <enable|disable`
- `clearfog.individual.values` - Permission for `/fog individual get`, `/fog individual set` and `/fog individual unset`
- `clearfog.individual.values.others` - Allows players to change the player specific view distances for other players.

## Credits

Credits go to `comphenix` / [dmulloy2](https://github.com/dmulloy2) for [TinyProtocol](https://github.com/dmulloy2/ProtocolLib/blob/master/TinyProtocol/src/main/java/com/comphenix/ticyprotocol/TinyProtocol.java) which I used some code from.

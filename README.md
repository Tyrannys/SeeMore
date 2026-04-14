# SeeMore
[Hangar](https://hangar.papermc.io/froobynooby/SeeMore) | [Modrinth](https://modrinth.com/plugin/seemore) | [Dev Builds](https://ci.froobworld.com/job/SeeMore/)

## About
SeeMore is a simple Paper plugin that sets a player's server-side view distance according to their client-side render distance.

## Configuration

```yaml
# Configuration for SeeMore.

# Please don't change this!
version: 3

# The delay (in ticks) before a player's view distance is lowered after their client settings change.
#  * This stops players overloading the server by constantly changing their view distance.
update-delay: 600

# Whether the plugin should log to the console when it changes a player's view distance.
log-changes: true

# Additional view distance restriction applied underground.
underground:
  # Whether underground view distance capping is enabled.
  enabled: true

  # The delay (in ticks) between checks for whether players are underground.
  # Defaults to the same value as update-delay.
  update-delay: 600

  # Apply the underground cap when a player goes below this Y level.
  underground-y: 0.0

  # Remove the underground cap when a player goes above this Y level.
  surface-y: 20.0

  # The maximum view distance allowed while underground.
  # Set to -1 to disable the cap without disabling underground checks.
  maximum-view-distance: 6

  # The simulation distance to apply while underground.
  # Set to -1 to use the world's configured simulation distance.
  simulation-distance: 4

# These settings can be specified per world.
#  * Note: If a world is not listed here or if a setting is missing, it will use the settings listed under the default
#    section.
world-settings:
  default:
    # The maximum view distance a player in this world can have.
    # Set to -1 to use the server's configured view distance for this world.
    maximum-view-distance: -1
```

## Commands

| Command            | Permission                | Description                                             |
|--------------------|---------------------------|---------------------------------------------------------|
| `/seemore reload`  | `seemore.command.reload`  | Reload the plugin's configuration.                      |
| `/seemore average` | `seemore.command.average` | Show the effective average view distance of the worlds. |
| `/seemore players` | `seemore.command.players` | Show the server-side view distance of all players.      |

## Building
If you would like to build the plugin yourself you can follow these steps.

1\. Install dependency NabConfiguration to maven local
```bash
git clone https://github.com/froobynooby/nab-configuration
cd nab-configuration
./gradlew clean install
```
2\. Clone SeeMore and build
```bash
git clone https://github.com/froobynooby/SeeMore
cd SeeMore
./gradlew clean build
```

3\. Find jar in `SeeMore/build/libs`

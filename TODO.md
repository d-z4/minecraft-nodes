# High level
-   The async tasks do not have proper synchronization. There isn't
    proper locking to make sure async stuff runs properly, TODO.
    Check filesystem saving sync as well.
-   No real API. Most functionality is attached to the Nodes object
    with intent that this basically acts as API. But not well defined
    and subject to change.
-   Nodes world state is global variable, so only supports single world.
    E.g. no separate nodes map for overworld + nether.
    This is likely not to change since goal is only single map/world.
-   No API for custom node/territory properties.
-   Cannot `/nodes reload` world. Inconvenient. TODO.
-   Get rid of java.io.File, swap to nio


# Nodes
-   Add the ability to add custom drops to ore and if they can only be dropped from a node with a resource eg coal (not adding to dynmap that would be canser for now)
    EG: 1 coal named "super coal coal" with lore "ik its a good joke super cool coal hehe"



# Minimap
-   Differnt port icon for ports under attack
-   Flag unicode for chunks under attack
-   change max size to 7 (add option in config.yml to set the max size [3-7])
-   Add a setting to change the max minimap size based on how many players are online | Lag protection
-   Chunks with war flags indicator, e.g. flashing or separate minimap symbol


# War
-   Claiming incontiguous territories short distance across seazone/wasteland
-   Seazone border claiming penalty only on edge chunks not bordering friendly land
-   Investigate MultiBlockChange packet to send flag sky beacon blocks?
    Need to do something to reduce fps drop when sky beacon blocks are created.
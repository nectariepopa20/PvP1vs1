# PvP1vs1 (decompiled & modifiable)

**Fork of the original [PvP 1vs1](https://dev.bukkit.org/projects/pvp-1vs1) plugin** (Bukkit/Spigot). The original source is not publicly available, so this repository contains source recovered from **PvP1vs1-1.7.1.jar** using [CFR](https://www.benf.org/other/cfr/) so the plugin can be modified and rebuilt.

- **Original project:** https://dev.bukkit.org/projects/pvp-1vs1  
- **Original author:** Orscrider  
- **This fork:** https://github.com/nectariepopa20/PvP1vs1

## What was done

- **Decompiled** the JAR with CFR 0.152 → Java source in `src/main/java`
- **Maven project** with Spigot API and Vault (provided scope)
- **Small fixes** for decompiler type inference (generics) so it compiles
- **Resources** (plugin.yml, config.yml, arena.yml, messages.yml) in `src/main/resources`

## Build

```bash
mvn clean package
```

Output JAR: `target/PvP1vs1-1.7.2-SNAPSHOT.jar`

## Run / test

Put the built JAR in your server’s `plugins/` folder (Spigot/Paper 1.20.x recommended). Vault is optional (soft depend).

## Project layout

| Path | Description |
|------|-------------|
| `src/main/java/com/gmail/Orscrider/PvP1vs1/` | Plugin source (edit here) |
| `src/main/resources/` | plugin.yml, config.yml, arena.yml, messages.yml |
| `PvP1vs1-1.7.1.jar` | Original JAR (kept for reference) |
| `target/` | Build output (JAR after `mvn package`) |

## Notes

- Decompiled code is not identical to the original (e.g. variable names, some generics). Logic is preserved.
- This is an unofficial fork; the original plugin is by [Orscrider](https://dev.bukkit.org/projects/pvp-1vs1).

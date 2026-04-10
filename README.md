# HC_Panel

Unified character panel UI that dynamically aggregates information from multiple HC plugins into a single interactive page with hierarchical navigation. Automatically detects which plugins are available at runtime and enables their corresponding modules. Opened via the `/menu` command.

## Features

- Modular architecture that auto-detects available HC plugins and enables corresponding panel sections
- Factions module showing faction membership and guild info (requires HC_Factions)
- Honor module displaying PvP rank and honor statistics (requires HC_Honor)
- Character module with attributes and class information (requires HC_Attributes or HC_Classes)
- Skills module showing profession progress (requires HC_Professions)
- Recruitment module for recruitment system data (requires HC_Recruitment)
- Characters module for multi-character management (requires HC_MultiChar)
- News/announcements section loaded from PostgreSQL database with JSON fallback
- Guide content with header images
- Settings section
- Player avatar rendering via HyUI dynamic image system
- Leveling integration for player level display
- Hot-reload aware -- re-detects available modules each time the panel is opened

## Dependencies

- **EntityModule** (required) -- Hytale entity system
- HC_Factions (optional) -- faction and guild data
- HC_Honor (optional) -- PvP honor data
- HC_Attributes (optional) -- player attribute data
- HC_Classes (optional) -- class system data
- HC_Professions (optional) -- profession/skill data
- HC_Recruitment (optional) -- recruitment data
- HC_MultiChar (optional) -- character switching
- HC_Leveling (optional) -- player level display

## Building

```bash
./gradlew build
```

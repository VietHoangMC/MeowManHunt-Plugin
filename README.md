# 🐱 MeowManhunt Plugin

**Author:** Việt Hoàng  
**Website:** https://viethoangmc.page.gd  
**Version:** 1.0.0  
**Minecraft:** 1.21+  
**API:** Paper / Spigot

---

## 📦 Building

### Requirements
- Java 21+
- Maven 3.8+
- Internet connection (to download Paper API)

### Build
```bash
cd MeowManhunt
mvn clean package
```

The compiled JAR will be in `target/MeowManhunt-1.0.0.jar`.  
Copy it to your server's `plugins/` folder and restart.

---

## 🎮 Features

### Core Manhunt Mechanics
- ✅ Speedrunner vs Hunter gameplay
- ✅ Compass auto-tracking (hunters track closest speedrunner)
- ✅ Hunter lives system (configurable 1-10)
- ✅ Win condition: Speedrunner kills Ender Dragon → Speedrunner wins
- ✅ Win condition: All speedrunners die → Hunters win
- ✅ Head start system (hunters are frozen for X seconds at start)
- ✅ Glow effect on speedrunner (hunters can see them through walls)
- ✅ Fireworks celebration on win
- ✅ Hunter respawn system

### GUI Menus
- 🖥️ **Main Menu** - Overview, start/stop, navigation
- 👥 **Team Menu** - Join as Runner, Hunter, or Spectator
- ⚙️ **Settings Menu** - Adjust lives, countdown, glow, head start
- 🌐 **Language Menu** - Switch between English and Vietnamese

### Commands (`/manhunt` or `/mh`)

| Command | Permission | Description |
|---------|------------|-------------|
| `/manhunt` | use | Open main menu |
| `/manhunt menu` | use | Open main menu |
| `/manhunt start` | admin | Start the game |
| `/manhunt stop` | admin | Stop the game |
| `/manhunt reset` | admin | Reset game data |
| `/manhunt join <runner\|hunter\|spectator>` | use | Join a team |
| `/manhunt leave` | use | Leave the game |
| `/manhunt setrunner <player>` | admin | Set player as speedrunner |
| `/manhunt sethunter <player>` | admin | Set player as hunter |
| `/manhunt removerunner <player>` | admin | Remove speedrunner |
| `/manhunt removehunter <player>` | admin | Remove hunter |
| `/manhunt lives <1-10>` | admin | Set hunter lives |
| `/manhunt countdown <seconds>` | admin | Set countdown time |
| `/manhunt glow <on\|off>` | admin | Toggle glow effect |
| `/manhunt give compass [player]` | admin | Give compass to hunters |
| `/manhunt status` | use | Show game status |
| `/manhunt lang <en\|vi>` | use | Change your language |
| `/manhunt reload` | admin | Reload config |
| `/manhunt help` | use | Show help |

### Permissions

| Node | Default | Description |
|------|---------|-------------|
| `meowmanhunt.use` | Everyone | Basic commands |
| `meowmanhunt.admin` | OP | Admin commands |
| `meowmanhunt.*` | OP | All permissions |

### Bilingual Support
- 🇬🇧 **English** - Full translation
- 🇻🇳 **Vietnamese** - Full translation
- Per-player language preference (survives session)
- All messages customizable in `messages_en.yml` / `messages_vi.yml`

### Scoreboard
- Shows game state, runner count, hunter count
- Personal lives display for hunters
- Live game timer
- Auto-updates every configurable interval

### BossBar
- Countdown phase: shows time remaining
- Running phase: shows game timer, runner count, hunter count
- Color customizable (RED, GREEN, YELLOW, BLUE, PURPLE, etc.)

---

## ⚙️ Configuration

Edit `plugins/MeowManhunt/config.yml`:

```yaml
language: en           # Default language: en / vi
game:
  hunter-lives: 3      # Lives per hunter (1-10)
  countdown-time: 10   # Countdown before start (seconds)
  compass-update-interval: 20  # Compass update (ticks)
  glow-effect: true    # Speedrunner glows for hunters
  head-start: 30       # Hunters frozen for X seconds at start
  auto-reset-delay: 15 # Auto-reset after game ends (seconds)
```

---

## 📁 File Structure

```
plugins/MeowManhunt/
├── config.yml          ← Main configuration
├── messages_en.yml     ← English messages
└── messages_vi.yml     ← Vietnamese messages
```

---

## 🐱 Credits

Made with ❤️ by **Việt Hoàng**  
Website: https://viethoangmc.page.gd

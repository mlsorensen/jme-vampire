# Elegy of Shadows -  Story Specification

This document describes the JSON formats currently consumed by the game along with optional metadata you can provide to an AI when authoring new content. The engine honours only the required fields described in §§1–3; any extra sections are ignored but can live alongside the data for documentation.

---

## 1. Storyline JSON

Storylines live under `resources/storylines/<storyId>/story.json` (one directory per story) and follow this schema:

```
{
  "id": "foo",
  "start": "nodeId",
  "nodes": {
    "someNode": { "type": "...", ... }
  }
}
```

- `id`: identifier for the storyline (used only for reference).
- `start`: the node id where the story begins.
- `nodes`: a map of node id → node definition.

Unknown top-level keys (for example `metadata`, `characters`) are ignored by the loader, so you can include them for descriptive purposes.

### 1.1 Node Types

| Type        | Required Fields                                                     | Description                                        |
|-------------|---------------------------------------------------------------------|----------------------------------------------------|
| `slideshow` | `slides` (array of slide objects), `next` (node id or `null`)        | Shows timed slides, then jumps to `next`.          |
| `level`     | `level` (path to level config), `onWin` (node id), `onLose` (node id) | Plays gameplay level, then branches on outcome.    |
| `ending`    | `endingType` (string)                                                | Final node, currently displays a “THE END” card.   |

### 1.2 Slide Objects (`SlideConfig`)

```
{
  "text": "Victor was hungry.",
  "duration": 3.0,
  "backgroundColor": "#000000",
  "backgroundImage": "Slides/foo/intro_slide1.png",
  "assetId": "intro_slide1",
  "characterFocus": ["victor"],
  "mood": "ominous"
}
```

- `text`, `duration`, `backgroundColor` are used directly by the engine.
- `backgroundImage` optionally points to a texture path.
- `assetId`, `characterFocus`, `mood` are optional metadata for tooling/AI; the runtime ignores them if present.

### 1.3 Example Storyline

```
{
  "id": "foo",
  "start": "slideshowA",
  "nodes": {
    "slideshowA": {
      "type": "slideshow",
      "slides": [
        { "text": "In the beginning, it was just Victor.", "duration": 3.0, "backgroundColor": "#000000" },
        { "text": "Victor was hungry.", "duration": 3.0, "backgroundColor": "#000000" }
      ],
      "next": "level1"
    },
    "level1": {
      "type": "level",
      "level": "levels/level1.json",
      "onWin": "slideshowB",
      "onLose": "level1"
    },
    "ending": {
      "type": "ending",
      "endingType": "gameOver"
    }
  }
}
```

---

## 2. Asset Prompts and Target Paths (Optional)

While the engine only requires the `slides` and `level` fields, you can generate supporting artwork by attaching prompt metadata. These prompts can be stored in a companion JSON or embedded under keys that the loader ignores (e.g., `"assets": { ... }`).

For each asset, include:

```
{
  "assetId": "intro_slide1",
  "targetPath": "Slides/foo/intro_slide1.png",
  "prompt": "Victorian alley at night, crimson moon, fog, silhouetted vampire.",
  "notes": "Keep Victor’s silhouette consistent."
}
```

### 2.1 Categories

| Category              | Purpose                                                   | Example `targetPath`                           |
|----------------------|-----------------------------------------------------------|-----------------------------------------------|
| `slides`              | Narrative backgrounds for slideshow nodes                 | `Slides/foo/intro_slide1.png`                 |
| `levelBackgrounds`    | Playfield textures (e.g., night version)                  | `Textures/foo/level2_background.png`          |
| `levelForegrounds`    | Alternate/warped foreground (e.g., day vs night)          | `Textures/foo/level2_foreground.png`          |
| `sprites`             | Optional new enemy or NPC sprites                         | `Textures/foo/enemies/hunter.png`             |

For level foreground/background pairs, ensure prompts mention the shared layout (rivers, trees, etc.) so the AI produces consistent variants.

---

## 3. Level Configuration (`levels/levelN.json`)

Each level file uses this schema:

```
{
  "backgroundImage": "Textures/foo/level1_background.png",
  "foregroundImage": "Textures/foo/level1_foreground.png",
  "music": "Audio/vampires.ogg",
  "perimeterVertices": [ { "x": 64.0, "y": 64.0 }, ... ],
  "enemies": [
    {
      "sprite": "human",
      "radius": 32.0,
      "spawnX": 400.0,
      "spawnY": 400.0,
      "movementClass": "com.turboio.games.vampires.controls.WanderingEnemyControl",
      "speed": 200.0
    }
  ]
}
```

Notes:
- Vertices describe the playable polygon (clockwise or counter-clockwise).
- `movementClass` references an AI control class. Currently only `WanderingEnemyControl` exists.
- Additional fields can be appended as mechanics evolve (e.g., attack patterns, health).

---

## 4. Optional Companion Metadata

To help a content-authoring AI stay consistent, supply additional information alongside the storyline. **These details are global canon for the game and should be reused across every storyline**—do not invent new protagonists or settings unless the design team updates this canon first.

- **Characters:** bios, relationships, keyword lists (see Canon below).
- **World Background:** short setting synopsis, timeline, tone (see Canon below).
- **Story Synopsis:** high-level arc and major beats.
- **Asset Prompts:** as outlined in §2.

Keep the canonical descriptions in a shared document (or embed them under unused top-level keys) that every storyline references. New stories should align with this source of truth unless the core lore explicitly evolves.

---

### Canon: World Background

- The game takes place in a gothic, modern-day city, where supernatural beings hide among humans.
- Tone: dark, tragic, suspenseful; neon lighting and fog blend old-world and modern aesthetics.

### Canon: Style
- The game style revolves around 2d pixel art. Character sprites are generally 64 x 64, but may vary in some cases (e.g. for special bosses)
- Slides should be dramatic still frames, mixing action shots that tell a story with scenes

### Canon: Core Characters

- **Victor** – - The vampire Victor Ravenwood is a wanderer. He carries a rapier capable of cutting interdimensional holes, which he uses to pull victims into his world where they are helpless and subject to his will. He wears a read cloak and fine baroque style clothing, and has flowing silver hair, with dark red eyes.  He has seen many wars, civilizations rising and falling, and the centuries have made him grow nihilistic, until one day... Keywords: brooding, refined, nihilistic, ancient.
- **Selene** – vampire hunter; resolute, calculating, ethically torn about alliances. Keywords: mysterious, disciplined.
- **Elias** – human scholar aiding Victor from the shadows. Keywords: reclusive, scholarly, anxious.
- Writers should reuse these characters unless new ones are added to the canon. Story lines can introduce new characters, or leave out anyone but Victor.

---

## 5. Prompting Workflow for AI

1. Share this spec plus existing examples (`storylines/storyline1.json`, sample levels).
2. Instruct the AI to output:
   - A story JSON matching the schema (`id`, `start`, node map).
   - Level configs referenced by the story (e.g., `levels/level5.json`).
   - Asset prompts with explicit `targetPath`s for slides, level backgrounds/foregrounds, and optional sprites.
3. Review/tweak the outputs.
4. Place the files into the project and test.

Encourage the AI to keep slide text concise (one or two sentences) and levels balanced (e.g., scaling enemy speed or mechanics as difficulty rises).

---

## 6. Future Extensions

As new mechanics arrive, you can safely add extra fields to nodes or level configs. Update this document alongside any format changes so AI-generated content stays compatible.

---

**Summary:** The game presently consumes a straightforward node-map storyline plus separate level configurations. Everything else (metadata, prompts, character bios) can live alongside the data to guide automated content creation without requiring engine changes.


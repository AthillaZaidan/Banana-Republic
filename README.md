# Banana Republic

## IF2010 Object Oriented Programming 2025/2026

Banana Republic is a JavaFX desktop board game for 3-4 local players. The game is inspired by resource-based hex board strategy games, with players competing as Nimons to build a Banana Republic by collecting resources, trading, expanding infrastructure, and reaching 10 victory points.

Players gather five resources: wood, brick, wheat, ore, and banana. These resources are produced by hexagonal terrain tiles based on dice rolls and are used to build pipes, monitoring posts, laboratories, and experiment cards. The project emphasizes Object-Oriented Programming through layered domain models, services, JavaFX controllers, persistence, custom exceptions, reflection-based plugins, and modular class responsibilities.

## Key Features

- **JavaFX desktop interface**  
  The application uses JavaFX, FXML, CSS, custom fonts, image assets, and audio assets to provide a complete graphical game experience.

- **Hexagonal board gameplay**  
  Banana Republic uses a hex board with terrain tiles, token numbers, intersections, paths, coastal harbors, and the Nimon Ungu blocker mechanic.

- **Resource economy**  
  Players collect wood, brick, wheat, ore, and banana from owned intersections adjacent to producing tiles. The bank uses a finite resource inventory.

- **Building system**  
  Players can build Pipes, Monitoring Posts, and Laboratories. Laboratories upgrade Monitoring Posts and increase production and victory points.

- **Turn and setup flow**  
  The game supports player configuration, initial setup placement, dice-based turn order, normal turn phases, dice rolling, trade/build actions, and automatic turn progression.

- **Domestic and maritime trade**  
  Players can trade resources with other players or with the bank through generic and resource-specific harbors.

- **Experiment cards**  
  The game includes development/experiment cards such as Knight, Road Building, Monopoly, and Victory Point cards, plus plugin-backed experiment cards.

- **Nimon Ungu mechanic**  
  Rolling a 7 activates Nimon Ungu behavior, including discard handling, movement, production blocking, and stealing resources from eligible players.

- **Victory calculation**  
  The game tracks victory points from buildings, special cards, and game state, then declares a winner when a player reaches the target score.

- **Save and load**  
  Game state can be saved and loaded through the persistence layer.

- **Plugin system**  
  External JAR plugins can extend the game with custom experiment cards, map generators, and bot strategies through interfaces and reflection-based loading.

- **Bot and custom map samples**  
  Sample plugin JARs are included under `plugin-samples/dist/`.

- **Audio and animation**  
  The app includes background music, sound effects, dice animation, and animated/living UI background components.

## Meet the Developers - BCC BurntCheeseCake

Made Branenda Jordhy  
13524026  
[GitHub Account](https://github.com/ethj0r)

Muhammad Nur Majiid  
13524028  
[GitHub Account](https://github.com/MAJIIDMN)

Jason Edward Salim  
13524034  
[GitHub Account](https://github.com/jsndwrd)

Bryan Pratama Putra Hendra  
13524067  
[GitHub Account](https://github.com/Bryannpph)

Athilla Zaidan Zidna Fann  
13524068  
[GitHub Account](https://github.com/AthillaZaidan)

## Directory

1. `src/main/java/com/bananarepublic/`: main application source code.
2. `src/main/java/com/bananarepublic/model/`: board, building, card, harbor, player, resource, and transport domain models.
3. `src/main/java/com/bananarepublic/service/`: board creation, setup, dice, resource production, trade, Nimon, timer, and victory services.
4. `src/main/java/com/bananarepublic/engine/`: game state, turn state, turn manager, configuration, and game engine orchestration.
5. `src/main/java/com/bananarepublic/controller/`: JavaFX FXML controllers.
6. `src/main/java/com/bananarepublic/ui/`: reusable JavaFX UI helpers and custom components.
7. `src/main/java/com/bananarepublic/plugin/`: plugin API contracts, loaders, adapters, and default plugin-related implementations.
8. `src/main/java/com/bananarepublic/persistence/`: save/load data models and services.
9. `src/main/resources/fxml/`: JavaFX screen and dialog layouts.
10. `src/main/resources/css/`: application styling.
11. `src/main/resources/images/`: board, harbor, and background image assets.
12. `src/main/resources/audio/`: BGM and SFX assets.
13. `src/test/java/com/bananarepublic/`: unit and JavaFX-related tests.
14. `plugin-samples/`: sample external plugin source code and prebuilt plugin JARs.
15. `plugin-build/`: compiled plugin build output.
16. `bcc/`: assignment specification, class design notes, and project report markdown.
17. `docs/`: exported project report.
18. `pom.xml`: Maven build configuration.
19. `java-version.txt`: Java version used by the project.

## Requirements

- Java Development Kit 21. The submitted `java-version.txt` uses Java `21.0.8`.
- Apache Maven.
- JavaFX 21.0.2 dependencies are managed by Maven.
- IntelliJ IDEA is recommended for development, but not required.

## Build, Run, and Test

### Run the Application

Use the JavaFX Maven plugin:

```bash
mvn javafx:run
```

The configured JavaFX entry point is:

```text
com.bananarepublic.App
```

### Build

```bash
mvn clean package
```

The compiled artifact is generated in `target/`.

### Run Tests

```bash
mvn test
```

For headless JavaFX/TestFX environments, use:

```bash
mvn test -Djava.awt.headless=true -Dtestfx.robot=glass -Dglass.platform=Monocle -Dmonocle.platform=Headless -Dprism.order=sw
```

### Clean

```bash
mvn clean
```

## Plugin Samples

Sample plugin artifacts are available at:

```text
plugin-samples/dist/greedy-bot-plugin.jar
plugin-samples/dist/sunburst-island-plugin.jar
```

Sample plugin source code is available under:

```text
plugin-samples/src/sample/plugins/
```

The plugin system is built around public interfaces in `com.bananarepublic.plugin`, including experiment cards, map generator plugins, and player strategies. Plugins are loaded dynamically from external JAR files and validated before being adapted into the game.

## Documentation

- Assignment specification: `bcc/BananaRepublic.md`
- Class design notes: `bcc/ClassDesign.md`
- Project report markdown: `bcc/IF2010_TB2_Laporan_BCC.md`
- Project report PDF: `docs/IF2010_TB2_Laporan_BCC.pdf`

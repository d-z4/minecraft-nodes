# Minecraft nodes plugin
![Nodes map screenshot](docs/src/images/nodes_map_example.jpg)
Map painting but in block game. Contains server plugin and nodes dynmap viewer/editor extension.

**Documentation:** <https://nodes.soy>  
**Editor:** <https://editor.nodes.soy/earth.html>  
**Nodes in action (by Jonathan):** <https://www.youtube.com/watch?v=RVtcc010FpM>



# Repo structure
```
minecraft-nodes/
 ├─ docs/                 - Documentation source
 ├─ dynmap/               - Dynmap editor/viewer
 ├─ nodes/                - Dynmap editor/viewer
 ├─ ports/                - Nodes ports plugin
 └─ scripts/              - Utility scripts
```



# Build
This repository contains the following separate projects:
1.  Nodes main server plugin (root directory)
2.  Dynmap viewer/editor
3.  Plugin documentation
4.  Ports plugin



## 1. Building main server plugin
Requirements:
- Java JDK 21 (current plugin target java version)

Go inside `nodes/` and run
```
./gradlew build
```
Built `nodes-VERSION.jar` will appear in `build/libs/`.

To build without kotlin shaded into the jar (e.g. if using separate kotlin
runtime plugin for example my <https://github.com/d-z4/minecraft-kotlin>),
run with following:
```
./gradlew build -P no-kotlin
```

-----------------------------------------------------------

## 2. Building dynmap viewer/editor
*See internal folder `dynmap/README.md` for more details*

Requirements:
- node.js
- Rust

-----------------------------------------------------------

## 3. Building plugin documentation
### Generating main plugin documentation
*See `docs/README.md`*

Requirements:
- Rust

### Generating nodes commands documentation
Requirements:
- Python 3

This script reads in documentation comments from in-game command
files in `src/main/kotlin/phonon/nodes/commands/*` and generates
documentation for in-game commands:
```
python scripts/generate_commands_docs.py
```
Run this in the git root directory every time in-game commands
are edited in `nodes/` source before re-building documentation site.

-----------------------------------------------------------

## 4. Building ports plugin
Requirements:
- Java JDK 21 (current plugin target java version)

### 1. Build ports plugin `nodes-ports.jar`:
Go inside `ports/` and run
```
./gradlew build
```
Built `.jar` will appear in `build/libs/nodes-ports-*.jar`.



# Issues/Todo
See [TODO.md](./TODO.md) for current high-level todo list.



# License
Licensed under [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html).
See [LICENSE.md](./LICENSE.md).



# Acknowledgements
Special thanks to early contributors:
- **phonon**: making the original plugin
- **Jonathan**: coding + map painting
- **Doneions**: coding + testing + lole

# Forgified Fabric API

> [!WARNING]
> This port of the Forgified Fabric API is a work in progress. Proceed with caution.

### Some things might still not work yet

- Running the root project
- Production
- Publishing
- CI

### Design philosophy

- Bring our own buildscript and pull changes from upstream into a git submodule
- Use a mojang-mapped FAPI source as a base for our code
- Keep changes to unrelated files at minimum, generate metadata and entrypoint files using Gradle
- Automate the update (rebase) process

### Why

- MOJMAP!
  - By using mojmap, we eliminate any necessary remapping, leading to greatly shorter build times and improved development
    time efficiency
  - Improved stability: we no longer need to rely on archloom's "hopes" and "prayers" for neo loom support
  - Improved dev experience: no more userdev remapping issues
- Syncing with upstream is (hopefully) made easier via automation scripts

## About the subprojects

Subprojects related to build setup can be found the in `api-meta` directory.

### How to use

After cloning, make sure to `init` and `update` git submodules to fetch the upstream FAPI repo.
Once imported into an IDE, the project is ready to run.

### Porting new modules

1. Copy the desired upstream module folder from `api-meta/fabric-api-upstream` into the project root
2. Enable the subproject in `settings.gradle.kts`
3. Remap upstream sources by running the subproject's `remapUpstreamSources` task
4. Copy the remapped sources from `api-meta/fabric-api-mojmap/<module>` into the project root
5. When prompted, overwrite all existing files
6. Run the game / make necessary changes to the code

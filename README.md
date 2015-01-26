Forge Relocation FMP Plugin
==========
This is a plugin for Forge Relocation that adds compatibility to FMP.  It is provided as a test case for implementation that shows an example of how to use the Forge Relocation API.  Features added by this plugin include the ability to move multipart tiles correctly, as well as being able to place parts inside frame blocks, allowing for some interesting possibilities.

- [![Build Status](https://travis-ci.org/MrTJP/ForgeRelocationFMP.svg)](https://travis-ci.org/MrTJP/ForgeRelocationFMP)
- [Minecraft Forum Thread](http://www.minecraftforum.net/topic/1885652-)
- [Website](http://projectredwiki.com)

*This mod is not affiliated with Minecraft Forge.*


Developing:
----------
Setup is slightly different depending on what system and IDE you use.
This assumes you know how to run gradle commands on your system.
The base command, `./gradlew` being used below is for Linux or Unix based systems. For windows, this would simply change to `gradlew`.
Of course, if you dont need to use the wrapper (as in, you have gradle installed on your system), you can simply go right to `gradle`.


1. Clone repository to empty folder.
2. Cd to the repository (folder where `src` and `resources` are located).
3. Run `./gradlew setupDecompWorkspace` to set up an environment.
4. Run `./gradlew eclipse` or `./gradlew idea` appropriately.
5. Open your IDE using the generated files (i.e., for IDEA, a ProjectRed.ipr is generated in `./`)
6. Edit, run, and debug your new code.
7. Once its bug free and working, you may submit it as a PR to the main repo.
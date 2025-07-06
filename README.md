Tribal Trouble
==============

Tribal Trouble is a realtime strategy game released by Oddlabs in 2004. In 2014 the source was released under GPL2 license.

This fork aims to:

1. Bring the game back online and make it available for everyone to play as it was originally.
2. Remaster and modernize the graphics.
3. Add more playable features later.

Mandatory step: Key generation:
-------------------------------

For both the client and the servers, before building any of them, perform this step:

- Go to the tools directory `cd tools`.
- Run `ant run generatekeys`.
- Enter a password when it asks for one.
- Make sure the keys are generated under `./common/static/`.

Building the game/client:
-------------------------

Dependencies: JDK-24 (or Open-JDK-24), Apache ant, lwjgl2, xrandr, openal-soft, git

Simply run "ant run" from the ./tt directory.

```
cd tt
ant run
```

To build the game, run

```
ant dist
```

The game is now built in `tt/builds/dist/common`

To generate an exe file, you will need launch4j and a succesful ant dist build.
By default, the `build.xml` points to `C:\Program Files (x86)\Launch4j`

If this isn't the location of your launch4j, change the `launch4j.dir` property in `./tt/build.xml`
and run in `./tt`

```
ant create-exe
```

The required dependencies to run the game are in `tt/builds/dist/windows-x86` with the generated exe.

Building the server:
--------------------

1. Database:

- Install mysql-server.
- Create the database schema from `initmysql.sql`.
- Create the user `matchmaker` with the password `U46TawOp`.

2. Revision number:

- The game client automatically detects the revision number using a git command when it's built.
- The server should set a minimum revision number for what clients can use it.
- That number goes as an entry in the database.
- The revision number of a certain git commit is retrieved using this command:

`git rev-list --count HEAD`

- Store that number in the `settings` table using the following query.

`UPDATE settings SET value = REVISION_NUMBER WHERE property = 'revision';`

3. Building and launching:

- There are two main servers needed. The matchmaker and the router.
- They could be hosted separately or with the same machine.
- The only time they actually need to be on the same machine is when one logs a crashing event to the database.
- To build and start the servers, run their respective scripts from the `server` directory.

```
cd ./server
./matchmaker &
./router &
```

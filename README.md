Built with JDK 24

Dependencies: Apache ant, lwjgl2, xrandr, openal-soft, git

==========================================================

Building the game/client:
-------------------------

Simply run "ant run" from the ./tt directory.

cd tt
ant run

==========================================================

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

3. Key generation:

- Go to the tools directory `cd tools`.
- Run `ant run generatekeys`.
- Enter a password when it asks for one.
- Make sure the keys are generated under `./common/static/`.

4. Building and launching:

- There are two main servers needed. The matchmaker and the router.
- They could be hosted separately or with the same machine.
- The only time they actually need to be on the same machine is when one logs a crashing event to the database.
- To build and start the servers, run their respective scripts from the `server` directory.

cd ./server
./matchmaker &
./router &

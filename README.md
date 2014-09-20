#cljs-2048

2048 game implemented in clojurescript.

2048 was originally created by [Gabriele Cirulli](http://gabrielecirulli.com/works/2048). The game play is the same but my
implementation is original.

[Play Here](http://hewittsoft.com/2048/2048.html)

## Tincan API integration

I created this project in order to learn clojurescript and the Tincan API.
When a player completes a game they can store their score to a remote learning record store (LRS) using the Tincan API.
This project includes a partial implementation of the [Tincan API](http://tincanapi.com) in clojurescript to store
statements when a game is completed with a score, and when a player wins the game.
It also queries a LRS using the Tincan API to retrieve a list of recent winners.

To connect to your own LRS you'll need to edit tincan_config.cljs and fill in these details
``` clj
(def record-store {:endpoint "YOUR ENDPOINT URL"
                   :username "YOUR USERNAME"
                   :password "YOUR PASSWORD"
                   :allowFail false })
```

## Development

This project uses Leiningen 2.x and cljsbuild

To do a single build run
```
$ lein cljsbuild once
```

To do active development I run a local server and have changes compiled immediately by running this in two separate consoles

```
$ lein ring server
$ lein cljsbuild auto
```
You can then visit http://localhost:3000/2048.html for testing.

When you make a change to a file just wait for compilation to
complete and force a refresh in your browser to get the changes.

### To connect a clojurescript REPL for the project
From a console run
```
$ lein trampoline cljsbuild repl-listen
```
From http://localhost:3000/2048.html javascript console run
```
> com.hewittsoft.twenty48.connect_repl.brepl();
```

After that you can enter clojurescript in the first console and see changes in your browser.

### Tests

Tests are running using [PhantomJS](http://phantomjs.org/) so you'll need to install that.

Then run
```
$ lein cljsbuild test
```

## TODO
- More mobile testing
- Local storage for storing own high score and keeping track of game
- Add more key mappings
- Add sliding animations
- Tincan query for leaderboard

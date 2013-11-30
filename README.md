# Chord

A lightweight Clojure/ClojureScript library designed to bridge the gap
between the triad of CLJ/CLJS, web-sockets and core.async.

## Usage

Include the following in your `project.clj`:

    [jarohen/chord "0.2.1"]

### Example project

There is an simple example server/client project under the
`example-project` directory. The client sends websocket messages to
the server, that get echoed back to the client and written on the
page.

You can run it with `lein dev` - an alias that starts up an http-kit
server using [frodo][1] and automatically re-compiles the CLJS.

[1]: https://github.com/james-henderson/lein-frodo

### ClojureScript

**Chord** only has one function, `chord.client/ws-ch`, which takes a
web-socket URL and returns a channel. When the connection opens
successfully, this channel then returns a two-way channel that you can
use to communicate with the web-socket server:

```clojure
(:require [chord.client :refer [ws-ch]]
          [cljs.core.async :refer [<! >! put! close!]])
(:require-macros [cljs.core.async.macros :refer [go]])

(go
  (let [ws (<! (ws-ch "ws://localhost:3000/ws"))]
    (>! ws "Hello server from client!")))
```
		
Messages that come from the server are received as a map with a
`:message` key:

```clojure
(go
  (let [ws (<! (ws-ch "ws://localhost:3000/ws"))]
    (js/console.log "Got message from server:" (:message (<! ws)))))
```
		
Errors in the web-socket channel are returned as a map with an
`:error` key:

```clojure
(go
  (let [ws (<! (ws-ch "ws://localhost:3000/ws"))
        {:keys [message error]} (<! ws)]
    (if error
      (js/console.log "Uh oh:" error)
	  (js/console.log "Hooray! Message:" message))))
```
		  
As of 0.2.1, you can configure the buffering of the channel by
(optionally) passing custom read/write channels, as follows:

```clojure
(:require [cljs.core.async :as a])
(ws-ch "ws://localhost:3000/ws"
       {:read-ch (a/chan (a/sliding-buffer 10))
	    :write-ch (a/chan 5)})
```

By default, Chord uses unbuffered channels, like core.async itself.

### Clojure

**Chord** wraps the websocket support provided by [http-kit][1], a
fast Clojure web server compatible with Ring. 

[1]: http://http-kit.org/index.html

Again, there's only one entry point to remember here: a wrapper around
http-kit's `with-channel` macro. The only difference is that, rather
than using http-kit's functions to interface with the channel, you can
use core.async's primitives.

Chord's `with-channel` is used as follows:

```clojure
(:require [chord.http-kit :refer [with-channel]]
          [clojure.core.async :refer [<! >! put! close! go]])

(defn your-handler [req]
  (with-channel req ws-ch
    (go
      (let [{:keys [message]} (<! ws-ch)]
        (println "Message received:" message)
        (>! ws-ch "Hello client from server!")
        (close! ws-ch)))))
```

This can take custom buffered read/write channels as well:

```clojure
(require '[clojure.core.async :as a])

(defn your-handler [req]
  (with-channel req ws-ch
    {:read-ch (a/chan (a/dropping-buffer 10))}
    (go
      (let [{:keys [message]} (<! ws-ch)]
        (println "Message received:" message)
        (>! ws-ch "Hello client from server!")
        (close! ws-ch)))))
```


## Bug reports/pull requests/comments/suggestions etc?

Yes please! Please submit these in the traditional GitHub manner.

## Thanks

Thanks to [Thomas Omans (eggsby)](https://github.com/eggsby) for
(unknowingly!) providing the idea of how to combine two core.async
channels together! https://gist.github.com/eggsby/6102537

## Changes

### 0.2.1

No breaking changes. Added ability to pass custom buffered channels to
use instead of the default unbuffered channels.

Thanks to [Timo Sulg (timgluz)](https://github.com/timgluz) for the
PR!

### 0.2.0

Breaking change - CLJS namespace now `chord.client` due to recent
versions of the CLJS compiler not liking single-segment namespaces

Thanks to [Joshua Griffith (hadronzoo)](https://github.com/hadronzoo)
for the PR!

### 0.1.1

No breaking changes - added adapter around http-kit for Clojure
support. 

### 0.1.0

Initial release.

## License

Copyright © 2013 James Henderson

Distributed under the Eclipse Public License, the same as Clojure.

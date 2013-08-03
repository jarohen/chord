# Chord

A ClojureScript library designed to bridge the gap between the triad
of CLJS, web-sockets and core.async.

## Usage

Include the following in your `project.clj`:

    [jarohen/chord "0.1.0"]

**Chord** only has one function, `chord/ws-ch`, which takes a
web-socket URL and returns a channel. When the connection opens
successfully, this channel then returns a two-way channel that you can
use to communicate with the web-socket server:

    (:require [chord :refer [ws-ch]]
	          [cljs.core.async :refer [<! >! put! close!]])
    (:require-macros [cljs.core.async.macros :refer [go]])
	
	(go
	  (let [ws (<! (ws-ch "ws://localhost:3000/ws"))]
	    (>! ws "Hello server from client!")))
		
Messages that come from the server are received as a map with a
`:message` key:

    (go
      (let [ws (<! (ws-ch "ws://localhost:3000/ws"))]
	    (js/console.log "Got message from server:" (:message (<! ws)))))
		
Errors in the web-socket channel are returned as a map with an
`:error` key:

    (go
      (let [ws (<! (ws-ch "ws://localhost:3000/ws"))
	        {:keys [message error]} (<! ws)]
	    (if error
          (js/console.log "Uh oh:" error)
		  (js/console.log "Hooray! Message:" message)))
		  
**And that's it!**

## Bug reports/pull requests/comments/suggestions etc?

Yes please! Please submit these in the traditional GitHub manner.

## Thanks

Thanks to [Thomas Omans (eggsby)](https://github.com/eggsby) for
(unknowingly!) providing the idea of how to combine two core.async
channels together! https://gist.github.com/eggsby/6102537

## License

Copyright © 2013 James Henderson

Distributed under the Eclipse Public License, the same as Clojure.

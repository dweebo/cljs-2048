; for local development if you want to have a browser repl
; from the browser console type
; com.hewittsoft.twenty48.connect_repl.brepl();
(ns com.hewittsoft.twenty48.connect-repl
  (:require [clojure.browser.repl :as repl]))

(defn ^:export brepl []
  "connect the browser repl"
  (do (enable-console-print!)
      (println "Start connect to repl")
      (repl/connect "http://localhost:9000/repl")
      (println "Finished connect to repl")))
; for local development
(ns com.hewittsoft.twenty48.server
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.resource :as resources]
            [ring.util.response :as response])
  (:gen-class))

(defn handler [request]
  (if (= "/" (:uri request))
      (response/redirect "/2048.html")
  (response/redirect (:uri request))))

(def app 
  (-> handler
    (resources/wrap-resource "public")))

(defn -main [& args]
  (jetty/run-jetty app {:port 3000}))


; small clojurescript wrapper around parts of the TinCan.js API (https://github.com/RusticiSoftware/TinCanJS)
(ns com.hewittsoft.twenty48.tincan
  (:require [com.hewittsoft.twenty48.tincan-config :as config]))

(def not-nil? (complement nil?))

(defn get-tincan-api [recordStore]
  "get a tincan api object"
  (->> (vector recordStore)
       (hash-map :recordStores)
       (clj->js)
       (js/TinCan.)))

(defn send-statement [tincan statement]
  "send a tincan statement"
  (.sendStatement tincan (clj->js statement)))

(defn get-statements [tincan query callback]
  "look for tincan statements"
  (letfn [(get-statements-callback [err results]
    (if (not-nil? err)
      (callback err nil)
      (callback nil (.. results -statements))))]
    (->> (assoc query :callback get-statements-callback)
         (clj->js)
         (.getStatements tincan))))

; 2048-specific tincan funtions
(defn submit-high-score [game-id name email score]
  "submit a high score"
  (let [tincan-api (get-tincan-api config/record-store)]
    (send-statement tincan-api
     {
         :actor {
             :name name
             :mbox (str "mailto:" email)
         }
         :verb {
             :id "http://adlnet.gov/expapi/verbs/scored"
             :display {
                 :en-US: "scored"
             }
         }
         :object {
             :id "http://hewittsoft.com/2048/"
             :display {
                 :en-US: "2048 Clojurescript TinCan Demo"
             }
         }
         :result {
             :score {
                 :raw score
             }
         }
         :context {
             :contextActivities {
                 :grouping {
                     :id "http://hewittsoft.com/2048/"
                 }
             }
             :extensions {
                 "http://hewittsoft.com/2048/gameId" game-id
             }
         }
     })))


(defn submit-win [game-id name email]
  "submit a win"
  (let [tincan-api (get-tincan-api config/record-store)]
    (send-statement tincan-api
     {
         :actor {
             :name name
              :mbox (str "mailto:" email)
         }
         :verb {
             :id "http://adlnet.gov/expapi/verbs/completed"
             :display {
                 :en-US: "completed"
             }
         }
         :object {
             :id "http://hewittsoft.com/2048/"
             :display {
                 :en-US: "2048 Clojurescript TinCan Demo"
             }
         }
         :context {
             :contextActivities {
                 :grouping {
                     :id "http://hewittsoft.com/2048/"
                 }
             }
             :extensions {
                 "http://hewittsoft.com/2048/gameId" game-id
             }
         }
     })))


(defn get-winners [callback]
  "get winners. asynchronous, winners will be sent to the callback function"
  (letfn [(get-winners-callback [err statements]
    (if (not-nil? err)
      (callback err nil)
      (let [winners (map #(hash-map :name (.. % -actor -name) :date (.. % -timestamp)) statements)
            winners (take 10 winners)]
         (callback nil winners))))]
    (let [tincan-api (get-tincan-api config/record-store)]
      (get-statements
          tincan-api
          {
              :params {
                  :verb {
                      :id "http://adlnet.gov/expapi/verbs/completed"
                  }
                  :activity {
                      :id "http://hewittsoft.com/2048/"
                  }
              }
          }
          get-winners-callback))))

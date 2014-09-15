; small clojurescript wrapper around parts of the TinCan.js API (https://github.com/RusticiSoftware/TinCanJS)
(ns com.hewittsoft.twenty48.tincan
  (:require [com.hewittsoft.twenty48.tincan-config :as config]))

(def not-nil? (complement nil?))

(defn get-tincan-api [recordStore]
  "get a tincan api object"
  (js/TinCan. (clj->js { :recordStores (vector recordStore) })))

(defn send-statement [tincan statement]
  "send a tincan statement"
  (.sendStatement tincan (clj->js statement)))

(defn get-statements [tincan query callback]
  "look for tincan statements"
  (letfn [(get-statements-callback [err results]
    (if (not-nil? err)
      (println err)
      (let [statements (.. results -statements)
            winners (map #(hash-map :name (.. % -actor -name) :date (.. % -timestamp)) statements)
            winners (take 10 winners)]
          (do
            (doall (map #(println (:name %) " at " (:date %)) winners))
            (callback winners)
            nil))))]
    (let [cfg (assoc query :callback get-statements-callback)
          cfgjs (clj->js cfg)]
      (.getStatements tincan cfgjs))))


; 2048-specific tincan funtions
(defn submit-high-score [game-id name email score]
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
        callback)))

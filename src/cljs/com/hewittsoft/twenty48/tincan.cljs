; small clojurescript implementation of parts of the TinCan API
(ns com.hewittsoft.twenty48.tincan2
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [com.hewittsoft.twenty48.tincan-config :as config]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [cljs-uuid.core :as uuid]
            [cljs-time.core :as time]
            [cljs-time.format :as time-format]))

(defn- get-uuid []
  "get a new random uuid"
  (.-uuid (uuid/make-random)))

(defn- get-current-timestamp []
  "get the current timestamp formatted for tincan api"
  (time-format/unparse (time-format/formatters :date-time) (time/now)))

(def base-tincan-request-params
  "request parameters always required by tincan"
  {:headers { "X-Experience-API-Version" "1.0.1" }
   :basic-auth {:username (:username config/record-store)
                :password (:password config/record-store)}
   :with-credentials? false })

(defn- create-tincan-request [query-params json-params]
  "create a request object suitable for cljs-http and the tincan api"
  (assoc base-tincan-request-params
         :query-params query-params
         :json-params json-params))

(defn send-statement [statement]
  "send a statement to LRS using tincan API 1.0.1"
  (let [statement-id (get-uuid)
        timestamp (get-current-timestamp)
        endpoint (str (:endpoint config/record-store) "statements")
        query-params { :statementId statement-id }
        statement (assoc statement :id statement-id :timestamp timestamp)]
    (http/put endpoint (create-tincan-request query-params statement))))


(defn submit-high-score [game-id name email score]
  "submit a high score, by sending over tincan to a LRS"
  (send-statement
   {
      :actor {
          :objectType "Agent"
          :name name
          :mbox (str "mailto:" email)
      }
      :verb {
          :id "http://adlnet.gov/expapi/verbs/scored"
          :display {
              :en-US "scored"
          }
      }
      :object {
          :id "http://hewittsoft.com/2048/"
          :objectType "Activity"
          :definition {
              :name {
                  :en-US "2048 Clojurescript TinCan Demo"
              }
          }
      }
      :result {
          :score {
              :raw score
          }
      }
      :context {
          :contextActivities {
              :grouping [{
                  :id "http://hewittsoft.com/2048/"
                  :objectType "Activity"
              }]
          }
          :extensions {
              "http://hewittsoft.com/2048/gameId" game-id
          }
      }}))

(defn submit-win [game-id name email]
  "submit a win"
  (send-statement
    {
      :actor {
          :objectType "Agent"
          :name name
          :mbox (str "mailto:" email)
      }
      :verb {
          :id "http://adlnet.gov/expapi/verbs/completed"
          :display {
              :en-US "completed"
          }
      }
      :object {
          :id "http://hewittsoft.com/2048/"
          :objectType "Activity"
          :definition {
              :name {
                  :en-US "2048 Clojurescript TinCan Demo"
              }
          }
      }
      :context {
          :contextActivities {
              :grouping [{
                  :id "http://hewittsoft.com/2048/"
                  :objectType "Activity"
              }]
          }
          :extensions {
              "http://hewittsoft.com/2048/gameId" game-id
          }
      }}))


(defn get-winners [callback]
  (go (let [response (<! (http/get (str (:endpoint config/record-store) "statements")
                                   {:query-params {:verb "http://adlnet.gov/expapi/verbs/completed"
                                                   :activity "http://hewittsoft.com/2048/"}
                                    :headers { "X-Experience-API-Version" "1.0.1" }
                                    :basic-auth {:username (:username config/record-store)
                                                 :password (:password config/record-store)}
                                    :with-credentials? false}))
            err (if (:success response)  nil "Error getting winners")
            winners (if err nil
                      (->> (:statements (:body response))
                           (map #(hash-map :name (:name (:actor %)) :date (:timestamp %)))
                           (take 10)))]
        (callback err winners))))

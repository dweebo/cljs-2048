; small clojurescript implementation of parts of the TinCan API
(ns com.hewittsoft.twenty48.tincan
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [com.hewittsoft.twenty48.tincan-config :as config]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [cljs-uuid.core :as uuid]
            [cljs-time.core :as time]
            [cljs-time.format :as time-format]))

(def twenty48-statement-template {
    :actor {
        :objectType "Agent"
    }
    :verb {
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
    }
})

(def void-statement-template {
    :actor {
        :objectType "Agent"
    }
    :verb {
        :id "http://adlnet.gov/expapi/verbs/voided"
    }
    :object {
        :objectType "StatementRef"
        :id nil
    }
})


(def verb-scored { :id "http://adlnet.gov/expapi/verbs/scored" :display { :en-US "scored" }})
(def verb-completed { :id "http://adlnet.gov/expapi/verbs/completed" :display { :en-US "completed" }})

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

(defn void-statement [name email stmt-id]
  "void a statement from LRS using tincan API 1.0.1"
  (let [void-statement (-> void-statement-template
                           (assoc-in [:object :id] stmt-id)
                           (update-in [:actor] assoc :name name :mbox (str "mailto:" email)))]
    (send-statement void-statement)))

(defn submit-high-score [game-id name email score]
  "submit a high score, by sending over tincan to a LRS"
  (send-statement
    (-> twenty48-statement-template
        (update-in [:actor] assoc :name name :mbox (str "mailto:" email))
        (assoc-in [:context :extensions] { "http://hewittsoft.com/2048/gameId" game-id})
        (assoc :result { :score { :raw score }} :verb verb-scored))))

(defn submit-win [game-id name email]
  "submit a win"
  (send-statement
    (-> twenty48-statement-template
        (update-in [:actor] assoc :name name :mbox (str "mailto:" email))
        (assoc-in [:context :extensions] { "http://hewittsoft.com/2048/gameId" game-id})
        (assoc :verb verb-completed))))


(defn get-winners [callback]
  (go (let [endpoint (str (:endpoint config/record-store) "statements")
            query-params {:verb "http://adlnet.gov/expapi/verbs/completed"
                          :activity "http://hewittsoft.com/2048/"}
            response (<! (http/get endpoint (create-tincan-request query-params nil)))
            err (if (:success response)  nil "Error getting winners")
            winners (if err nil
                      (->> (:statements (:body response))
                           (filter #(= "http://adlnet.gov/expapi/verbs/completed" (:id (:verb %)))) ; ignore voided completions
                           (map #(hash-map :name (:name (:actor %)) :date (:timestamp %)))
                           (take 10)))]
        (callback err winners))))

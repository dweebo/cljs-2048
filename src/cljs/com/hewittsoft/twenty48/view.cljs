(ns com.hewittsoft.twenty48.view
  (:require [com.hewittsoft.twenty48.core :as core]
            [com.hewittsoft.twenty48.tincan :as tincan]
            [jayq.core :as jayq]
            [cljs-uuid.core :as uuid]
            [cljs-time.core :as time]
            [cljs-time.format :as time-format]))

(def not-nil? (complement nil?))

; get a dom element by id
(defn by-id [id]
  (.getElementById js/document id))

; set the html of a dom element
(defn set-html! [dom content]
  (set! (. dom -innerHTML) content))

; set the classes of a dom element
(defn set-class! [dom cls]
  (set! (. dom -className) cls))

; update a blocks color on the board
(defn update-color [div val]
  (let [cls (str "block2048 c" val)]
    (set-class! div cls)))

; after a move, update the board and score with changes
(defn update-board [board score]
  (do
    (doall (set-html! (by-id "score") (str score)))
    (doall (for [ x (range 0 4) y (range 0 4) ]
      (let [val (core/board-val board x y)
            html (if (= 0 val) "" val)
            div (by-id (str y x))]
        (do
          (update-color div val)
          (set-html! div html)))))))

; save high score to server (using tincan api to LRS)
(defn submit-high-score []
  (let [name (. (by-id "name") -value)
        email (. (by-id "email") -value)
        game-id (.-uuid  (uuid/make-random))]
    (if (or (= name "") (= email ""))
      (set-html! (by-id "game-over-error") "Please enter your name and email")
      (do
        (println "submit " name email)
        (.modal (jayq/$ :#game-over-modal) "hide")
        (if (core/win? @core/board)
          (tincan/submit-win game-id name email))
        (tincan/submit-high-score game-id name email @core/score)))))

; handle a loss
(defn lost []
  (do
    (.modal (jayq/$ :#game-over-modal) "show")))

; handle a win
(defn won[]
  (.modal (jayq/$ :#won-modal) "show"))

; if playing with a keyboard the arrow keys can make moves
(defn handle-keys [e]
  (let [key (.-keyCode e)
        move-func (case key
                     37 core/left-compact-rows
                     38 core/up-compact-rows
                     39 core/right-compact-rows
                     40 core/down-compact-rows
                     nil)]
    (if (not-nil? move-func) (core/make-move move-func lost won update-board))))

; if playing with a touchscreen finger swipes can make moves
(defn swipe-left [] (core/make-move core/left-compact-rows lost won update-board))
(defn swipe-right [] (core/make-move core/right-compact-rows lost won update-board))
(defn swipe-up [] (core/make-move core/up-compact-rows lost won update-board))
(defn swipe-down [] (core/make-move core/down-compact-rows lost won update-board))

; resize the board to fill as much of the viewport as possible
(defn resize-board []
  (let [
        screen-width  (.getWidth js/window.viewportSize)
        screen-height (- (.getHeight js/window.viewportSize) (.height (jayq/$ :#control-panel)) 50)

        ;screen-height (- (.height (jayq/$ :body)) (.height (jayq/$ :#control-panel)))
        limiting-dimension (if (> screen-width screen-height) screen-height screen-width)
        block-size (/ (- limiting-dimension 40) 4)]
    (do
      (println screen-width " by " screen-height " " limiting-dimension " " block-size)
      (.width (jayq/$ :.block2048) block-size)
      (.height (jayq/$ :.block2048) block-size)
      (.width (jayq/$ :#control-panel) (.width (jayq/$ :#board2048))))))


(defn format-date [date]
  (let [dt (time-format/parse (time-format/formatters :date-time) date)
        dt-tz (time/plus dt (time/hours -4))] ; TODO format to local timezone, cljs-time doesn't support that quite yet
        ;dt-tz (time/to-time-zone dt (time/time-zone-for-offset -4))] API says to do this but it isn't implemented
    (time-format/unparse (time-format/formatter "MM/dd/yyyy HH:mm") dt-tz)))

(defn get-winner-table-row [winner]
  (str "<tr><td>" (:name winner) "</td><td>" (format-date (:date winner)) " EDT</td></tr>"))

(defn get-winners []
  (letfn [(get-winners-callback [winners]
    (let [table (jayq/$ :#winner-table)]
      (do
        (jayq/empty table)
        (jayq/append table "<tr><th>Name</th><th>Date</th></tr>")
        (doall (map #(jayq/append table (get-winner-table-row %)) winners))
        (.modal (jayq/$ :#winners-modal) "show"))))]
      (tincan/get-winners get-winners-callback)))

(defn show-about []
  (.modal (jayq/$ :#about-modal) "show"))

; start a new game
(defn ^:export new-game []
  (do
    (println "new game")
    (core/new-game)
    (update-board @core/board 0)))

; initialize view, game, event listeners
(defn ^:export init []
  (do
      (enable-console-print!)
      (println "start init")
      (resize-board)
      (.on (jayq/$ :#new-game-button) "click" new-game)
      (.on (jayq/$ :#winners-button) "click" get-winners)
      (.on (jayq/$ :#about-button) "click" show-about)
      (.on (jayq/$ :#submit-high-score-button) "click" submit-high-score)
      (.on (jayq/$ js/window) "keydown" handle-keys)
      (.on (jayq/$ js/window) "swipeleft" swipe-left)
      (.on (jayq/$ js/window) "swiperight" swipe-right)
      (.on (jayq/$ js/window) "swipedown" swipe-down)
      (.on (jayq/$ js/window) "swipeup" swipe-up)
      (new-game)
      (println "end init")))

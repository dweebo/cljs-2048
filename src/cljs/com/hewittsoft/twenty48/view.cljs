; handle the browser view of the 2048 board
; since there aren't many events to keep track of is also the "controller" of the app
(ns com.hewittsoft.twenty48.view
 (:require [com.hewittsoft.twenty48.core :as core]
           [com.hewittsoft.twenty48.tincan :as tincan]
           [cljs-uuid.core :as uuid]
           [cljs-time.core :as time]
           [cljs-time.format :as time-format]
           [dommy.utils :as utils]
           [dommy.core :as dommy]
           [viewportSize] [tocca])
 (:use-macros
   [dommy.macros :only [node sel sel1 deftemplate]]))


(def not-nil? (complement nil?))

(defn set-class! [dom cls]
  "set the classes of a dom element"
  (set! (. dom -className) cls))

(defn update-color! [div val]
  "update the color of a blocks on the board"
  (let [cls (str "block2048 c" val)]
    (set-class! div cls)))

(defn show-modal [modal]
  (dommy/set-style! modal :display "block")
  (let [board-pos (dommy/bounding-client-rect (sel1 :#board2048))
        dialog-pos (dommy/bounding-client-rect modal)
        board-height (- (:bottom board-pos) (:top board-pos))
        dialog-height (- (:bottom dialog-pos) (:top dialog-pos))
        top-offset (/ (- board-height dialog-height) 2)
        dialog-top (+ top-offset (:top board-pos))
        dialog-width (- (:right board-pos) (:left board-pos) 40)]
    (dommy/set-style! modal :top dialog-top :width dialog-width)))

(defn hide-modal [modal]
  (dommy/set-style! modal :display "none"))

(defn hide-all-modals []
  (doall (->> (sel :.modal-dialog)
              (map #(dommy/set-style! % :display "none")))))

(defn update-board [board score]
  "after a move, update the board and score with changes"
  (do
    (dommy/set-html! (sel1 :#score) (str score))
    (doall (for [ x (range 0 4) y (range 0 4) ]
      (let [val (core/board-val board x y)
            html (if (= 0 val) "" val)
            div (sel1 (str "#b" y x))]
        (do
          (update-color! div val)
          (dommy/set-html! div html)))))))

(defn submit-high-score []
  "save a high score and win to server (using tincan api to LRS)"
  (let [name (dommy/value (sel1 :#name))
        email (dommy/value (sel1 :#email))
        game-id (.-uuid  (uuid/make-random))]
    (if (or (= name "") (= email ""))
      (dommy/set-html! (sel1 :#game-over-error) "Please enter your name and email")
      (do
        (println "submit " name email)
        (hide-modal (sel1 :#game-over-modal))
        (if (core/win? @core/board)
          (tincan/submit-win game-id name email))
        (tincan/submit-high-score game-id name email @core/score)))))

(defn lost []
  "handle a loss"
  (show-modal (sel1 :#game-over-modal)))

(defn won[]
  "handle a win"
  (show-modal (sel1 :#won-modal)))


(defn handle-keys [e]
  "if playing with a keyboard the arrow keys can make moves"
  (let [key (.-keyCode e)
        move-func (case key
                     37 core/left-compact-rows
                     38 core/up-compact-rows
                     39 core/right-compact-rows
                     40 core/down-compact-rows
                     nil)]
    (if (not-nil? move-func)
      (core/make-move move-func lost won update-board))))

; if playing with a touchscreen finger swipes can make moves
(defn swipe-left [] (core/make-move core/left-compact-rows lost won update-board))
(defn swipe-right [] (core/make-move core/right-compact-rows lost won update-board))
(defn swipe-up [] (core/make-move core/up-compact-rows lost won update-board))
(defn swipe-down [] (core/make-move core/down-compact-rows lost won update-board))

(defn resize-board []
  "resize the board to fill as much of the viewport as possible"
  (let [
        screen-width  (.getWidth js/window.viewportSize)
        screen-height (- (.getHeight js/window.viewportSize)
                         (dommy/px (sel1 :#control-panel) :height)
                         (dommy/px (sel1 :#score-panel) :height)
                         50)
        limiting-dimension (if (> screen-width screen-height) screen-height screen-width)
        block-size (/ (- limiting-dimension 40) 4)]  ;TODO this is hacky, could be improved on mobile

      (println screen-width " by " screen-height " " limiting-dimension " " block-size)
      (doall (->> (sel :.block2048)
                  (map #(dommy/set-px! % :width block-size :height block-size))))
      (dommy/set-px! (sel1 :#score-panel) :width (dommy/px (sel1 :#board2048) :width))
      (dommy/set-px! (sel1 :#control-panel) :width (dommy/px (sel1 :#board2048) :width))))

(defn format-date [date]
  (let [dt (time-format/parse (time-format/formatters :date-time) date)
        dt-tz (time/plus dt (time/hours -4))] ; TODO format to local timezone, cljs-time doesn't support that quite yet
        ;dt-tz (time/to-time-zone dt (time/time-zone-for-offset -4))] API says to do this but it isn't implemented
    (time-format/unparse (time-format/formatter "MM/dd/yyyy HH:mm") dt-tz)))

(deftemplate winner-table-row [winner]
  [:tr [:td (:name winner)][:td (str (format-date (:date winner)) " EDT")]])

(defn get-winners []
  "get recent winners and display to user"
  (letfn [(get-winners-callback [err winners]
    (if (not-nil? err)
      (println "Error: " err) ; TODO show error to user
      (let [table (sel1 :#winner-table)]
        (do
          (dommy/clear! table)
          (dommy/append! table (node [:tr [:th "Name"][:th "Date"]]))
          (doall (map #(dommy/append! table (winner-table-row %)) winners))
          (show-modal (sel1 :#winners-modal))))))]
      (tincan/get-winners get-winners-callback)))

(defn show-about []
  "show info about app"
  (show-modal (sel1 :#about-modal)))

(defn new-game []
  "start a new game"
  (do
    (core/new-game)
    (update-board @core/board 0)))

(defn ^:export init []
  "Entry point from browser.
   Initialize view, game, event listeners."
  (do
      (resize-board)
      (doall (->> (sel :.modal-close)
                  (map #(dommy/listen! % :click hide-all-modals))))
      (dommy/listen! (sel1 :#new-game-button) :click new-game)
      (dommy/listen! (sel1 :#winners-button) :click get-winners)
      (dommy/listen! (sel1 :#about-button) :click show-about)
      (dommy/listen! (sel1 :#submit-high-score-button) :click submit-high-score)
      (dommy/listen! (sel1 :body) :keydown handle-keys)
      (dommy/listen! (sel1 :body) "swipeleft" swipe-left)
      (dommy/listen! (sel1 :body) "swiperight" swipe-right)
      (dommy/listen! (sel1 :body) "swipedown" swipe-down)
      (dommy/listen! (sel1 :body) "swipeup" swipe-up)
      (new-game)))
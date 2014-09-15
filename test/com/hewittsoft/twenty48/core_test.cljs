(ns com.hewittsoft.twenty48.core-test
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:require [cemerick.cljs.test :as t]
            [com.hewittsoft.twenty48.core :as core]))

(deftest create-empty-board
  (is (= [[0 0 0 0] [0 0 0 0] [0 0 0 0] [0 0 0 0]] (core/create-empty-board))))

(deftest filled?
  (let [board [[0 1] [0 0]]]
    (is (= false (core/filled? board 0 0)))
    (is (= true  (core/filled? board 1 0)))
    (is (= false (core/filled? board 0 1)))
    (is (= false (core/filled? board 1 1)))))

(deftest board-val []
  (let [board [[1 2] [3 4]]]
    (is (= 1 (core/board-val board 0 0)))
    (is (= 2 (core/board-val board 1 0)))
    (is (= 3 (core/board-val board 0 1)))
    (is (= 4 (core/board-val board 1 1)))))

(deftest add-num-to-board
  (let [board [[5 5 5 5] [5 5 5 5] [5 5 5 5] [5 5 5 0]]
        new-board (core/add-num-to-board board)
        new-val (core/board-val new-board 3 3)]
    (is (or (= 2 new-val) (= 4 new-val)))))

(deftest get-column
  (let [board [[1 2 3 4] [5 6 7 8]]]
    (is (= [1 5] (core/get-column board 0)))
    (is (= [2 6] (core/get-column board 1)))
    (is (= [3 7] (core/get-column board 2)))
    (is (= [4 8] (core/get-column board 3)))))

(deftest rotate-board
  (let [board [[0 1 2 3] [4 5 6 7] [8 9 10 11] [12 13 14 15]]
        rot (core/rotate-board board)]
    (is (= [0 4 8 12] (core/get-column board 0)))
    (is (= [0 1 2 3] (core/get-column rot 0)))
    (is (= [4 5 6 7] (core/get-column rot 1)))
    (is (= [8 9 10 11] (core/get-column rot 2)))))

(deftest left-compact-row-empty
  (let [row [0 0 0 0]
        new-row (core/left-compact-row row)]
    (is (= [0 0 0 0] new-row))))

(deftest left-compact-row-slide
  (let [row [0 1 2 3]
        new-row (core/left-compact-row row)]
    (is (= [1 2 3 0] new-row))))

(deftest left-compact-row-slide2
  (let [row [0 1 0 2]
        new-row (core/left-compact-row row)]
    (is (= [1 2 0 0] new-row))))

(deftest left-compact-row-combine
  (let [row [2 0 0 2]
        new-row (core/left-compact-row row)]
    (is (= [4 0 0 0] new-row))))

(deftest left-compact-row-double-combine
  (let [row [2 2 2 2]
        new-row (core/left-compact-row row)]
    (is (= [4 4 0 0] new-row))))

(deftest left-compact-row-double-combine2
  (let [row [2 2 3 3]
        new-row (core/left-compact-row row)]
    (is (= [4 6 0 0] new-row))))

(deftest right-compact-rows
  (let [board [[2 0 0 0]
               [2 0 2 0]
               [8 2 4 2]
               [2 2 4 4]]
        new-board (core/right-compact-rows board)]
    (is (= [[0 0 0 2]
            [0 0 0 4]
            [8 2 4 2]
            [0 0 4 8]] new-board))))

(deftest up-compact-rows
  (let [board [[2 0 0 0]
               [2 0 2 0]
               [8 2 4 2]
               [2 2 4 4]]
        new-board (core/up-compact-rows board)]
    (is (= [[4 4 2 2]
            [8 0 8 4]
            [2 0 0 0]
            [0 0 0 0]] new-board))))

(deftest down-compact-rows
  (let [board [[2 0 0 0]
               [2 0 2 0]
               [8 2 4 2]
               [2 2 4 4]]
        new-board (core/down-compact-rows board)]
    (is (= [[0 0 0 0]
            [4 0 0 0]
            [8 0 2 2]
            [2 4 8 4]] new-board))))

(deftest no-lose-full-board
  (let [board [[4 2 2 8]
               [8 4 8 4]
               [4 8 4 8]
               [8 4 8 4]]]
    (is (= false (core/lose? board)))))

(deftest lose-full-board
  (let [board [[4 8 4 8]
               [8 4 8 4]
               [4 8 4 8]
               [8 4 8 4]]]
    (is (= true (core/lose? board)))))

(deftest no-win
  (let [board [[4 8 4 8]
               [8 4 8 4]
               [4 8 4 8]
               [8 4 8 4]]]
    (is (= nil (core/win? board)))))

(deftest yes-win
  (let [board [[4 8 4 8]
               [8 4 8 4]
               [4 2048 4 8]
               [8 4 8 4]]]
    (is (= true (core/win? board)))))

(deftest score-move
  (let [old-board [[2 2 4 4]
                   [8 0 0 8]
                   [16 0 16 0]
                   [32 32 2 4]]
        new-board (core/left-compact-rows old-board)]
    (is (= (+ 4 8 16 32 64) (core/score-move old-board new-board)))))

(defn setup-game [board score playing won]
  (do
    (reset! core/board board)
    (reset! core/score score)
    (reset! core/playing playing)
    (reset! core/won won))
  nil)

(deftest make-move-no-win-loss
  (let [won (atom false)
        won-callback (fn[] (reset! won true))
        lost (atom false)
        lost-callback (fn[] (reset! lost true))
        new-board (atom [])
        new-score (atom 0)
        update-view (fn[board score] (do (reset! new-board board) (reset! new-score score)))
        a (setup-game [[2 0 0 0]
                       [2 0 2 0]
                       [8 2 4 2]
                       [2 2 4 4]] 0 true false)
        b (core/make-move core/down-compact-rows  lost-callback won-callback update-view)]
    (is (= false @won))
    (is (= false @lost))
    (is (= 16 @new-score 16))))

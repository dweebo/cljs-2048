; 2048 board model and operations
(ns com.hewittsoft.twenty48.core)

(def size 4)
(def win-amount 2048)

; new numbers added to the board are either 2 or 4.
; this defines the probability of the new number being a 2
(def two-probability 0.8)



(def not-zero? (complement zero?))

(def reversev
  "reverse a vector"
  (comp vec reverse))

(defn create-empty-board []
  (vec (take size (repeatedly #(vec (take size (repeat 0)))))))

(defn filled? [board x y]
  (> (get (get board y) x) 0))

(defn add-num-to-board [board]
   "add a new 2 or 4 to a random unoccupied spot on a board"
   (let [y (rand-int size)
         x (rand-int size)
         num (if (< (rand) two-probability) 2 4)
         row (assoc (get board y) x num)]
     (if (filled? board x y)
       (recur board)
       (assoc board y row))))

(defn init-board[]
  "setup a new board with the first number"
  (->> (create-empty-board)
       (add-num-to-board)))

(defn board-val [board x y]
  "get the value at a position on a board"
  (get (get board y) x))

(defn get-column [board col-index]
  "get a coloumn of values from a board"
  (vec (map #(nth % col-index) board)))

(defn rotate-board [board]
  "rotate a board, so that columns become rows"
  (mapv (partial get-column board) (range size)))

(defn remove-zeroes [input-row]
  "for a vector of values move all the entries with 0 to the end
   e.g. input of [4 0 2 0] returns [4 2 0 0]
        input of [0 0 0 1] returns [1 0 0 0]"
  (let [row (vec (filter #(> % 0) input-row))
        zero-len (- size (count row))]
    (into row (vec (repeat zero-len 0)))))

(defn compact-vec [input-row]
  "compact a vector of values using 2048 logic, if two adjacent values are the same combine them
   e.g. input of [8 8 4 2] returns [16 4 2 0]
        input of [4 4 4 4] returns [8 8 0 0] (you only compact a value once per move)"
  (letfn [(compact-recurse [row]
    (if (> 2 (count row)) row
    (let [a (first row)
          b (second row)]
      (if (= a b) (apply vector (+ a b) (compact-recurse (subvec row 2)))
                  (apply vector a (compact-recurse (subvec row 1)))))))]
  (compact-recurse input-row)))

(defn left-compact-row [input-row]
  "compact a row to the left"
  (let [row (compact-vec (remove-zeroes input-row))
        zero-len (- size (count row))]
    (into row (vec (repeat zero-len 0)))))

(defn left-compact-rows [board]
  "compact a board to the left"
  (mapv left-compact-row board))

(defn right-compact-rows [board]
  "compact a board to the right"
  (->> (mapv reversev board)
       (left-compact-rows)
       (mapv reversev)))

(defn up-compact-rows [board]
  "compact a board up"
  (->> (rotate-board board)
       (left-compact-rows)
       (rotate-board)))

(defn down-compact-rows [board]
  "compact a board down"
  (->> (rotate-board board)
       (right-compact-rows)
       (rotate-board)))

(defn lose? [board]
  "is a board in a losing position (no valid moves)"
  (= board (left-compact-rows board)
           (right-compact-rows board)
           (up-compact-rows board)
           (down-compact-rows board)))

(defn win? [board]
  "is a board in a winning position (there is at least one value >= 2048)"
  (some true? (map (fn [row] (some #(= win-amount %) row)) board)))

; this exists in clojure but not clojurescript
(defn nthrest
  "for a collection return the rest of the collection starting at a particular index"
  [coll n]
    (loop [n n xs coll]
      (if (and (pos? n) (seq xs))
        (recur (dec n) (rest xs))
        xs)))

(defn get-sorted-board [board]
  "take all the values on the board and sort them into a single collection"
  (reverse (sort (filter not-zero? (flatten board)))))

(defn score-seq [old-board new-board score]
  "calculate how much a score increased from moves between an old board and a new board"
  (if (or (empty? old-board) (empty? new-board)) score
          (let [new-num (first new-board)
                old-num (first old-board)
                old-board (if (= new-num old-num) (rest old-board) (nthrest old-board 2))
                new-board (rest new-board)
                score (if (= new-num old-num) score (+ score new-num))]
            (recur old-board new-board score))))

; FUTURE might make more sense to do this during the board compaction process
(defn score-move [old-board new-board]
  "calculate how much one move increased the score"
  (let [ob (get-sorted-board old-board)
        nb (get-sorted-board new-board)]
      (score-seq ob nb 0)))



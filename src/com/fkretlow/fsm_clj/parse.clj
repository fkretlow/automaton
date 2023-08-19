(ns com.fkretlow.fsm-clj.parse)

(defn pad [n coll val] (take n (concat coll (repeat val))))

(defn normalize-actions
  "Given `actions` conforming to the grammar shown in `make-fsm`, combine them into a single function.
  
  Examples:
  `nil` -> `identity`
  `[]` -> `identity`
  `f` -> `f`
  `[f]` -> `f`
  `[f g]` -> `(comp g f)`
  "
  [actions]
  (cond
    (fn? actions) actions
    (empty? actions) identity
    (and (coll? actions) (every? fn? actions)) (apply comp (reverse actions))
    :else (throw (ex-info "invalid actions" {:actions actions}))))

(defn normalize-transition
  "Given a `transition` conforming to the grammar shown in `make-fsm`, normalize it into a transition vector.
  
  Examples:
  `:a` -> `[:a identity]`
  `[:a]` -> `[:a identity]`
  `[:a nil]` -> `[:a identity]`
  `[:a f]` -> `[:a f]`
  `[:a [f]]` -> `[:a f]`
  `[:a [f g]]` -> `[:a (comp g f)]`"
  [transition]
  (cond
    (keyword? transition) (let [state-key transition] [state-key identity])
    (coll? transition) (let [[state-key actions] transition] [state-key (normalize-actions actions)])
    :else (throw (ex-info "invalid transition" transition))))

(normalize-transition [:a [inc inc]])

(defn normalize-state
  "Given a `state` conforming to the grammar shown in `make-fsm`, transform it into a more useful map.
  
  Examples:
  `[:a, 'b :b]` -> `{:a {'b [:b identity]}}`
  `[:a, 'b :b, :c]` -> `{:a {'b [:b identity], :_fsm/* [:c identity]}}`
  `[:a, 'b [:b f], 'c [:c [f g]]]` -> `{:a {'b [:b f], 'c [:c (comp g f)]}}`"

  [[state-key & transitions]]
  (let [transition-vectors (partition 2 2 nil transitions)]
    {state-key (reduce
                (fn [transitions-map transition-vector]
                  (let [[transition event] (pad 2 (reverse transition-vector) :_fsm/*)]
                    (assoc transitions-map event (normalize-transition transition))))
                {}
                transition-vectors)}))


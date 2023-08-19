(ns com.fkretlow.fsm-clj.parse)

(defn normalize-actions
  "Given `actions` conforming to the grammar shown in `make-fsm`, combine them into a single function.
  
  Examples:
  `nil` -> `identity`
  `f` -> `f`
  `[f]` -> `f`
  `[f g]` -> `(comp g f)`
  "
  [actions]
  (cond
    (fn? actions) actions
    (nil? actions) identity
    (and (coll? actions) (seq actions) (every? fn? actions)) (apply comp (reverse actions))
    :else (throw (ex-info "invalid actions" {:actions actions}))))

(defn normalize-transition
  "Given a `transition` conforming to the grammar shown in `make-fsm`, normalize it into a transition vector.
  
  Examples:
  `:a` -> `[:a identity]`
  `[:a]` -> `[:a identity]`
  `:a f` -> `[:a f]`
  `:a [f]` -> `[:a f]`
  `:a [f g]` -> `[:a (comp g f)]`"
  [transition]
  (cond
    (keyword? transition) (let [state-key transition] [state-key identity])
    (coll? transition) (let [[state-key actions] transition] [state-key (normalize-actions actions)])
    :else (throw (ex-info "invalid transition" transition))))

(defn- is-actions-element? [element]
  (boolean (or (fn? element) (and (coll? element) (seq element) (every? fn? element)))))

(defn- take-first-transition
  "Same as `(split-at n transition-list)` where `n` is the length of the first transition.
  
  Examples:
  `['a :a, 'b :b]` -> `[('a :a), ('b :b)]`
  `['a :a f, 'b :b]` -> `[('a :a f), ('b :b)]`
  `['a :a [f], 'b :b]` -> `[('a :a [f]), ('b :b)]`
  `['a :a [f g], 'b :b]` -> `[('a :a [f g]), ('b :b)]`"
  [transition-list]
  (split-at
   (if (and (> (count transition-list) 2) (is-actions-element? (nth transition-list 2))) 3 2)
   transition-list))

(defn normalize-state
  "Given a `state` conforming to the grammar shown in `make-fsm`, transform it into a more useful map.
  
  Examples:
  `[:a, 'b :b]` -> `{:a {'b [:b identity]}}`
  `[:a, 'b :b, :c]` -> `{:a {'b [:b identity], :_fsm/* [:c identity]}}`
  `[:a, 'b :b f, 'c :c [f g]]` -> `{:a {'b [:b f], 'c [:c (comp g f)]}}`"
  [[state-key & transition-list]]
  (loop [transition-map {}
         transition-list transition-list]
    (if (empty? transition-list)
      {state-key transition-map}
      (let [[current remaining] (take-first-transition transition-list)
            [event transition]
            (if (and (> (count current) 1) (not (is-actions-element? (second current))))
              [(first current) (rest current)]
              [:_fsm/* current])]
        (recur (assoc transition-map event (normalize-transition transition))
               remaining)))))

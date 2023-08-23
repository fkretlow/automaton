(ns com.fkretlow.fsm-clj.coerce)

(defn compile-update-data [update-data]
  (cond
    (fn? update-data) update-data
    (nil? update-data) identity
    (and (coll? update-data) (seq update-data) (every? fn? update-data)) (apply comp (reverse update-data))
    :else (throw (ex-info "invalid update-data element" {:update-data update-data}))))

(defn coerce-transition [transition]
  (when transition
    (cond
      (keyword? transition) (as-> transition state-key [state-key identity])
      (coll? transition) (let [[state-key update-data] transition] [state-key (compile-update-data update-data)])
      :else (throw (ex-info "invalid transition" transition)))))

(defn is-update-data-element? [element]
  (boolean (or (fn? element) (and (coll? element) (seq element) (every? fn? element)))))

(defn split-off-first-transition [transition-list]
  (split-at
   (if (and (> (count transition-list) 2) (is-update-data-element? (nth transition-list 2))) 3 2)
   transition-list))

(defn transition-list->map [transition-list]
  (loop [transition-map {}
         transition-list transition-list]
    (if (empty? transition-list)
      transition-map
      (let [[current remaining] (split-off-first-transition transition-list)
            [event transition]
            (if (and (> (count current) 1) (not (is-update-data-element? (second current))))
              [(first current) (rest current)]
              [:_fsm/* current])]
        (recur (assoc transition-map event (coerce-transition transition))
               remaining)))))

(defn compile-compute-transition [transition-list]
  (if (empty? transition-list)
    (constantly nil)
    (let [transition-map (transition-list->map transition-list)]
      (fn [event _] (or (get transition-map event)
                        (get transition-map :_fsm/*))))))

(defn normalize-state-map [{:keys [transitions compute-transition on-enter on-leave], :as state-map}]
  (when (and transitions compute-transition)
    (throw (ex-info "state map with both compute-transition and transitions" {:state-map state-map})))
  (cond-> nil
    transitions (assoc :compute-transition (compile-compute-transition transitions))
    compute-transition (assoc :compute-transition (comp coerce-transition compute-transition))
    on-enter (assoc :on-enter on-enter)
    on-leave (assoc :on-leave on-leave)))

(defn coerce-state [state]
  (cond
    (map? state) (normalize-state-map state)
    (vector? state) (as-> state transitions {:compute-transition (compile-compute-transition transitions)})
    (fn? state) (as-> state compute-transition {:compute-transition (comp coerce-transition compute-transition)})
    (nil? state) {:compute-transition (constantly nil)}
    :else (throw (ex-info "invalid state" {:state state}))))

(defn coerce-states [states]
  (zipmap (keys states) (map coerce-state (vals states))))

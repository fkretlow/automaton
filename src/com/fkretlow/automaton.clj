(ns com.fkretlow.automaton
  (:require
   [com.fkretlow.automaton.coerce :refer [coerce-states]]))

(defn make-fsm
  [states & {:keys [data initial-state]}]
  {:state (or initial-state (first (keys states)))
   :data data,
   :_fsm/states (coerce-states states)})

(defn process-event [fsm event]
  (let [{data :data, state-key :state, states :_fsm/states} fsm
        {:keys [compute-transition on-leave]} (get states state-key)
        [state-key' update-data] (compute-transition event data)
        is-state-change? (and state-key' (not= state-key state-key'))
        on-enter (get-in states [state-key' :on-enter])]
    (cond-> fsm
      update-data (update :data update-data)
      (and is-state-change? on-leave) (update :data on-leave)
      is-state-change? (assoc :state state-key')
      (and is-state-change? on-enter) (update :data on-enter))))

(defn- to-state-data-pair [{:keys [state data]}] [state data])

(defn reduce-fsm
  "Given an fsm and a seq of events, process the events one by one and return
  the last state of the machine as a pair of the form `[state-key data]`."
  [fsm events]
  (to-state-data-pair (reduce process-event fsm events)))

(defn iterate-fsm
  "Given an fsm and a seq of events, process the events one by one and
  return a lazy seq of all intermediary states as pairs of the form
  `[state-key data]`. Useful for stopping event processing depending on
  some reached state."
  [fsm events]
  (lazy-seq
   (cons (to-state-data-pair fsm)
         (when (seq events)
           (iterate-fsm (process-event fsm (first events))
                        (rest events))))))


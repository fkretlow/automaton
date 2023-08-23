(ns com.fkretlow.fsm-clj
  (:require
   [com.fkretlow.fsm-clj.coerce :refer [coerce-states]]))

(defn make-machine
  [states & {:keys [data initial-state]}]
  {:state (or initial-state (first (keys states)))
   :data data,
   :_fsm/states (coerce-states states)})

(defn process-event [machine event]
  (let [{data :data, state-key :state, states :_fsm/states} machine
        {:keys [compute-transition on-leave]} (get states state-key)
        [state-key' update-data] (compute-transition event data)
        is-state-change? (and state-key' (not= state-key state-key'))
        on-enter (get-in states [state-key' :on-enter])]
    (cond-> machine
      update-data (update :data update-data)
      (and is-state-change? on-leave) (update :data on-leave)
      is-state-change? (assoc :state state-key')
      (and is-state-change? on-enter) (update :data on-enter))))

(ns com.fkretlow.fsm
  (:require
   [com.fkretlow.fsm.parse :refer [normalize-state]]))

(defn make-fsm
  "Construct a new fsm from the given `states-vector` and with the given 
  initial `value` (default `nil`). The initial state will be the first one.
  
  The `states-vector` must satisfy the following grammar:
  
  states-vector:        [state+]
  state:                [state-key transition-on-event* default-transition?]
  state-key:            keyword
  transition-on-event:  event transition
  default-transition:   transition
  event:                anything
  transition:           state-key | [state-key actions]
  actions:              function | seqable of functions"
  [states-vector & [value]]
  (let [init-state (first (first states-vector))]
    {:current-state init-state,
     :value value,
     :states (apply merge (map normalize-state states-vector))}))

(defn process-event
  "Process the given `event` with the given `fsm` and return the `fsm`."
  [{:keys [current-state states], :as fsm} event]
  (if-let [[next-state action]
           (or (get-in states [current-state event])
               (get-in states [current-state :_fsm/*]))]
    (-> fsm
        (update :value action)
        (assoc :current-state next-state))
    fsm))

(defn reduce-fsm
  "Process the given `events` one by one with the given `fsm` and return the final value."
  [fsm events]
  (:value (reduce process-event fsm events)))

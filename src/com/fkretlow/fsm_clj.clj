(ns com.fkretlow.fsm-clj
  (:require
   [com.fkretlow.fsm-clj.parse :refer [normalize-state normalize-transition]]))

(defn make-fsm
  "Construct a new FSM from the given `states-vector` and with the given 
  initial `value` (default `nil`). The initial state will be the first given state.
  
  The `states-vector` must satisfy the following grammar
    
  states-vector:        [state+]
  state:                state-key | [state-key transition-list | state-function]
  transition-list       transition-on-event* default-transition?
  state-key:            _keyword_
  transition-on-event:  event transition
  default-transition:   transition
  state-function:       _a function taking an event and the current value and returning a state-key 
                        or a vector containing the elements of a transition as defined below_
  event:                _anything except for a function or a collection of functions_
  transition:           state-key actions?
  actions:              _function_ | _non-empty collection of functions_
  
  where `*` means \"zero or more\", `?` means \"at most one\", `+` means \"at least one\",
  and `|` means \"or\"."
  ([states-vector] (make-fsm states-vector nil))
  ([states-vector value]
   (let [init-state-key (first (first states-vector))]
     {:state-key init-state-key,
      :states (apply merge (map normalize-state states-vector)),
      :value value})))

(defn process-event
  "Process the given `event` with the given `fsm` and return the `fsm`."
  [{:keys [state-key states value], :as fsm} event]
  (let [state (get states state-key)]
    (if-let [[next-state-key action]
             (cond
               (map? state) (or (get-in states [state-key event])
                                (get-in states [state-key :_fsm/*]))
               (fn? state) (normalize-transition (state event value))
               :else (throw (ex-info "invalid state" {:state state})))]
      (-> fsm
          (update :value action)
          (assoc :state-key next-state-key))
      fsm)))

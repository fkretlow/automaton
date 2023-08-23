# Automaton

A library to model state machines.

## Usage

```clojure
(use '[com.fkretlow.fsm-clj :refer [make-fsm process-event]])

(let [states [[:0,                    ; 1st state: :0
               \a :a],                ;   - transitions to :a on event \a
              [:a,                    ; 2nd state: :a
               \a :a,                 ;   - circles back to :a on event \a
               \b :0 inc,             ;   - transitions to :0 on event \b and applies inc to the data
               :0]],                  ;   - transitions to :0 on all other events
      count-ab (make-fsm states 0)]   ; make the state machine with 0 as initial data
  ; process single events
  (prn (-> count-ab                     
           (process-event \a)
           (process-event \b)
           :data))
  ; or use reduce to process all at once
  (prn (:data (reduce process-event count-ab "ababab")))
;;=> 1
;;=> 3
```

The vector of states given to `make-fsm` must satisfy the following grammar
```
states-vector:        [state+]
state:                state-key | [state-key transition-list | state-function]
transition-list       transition-on-event* default-transition?
state-key:            keyword
transition-on-event:  event transition
default-transition:   transition
state-function:       a function taking an event and the current data and returning a state-key 
                      or a vector containing the elements of a transition as defined below
event:                anything except for a function or a collection of functions
transition:           state-key actions?
actions:              function | non-empty collection of functions
```
where `*` means \"zero or more\", `?` means \"at most one\", `+` means \"at least one\", and `|` means \"or\"."

If more than one function is given for `actions`, the functions will be composed in the given order and applied to the current data.

The FSM will be initialized in the first given state.

The events you process with the FSM can by anything except for functions or collections of functions.

## See also

... and better use instead:

- https://github.com/cdorrat/reduce-fsm: mature, fast FSM library
- https://github.com/metosin/tilakone: smaller than reduce-fsm, no macros

## License

Copyright © 2023 Florian Kretlow

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

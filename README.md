# fsm-clj

A tiny library to model finite state machines.

## Usage

```clojure
(let [states [[:start,                  ; 1st state: :start
               \a :found-a]             ;   - transitions to :found-a on event \a
              [:found-a,                ; 2nd state: :found-a
               \a :found-a,             ;   - transitions to :found-a on event \a
               \b [:start inc],         ;   - transitions to :start on event \b and applies inc to the value
               :start]]                 ;   - transitions to :start on all other events
      count-ab (make-fsm states 0)]     ; make the state machine with an initial value of 0
  (prn (reduce-fsm count-ab "ababab"))  ; process the characters one by one and return the final value
  (prn (-> count-ab                     ; can also process single events
           (process-event \a)
           (process-event \b)
           :value)))
;;=> 3
;;=> 1
```

The vector of states given to `make-fsm` must satisfy the following grammar:
```
states-vector:        [state+]
state:                [state-key transition-on-event* default-transition?]
state-key:            keyword
transition-on-event:  event transition
default-transition:   transition
event:                anything
transition:           state-key | [state-key actions]
actions:              function | seqable of functions
```

If more than one function is given for `actions`, the functions will be composed in the given order.
The FSM will be initialized in the first given state.

## See also

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

(ns com.fkretlow.fsm-test
  (:require
   [clojure.test :refer [deftest is]]
   [com.fkretlow.fsm :refer [make-fsm process-event reduce-fsm]]))

(def ^:private count-ab-states [[:init, \a :a, :init]
                                [:a, \a :a, \b [:init inc], :init]])

(deftest test-make-fsm
  (is (= {:value 0,
          :current-state :init,
          :states {:init {\a [:a identity],
                          :_fsm/* [:init identity]},
                   :a {\a [:a identity],
                       \b [:init inc],
                       :_fsm/* [:init identity]}}}
         (make-fsm count-ab-states 0))))

(deftest test-process-event
  (let [fsm (make-fsm count-ab-states 0)
        fsm' (process-event fsm \a)
        fsm'' (process-event fsm' \b)]
    (is (= :a (:current-state fsm')))
    (is (= 0 (:value fsm')))
    (is (= :init (:current-state fsm'')))
    (is (= 1 (:value fsm'')))))

(deftest test-reduce-fsm
  (let [fsm (make-fsm count-ab-states 0)]
    (is (= 3 (reduce-fsm fsm "abcabdab")))))

(ns com.fkretlow.fsm-clj-test
  (:require
   [clojure.test :refer [deftest is run-tests testing]]
   [com.fkretlow.fsm-clj :refer [make-fsm process-event]]))

(def ^:private count-ab-states [[:0, \a :a, :0]
                                [:a, \a :a, \b :0 inc, :0]])

(deftest test-make-fsm
  (is (= {:value 0,
          :state-key :0,
          :states {:0 {\a [:a identity],
                       :_fsm/* [:0 identity]},
                   :a {\a [:a identity],
                       \b [:0 inc],
                       :_fsm/* [:0 identity]}}}
         (make-fsm count-ab-states 0))))

(deftest test-process-event
  (let [fsm (make-fsm count-ab-states 0)
        fsm' (process-event fsm \a)
        fsm'' (process-event fsm' \b)]
    (is (= :a (:state-key fsm')))
    (is (= 0 (:value fsm')))
    (is (= :0 (:state-key fsm'')))
    (is (= 1 (:value fsm'')))))

(deftest test-for-regex
  (testing "returns true if the pattern /(abc)+d/ exists in the input string"
    (let [reduce-value (fn [fsm events] (:value (reduce process-event fsm events)))
          states
          [[:0, \a :a],
           [:a, \a :a, \b :b, :0],
           [:b, \c :c, :0],
           [:c, \a :a, \d :d (constantly true), :0]
           [:d]]
          fsm (make-fsm states false)]
      (is (reduce-value fsm "abcd"))
      (is (not (reduce-value fsm "abcxd")))
      (is (reduce-value fsm "abcabcd"))
      (is (reduce-value fsm "xxxabcabcdxxx"))
      (is (not (reduce-value fsm "abd"))))))

(run-tests)

(ns com.fkretlow.fsm-clj-test
  (:require
   [clojure.test :refer [deftest is run-tests testing]]
   [com.fkretlow.fsm-clj :refer [make-machine process-event]]))

(defn- reduce-fsm [fsm events] (:data (reduce process-event fsm events)))

(def ^:private count-ab-states {:0 [\a :a],
                                :a [\a :a, \b :0 inc, :0]})

(deftest test-process-event
  (let [fsm (make-machine count-ab-states :data 0)
        fsm' (process-event fsm \a)
        fsm'' (process-event fsm' \b)]
    (is (= :a (:state fsm')))
    (is (= 0 (:data fsm')))
    (is (= :0 (:state fsm'')))
    (is (= 1 (:data fsm'')))))

(deftest test-states-as-functions
  (let [fsm (make-machine {:0 (fn [c & _] (when (= \a c) :a))
                           :a (fn [c & _] (if (= \b c) [:0 inc] :0))}
                          :data 0)]
    (is (= 3 (reduce-fsm fsm "ababab")))
    (is (= 1 (reduce-fsm fsm "xxxaxabxxx"))))
  (let [fsm (make-machine {:0 [\a :a]
                           :a (fn [c & _] (case c \a :a, \b [:0 [inc inc]], :0))},
                          :data 0)]
    (is (= 6 (reduce-fsm fsm "ababab")))))

(deftest test-pattern-example
  (testing "returns true if the pattern /(abc)+d/ exists in the input string"
    (let [states
          {:0 [\a :a],
           :a [\a :a, \b :b, :0],
           :b [\a :a, \c :c, :0],
           :c [\a :a, \d :d (constantly true), :0],
           :d nil}
          fsm (make-machine states false)]
      (is (reduce-fsm fsm "abcd"))
      (is (reduce-fsm fsm "ababcd"))
      (is (not (reduce-fsm fsm "abcxd")))
      (is (reduce-fsm fsm "abcabcd"))
      (is (reduce-fsm fsm "xxxabcabcdxxx"))
      (is (not (reduce-fsm fsm "abd"))))))

(deftest test-credentials-example
  (let [fsm (make-machine
             {:claim-pending
              (fn [claim _]
                [:challenge-pending
                 (fn [process] (assoc process :claim claim, :challenge #(= "pw" %)))]),
              :challenge-pending
              (fn [answer process] (if ((:challenge process) answer) :success :failure)),
              :success {:on-enter #(select-keys % [:claim])} ,
              :failure {:on-enter (constantly nil)}})]
    (let [{:keys [state data]} (reduce process-event fsm ["username" "pw"])]
      (is (= :success state))
      (is (= {:claim "username"} data)))
    (is (not (:success (reduce-fsm fsm ["username" "bad password"]))))))

(deftest test-binary-number-has-even-number-of-0s
  (let [fsm (make-machine
             {:even [\0 :uneven not],
              :uneven [\0 :even not]},
             :data true)]
    (is (reduce-fsm fsm "0101010"))
    (is (not (reduce-fsm fsm "010101")))))

(run-tests)

(ns com.fkretlow.fsm-clj-test
  (:require
   [clojure.test :refer [deftest is run-tests testing]]
   [com.fkretlow.fsm-clj :refer [make-fsm process-event]]))

(defn- reduce-fsm [fsm events] (:value (reduce process-event fsm events)))

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

(deftest test-states-as-functions
  (let [fsm (make-fsm [[:0 (fn [c & _] (if (= \a c) :a :0))]
                       [:a (fn [c & _] (if (= \b c) [:0 inc] :0))]] 0)]
    (is (= 3 (reduce-fsm fsm "ababab")))
    (is (= 1 (reduce-fsm fsm "xxxaxabxxx"))))
  (let [fsm (make-fsm [[:0, \a :a]
                       [:a (fn [c & _] (case c \a :a, \b [:0 [inc inc]], :0))]] 0)]
    (is (= 6 (reduce-fsm fsm "ababab")))))

(deftest test-pattern-example
  (testing "returns true if the pattern /(abc)+d/ exists in the input string"
    (let [states
          [[:0, \a :a],
           [:a, \a :a, \b :b, :0],
           [:b, \a :a, \c :c, :0],
           [:c, \a :a, \d :d (constantly true), :0]
           [:d]]
          fsm (make-fsm states false)]
      (is (reduce-fsm fsm "abcd"))
      (is (reduce-fsm fsm "ababcd"))
      (is (not (reduce-fsm fsm "abcxd")))
      (is (reduce-fsm fsm "abcabcd"))
      (is (reduce-fsm fsm "xxxabcabcdxxx"))
      (is (not (reduce-fsm fsm "abd"))))))

(deftest test-credentials-example
  (let [fsm (make-fsm
             [[:claim-pending
               (fn [claim & _]
                 [:challenge-pending
                  (fn [process] (assoc process :claim claim, :challenge #(= "pw" %)))])],
              [:challenge-pending
               (fn [answer process]
                 (if ((:challenge process) answer)
                   [:success #(-> % (assoc :success true) (dissoc :challenge))]
                   [:failure #(-> % (assoc :success false) (dissoc :challenge))]))],
              :success,
              :failure])]
    (is (:success (reduce-fsm fsm ["username" "pw"])))
    (is (not (:success (reduce-fsm fsm ["username" "bad password"]))))))

(deftest test-binary-number-has-even-number-of-0s
  (let [fsm (make-fsm
             [[:even, \0 :uneven not]
              [:uneven, \0 :even not]]
             true)]
    (is (reduce-fsm fsm "0101010"))
    (is (not (reduce-fsm fsm "010101")))))

(run-tests)

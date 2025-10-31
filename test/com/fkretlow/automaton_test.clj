(ns com.fkretlow.automaton-test
  (:require
   [clojure.test :refer [deftest is run-tests testing]]
   [com.fkretlow.automaton :refer [iterate-fsm make-fsm process-event
                                   reduce-fsm]]))

(def ^:private count-ab-states {:0 [\a :a],
                                :a [\a :a, \b :0 inc, :0]})

(deftest test-iterate-fsm
  (let [fsm (make-fsm count-ab-states :data 0)
        states (iterate-fsm fsm "aba")]
    (is (= [[:0 0] [:a 0] [:0 1] [:a 1]]
           states))))

(deftest test-process-event
  (let [fsm (make-fsm count-ab-states :data 0)
        fsm' (process-event fsm \a)
        fsm'' (process-event fsm' \b)]
    (is (= :a (:state fsm')))
    (is (= 0 (:data fsm')))
    (is (= :0 (:state fsm'')))
    (is (= 1 (:data fsm'')))))

(deftest test-states-as-functions
  (let [fsm (make-fsm {:0 (fn [c & _] (when (= \a c) :a))
                       :a (fn [c & _] (if (= \b c) [:0 inc] :0))}
                      :data 0)]
    (is (= 3 (second (reduce-fsm fsm "ababab"))))
    (is (= 1 (second (reduce-fsm fsm "xxxaxabxxx")))))
  (let [fsm (make-fsm {:0 [\a :a]
                       :a (fn [c & _] (case c \a :a, \b [:0 [inc inc]], :0))},
                      :data 0)]
    (is (= 6 (second (reduce-fsm fsm "ababab"))))))

(deftest test-pattern-example
  (testing "returns true if the pattern /(abc)+d/ exists in the input string"
    (let [states
          {:0 [\a :a],
           :a [\a :a, \b :b, :0],
           :b [\a :a, \c :c, :0],
           :c [\a :a, \d :d (constantly true), :0],
           :d nil}
          fsm (make-fsm states false)]
      (is (second (reduce-fsm fsm "abcd")))
      (is (second (reduce-fsm fsm "ababcd")))
      (is (not (second (reduce-fsm fsm "abcxd"))))
      (is (second (reduce-fsm fsm "abcabcd")))
      (is (second (reduce-fsm fsm "xxxabcabcdxxx")))
      (is (not (second (reduce-fsm fsm "abd")))))))

(deftest test-credentials-example
  (let [fsm (make-fsm
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
    (is (not (:success (second (reduce-fsm fsm ["username" "bad password"])))))))

(deftest test-binary-number-has-even-number-of-0s
  (let [fsm (make-fsm
             {:even [\0 :uneven not],
              :uneven [\0 :even not]},
             :data true)]
    (is (second (reduce-fsm fsm "0101010")))
    (is (not (second (reduce-fsm fsm "010101"))))))

(defn- isalnum? [c] (or (<= (int \a) (int c) (int \z))
                        (<= (int \A) (int c) (int \Z))
                        (<= (int \0) (int c) (int \9))))

(deftest test-vim-word-object
  (testing "can determine the end of the current/next word object"
    (let [inc-index (fn [data] (update data :index inc))
          inc-word-count (fn [data] (update data :word-count inc))
          states {:start (fn [c _] (if (isalnum? c)
                                     [:word [inc-index inc-word-count]]
                                     [:not-word inc-index]))
                  :word (fn [c _] (if (isalnum? c) [:word inc-index] [:not-word inc-index]))
                  :not-word (fn [c _] (if (isalnum? c)
                                        [:word [inc-index inc-word-count]]
                                        [:not-word inc-index]))}
          fsm (make-fsm states :data {:index -1, :word-count 0})
          input " ab "]
      (->> (iterate-fsm fsm input)
           (drop-while (fn [[state data]] (or (= state :word) (zero? (:word-count data)))))
           (prn)))))

(run-tests)

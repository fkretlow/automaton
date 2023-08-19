(ns com.fkretlow.fsm-clj.parse-test
  (:require
   [clojure.test :refer [deftest is run-tests]]
   [com.fkretlow.fsm-clj.parse :refer [normalize-actions normalize-state
                                       normalize-transition]]))

(deftest test-normalize-actions
  (is (= identity (normalize-actions nil)))
  (is (= identity (normalize-actions [])))
  (is (= inc (normalize-actions inc)))
  (is (= inc (normalize-actions [inc])))
  (let [f (normalize-actions [inc (constantly 42)])] (is (= 42 (f 0))))
  (is (thrown? clojure.lang.ExceptionInfo (normalize-actions "bad"))))

(deftest test-normalize-transition
  (is (= [:a identity] (normalize-transition :a)))
  (is (= [:a identity] (normalize-transition [:a])))
  (is (= [:a identity] (normalize-transition [:a nil])))
  (is (= [:a identity] (normalize-transition [:a []])))
  (is (= [:a inc] (normalize-transition [:a inc])))
  (is (= [:a inc] (normalize-transition [:a [inc]])))
  (let [[_ f] (normalize-transition [:a [inc (constantly 42)]])] (is (= 42 (f 0))))
  (is (thrown? clojure.lang.ExceptionInfo (normalize-transition [:a "do this"]))))

(deftest test-normalize-state
  (is (= {:a {'a [:a identity]}} (normalize-state [:a, 'a :a])))
  (is (= {:a {'a [:a identity], :_fsm/* [:b inc]}} (normalize-state [:a, 'a :a, :b inc]))))

(run-tests)

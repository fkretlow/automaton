(ns com.fkretlow.fsm-clj.coerce-test
  (:require
   [clojure.test :refer [deftest is run-tests]]
   [com.fkretlow.fsm-clj.coerce :refer [compile-compute-transition is-update-data-element?
                                       compile-update-data coerce-transition
                                       transition-list->map
                                       split-off-first-transition]]))

(deftest test-compile-update-data
  (is (= identity (compile-update-data nil)))
  (is (= inc (compile-update-data inc)))
  (is (= inc (compile-update-data [inc])))
  (let [f (compile-update-data [(constantly 42) dec])] (is (= 41 (f 0))))
  (is (thrown? clojure.lang.ExceptionInfo (compile-update-data "bad")))
  (is (thrown? clojure.lang.ExceptionInfo (compile-update-data []))))

(deftest test-coerce-transition
  (is (nil? (coerce-transition nil)))
  (is (= [:a identity] (coerce-transition :a)))
  (is (= [:a identity] (coerce-transition [:a])))
  (is (= [:a inc] (coerce-transition [:a inc])))
  (is (= [:a inc] (coerce-transition [:a [inc]])))
  (let [[k f] (coerce-transition [:a [(constantly 42) dec]])]
    (is (= 41 (f 0)))
    (is (= :a k)))
  (is (thrown? clojure.lang.ExceptionInfo (coerce-transition [:a "do this"]))))

(deftest test-is-update-data-element?
  (is (is-update-data-element? inc))
  (is (is-update-data-element? [inc]))
  (is (not (is-update-data-element? nil)))
  (is (not (is-update-data-element? []))))

(deftest test-split-off-first-transition
  (is (= [[:a] []] (split-off-first-transition [:a])))
  (is (= [['a :a] ['b :b]] (split-off-first-transition ['a :a, 'b :b])))
  (is (= [['a :a inc] ['b :b]] (split-off-first-transition ['a :a inc, 'b :b])))
  (is (= [['a :a [inc]] ['b :b]] (split-off-first-transition ['a :a [inc], 'b :b])))
  (is (= [['a :a [inc dec]] ['b :b]] (split-off-first-transition ['a :a [inc dec], 'b :b]))))

(deftest test-transition-list->map
  (is (= {'b [:b identity]} (transition-list->map ['b :b])))
  (is (= {'b [:b identity], 'c [:c identity]} (transition-list->map ['b :b, 'c :c])))
  (is (= {'b [:b inc], 'c [:c identity]} (transition-list->map ['b :b inc, 'c :c])))
  (is (= {'b [:b inc], :_fsm/* [:0 identity]} (transition-list->map ['b :b inc, :0]))))

(deftest test-compile-compute-transition
  (let [f (compile-compute-transition ['a :a inc, 'b :b, :0])
        g (compile-compute-transition ['a :a inc, 'b :b])]
    (is (= [:a inc] (f 'a nil)))
    (is (= [:b identity] (f 'b nil)))
    (is (= [:0 identity] (f 'x nil)))
    (is (nil? (g 'x nil)))))

(run-tests)

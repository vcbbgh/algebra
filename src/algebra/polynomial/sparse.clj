(ns algebra.polynomial.sparse
  (:require [clojure.core.match :refer [match]]
            [algebra :as a]
            [algebra.polynomial :as ap])
  (:import [algebra.polynomial Polynomial]
           (algebra Ring)))


(defn shnf-pop?
  [x]
  (and (vector? x) (= (first x) ::pop)))

(defn shnf-pow?
  [x]
  (and (vector? x) (= (first x) ::pow)))

(defn shnf-pop [i p]
  (cond (= i 0) p
        (shnf-pop? p) [::pop (+ i (second p)) (nth p 2)]
        (shnf-pow? p) [::pop i p]
        :else p))

(defn shnf-pow [i p q]
  (cond (= p 0) (shnf-pop 1 q)
        (and (shnf-pow? p) (= (nth p 3) 0)) [::pow (+ i (second p)) (nth p 2) q]
        :else [::pow i p q]))

(defn ->shnf
  [^Polynomial p]
  (let [r (.ring p)]
    (letfn [(shnf+ [x y]
              (cond (shnf-pop? x) (cond (shnf-pop? y) (let [[_ i p] x
                                                            [_ j q] y]
                                                        (cond (= i j) (shnf-pop i (shnf+ p q))
                                                              (> i j) (shnf-pop j (shnf+ [::pop (- i j) p] q))
                                                              :else (shnf-pop i (shnf+ [::pop (- j i) q] p))))
                                        (shnf-pow? y) (let [[_ i p] x
                                                            [_ j q r] y]
                                                        (cond (= i 1) [::pow j q (shnf+ r p)]
                                                              (> i 1) [::pow j q (shnf+ r [::pop (dec i) p])]))
                                        :else (recur y x))
                    (shnf-pow? x) (cond (shnf-pop? y) (recur y x)
                                        (shnf-pow? y) (let [[_ i p q] x
                                                            [_ j r s] y]
                                                        (cond (= i j) (shnf-pow i (shnf+ p r) (shnf+ q s))
                                                              (> i j) (shnf-pow j (shnf+ [::pow (- i j) p 0] r) (shnf+ q s))
                                                              :else (shnf-pow i (shnf+ [::pow (- j i) r 0] p) (shnf+ s q))))
                                        :else (recur y x)   ;; redundant with pop term above
                                        )
                    ;; x is primitive
                    :else (cond (shnf-pop? y) (let [[_ i p] y]
                                                [::pop i (shnf+ x p)])
                                (shnf-pow? y) (let [[_ i p q] y]
                                                [::pow i p (shnf+ x q)])
                                :else (a/add r x y))))
            (shnf* [x y]
              (cond (shnf-pop? x) (cond (shnf-pop? y) (let [[_ i p] x
                                                            [_ j q] y]
                                                        (cond (= i j) (shnf-pop i (shnf* p q))
                                                              (> i j) (shnf-pop j (shnf* [::pop (- i j) p] q))
                                                              :else (shnf-pop i (shnf* [::pop (- j i) q] p))))
                                        (shnf-pow? y) (let [[_ i p] x
                                                            [_ j q r] y]
                                                        (cond (= i 1) [::pow j (shnf* x q) (shnf* p r)]
                                                              (> i 1) [::pow j (shnf* x q) (shnf* [::pop (dec i) p] r)]))
                                        :else (recur y x))
                    (shnf-pow? x) (cond (shnf-pop? y) (recur y x)
                                        (shnf-pow? y) (let [[_ i p q] x
                                                            [_ j r s] y]
                                                        (shnf+
                                                          (shnf+
                                                            (shnf-pow (+ i j) (shnf* p r) (shnf* q s))
                                                            (shnf-pow i (shnf* p (shnf-pop 1 s)) 0))
                                                          (shnf-pow i (shnf* r (shnf-pop 1 q)) 0)))
                                        :else (recur y x))
                    ;; x is primitive
                    :else (cond (shnf-pop? y) (let [[_ i p] y]
                                                (shnf-pop i (shnf* x p)))
                                (shnf-pow? y) (let [[_ i p q] y]
                                                (shnf-pow i (shnf* x p) (shnf* x q)))
                                :else (a/mul r x y))))
            (shnf-expt [x n]
              (reduce shnf* (repeat n x)))
            (term->shnf [t]
              (reduce shnf* (cons (ap/coefficient t) (let [xs (ap/exponents t)]
                                                       (for [i (range (count xs))
                                                             :let [e (nth xs i)]
                                                             :when (> e 0)]
                                                         (shnf-expt (shnf-pop i [::pow 1 1 0]) e))))))
            ]
      (reduce shnf+ 0 (map term->shnf (.terms p))))))

(defn shnf-eval
  [^Ring r h xs]
  (match [h]
         [[::pop i p]] (recur r p (drop i xs))
         [[::pow i p q]] (if xs
                           (a/add r
                                  (a/mul r
                                         (reduce #(a/mul r %1 %2) (a/multiplicative-identity r) (repeat i (first xs)))
                                         (shnf-eval r p xs))
                                  (shnf-eval r q (next xs)))
                           (a/additive-identity r))
         [_] h))

(ns algebra)

(defprotocol Ordered
  (cmp [this x y]))

(defprotocol Ring
  (member? [this x])
  (additive-identity [this])
  (additive-identity? [this x])
  (multiplicative-identity [this])
  (multiplicative-identity? [this x])
  (add [this x y])
  (subtract [this x y])
  (negate [this x])
  (mul [this x y])
  )

(defprotocol Euclidean
  (quotient [this x y])
  (remainder [this x y]))

(defprotocol Field
  (divide [this x y]))

(def NativeArithmetic
  "The 'ring' of Clojure's native arithmetic (overflow-safe) operators."
  (reify
    Ring
    (member? [this x] (number? x))
    (additive-identity [this] 0)
    (additive-identity? [this x] (zero? x))
    (multiplicative-identity [this] 1)
    (multiplicative-identity? [this x] (= x 1))
    (add [this x y] (+' x y))
    (subtract [this x y] (-' x y))
    (negate [this x] (-' x))
    (mul [this x y] (*' x y))
    Euclidean
    (quotient [this x y] (quot x y))
    (remainder [this x y] (rem x y))
    Ordered
    (cmp [this x y] (compare x y))
    Field
    (divide [this x y] (/ x y))
    Object
    (toString [this] "NativeArithmetic")))

(defn exponentiation-by-squaring
  [R x e]
  (if (= e 0) (multiplicative-identity R)
              (loop [x x
                     y (multiplicative-identity R)
                     n e]
                (cond (<= n 1) (mul R x y)
                      (even? n) (recur (mul R x x) y (bit-shift-right n 1))
                      :else (recur (mul R x x) (mul R x y) (bit-shift-right (dec n) 1))))))

(defn evenly-divides?
  [R u v]
  (additive-identity? R (remainder R v u)))

(defn euclid-gcd
  [R u v]
  (let [step (fn [u v]
               (if (additive-identity? R v) u
                   (recur v (remainder R u v))))]
    (step u v)))

(defn euclid-gcd-seq
  [R as]
  (let [gcd-1 (fn [a b]
                (let [g (euclid-gcd R a b)]
                  (if (multiplicative-identity? R g) (reduced g) g)))]
    (reduce gcd-1 as)))

(ns web-editor.frp
  (:require [clojure.core.async :as async]
            [reagent.core :as r])
  (:refer-clojure :rename {apply core-apply
                           reduce core-reduce}))

(defrecord Publisher [chan pub -topic])

(defn publisher
  "Returns a `Publisher` that has a channel and an associated pub object"
  []
  (let [ch (async/chan)
        topic (gensym)
        pub (async/pub ch (fn [_] topic))]
    (Publisher. ch pub topic)))

(defn publish
  "Publish a value to the `Publisher`'s channel. When passed just
  a `Publisher`, returns a single-argument function that publishes
  any value passed into it."
  ([pub] #(publish pub %))
  ([pub x] (async/put! (:chan pub) x)))

(defn subscribe
  "Returns a channel that subscribes to the `Publisher`'s channel"
  [pub]
  (let [res (async/chan)]
    (async/sub (:pub pub) (:-topic pub) res)
    res))

(defn hold
  "Returns an atom that holds the last value returned by the channel"
  [init ch]
  (let [a (r/atom init)]
    (async/go-loop []
      (reset! a (async/<! ch))
      (recur))
    a))

(defn apply
  "Returns an atom that at any moment holds the result of application
  of `f` to the values of provided atoms `as`"
  [f & as]
  (let [app #(core-apply f (map deref as))
        res (r/atom (app))
        k (gensym)]
    (doseq [a as]
      (add-watch a k #(reset! res (app))))
    res))

(defn reduce
  "Returns an atom that accumulates values produced by the channel `ch`"
  [f init ch]
  (let [a (r/atom nil)]
    (async/go-loop [acc init]
      (reset! a acc)
      (recur (f acc (async/<! ch))))
    a))

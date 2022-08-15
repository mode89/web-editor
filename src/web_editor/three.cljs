(ns web-editor.three
  (:require [threeagent.core :as tha]
            ["three" :as three]))

(defn render [root canvas {:keys [clear-color] :or {clear-color [0 0 0]}}]
  (let [ctx (tha/render root canvas)]
    (.setClearColor (:threejs-renderer ctx)
                    (apply #(three/Color. %1 %2 %3) clear-color))))

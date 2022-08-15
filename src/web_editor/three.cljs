(ns web-editor.three
  (:require [clojure.math :as math]
            [threeagent.core :as tha]
            [threeagent.entity :refer [IEntityType]]
            ["three" :as three]))

(defn vec3 [& args]
  (let [arg0 (nth args 0)]
    (if (coll? arg0)
      (apply three/Vector3. arg0)
      (three/Vector3 arg0 (nth args 1) (nth args 2)))))

(defn grid-helper []
  [:instance {:object (three/GridHelper.)}])

(defn axes-helper []
  [:object
    [:instance {:object (three/ArrowHelper.
                          (three/Vector3.) (three/Vector3.) 2 0xFF0000)
                :rotation [0 0 (/ math/PI -2)]}]
    [:instance {:object (three/ArrowHelper.
                          (three/Vector3.) (three/Vector3.) 2 0x00FF00)
                :rotation [0 0 0]}]
    [:instance {:object (three/ArrowHelper.
                          (three/Vector3.) (three/Vector3.) 2 0x0000FF)
                :rotation [(/ math/PI 2) 0 0]}]])

(defn render [root canvas {:keys [clear-color] :or {clear-color [0 0 0]}}]
  (let [ctx (tha/render root canvas)]
    (.setClearColor (:threejs-renderer ctx)
                    (apply #(three/Color. %1 %2 %3) clear-color))))

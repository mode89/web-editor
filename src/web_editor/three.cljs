(ns web-editor.three
  (:require [clojure.math :as math]
            [threeagent.core :as tha]
            [threeagent.entity :refer [IEntityType]]
            ["three" :as three]))

(defn vec3
  ([coll] (three/Vector3. (nth coll 0) (nth coll 1) (nth coll 2)))
  ([x y z] (three/Vector3. x y z)))

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

(deftype LineEntity [^:unsynchronized-mutable geometry
                     ^:unsynchronized-mutable material]
  IEntityType
  (create [_ _ {:keys [points width color] :or {width 1 color 0xFF00FF}}]
    (set! geometry
          (-> (three/BufferGeometry.)
              (.setFromPoints (->> points
                                   (map vec3)
                                   (#(apply array %))))))
    (set! material (three/LineBasicMaterial.
                     (js-obj "color" color
                             "linewidth" width)))
    (three/Line. geometry material))
  (destroy! [_ _ _ _]
    (.dispose geometry)
    (.dispose material)))

(defn render [root canvas {:keys [clear-color] :or {clear-color [0 0 0]}}]
  (let [ctx (tha/render root
                        canvas
                        {:entity-types {:line (->LineEntity nil nil)}})]
    (.setClearColor (:threejs-renderer ctx)
                    (apply #(three/Color. %1 %2 %3) clear-color))))

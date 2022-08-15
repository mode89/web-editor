(ns web-editor.three
  (:require [threeagent.core :as tha]
            [threeagent.entity :refer [IEntityType]]
            ["three" :as three]))

(defn vec3
  ([[x y z]] (new three/Vector3 x y z))
  ([x y z] (new three/Vector3 x y z)))

(defn color
  ([x] (if (coll? x)
         (let [[r g b] x]
           (new three/Color r g b))
         (new three/Color x)))
  ([r g b] (new three/Color r g b)))

(defn grid-helper []
  [:instance {:object (new three/GridHelper)}])

(defn axes-helper [& {:keys [orig length width] :or {length 1 width 1}}]
  [:object
    [:line {:points [orig (map + orig [length 0 0])]
            :width width
            :color 0xFF0000}]
    [:line {:points [orig (map + orig [0 length 0])]
            :width width
            :color 0x00FF00}]
    [:line {:points [orig (map + orig [0 0 length])]
            :width width
            :color 0x0000FF}]])

(deftype LineEntity [^:unsynchronized-mutable geometry
                     ^:unsynchronized-mutable material]
  IEntityType
  (create [_ _ {:keys [points width color] :or {width 1 color 0xFF00FF}}]
    (set! geometry
          (-> (new three/BufferGeometry)
              (.setFromPoints (->> points
                                   (map vec3)
                                   (#(apply array %))))))
    (set! material (new three/LineBasicMaterial
                     (js-obj "color" color
                             "linewidth" width)))
    (new three/Line geometry material))
  (destroy! [_ _ _ _]
    (.dispose geometry)
    (.dispose material)))

(defn render [root canvas {:keys [clear-color]
                           :or {clear-color [0 0 0]}
                           :as config}]
  (let [ctx (tha/render root
                        canvas
                        (merge config
                               {:entity-types
                                 {:line (->LineEntity nil nil)}}))]
    (.setClearColor (:threejs-renderer ctx) (color clear-color))))

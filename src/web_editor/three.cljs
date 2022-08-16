(ns web-editor.three
  (:require [clojure.core.async :as async]
            [threeagent.core :as tha]
            [threeagent.entity :refer [IEntityType]]
            [threeagent.system :refer [ISystem]]
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

(defn normalized-device-coordinates [[x y] {:keys [width height]}]
  [(- (* 2 (/ x width)) 1)
   (+ (* -2 (/ y height)) 1)])

(deftype PickObjectSystem [points
                           viewport-size
                           entities
                           objects-array
                           camera
                           loop-chan]
  ISystem
  (init [_ _]
    (let [raycaster (new three/Raycaster)]
      (reset! loop-chan
        (async/go-loop []
          (let [[x y] (normalized-device-coordinates
                        (async/<! points) @viewport-size)]
            (.setFromCamera raycaster
                            (new three/Vector2 x y)
                            @camera)
            (doseq [iter (.intersectObjects raycaster @objects-array)]
              (let [entity (get @entities (-> iter .-object .-id))
                    callback (:config entity)]
                (callback))))
          (recur)))))
  (destroy [_ _]
    (async/close! @loop-chan))
  (on-entity-added [_ _ entity-id obj config]
    (if (= config :use-this-camera)
      (reset! camera obj)
      (do
        (swap! entities assoc (.-id obj) {:object obj
                                          :id entity-id
                                          :config config})
        (.push @objects-array obj))))
  (on-entity-removed [_ _ _ obj _]
    (let [id (.-id obj)]
      (swap! entities dissoc id)
      (reset! objects-array
              (.filter @objects-array #(not= id (.-id %))))))
  (tick [_ _]))

(defn render [root canvas {:keys [clear-color pick-points viewport-size]
                           :or {clear-color [0 0 0]}
                           :as config}]
  (let [ctx (tha/render
              root
              canvas
              (merge
                config
                {:entity-types {:line (->LineEntity nil nil)}
                 :systems (merge
                            (when (some? pick-points)
                              {:on-pick (->PickObjectSystem
                                          pick-points
                                          viewport-size
                                          (atom {})
                                          (atom (array))
                                          (atom nil)
                                          (atom nil))}))}))
        renderer (:threejs-renderer ctx)]
    (.setClearColor renderer (color clear-color))
    (add-watch viewport-size
               :resize-renderer
               #(.setSize renderer (:width %4) (:height %4)))
    ctx))

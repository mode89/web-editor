(ns web-editor.three2
  (:require [clojure.spec.alpha :as spec]
            ["three" :as three]))

(spec/def ::color (spec/and int? #(< % 0x1000000)))
(spec/def ::vector3 (spec/coll-of number? :count 3))
(spec/def ::rotation (spec/and vector? #(every? number? (take 3 %))))

(defonce context (atom nil))

(declare update-object!)

(defn set-xform! [^js obj xform]
  (spec/assert ::vector3 (:position xform))
  (spec/assert ::rotation (:rotation xform))
  (let [[x y z] (:position xform)]
    (.set (.-position obj) x y z)))

(defn create-geometry! [[geom-type conf]]
  (println "create-geometry" geom-type conf)
  (case geom-type
    :box (let [{:keys [width height depth]} conf]
           (spec/assert (spec/keys :req-un [::width ::height ::depth]) conf)
           (new three/BoxGeometry width height depth))))

(defn create-material! [conf]
  (spec/assert ::color (:color conf))
  (new three/MeshBasicMaterial (clj->js conf)))

(defn create-object! [[obj-type conf children]]
  (println "create-object!" obj-type conf children)
  (let [obj (case obj-type
              :mesh (let [{:keys [geometry material]} conf
                          geom (create-geometry! geometry)
                          mat (create-material! material)]
                      (new three/Mesh geom mat)))]
    (set-xform! obj (:xform conf))
    (doseq [child children]
      (.add obj (create-object! child)))
    obj))

(defn destroy-object! [^js obj obj-type]
  (println "destroy-object!")
  (case obj-type
    :mesh (do
            (-> obj .-geometry .dispose)
            (-> obj .-material .dispose))))

(defn update-object-of-type! [^js obj obj-type old-conf new-conf]
  (case obj-type
    :mesh (let [old-geom (:geometry old-conf)
                old-mat (:material old-conf)
                new-geom (:geometry new-conf)
                new-mat (:material new-conf)]
            (println "Updating mesh object")
            (when (not= old-geom new-geom)
              (-> obj .-geometry .dispose)
              (set! (.-geometry obj) (create-geometry! new-geom)))
            (when (not= old-mat new-mat)
              (throw "Change of material is not supported")))))

(defn replace-child! [^js parent index ^js new-child]
  (println "Replacing child")
  (aset (-> parent .-children) index new-child)
  (set! (.-parent new-child) parent))

(defn update-object-children! [^js parent-obj old-children new-children]
  ; Update common children
  (dotimes [i (min (count old-children) (count new-children))]
    (let [[old-ch-type _ _ :as old-ch-desc] (get old-children i)
          new-ch-desc (get new-children i)]
      (when (not (identical? old-ch-desc new-ch-desc))
        (let [old-ch-obj (-> parent-obj .-children (aget i))
              new-ch-obj (update-object! old-ch-obj old-ch-desc new-ch-desc)]
          (when (not (identical? old-ch-obj new-ch-obj))
            (replace-child! parent-obj i new-ch-obj)
            (destroy-object! old-ch-obj old-ch-type))))))
  ; Remove unused children
  (when (> (count old-children) (count new-children))
    (println "Removing unused children")
    (dotimes [i (- (count old-children) (count new-children))]
      (let [old-obj (-> parent-obj .-children .pop)
            [old-obj-type _ _] (get old-children (+ i (count new-children)))]
        (destroy-object! old-obj old-obj-type))))
  ; Add new children
  (when (< (count old-children) (count new-children))
    (println "Adding new children")
    (dotimes [i (- (count new-children) (count old-children))]
      (let [new-ch-desc (get new-children (+ i (count old-children)))]
        (.add parent-obj (create-object! new-ch-desc))))))

(defn update-object! [^js obj old-desc new-desc]
  (let [[old-type old-conf old-children] old-desc
        [new-type new-conf new-children] new-desc]
    (if (= old-type new-type)
      (do
        (when (not (identical? old-conf new-conf))
          (update-object-of-type! obj old-type old-conf new-conf))
        (when (not (identical? old-children new-children))
          (update-object-children! obj old-children new-children))
        obj)
      (create-object! new-desc))))

(def camera (new three/PerspectiveCamera 75 1.0 1 100))
(set! (.-z (.-position camera)) 7)

(defn animate! [context virtual-scene camera-id]
  (let [old-scene (:virtual-scene context)
        new-scene virtual-scene
        threejs-scene (:threejs-scene context)]
    (update-object-children! threejs-scene old-scene new-scene)
    (doto (:renderer context)
          (.setClearColor 0x303030)
          (.render threejs-scene camera))
    (if (identical? old-scene new-scene)
      context
      (assoc context :virtual-scene virtual-scene))))

(defn create-context [canvas]
  {:canvas canvas
   :threejs-scene (new three/Scene)
   :virtual-scene nil
   :renderer (new three/WebGLRenderer #js {:canvas canvas})})

(defn create-or-reuse-context! [context canvas]
  (if (identical? canvas (:canvas context))
    context
    (do
      (when (some? (:renderer context))
        (.dispose (:renderer context)))
      (create-context canvas))))

(defn render [virtual-scene camera-id canvas]
  (swap! context
         (fn [existing-context]
           (let [ctx (create-or-reuse-context! existing-context canvas)]
             (doto (:renderer ctx)
                   (.setSize 300 300)
                   (.setAnimationLoop
                     #(swap! context animate! @virtual-scene @camera-id)))
             ctx))))

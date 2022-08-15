(ns ^:figwheel-hooks web-editor.core
  (:require [clojure.math :as math]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [web-editor.frp :as frp]
            [web-editor.three :as th]))

(def CAMERA-ROTATION-SPEED 0.01)
(def CAMERA-ZOOM-SPEED 0.003)
(def CAMERA-SHIFT-SPEED 0.001)

(declare camera-rotation-update)
(declare camera-distance-update)
(declare camera-position-update)

(defonce mouse-movement-events (frp/publisher))
(defonce mouse-wheel-events (frp/publisher))

(defonce viewport (r/atom {:width 0 :height 0}))

(defonce camera-rotation
  (frp/reduce #(camera-rotation-update %1 %2)
              {:pitch 0 :yaw 0}
              (frp/subscribe mouse-movement-events
                             (filter #(bit-test (.-buttons %) 2))
                             (filter #(not (.-shiftKey %))))))

(defonce camera-distance (frp/reduce #(camera-distance-update %1 %2)
                                     10.0
                                     (frp/subscribe mouse-wheel-events)))

(defonce camera-position
  (frp/reduce #(camera-position-update
                 %1 @camera-rotation @camera-distance %2)
              [0 0 0]
              (frp/subscribe mouse-movement-events
                             (filter #(bit-test (.-buttons %) 2))
                             (filter #(.-shiftKey %)))))

(defn camera-rotation-update [rotation event]
  (let [dx (->> event .-movementX (* CAMERA-ROTATION-SPEED))
        dy (->> event .-movementY (* CAMERA-ROTATION-SPEED))]
    (-> rotation
        (update :pitch #(- % dy))
        (update :yaw #(- % dx)))))

(defn camera-position-update [position rotation distance event]
  (let [dx (-> event .-movementX (* CAMERA-SHIFT-SPEED distance))
        dy (-> event .-movementY (* CAMERA-SHIFT-SPEED distance))
        delta (-> (th/vec3 (- dx) dy 0)
                  (.applyAxisAngle (th/vec3 1 0 0) (:pitch rotation))
                  (.applyAxisAngle (th/vec3 0 1 0) (:yaw rotation)))]
    (map #(+ %1 %2) position delta)))

(defn camera-distance-update [distance event]
  (let [d (.-deltaY event)]
    (* distance (math/exp (* d CAMERA-ZOOM-SPEED)))))

(defn canvas []
  [:canvas {:id "canvas"
            :on-mouse-move (frp/publish mouse-movement-events)
            :on-wheel (frp/publish mouse-wheel-events)}])

(defn camera []
  [:object {:position @camera-position
            :rotation [0 (:yaw @camera-rotation) 0]}
    [:object {:rotation [(:pitch @camera-rotation) 0 0]}
      [:perspective-camera {:fov 60.0
                            :near 1.0
                            :far (+ @camera-distance 100)
                            :aspect @(frp/apply #(/ (:width %) (:height %))
                                                viewport)
                            :position [0 0 @camera-distance]}]]])

(defn root []
  [:object
    [camera]
    [th/grid-helper]
    [th/axes-helper :orig [0 0 0]
                    :length (* 0.2 @camera-distance)
                    :width 3]
    [:directional-light {:intensity 1.0
                         :position [5 2.5 2]}]
    [:ambient-light {:intensity 0.5}]
    [:box]])

(defn init! []
  (rdom/render [canvas] (js/document.getElementById "app"))
  (let [canvas-el (js/document.getElementById "canvas")]
    (reset! viewport {:width (.-clientWidth canvas-el)
                      :height (.-clientHeight canvas-el)})
    (th/render root canvas-el {:clear-color [0.3 0.3 0.3]})))

(init!)

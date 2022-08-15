(ns ^:figwheel-hooks web-editor.core
  (:require [clojure.math :as math]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [web-editor.frp :as frp]
            [web-editor.three :as th]))

(def CAMERA-ROTATION-SPEED 0.01)
(def CAMERA-ZOOM-SPEED 0.003)

(declare camera-rotation-update)
(declare camera-distance-update)

(defonce mouse-movement-events (frp/publisher))
(defonce mouse-wheel-events (frp/publisher))

(defonce viewport (r/atom {:width 0 :height 0}))

(defonce camera-rotation (frp/reduce #(camera-rotation-update %1 %2)
                                     {:pitch 0 :yaw 0}
                                     (frp/subscribe mouse-movement-events)))

(defonce camera-distance (frp/reduce #(camera-distance-update %1 %2)
                                     10.0
                                     (frp/subscribe mouse-wheel-events)))

(defn camera-rotation-update [rotation event]
  (if (bit-test (.-buttons event) 2)
    (let [dx (->> event .-movementX (* CAMERA-ROTATION-SPEED))
          dy (->> event .-movementY (* CAMERA-ROTATION-SPEED))]
      (-> rotation
          (update :pitch #(- % dy))
          (update :yaw #(- % dx))))
    rotation))

(defn camera-distance-update [distance event]
  (let [d (.-deltaY event)]
    (* distance (math/exp (* d CAMERA-ZOOM-SPEED)))))

(defn canvas []
  [:canvas {:id "canvas"
            :on-mouse-move (frp/publish mouse-movement-events)
            :on-wheel (frp/publish mouse-wheel-events)}])

(defn camera []
  [:object {:rotation [0 (:yaw @camera-rotation) 0]}
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

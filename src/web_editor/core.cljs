(ns web-editor.core
  (:require [clojure.math :as math]
            [clojure.spec.alpha :as spec]
            [clojure.spec.test.alpha :as spec-test]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [web-editor.frp :as frp]
            [web-editor.three :as th]
            [web-editor.three2 :as th2]
            [web-editor.utils :as u]
            ["three" :as three]))

(def CAMERA-ROTATION-SPEED 0.01)
(def CAMERA-ZOOM-SPEED 0.003)
(def CAMERA-SHIFT-SPEED 0.001)

(declare camera-rotation-update)
(declare camera-distance-update)
(declare camera-position-update)

(defonce mouse-movement-events (frp/publisher))
(defonce mouse-wheel-events (frp/publisher))
(defonce mouse-click-events (frp/publisher))

(defonce canvas-size (r/atom nil))

(defonce camera-rotation
  (frp/accum #(camera-rotation-update %1 %2)
             {:pitch (/ math/PI -4)
              :yaw (/ math/PI 4)}
             (frp/subscribe mouse-movement-events
                            (filter #(bit-test (.-buttons %) 2))
                            (filter #(not (.-shiftKey %))))))

(defonce camera-distance (frp/accum #(camera-distance-update %1 %2)
                                    10.0
                                    (frp/subscribe mouse-wheel-events)))

(defonce camera-position
  (frp/accum #(camera-position-update
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

(defn size-sensor [size component]
  (r/with-let [this (r/current-component)
               update-size #(reset! size
                                    (let [el (rdom/dom-node this)]
                                      {:width (.-clientWidth el)
                                       :height (.-clientHeight el)}))
               _ (js/setTimeout update-size) ; Trigger initial size update
               _ (js/window.addEventListener "resize" update-size)]
    [:div {:style {:width "100%" :height "100%"}} component]
    (finally (js/window.removeEventListener "resize" update-size))))

(defn canvas []
  [size-sensor canvas-size
    [:canvas {:id "canvas"
              :style @canvas-size
              :on-mouse-move (frp/publish mouse-movement-events)
              :on-wheel (frp/publish mouse-wheel-events)
              :on-click (frp/publish mouse-click-events)}]])

(defn camera []
  [:object {:position @camera-position
            :rotation [0 (:yaw @camera-rotation) 0]}
    [:object {:rotation [(:pitch @camera-rotation) 0 0]}
      [:perspective-camera {:fov 60.0
                            :near 1.0
                            :far (+ @camera-distance 100)
                            :aspect @(frp/lift #(/ (:width %) (:height %))
                                               canvas-size)
                            :position [0 0 @camera-distance]
                            :on-pick :use-this-camera}]]])

(defn box [_]
  (let [selected (r/atom false)]
    (fn [init-pos]
      [:box {:position init-pos
             :material {:color (if @selected 0xFFFFAF 0xFFFFFF)}
             :on-pick #(reset! selected true)
             }])))

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
    (map #(vector [box [(* 3 %) 0 0]]) (range 10000))])

; (defn init! []
;   (rdom/render [canvas] (js/document.getElementById "app"))
;   (let [ctx (th/render root
;              (js/document.getElementById "canvas")
;              {:clear-color [0.3 0.3 0.3]
;               :viewport-size canvas-size
;               :pick-points
;                 (frp/subscribe
;                   mouse-click-events
;                   (map #(vector (.-clientX %) (.-clientY %))))})]
;     (when (not (identical? @thctx ctx))
;       (println "Created new context")
;       (reset! thctx ctx))))
; 
; (init!)

; (defn init! []
;   (rdom/render [canvas] (js/document.getElementById "app"))
;   )
; 
; (init!)

(defn box2 [conf & children]
  [:mesh {:xform (u/select-keys* conf {:position [0 0 0]
                                       :rotation [0 0 0]
                                       :scale [1 1 1]})
          :geometry [:box (u/select-keys* conf {:width 1
                                                :height 1
                                                :depth 1})]
          :material (or (:material conf) {:color 0xFFFFFF})}
   children])

(defonce virtual-scene
  (atom [[:perspective-camera {:xform {:position [0 0 7]
                                       :rotation [0 0 0]
                                       :scale [1 1 1]}
                               :fov 75
                               :aspect 1
                               :near 1
                               :far 1000}]
         (box2 {:position [0 0 0]}
               ; (box2 {:position [2 0 0]})
               )]))

; (js/setInterval #(.rotateZ box1 0.1) 100)
(js/setTimeout #(swap! virtual-scene
                       ; assoc-in
                       ; [0 1 :geometry 1 :width]
                       ; 2
                       assoc-in [0 2] [(box2 {:position [2 0 0]})]
                       ; assoc-in [0 2] nil
                       )
               1000)

(th2/render virtual-scene (js/document.getElementById "canvas"))

(spec/check-asserts true)
(spec-test/instrument)

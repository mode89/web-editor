(ns ^:figwheel-hooks web-editor.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [threeagent.core :as th]
            [web-editor.frp :as frp]
            ["three" :as three]))

(def CAMERA-ROTATION-MULT 0.01)

(declare camera-rotation-update)

(defonce viewport (r/atom {:width 0 :height 0}))

(defonce mouse-movement (frp/publisher))

(defonce camera-rotation (frp/creduce #(camera-rotation-update %1 %2)
                                      {:pitch 0 :yaw 0}
                                      (frp/subscribe mouse-movement)))

(defn camera-rotation-update [rotation event]
  (if (bit-test (.-buttons event) 2)
    (let [dx (->> event .-movementX (* CAMERA-ROTATION-MULT))
          dy (->> event .-movementY (* CAMERA-ROTATION-MULT))]
      (-> rotation
          (update :pitch #(- % dy))
          (update :yaw #(- % dx))))
    rotation))

(defn canvas []
  [:canvas {:id "canvas"
            :on-mouse-move (frp/publish mouse-movement)}])

(defn camera []
  [:object {:rotation [0 (:yaw @camera-rotation) 0]}
    [:object {:rotation [(:pitch @camera-rotation) 0 0]}
      [:perspective-camera {:fov 60.0
                            :near 1.0
                            :far 100.0
                            :aspect @(frp/rapply #(/ (:width %) (:height %))
                                                 viewport)
                            :position [0 0 10]}]]])

(defn root []
  [:object
    [camera]
    [:directional-light {:intensity 1.0
                         :position [5 2.5 2]}]
    [:ambient-light {:intensity 0.5}]
    [:instance {:object (three/GridHelper.)}]
    [:box]])

(defn init! []
  (rdom/render [canvas] (js/document.getElementById "app"))
  (let [canvas-el (js/document.getElementById "canvas")]
    (reset! viewport {:width (.-clientWidth canvas-el)
                      :height (.-clientHeight canvas-el)})
    (let [th-ctx (th/render root canvas-el)]
      (.setClearColor (:threejs-renderer th-ctx)
                      (three/Color. 0.3 0.3 0.3)))))

(init!)

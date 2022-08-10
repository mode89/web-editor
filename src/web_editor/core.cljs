(ns ^:figwheel-hooks web-editor.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [threeagent.core :as th]
            ["three" :as three]))

(defonce state (r/atom {:viewport {:width 0
                                   :height 0}}))

(defn canvas []
  [:canvas {:id "canvas" }])

(defn root []
  [:object
    [:perspective-camera {:fov 60.0
                          :near 1.0
                          :far 100.0
                          :aspect (let [vp @(r/cursor state [:viewport])]
                                    (/ (:width vp) (:height vp)))
                          :position [0 3 10]}]
    [:instance {:object (three/GridHelper.)}]
    [:directional-light {:intensity 1.0
                         :position [1 1 1]}]
    [:box]])

(defn init! []
  (rdom/render [canvas] (js/document.getElementById "app"))
  (let [canvas-el (js/document.getElementById "canvas")]
    (swap! state assoc :viewport {:width (.-clientWidth canvas-el)
                                  :height (.-clientHeight canvas-el)})
    (let [th-ctx (th/render root canvas-el)]
      (.setClearColor (:threejs-renderer th-ctx)
                      (three/Color. 0.3 0.3 0.3)))))

(init!)

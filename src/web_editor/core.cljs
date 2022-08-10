(ns ^:figwheel-hooks web-editor.core
  (:require [reagent.dom :as rdom]))

(defn hello-world []
  [:button {:class "btn btn-primary"} "Hello, World!"])

(defn init! []
  (rdom/render [hello-world] (js/document.getElementById "app")))

(init!)

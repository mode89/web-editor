(defproject web-editor "0.1.0"
  :dependencies [
    [doughamil/threeagent "1.0.1"]
    [org.clojure/clojurescript "1.11.60"]
    [reagent "1.1.1"]]
  :profiles {:dev {:dependencies [
    [com.bhauman/figwheel-main "0.2.18"]]}}
  :aliases {
    "fig" ["trampoline" "run" "-m" "figwheel.main"]
    "fig-dev" ["trampoline" "run" "-m" "figwheel.main"
               "--build" "dev" "--repl"]})

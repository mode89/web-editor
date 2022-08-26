(ns web-editor.utils)

(defn select-keys*
  "Like select-keys, but also provides defaults for missing keys."
  [coll keys-and-defaults]
  (merge keys-and-defaults
         (select-keys coll (keys keys-and-defaults))))

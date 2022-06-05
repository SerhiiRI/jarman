(ns jarman.application.collector-custom-themes
  (:require
   [jarman.org         :refer :all]
   [jarman.config.vars :refer [defvar setj setq]]))

(def ^:private selected-theme (atom nil))
(defn selected-theme-get [] (deref selected-theme))

(def ^:private system-ThemePlugin-list (ref []))
(defn system-ThemePlugin-list-get [] (deref system-ThemePlugin-list))
(defrecord ThemePlugin
    [theme-name
     theme-description
     theme-loader-fn])
(defn constructThemePlugin [{:keys [name description loader]}]
  (assert (string? name) "Theme plugin `:name` must be symbol")
  (assert (string? description) "Theme plugin `:description` is not string type")
  (assert (fn? loader) "Theme plugin `:loader` might be a function (fn []..)")
  (map->ThemePlugin
   {:theme-loader-fn loader
    :theme-description description
    :theme-name name}))

;; register-custom-theme-plugin ->
(defn register-theme [& {:as args}]
  (dosync (alter system-ThemePlugin-list
            (fn [l] (let [plugin (constructThemePlugin args)
                         l-without-old-plugin
                         (filterv #(not= (:theme-name plugin) (:theme-name %)) l)]
                     (conj l-without-old-plugin plugin)))))
  true)

(defn do-load-theme [theme-name]
  {:pre [(string? theme-name)]}
  (let [theme (first (filter #(= (:theme-name %) theme-name) (system-ThemePlugin-list-get)))]
    (print-header
      (format "Choose `%s` theme" theme-name)
      ((:theme-loader-fn theme))
      (swap! selected-theme (fn [_]theme))
      (setq jarman.variables/theme-selected theme-name)
      (setj jarman.variables/theme-selected theme-name)
      nil)))

(comment
  (system-ThemePlugin-list-get)
  (system-ViewPlugin-list-get))

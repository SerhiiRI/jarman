(ns plugin.aaa.main-plugin-file)
(require 'plugin.aaa.first-file)
(require 'plugin.aaa.second-file)
(require 'jarman.interaction)

(println "3. Load third main file")

(jarman.interaction/register-view-plugin
 :name 'aaa
 :description "Somethign to manipulation with aaa"
 :entry (fn [plugin-path global-configuration])
 :toolkit (fn [configuraion])
 :spec-list [])


(ns jarman.variables
  (:require [jarman.config.vars :refer [defvar]]))

(defvar dataconnection-saved nil
  :name "Selected Datasource"
  :doc "Is DB configuration key form `dataconnection-alist`"
  :type clojure.lang.Keyword
  :group :datasource)

(defvar dataconnection-alist {}
  :name "Datasources"
  :doc "Connection map list"
  :type clojure.lang.PersistentArrayMap
  :group :datasource)

(defvar theme-selected "Jarman Light"
  :name  "Theme"
  :doc   "Select loading theme"
  :type  java.lang.String
  :group :system)

(defvar user-menu {}
  :name "Buisness menu"
  :doc "Left side user menu"
  :type clojure.lang.PersistentArrayMap
  :group :system)

(defvar view-src :database
  :name "Defviews src"
  :doc  "Source of defviews. View.clj or database [:view.clj :database]."
  :type clojure.lang.Keyword
  :group :system)

(defvar language-selected :en
  :name "Language"
  :doc "Define translation language to jarman"
  :type clojure.lang.Keyword
  :group :view-params)

(defvar language-supported [:en :pl]
  :name "Supported Languages"
  :doc "Define supported language inside jarman"
  :type clojure.lang.PersistentVector
  :group :view-params)

(defvar jarman-update-repository-list ["ftp://jarman:dupa@trashpanda-team.ddns.net"]
  :doc "List of update reposiotries"
  :type clojure.lang.PersistentList
  :group :update-system)

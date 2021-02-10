(ns jarman.config.spec
  (:import (java.io IOException))
  (:require [jarman.tools.lang :refer [in? blet]]
            [jarman.config.tools :refer [recur-walk-throw]]
            [clojure.spec.alpha :as s]))


;; {:name ["[\w\d\s]+" (vector (:\w)+)]
;;       :display [:nil :edit :noedit]
;;       :type [:block :param :file :directory :error]
;;       :value {:param? <param-spec>
;; 	      :block? <block-spec>}}

;;      ;;; <block-spec>
;;      {:name ["[\w\d\s]+" (vector (:\w)+)]
;;       :doc ["\.+" nil (vector (:\w)+)]
;;       :type [:block :param :error]
;;       :display [:nil :edit :noedit]
;;       :value {:param? <param-spec>
;; 	      :block? <block-spec>}}

;;      ;;; <param-block>
;;      {:name ["[\w\d\s]+" (vector (:\w)+)]
;;       :doc ["\.+" nil (vector (:\w)+)]
;;       :type [:block :param :error]
;;       :display [:nil :edit :noedit]
;;       :component [:text :textnumber :textlist :textcolor :checkbox :listbox :selectbox]
;;       :value *}

;;      ;;; <error-spec>
;;      {:log "\.+"
;;       :type :error}

(s/def ::ne-string (every-pred string? not-empty))
(s/valid? ::ne-string "123")

(s/def ::url (s/and ::ne-string (partial re-matches #"(?i)^http(s?)://.*")))
(s/valid? ::url "test")
(s/valid? ::url "http://test.com")
(s/valid? ::url nil)


(s/def ::url-list (s/coll-of ::url))
(s/valid? ::url-list ["http://test.com" "http://ya.ru"]) ;; => true
(s/valid? ::url-list ["http://test.com" "dunno.com"]);; => false

(s/def ::params (s/map-of keyword? string?))
(s/valid? ::params {:foo "test"});; => true
(s/valid? ::params {"foo" "test"});; => false


;;; Notation of spec/keys
;; :req - required key
;; :opt - optional key
;; -un - unqualified namespace of key
;; -fq - fully qualified key namespecing
;;; Examples
;; :req, :req-un, :opt, :opt-un
(s/def ::page
  (s/keys :req-un [:page/address
                   :page/description]))


(s/valid? ::page {:address "clojure.org" :description "Clojure Language"})
(s/valid? ::page {:address "https://clojure.org/" :description ""})
(s/valid? ::page {:address "https://clojure.org/"})
(s/valid? ::page {:page/address "https://clojure.org/":page/description "Clojure Language"})

(s/def ::page-fq
  (s/keys :req-fq [:page/address
                   :page/description]))

(s/valid? ::page-fq {:address "clojure.org" :description "Clojure Language"})
(s/valid? ::page-fq {:address "https://clojure.org/" :description ""})
(s/valid? ::page-fq {:address "https://clojure.org/"})
(s/valid? ::page-fq {:page/address "https://clojure.org/":page/description "Clojure Language"})


(s/def :page/status int?)
(s/def ::page-status
  (s/keys :req-un [:page/address
                   :page/description]
          :opt-un [:page/status]))

(s/def ::ne-string (every-pred string? not-empty))
(s/def :block/name-str (s/and ::ne-string (partial re-matches #"[\w\d\s]+")))
(s/def :block/doc-str ::ne-string)
(s/def :block/keywoard-path (every-pred vector? (partial every? keyword?)))
(s/def :block/name (fn [x] (or (s/valid? :block/name-str x) (s/valid? :block/keywoard-path x))))

(s/def :block/type (fn [x] (some #(= x %) [:block :param :file :directory :error])))
(s/def :block/display (fn [x] (some #(= x %) [:none :edit :noedit])))
(s/def :block/component (fn [x] (some #(= x %) [:text :textnumber :textlist :textcolor :checkbox :listbox :selectbox])))
(s/def :block/value any?)
(s/def :block/doc (fn [x](or (s/valid? :block/doc-str x) (s/valid? :block/keywoard-path x))))
(s/def :block/log ::ne-string)


;;; parts of configuration structure validateion
;;; patterns
(s/def :block/block
  (s/keys :req-un [:block/type
                   :block/value]
          :opt-un [:block/name
                   :block/doc
                   :block/display]))
(s/def :block/parameter
  (s/keys :req-un [:block/component
                   :block/type
                   :block/value
                   :block/name]
          :opt-un [:block/doc
                   :block/display]))
(s/def :block/error
  (s/keys :req-un [:block/type
                   :block/log]))

(s/def :block/valid-segment
  (fn [m] (s/valid? (condp in? (:type m)
                     [:block
                      :file
                      :directory]  :block/block
                     [:param]      :block/parameter
                     [:error]      :block/error 
                     (fn [_] false)) m)))

(defn- valid-config-logger [m logger]
  (let [sumvalid (atom true)
        f (fn [block path]
            (let [vld? (s/valid? :block/valid-segment block)]
              (swap! sumvalid (fn [a](and a vld?)))
              (if-not vld? (logger block path))))]
    (recur-walk-throw m f [])
    @sumvalid))


(defn valid-param [m]
  (s/valid? :block/parameter m))
(defn valid-block [m]
  (s/valid? :block/block m))
(defn valid-segment [m]
  (blet {:valid? (valid-config-logger m logger) :output @output}
        [output (atom [])
         logger (fn [segment path]
                  (swap! output 
                   #(conj %
                     {:path path
                      :message (format "Not valid section - %s" (str (assoc segment :value '...)))})))]))







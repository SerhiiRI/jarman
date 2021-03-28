;; For more information about specyfication, please read README.org file in this directory.
(ns jarman.config.spec
  (:import (java.io IOException))
  (:require [jarman.tools.lang :refer [in? in-r? wlet blet where]]
            [jarman.config.tools :refer [recur-walk-throw]]
            [clojure.spec.alpha :as s]))

;;;;;;;;;;;;;;;;;;;
;;; SPEC SCHEME ;;;
;;;;;;;;;;;;;;;;;;;

;; Quick Scheme intro about
;; spec's to  every part of 
;; configuration map:
;; 
;;  SEGMENT SPEC :block/block -- (See `block-segment`)
;;   :name    :block/name 
;;   :doc     :block/doc
;;   :type    :block/type -- (See `segment-type`) REQUIRED 
;;   :display :block/display -- (See `segment-display`)
;;   :value   :block/value -- REQUIRED 
;;
;;  PARAMETER SPEC :block/param -- (See `param-segment`)
;;   :name      :block/name
;;   :doc       :block/doc
;;   :type      :block/type -- (See `segment-type`) REQUIRED
;;   :display   :block/display -- (See `segment-display`)
;;   :component :block/component -- (See `parameter-components`) REQUIRED
;;   :value     :block/value -- REQUIRED
;;
;;  ERROR SPEC :block/error -- (See `error-segment`)
;;   :log   :block/log  -- REQUIRED
;;   :type  :block/type -- REQUIRED

;; (do
;;   (println "----") 
;;   (map #(println (format "(def %s :%s)" (name %) (name %))) '(:block :file :directory :param :error :block :file :directory :param :error :none :edit :noedit)))
;; (do
;;   (println "----") 
;;   (map #(println (format "(def %s :%s)" (name %) (name %))) '(:listurl :texturl :textlist :listbox :selectbox :text :textcolor :textnumber :checkbox)))
(def $block :block)
(def $file :file)
(def $directory :directory)
(def $param :param)
(def $error :error)
(def $block :block)
(def $file :file)
(def $directory :directory)
(def $param :param)
(def $error :error)
(def $none :none)
(def $edit :edit)
(def $noedit :noedit)

;;; component spec key
(def $listurl :listurl)
(def $texturl :texturl)
(def $textlist :textlist)
(def $listbox :listbox)
(def $selectbox :selectbox)
(def $text :text)
(def $textcolor :textcolor)
(def $textnumber :textnumber)
(def $checkbox :checkbox)

(def block-segment [$block $file $directory])
(def param-segment [$param])
(def error-segment [$error])
(def segment-type [$block $file $directory $param $error])
(def segment-display [$none $edit $noedit])
;;; component types
(def component-n-url [$listurl])
(def component-url [$texturl])
(def component-n-text-num [$textlist $listbox $selectbox])
(def component-text [$text])
(def component-color [$textcolor])
(def component-number [$textnumber])
(def component-checkbox [$checkbox])
(def parameter-components (flatten [component-n-url component-url component-n-text-num component-text component-number component-checkbox component-color]))

;;;;;;;;;;;;;;;;;;;;;;
;;; Quick template ;;;
;;;;;;;;;;;;;;;;;;;;;;

(defn error-block
  "Generate error segment by spec convinience" [log]
  {:pre [(string? log)]}
  {:type :error :log log})

(defn directory-block [dir-name value]
  {:pre [(string? dir-name)]}
  {:name dir-name :display :edit :type :directory :value value})

(defn file-block [file-name value]
  {:pre [(string? file-name)]}
  {:name file-name :display :edit :type :file :value value})

(defn config-block [value]
  {:display :edit :type :block :value value})

(defn param-block [value]
  {:display :edit :type :param :value value})

;;;;;;;;;;;;;;;;;;;;;;
;;; List of spec's ;;;
;;;;;;;;;;;;;;;;;;;;;;

(s/def ::ne-string (every-pred string? not-empty))
;; (s/valid? ::ne-string "123")

(s/def ::url (s/and ::ne-string (partial re-matches #"(?i)^http(s?)://.*")))
;; (s/valid? ::url "test")
;; (s/valid? ::url "http://test.com")
;; (s/valid? ::url nil)

(s/def ::url-list (s/coll-of ::url))
;; (s/valid? ::url-list ["http://test.com" "http://ya.ru"]) ;; => true
;; (s/valid? ::url-list ["http://test.com" "dunno.com"]);; => false

(s/def ::rgb-color (s/and ::ne-string (partial re-matches #"#([0-9a-f]{3}|[0-9a-f]{6})")))
;; (s/valid? ::rgb-color"#00f")
;; (s/valid? ::rgb-color "#00fa0f")

;; (s/def ::params (s/map-of keyword? string?))
;; (s/valid? ::params {:foo "test"});; => true
;; (s/valid? ::params {"foo" "test"});; => false

;;; Notation of spec/keys
;; :req - required key
;; :opt - optional key
;; -un - unqualified namespace of key
;; -fq - fully qualified key namespecing
;;; Examples
;; :req, :req-un, :opt, :opt-un

;; (s/def ::page
;;   (s/keys :req-un [:page/address
;;                    :page/description]))


;; (s/valid? ::page {:address "clojure.org" :description "Clojure Language"})
;; (s/valid? ::page {:address "https://clojure.org/" :description ""})
;; (s/valid? ::page {:address "https://clojure.org/"})
;; (s/valid? ::page {:page/address "https://clojure.org/":page/description "Clojure Language"})

;; (s/def ::page-fq
;;   (s/keys :req-fq [:page/address
;;                    :page/description]))

;; (s/valid? ::page-fq {:address "clojure.org" :description "Clojure Language"})
;; (s/valid? ::page-fq {:address "https://clojure.org/" :description ""})
;; (s/valid? ::page-fq {:address "https://clojure.org/"})
;; (s/valid? ::page-fq {:page/address "https://clojure.org/":page/description "Clojure Language"})
;; (s/def :page/status int?)
;; (s/def ::page-status
;;   (s/keys :req-un [:page/address
;;                    :page/description]
;;           :opt-un [:page/status]))


;;;;;;;;;;;;;;;;;;;;;;;
;;; Logistic spec's ;;;
;;;;;;;;;;;;;;;;;;;;;;;

;;; structural block logic
(s/def :block/name-str ::ne-string)
(s/def :block/doc-str ::ne-string)
(s/def :block/keyword-path (every-pred vector? (partial every? keyword?)))
(s/def :block/name (s/or :name :block/name-str :keyword-path :block/keyword-path))
;; (s/def :block/name (fn [x] (or (s/valid? :block/name-str x) (s/valid? :block/keywoard-path x))))
(s/def :block/type (fn [x] (in-r? x segment-type)))
(s/def :block/display (fn [x] (in-r? x segment-display))) 
(s/def :block/component (fn [x] (in-r? x parameter-components)))
(s/def :block/value any?)
(s/def :block/doc (s/or :string :block/doc-str :keyword-path :block/keyword-path))
(s/def :block/log ::ne-string)
;;; parameter spec's
(s/def :param/value any?)
(s/def :param/component/text ::ne-string)
(s/def :param/component/сheck boolean?)
(s/def :param/component/number number?)
(s/def :param/component/url ::url)
(s/def :param/component/color ::rgb-color)
(s/def :param/component/urlist ::url-list)
(s/def :param/component/list (s/and vector? (s/or :textlist (s/coll-of ::ne-string) :numberlist (s/coll-of number?))))


;; (let [;; m {:type :param :display :edit :component :textnumber :value 123}
;;       ;; m {:type :param :display :edit :component :selectbox :value ["1" "2" "3"]}
;;       ;; m {:type :param :display :edit :component :selectbox :value [0 1 2 3]}
;;       ;; m {:type :param :display :edit :component :texturl :value "http://test.com"}
;;       ;; m {:type :param :display :edit :component :listurl :value ["http://test.com" "http://ya.ru"]}
;;       ;; m {:type :param :display :edit :component :checkbox :value false}
;;       ;; m {:type :param :display :edit :component :text :value "123"}
;;       ;; m {:type :param :display :edit :component :BAD :value 123}
;;       ;; m {:type :param :display :edit :component :textcolor :value "#fff000"}
;;       ;; m {:type :param :display :edit :component :textcolor :value "#f0e"}
;;       ]
;;   (s/valid? (doto (condp in? (:component m)
;;               component-n-url :param/component/urlist
;;               component-url :param/component/url
;;               component-n-text-num :param/component/list
;;               component-text :param/component/text
;;               component-number :param/component/number
;;               component-checkbox :param/component/сheck
;;               component-color :param/component/rgb-color
;;               (fn [_] false)) println )
;;            (:value m)))
;; (s/def ::ltext (s/coll-of :param/component/text))
;; (s/valid? ::ltext ["1" "2"])
;; (s/def ::lnums (s/coll-of :param/component/number))
;; (s/valid? ::lnums [1 2 3])
;; (s/def ::llist (s/or :text ::ltext
;;                      :list ::lnums))
;; (s/valid? ::llist [1 2 3])
;; (s/valid? ::llist ["1 2 3"])


;; Whole map validators
;; for block `block-segment`
(s/def :block/block
  (s/keys :req-un [:block/type :block/value]
          :opt-un [:block/name :block/doc :block/display]))

;; Whole map validators
;; for block `param-segment`
(s/def :block/parameter
  (s/keys :req-un [:block/component :block/type :param/value]
          :opt-un [:block/name :block/doc :block/display]))

;; Whole map validators
;; for block `error-segment`
(s/def :block/error
  (s/keys :req-un [:block/type :block/log]))

;; Select one of spec validator
;; depending on `segment-type` in
;; `:type` key in segment
(s/def :block/segment
  (fn [m] (s/valid?
          (condp in? (:type m)
            block-segment :block/block
            param-segment :block/parameter
            error-segment :block/error 
            (fn [_] false)) m)))

(defn- valid-config-logger
  "Description
    Logistic function which do `recur-walk-throw`
    throw the configuration map

  Params
    m - configuraion map
    logger - function which apply map segment of
      configuration, and keyword path to segment
      (logger {...} [:one...])"
  [m logger]
  (let [sumvalid (atom true)
        f (fn [block path]
            (let [vld? (s/valid? :block/segment block)]
              (swap! sumvalid (fn [a](and a vld?)))
              (if-not vld? (logger block path))))]
    (recur-walk-throw m f [])
    @sumvalid))

;;; (s/valid? :block/segment @jarman.gui.gui-app/configuration-copy)

(defn segment-short
  "Description
    Get segment block map, and return bit more shortest
    version of it, prepared to printing.

  Example
    Original long segment 
     {:doc \"some really long documentation string\"
      :name \"looooong, very long string\"
      :display :edit
      :value {:param1 {:type :param...} ...}}
    Will be transfromed to
     {:doc \"some really...\"
      :name \"looooong....\"
      :display :edit
      :value '...}"
  [segment]
  (wlet
   (-> segment hide-value short-name short-doc)
   ((hide-value
     (fn [m] (if (:value m) (assoc m :value '...) m)))
    (short-doc
     (fn [m] (let [mdoc (:doc m)]
              (if (and mdoc (string? mdoc) (< 25 (count mdoc)))
                (assoc m :doc (apply str (concat (take 19 mdoc) "..."))) m))))
    (short-name
     (fn [m] (let [mname (:name m)]
              (if (and mname (string? mname) (< 25 (count mname)))
                (assoc m :name (apply str (concat (take 19 mname) "..."))) m)))))))

(defn- unvalid-block-out
  "Description
    return information about unvalid block

  Warning
    This function does not try valid segment!
    Only test parameter. It mean that parameter
    `m` already being unvalid.

  Example 
     (unvalid-block-out {:type :param, :display :BAD, :component :BAD, :value \"10.0.0.69\"} [:database ...])
      ;;=> {:path [:database ...]
            :messages
            [\"Undefinied dispaly status key ':BAD' in {:type :param, :display :BAD, :component :BAD, :value ...}\"
             \"Unknown param. component type key ':BAD' in {:type :param, :display :BAD, :component :BAD, :value ...}\"
            ]}" [m path-to-section]
  (wlet
   (if-let [T (:type m)]      (if (not (s/valid? :block/type T))      (join (format "Undefinied section type key '%s'" T))))
   (if-let [N (:name m)]      (if (not (s/valid? :block/name N))      (join (format "Name is not String or Keyword-Path '%s'" N))))
   (if-let [D (:doc m)]       (if (not (s/valid? :block/doc  D))      (join (format "Documentation is not String or Keyword-Path '%s'" D))))
   (if-let [D (:display m)]   (if (not (s/valid? :block/display D))   (join (format "Undefinied dispaly status key '%s'" D))))
   (if-let [C (:component m)] (if (not (s/valid? :block/component C)) (join (format "Unknown param. component type key '%s'" C))))
   (if (empty? @MESSAGES)     (join "Undefinied valid exception"))
   {:path path-to-section :messages @MESSAGES}
   ((MESSAGES (ref []))
    (short-m  (str (segment-short m)))
    (join #(dosync (alter MESSAGES (fn [MX](conj MX (format "%s in %s" % short-m)))))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Validators without output ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn valid-param-test
  "Description
    Return true/false after do validation
    on parameter map block"
  [m] {:pre [(map? m)]}
  (s/valid? :block/parameter m))

(defn valid-block-test
  "Description
    Return true/false after do validation
    on segment map block"
  [m] {:pre [(map? m)]}
  (s/valid? :block/block m))

(defn valid-error-test
  "Description
    Return true/false after do validation
    on error map block"
  [m] {:pre [(map? m)]}
  (s/valid? :block/error m))

(defn valid-segment-test
  "Description
    Return true/false after do validation
    on undefinied segment map block"
  [m] {:pre [(map? m)]}
  (valid-config-logger m (fn [_unused_1 _unused_2])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Validators with information output ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn valid-block
  "Description
    Do validation on block segment of configuraion,
    and return information map as result

  Example
    (valid-block {:type :block, :display :BAD :value {....})
    ;;=>
     {:valid? false
      :output {:path [:path :in :map]
               :messages [\"Undefinied dispaly status key ':BAD' in {:type :block, :display :BAD, :component :BAD, :value ...}\"]}}"
  ([m] (valid-block m nil))
  ([m path] {:pre [(map? m)]}
   (if (s/valid? :block/block m)
     {:valid? true :output nil}
     {:valid? false :output (unvalid-block-out m path)})))

(defn valid-param
  "Description
    Do validation on parameter segment of configuraion,
    and return information map as result

  Example
    (valid-param {:type :param, :display :BAD, :component :BAD, :value \"10.0.0.69\"})
    ;;=>
     {:valid? false
      :output {:path [:path :in :map]
               :messages [\"Undefinied dispaly status key ':BAD' in {:type :param, :display :BAD, :component :BAD, :value ...}\"
                          \"Unknown param. component type key ':BAD' in {:type :param, :display :BAD, :component :BAD, :value ...}\"]}}"
  ([m] (valid-param m nil))
  ([m path] {:pre [(map? m)]}
   (if (s/valid? :block/parameter m)
     {:valid? true :output nil}
     {:valid? false :output (unvalid-block-out m path)})))

(defn valid-segment
  "Description
    Do recursive validation on undefinied segment
    of configuraion, and return information map as result

  Return Example
    {:valid? false
     :output [{:path [:path :in :map]
              :messages [\"Undefinied dispaly status key ':BAD' in {:type :param, :display :BAD, :component :BAD, :value ...}\"
                         \"Unknown param. component type key ':BAD' in {:type :param, :display :BAD, :component :BAD, :value ...}\"]}}"
  [m] {:pre [(map? m)]}
  (blet {:valid? (valid-config-logger m logger) :output @output}
        [output (atom [])
         logger (fn [segment path]
                  (swap! output 
                   #(conj % (unvalid-block-out segment path))))]))



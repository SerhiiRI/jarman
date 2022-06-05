(ns jarman.logic.metadata-test
  (:require
   [clojure.data :as data]
   [clojure.string :as string]
   [jarman.config.storage :as storage]
   [jarman.config.environment :as env]
   [jarman.lang :refer :all]
   [jarman.logic.connection :as db]
   [datascript.core :as d]
   [jarman.logic.metadata                  :refer :all]
   [jarman.logic.sql-tool                  :refer [select! update! insert! alter-table! create-table! delete! show-table-columns ssql-type-parser]]
   [jarman.managment.data-metadata-shorts  :refer [table field table-link field-link field-composite prop]])
  (:import (java.util Date)
           (java.text SimpleDateFormat)
           (jarman.logic.metadata TableMetadata)))

(comment
  (require '[jarman.managment.data-metadata-shorts :refer [table field table-link field-link field-composite prop]])
  (.return-columns-flatten u)
  (.return-columns-flatten-wrapp u)
  (.return-columns-join u)
  (.return-columns-join-wrapp u)
  (.return-columns-composite u)
  (.return-columns-composite-wrapp u)
  (.return-columns u)
  (.return-columns-wrapp u)
  (.ungroup u (.group u {:user.last_name "", :user.profile-label "", :user.login "", :user.id_permission "", :user.password "", :user.first_name "", :user.profile-url ""}))
  (create-table-by-meta u))

(comment
  ;;; Julia test segment
  ;;; ------------------
  (require '[jarman.managment.data-metadata-shorts :refer [table field table-link field-link field-composite prop]])
  ;;; ------------------
  (def s (TableMetadata. 
          {:table_name "seal",
           :prop
           (prop
            :table (table :field "seal" :representation "seal"),
            :columns
            [(field :field :seal_number :component-type [:text])
             (field :field :datetime_of_use :component-type [:datetime :date :text])
             (field :field :datetime_of_remove :component-type [:datetime :date :text])]
            :columns-composite
            [(field-composite :field :site :component-type [:comp-url] :constructor #'jarman.logic.metadata/map->Link
                              :columns [(field :field :site_name :constructor-var :text :component-type [:text])
                                        (field :field :site_url :constructor-var :link :component-type [:text])])
             (field-composite :field :loc_file :component-type [:comp-file] :constructor #'jarman.logic.metadata/map->File
                              :columns [(field :field :file_name :constructor-var :file-name :component-type [:text])
                                        (field :field :file :constructor-var :file  :component-type [:blob])])
             (field-composite :field :ftp_file :component-type [:comp-ftp] :constructor #'jarman.logic.metadata/map->FtpFile
                              :columns [(field :field :ftp_file_name :constructor-var :file-name :component-type [:text])
                                        (field :field :ftp_file_path :constructor-var :file-path :component-type [:text])])])}
          ;;(first (return-metadata :seal))
          ))

  (def ftp-composite-field (nth (return-columns-composite-wrapp s) 2))

  (.group ftp-composite-field {:seal.ftp_file_name "one"})

  (.find-field-by-comp-var s :file-name :seal.loc_file)
  (.find-field-by-comp-var (.find-field-qualified s :seal.loc_file) :file-name :seal.loc_file)

  (.find-field-qualified s :seal.loc_file)

  (.ungroup s {:seal.seal_number "jj", :seal.datetime_of_use "2021-08-31", :seal.datetime_of_remove "2021-09-25", :seal.site {:seal.site #jarman.logic.composite_components.Link{:text "kk", :link "kkk"}}, :seal.loc_file {:seal.loc_file #jarman.logic.composite_components.File{:file-name "test.txt", :file "/home/julia/test.txt"}}, :seal.ftp_file {:seal.ftp_file #jarman.logic.composite_components.FtpFile{:login "kjjj", :password "mm", :file-name "test.txt", :file-path "/home/julia/test.txt"}}})
  
  (.find-field s :site)
  (.find-field-qualified s :seal.site)
  (.find-field ftp-composite-field :ftp_login)

  (.return-columns-flatten s)
  (.return-columns-flatten-wrapp s)
  (.return-columns-join s)
  (.return-columns-join-wrapp s)
  (.return-columns-composite s)
  (.return-columns-composite-wrapp s)
  (.return-columns s)
  (.return-columns-wrapp s)
  (.group (.find-field-qualified s :seal.site) {:seal.site_name "ddd"})

  (let [name :seal.site]
    (first (filter (comp not nil?) (map (fn [item] (if (= name (:field-qualified item))
                                                     (vec (map (fn [column] [(:field-qualified column) (:component-type column)]) (:columns item))) nil)) (.return-columns-composite s)))))
  ;;; ------------------
  ;;; return all columsn 
  (map #(.return-field-qualified %) (.return-columns-flatten-wrapp s))
  ;;; quick select data
  (def s-e (first (db/query (select! 
                             {:table_name :seal,
                              :column
                              [:#as_is :seal.seal_number :seal.datetime_of_use :seal.datetime_of_remove :seal.site_name :seal.site_url :seal.file_name :seal.file  :seal.ftp_file_name :seal.ftp_file_path]}))))
  (.ungroup s (.group s s-e))
  
  (.ungroup s (.group s {:text "ff"}))
  (.ungroup s {:seal.site #jarman.logic.composite_components.Link{:text nil, :link nil}, :seal.loc_file #jarman.logic.composite_components.File{:file-name nil, :file nil}, :seal.ftp_file #jarman.logic.composite_components.FtpFile{:login nil, :password nil, :file-name nil, :file-path nil}})
  (.ungroup s  {:seal.ftp_file {:login "fj"}, :seal.loc_file {:file-name "k"}}))

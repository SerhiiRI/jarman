;;;heyyy
;; (first (getset "user"))

;; (create-all-table-by-meta ["user" "permission"])

;; (do-create-meta-database)
;; (do-clear-meta)

 ;; (let [meta (db/query (select :metadata :where [:= :metadata.table table-name]))]
 ;;    (if (empty? meta)
 ;;      (db/exec (update-sql-by-id-template "metadata" (get-meta table-name)))))

;;(db/connection-get)



;;; TODO unit test
;; (create-table-by-meta (first (getset "user")))
;; (create-table :user
;;               :columns [{:login [:varchar-100 :nnull]}
;;                         {:password [:varchar-100 :nnull]}
;;                         {:first_name [:varchar-100 :nnull]}
;;                         {:last_name [:varchar-100 :nnull]}
;;                         {:id_permission [:bigint-120-unsigned :nnull]}]
;;               :foreign-keys [{:id_permission :permission} {:delete :cascade :update :cascade}])
;; ----- CREATE TABLE 
;; ----- From script 
;; => "CREATE TABLE IF NOT EXISTS `user` (`id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT, `login` VARCHAR(100) NOT NULL, `password` VARCHAR(100) NOT NULL, `first_name` VARCHAR(100) NOT NULL, `last_name` VARCHAR(100) NOT NULL, `id_permission` BIGINT(120) UNSIGNED NOT NULL, PRIMARY KEY (`id`), KEY `user17738` (`id_permission`), CONSTRAINT `user17738` FOREIGN KEY (`id_permission`) REFERENCES `permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;"
;; ----- From meta
;; => "CREATE TABLE IF NOT EXISTS `user` (`id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT, `login` VARCHAR(100) NOT NULL, `password` VARCHAR(100) NOT NULL, `first_name` VARCHAR(100) NOT NULL, `last_name` VARCHAR(100) NOT NULL, `id_permission` BIGINT(120) UNSIGNED NOT NULL, PRIMARY KEY (`id`), KEY `user17760` (`id_permission`), CONSTRAINT `user17760` FOREIGN KEY (`id_permission`) REFERENCES `permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;"

;; (create-table :point_of_sale_group_links
;;               :columns [{:id_point_of_sale_group [:bigint-20-unsigned :default :null]}
;;                         {:id_point_of_sale [:bigint-20-unsigned :default :null]}]
;;               :foreign-keys [[{:id_point_of_sale_group :point_of_sale_group} {:delete :cascade :update :cascade}]
;;                              [{:id_point_of_sale :point_of_sale}]])
;; (create-table-by-meta (first (getset "point_of_sale_group_links")))
;; ----- CREATE TABLE 
;; ----- From script
;; => "CREATE TABLE IF NOT EXISTS `point_of_sale_group_links` (`id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT, `id_point_of_sale_group` BIGINT(20) UNSIGNED DEFAULT NULL, `id_point_of_sale` BIGINT(20) UNSIGNED DEFAULT NULL, PRIMARY KEY (`id`), KEY `point_of_sale_group_links17774` (`id_point_of_sale_group`), CONSTRAINT `point_of_sale_group_links17774` FOREIGN KEY (`id_point_of_sale_group`) REFERENCES `point_of_sale_group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE, KEY `point_of_sale_group_links17775` (`id_point_of_sale`), CONSTRAINT `point_of_sale_group_links17775` FOREIGN KEY (`id_point_of_sale`) REFERENCES `point_of_sale` (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;"
;; ----- From meta
;; => "CREATE TABLE IF NOT EXISTS `point_of_sale_group_links` (`id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT, `id_point_of_sale_group` BIGINT(20) UNSIGNED DEFAULT NULL, `id_point_of_sale` BIGINT(20) UNSIGNED DEFAULT NULL, PRIMARY KEY (`id`), KEY `point_of_sale_group_links17799` (`id_point_of_sale_group`), CONSTRAINT `point_of_sale_group_links17799` FOREIGN KEY (`id_point_of_sale_group`) REFERENCES `point_of_sale_group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE, KEY `point_of_sale_group_links17800` (`id_point_of_sale`), CONSTRAINT `point_of_sale_group_links17800` FOREIGN KEY (`id_point_of_sale`) REFERENCES `point_of_sale` (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;"

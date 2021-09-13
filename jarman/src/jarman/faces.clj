(ns jarman.faces)
(require '[jarman.gui.faces-system :refer [define-face]])

;; s-...   size
;; c-...   color
;; cvv-...  colors in vectors like [[#aaa #bbb] [#ccc #ddd]]

;;; UI elemnt colors
;;; Global default styles
(define-face c-foreground)
(define-face c-caret)

(define-face c-layout-background)
(define-face c-layout-background-light)

(define-face c-compos-background)
(define-face c-compos-background-light)
(define-face c-compos-background-dark)

(define-face c-background-detail)

(define-face c-on-focus)
(define-face c-on-focus-dark)
(define-face c-on-focus-light)
(define-face c-on-focus-detail)

(define-face c-underline)
(define-face c-underline-detail)
(define-face c-underline-light)
(define-face c-underline-on-focus)
(define-face c-underline-on-focus-light)
(define-face s-underline)


;;----------------------------------
;;Custom elements
       
;;; main menu
(define-face c-main-menu-bg)
(define-face c-main-menu-vhr)

;; expand btn lvls colors
(define-face cvv-button-expand)

;; button
(define-face c-btn-bg)
(define-face c-btn-bg-focus)
(define-face c-btn-foreground)
(define-face c-btn-underline)
(define-face c-btn-underline-on-focus)
(define-face s-btn-underline)

;; slider menu
(define-face c-slider-bg)
(define-face c-slider-fg)
(define-face c-slider-bg-on-focus)
(define-face c-slider-underline)
(define-face c-slider-underline-on-focus)

;; menu bars
(define-face c-icon-btn-focus)
(define-face c-menu-bar-on-focus)

;; view service
(define-face c-tab-active)
(define-face c-tabbar-bg)
(define-face c-tab-underline)
(define-face s-tab-underline)

;; Table
(define-face c-table-select-row-fg)
(define-face c-table-select-row-bg)
(define-face c-table-select-cell)

;; Table header
(define-face c-table-header-fg)
(define-face c-table-header-bg)
(define-face c-table-header-border)

;; inputs
(define-face c-input-bg)
(define-face c-input-header)

;; button expand
(define-face c-btn-expand-bg)
(define-face c-btn-expand-fg)
(define-face c-btn-expand-offset)

;; [underscore-panel
;;  button-border-bottom
;;  button-border-top
;;  button-border-right]

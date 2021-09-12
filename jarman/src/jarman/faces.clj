(ns jarman.faces)
(require '[jarman.gui.faces-system :refer [define-face]])

;; s-...   size
;; c-...   color
;; cvv-...  colors in vectors like [[#aaa #bbb] [#ccc #ddd]]

;;; UI elemnt colors
;;; Global styles
(define-face c-foreground)
(define-face c-caret)
(define-face c-layout-background)
(define-face c-compos-background)
(define-face c-on-focus)

;;; components styles
(define-face c-main-menu-vhr)
(define-face c-main-menu-bg)
(define-face cvv-button-expand)
(define-face c-btn-bg)
(define-face c-btn-bg-focus)
(define-face c-btn-foreground)
(define-face c-underline)
(define-face c-underline-on-focus)
(define-face c-underline-on-mouse)

(define-face c-slider-bg)
(define-face c-slider-underline)
(define-face c-slider-underline-on-focus)

(define-face c-menu-bar-on-focus)
(define-face c-tab-active)

(define-face c-icon-btn-focus)

;; Table
(define-face c-table-select-row-fg)
(define-face c-table-select-row-bg)
(define-face c-table-select-cell)

;; Table header
(define-face c-table-header-fg)
(define-face c-table-header-bg)
(define-face c-table-header-border)

(define-face c-input-bg)
(define-face c-input-header)
;;; components sizes
(define-face s-underline)
;; [underscore-panel
;;  button-border-bottom
;;  button-border-top
;;  button-border-right]

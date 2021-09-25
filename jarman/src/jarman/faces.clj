(ns jarman.faces)
(require '[jarman.gui.faces-system :refer [define-face]])

;; s-...   size
;; c-...   color
;; cvv-...  colors in vectors like [[#aaa #bbb] [#ccc #ddd]]

;;; UI elemnt colors
;;; Base color-palette
(define-face c-white)
(define-face c-black)
(define-face c-red)
(define-face c-orange)
(define-face c-yellow)
(define-face c-green)
(define-face c-light-blue)
(define-face c-strong-blue)
(define-face c-purple)

;;; Global default styles
(define-face c-foreground)
(define-face s-foreground)
(define-face c-caret)

(define-face c-layout-background)
(define-face c-layout-background-light)

(define-face c-compos-background)
(define-face c-compos-background-light)
(define-face c-compos-background-dark)
(define-face c-compos-background-darker)

(define-face c-background-detail)

(define-face c-border)
(define-face c-border-warning)
(define-face c-border-danger)
(define-face c-border-success)

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

;; Icon color
(define-face c-icon)
(define-face s-icon)
(define-face c-icon-focus)
(define-face c-icon-close-focus)
(define-face c-icon-info)
(define-face c-icon-warning)
(define-face c-icon-danger)
(define-face c-icon-success)

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

;; item-expand in inputs
(define-face c-item-expand-left)
(define-face c-item-expand-right)

;; min-scrollbox
(define-face c-min-scrollbox-bg)

;; Alert popup
(define-face c-alert-bg)
(define-face c-alert-fg)
(define-face c-alert-alert-border)
(define-face c-alert-warning-border)
(define-face c-alert-danger-border)
(define-face c-alert-timelife)
(define-face c-alert-success-border)
(define-face c-alert-history-focus)

;; Popup box
(define-face c-popup-body-background)
(define-face c-popup-head-background)
(define-face c-popup-border)

(comment [underscore-panel
          button-border-bottom
          button-border-top
          button-border-right])

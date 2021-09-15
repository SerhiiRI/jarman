;; (let ((buffer (get-buffer "test.el")))
;;   (when buffer
;;     (with-current-buffer buffer
;;       (vc-root-dir))))

(defun file-from-root (FILE)
  (concat (vc-git-root ".git") FILE))

(file-from-root "jarman")

;; Org-Log file
;; # -*- mode: org; mode: auto-revert-tail; -*-


(-name "MAIN")
(frame-n)

(let ((title "Debug Printing"))
 (setq my/frame-id (make-frame-command))
 (set-frame-name title)
 (find-file-other-frame ".gitignore" "agenda.org"))
(find-file-name-handler)
(add-hook 'after-make-frame-functions
	  `(lambda (frame)
	     (select-frame frame)
	     (when (display-graphic-p frame)
               (load-theme ,theme t))))

(after-make-frame-)

(find-file)

(selected-frame)
(raise-frame (selected-frame)) 

(setq jarman/debug-frame-name-v "JarmanOut")
(defun jarman/project-debug-frame ()
  (let ((current-frame (selected-frame)))
    (make-frame '((name . )))
    (select-frame-by-name "JarmanOut")
    (find-file "agenda.org")
    (find-file-other-window ".gitignore")
    (raise-frame current-frame)))

	    

(progn
  (find-file "test.el")
  (find-file-other-frame ".gitignore"))



*** create startup frames 
#+BEGIN_SRC emacs-lisp  :results none
(set-frame-name "MAIN")
(make-frame '((name . "FM")))
(make-frame '((name . "TERM"))) 
(make-frame '((name . "ORG")))




#+END_SRC
*** binds for frames 
#+BEGIN_SRC emacs-lisp  :results none
(global-set-key (kbd "s-a") (lambda () (interactive) (select-frame-by-name "MAIN")))
(global-set-key (kbd "s-s") (lambda () (interactive) (select-frame-by-name "TERM")))
(global-set-key (kbd "s-d") (lambda () (interactive) (select-frame-by-name "ORG"))) 
(global-set-key (kbd "s-z") (lambda () (interactive) (select-frame-by-name "FM"))) 
#+END_SRC
*** open in frames 
#+BEGIN_SRC emacs-lisp  :results none
(select-frame-by-name "FM") (dired-jump  "~/")
(select-frame-by-name "TERM") (eshell)
;; initial frame
(select-frame-by-name "MAIN") (find-file "~/org/files/agenda/TODO.org")

#+END_SRC







;;;;;;;;;;
;; dfso ;;
;;;;;;;;;;

(defun jarman-comment-box (b e)
  "Draw a box comment around the region but arrange for the region to extend to at least the fill column. Place the point after the comment box."

  (interactive "r")
  (let ((e (copy-marker e t)))
    (goto-char b)
    (end-of-line)
    (insert-char ?  (- fill-column (current-column)))
    (comment-box b e 1)
    (goto-char e)
    (set-marker e nil)))

(global-set-key (kbd "C-c b b") 'bjm-comment-box)

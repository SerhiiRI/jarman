(defun file-from-root (FILE)
  (concat (vc-git-root ".git") FILE))

(defun jarman-comment-box (start end)
  "Draw a box comment around the region but arrange for the region to extend to at least the fill column. Place the point after the comment box."
  (interactive "r")
  (let ((end-cp (copy-marker end t)))
    (goto-char start)
    ;; (goto-char end)
    ;; (insert-char ?  (- fill-column (current-column)))
    (comment-box start end 1)
    (goto-char end-cp)
    (set-marker end-cp nil)))

;; This function will re-read the dir-locals file and set the new values for the current buffer:
(defun jarman-reload-dir-locals-for-current-buffer ()
  "reload dir locals for the current buffer"
  (interactive)
  (let ((enable-local-variables :all))
    (hack-dir-local-variables-non-file-buffer)))

;; And if you want to reload dir-locals for every buffer in your current buffer's directory:
(defun jarman-reload-dir-locals-for-all-buffer-in-this-directory ()
  "For every buffer with the same `default-directory` as the 
   current buffer's, reload dir-locals."
  (interactive)
  (let ((dir default-directory))
    (dolist (buffer (buffer-list))
      (with-current-buffer buffer
        (when (equal default-directory dir)
          (my-reload-dir-locals-for-current-buffer))))))

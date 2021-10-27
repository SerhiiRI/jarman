(ns jarman.logic.security
  (:require
   [clojure.string :as string]
   [clojure.pprint :refer [cl-format]]
   [clojure.java.io :as io]
   [jarman.config.environment :as env]
   [jarman.tools.lang :refer :all]
   [jarman.tools.org  :refer :all]))

(java.security.Security/addProvider (org.bouncycastle.jce.provider.BouncyCastleProvider.))

;;; keypair functionals ;;; 
(defn- kp-generator [length]
  (doto (java.security.KeyPairGenerator/getInstance "RSA")
    (.initialize length)))
(defn generate-keypair [length]
  (assert (>= length 512) "RSA Key must be at least 512 bits long.")
  (.generateKeyPair (kp-generator length)))

;;; CHAR DECODERS ;;;
(defn decode64 [str]
  (.decode (java.util.Base64/getDecoder) str))
(defn encode64 [bytes]
  (.encodeToString (java.util.Base64/getEncoder) bytes))

(defn encrypt [message public-key]
  "Perform RSA public key encryption of the given message string.
   Returns a Base64-encoded string of the encrypted data."
  (encode64
   (let [cipher (doto (javax.crypto.Cipher/getInstance "RSA/ECB/PKCS1Padding")
                  (.init javax.crypto.Cipher/ENCRYPT_MODE public-key))]
     (.doFinal cipher (.getBytes message)))))

(defn decrypt [message private-key]
  "Use an RSA private key to decrypt a Base64-encoded string
   of ciphertext."
  (let [cipher (doto (javax.crypto.Cipher/getInstance "RSA/ECB/PKCS1Padding")
                 (.init javax.crypto.Cipher/DECRYPT_MODE private-key))]
    (->> message
         decode64
         (.doFinal cipher)
         (map char)
         (apply str))))

(defn sign
  "RSA private key signing of a message. Takes message as string"
  [message private-key]
  (encode64
   (let [msg-data (.getBytes message)
         sig (doto (java.security.Signature/getInstance "SHA256withRSA")
               (.initSign private-key (java.security.SecureRandom.))
               (.update msg-data))]
     (.sign sig))))
(defn verify
  "RSA public key verification of a Base64-encoded signature and an
   assumed source message. Returns true/false if signature is valid."
  [encoded-sig message public-key]
  (let [msg-data (.getBytes message)
        signature (decode64 encoded-sig)
        sig (doto (java.security.Signature/getInstance "SHA256withRSA")
              (.initVerify public-key)
              (.update msg-data))]
    (.verify sig signature)))

(defn- keydata [reader]
 (->> reader
      (org.bouncycastle.openssl.PEMParser.)
      (.readObject)))

(defn pem-string->key-pair [string]
  "Convert a PEM-formatted private key string to a public/private keypair.
   Returns java.security.KeyPair."
  (let [kd (keydata (io/reader (.getBytes string)))]
    (.getKeyPair (org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter.) kd)))

(defn pem-string->pub-key [string]
  "Convert a PEM-formatted public key string to an RSA public key.
   Returns sun.security.rsa.RSAPublicKeyImpl"
  (let [kd (keydata (io/reader (.getBytes string)))
        kf (java.security.KeyFactory/getInstance "RSA")
        spec (java.security.spec.X509EncodedKeySpec. (.getEncoded kd))]
    (.generatePublic kf spec)))

(comment
  (deftest security-encription
    (is (let [keypair      ( generate-keypair 512)
              public-key   (.getPublic keypair)
              private-key  (.getPrivate keypair)
              message      "{:hello \"World\"}"]
          (= message (decrypt (encrypt message public-key) private-key))))))


(let [security-private-key `~(eval (slurp "../security/private-key.pem"))
      security-public-key  `~(eval (slurp "../security/public-key.pem"))]
  
 (defn encrypt-local [message]
   (wlet
    (encrypt message public-key)
    ((public-key (pem-string->pub-key security-public-key)))))

 (defn decrypt-local [message]
   (wlet
    (decrypt message private-key)
    ((private-key (.getPrivate (pem-string->key-pair security-private-key)))))))


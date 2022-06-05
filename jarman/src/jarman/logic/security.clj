(ns jarman.logic.security
  (:require
   [clojure.string :as string]
   [clojure.pprint :refer [cl-format]]
   [clojure.java.io :as io]
   [jarman.config.environment :as env]
   [jarman.lang :refer :all]
   [jarman.org  :refer :all]))

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

(defmacro load-key-string [file-key-path]
  (let [file-key `(do ~(slurp file-key-path))]
   `(eval ~file-key)))

;; (load-key-string "../security/private-key.pem")
;; (load-key-string "../security/public-key.pem")

(let [security-private-key "-----BEGIN RSA PRIVATE KEY-----\r\nMIIEpgIBAAKCAQEAnOXVfoi7m97Jtbe2Q4ABFqRGa0j2l6RRSEJGZ5ZVRA/60nyt\r\nVx/lOcdGetyOByPWmSceUFaFYGHqFEV1WT7tXLt5z2rPprdvnsIGLEjulSfiS8hI\r\nYu1INUugKkAf98zF0NbbrAhJIFg5sH13GOfxOG+Bov33dAKzemCopNkbH5jCKHNC\r\nUyT4wdbHQRxaYIbCVdtUf0TSBcHhJD+ZppPvnpVIm/GjA/uvX2wgw4L1vATntHR/\r\nufh6R1WzG4CGdHD//wzn0nokoiXEVKJx6QM8EoZtjrQiF5N7P+b7GUCAgqu45qCx\r\nVTydepGC2sObRz+zOgSWIIm1GNvhfMxHbufXVwIDAQABAoIBAQCCXzSE3SdPgNOJ\r\nbOtFsYK2BrEBCvLk1MQ5z+BiyPd3A/Q+nR0ITVeTKDQ3eTeHVU9HmcrFpO9VRGdW\r\nitTU4MYpjRrNsIp1lO1qNP0eJUgipq6SnA0SLusWZg3jrb8ikIQ7YZ3NmGDzQHxH\r\nXCnvhyPY6kcFYhUR0ADFCrZ3rHOuT1qr64s2eAOdqAODvx3ds/48qyXPVWx5mPlK\r\nc8v66G8gX/nZNDrSLKgZ9Rei+7jr06L3lncgNnRMFvcUPs45KJcS/79oI0W6dqkL\r\nEVWfTMZ0C1Uxnva8QvtGetnDuLCRshU35y0RLYU6zUO6LBaLUyXk4i8m3Mp5phRx\r\nKDkcbQxJAoGBAM4XbzJOIWGX5Ui384goGK+cQIX/qUvvsQxOvtKuhjHFl3wifEYB\r\n1z9UDCQywk3nhPenK04U9CcvPXA3Ph8GpFQpw4pHmTmh53w668rxRuHtIV3LMkAe\r\nwN5fgJ7pzpobOX1pUrnk1gJVQBbjf21b6UiuvS6lZf1Pu24qw5OXw8K7AoGBAMLk\r\npuTI6npCW232GeW/wc/C8GKjMB4BxQUDGOJQ2R09hoxrGfLJ0WOnjwHo+npRSepP\r\nnBPtOb5tsq1yffjrQm8NPPsuIooMto2g/bttkWyVheMiDpSmShIsM8ICk6zOYBPj\r\ngGJr+XUSOtcu6ZREPlY8W+Bw7LN2obsmYx6Tx7oVAoGBAMAfeorT6SyYtKd4zO+m\r\nKXU0XbZNYt6mgU9u7VdsWS92+0Q+xclhl+6yhGsOYrLLXNqeHbG5uO36jPTamu/D\r\nelf3YxG6s23Vr/3Q09Wp3aFqNUvBzIOX58amEzqRzgc9z/dIw1UGrjcYkBmGVVl2\r\ncGBQfY5JpG1j8d++v/oMZfNXAoGBAKb7L39AIZ4ksp9xR1kQxebTeBCbDwP2si6M\r\nK8rkSFGEQH6Y2P+rKQtAKggR7GHWugTWtrxf+aSN6pQvT31xXFc3uXLgYVl0cQjB\r\n0gO/aeTl63Pi3yk/nKEbXDEy8gISREFmH760EmTa/K0XlauiGOLcDkAroLJWx1VJ\r\nG428QljNAoGBALlXUjrqHqxXiqjQ0Sd5/MVSUpH9sDGno/zGVIT3QoJID+WE6Jke\r\nbA5MShQdx+TOHjgmdM2lkYv+uO3EcJ3BSIiwdpZ22afSMUH1A2k4K+ULoCHfYRYW\r\najr1MXoQzPzuDn+YsoNrT4omGqYBuAfbDMksV6XE8e9NQcS+y5Kjdof4\r\n-----END RSA PRIVATE KEY-----\r\n"
      security-public-key  "-----BEGIN PUBLIC KEY-----\r\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnOXVfoi7m97Jtbe2Q4AB\r\nFqRGa0j2l6RRSEJGZ5ZVRA/60nytVx/lOcdGetyOByPWmSceUFaFYGHqFEV1WT7t\r\nXLt5z2rPprdvnsIGLEjulSfiS8hIYu1INUugKkAf98zF0NbbrAhJIFg5sH13GOfx\r\nOG+Bov33dAKzemCopNkbH5jCKHNCUyT4wdbHQRxaYIbCVdtUf0TSBcHhJD+ZppPv\r\nnpVIm/GjA/uvX2wgw4L1vATntHR/ufh6R1WzG4CGdHD//wzn0nokoiXEVKJx6QM8\r\nEoZtjrQiF5N7P+b7GUCAgqu45qCxVTydepGC2sObRz+zOgSWIIm1GNvhfMxHbufX\r\nVwIDAQAB\r\n-----END PUBLIC KEY-----\r\n"]
 (defn encrypt-local [message]
   (wlet
    (encrypt message public-key)
    ((public-key (pem-string->pub-key security-public-key)))))

 (defn decrypt-local [message]
   (wlet
    (decrypt message private-key)
    ((private-key (.getPrivate (pem-string->key-pair security-private-key)))))))


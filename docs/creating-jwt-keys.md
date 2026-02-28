# Generating Elliptic Curve (EC) Keys for JWT

This guide explains how to generate the cryptographic key pairs required for signing and verifying JSON Web Tokens (JWT) using the **ES256** algorithm (ECDSA with P-256 curve).

## The Generation Workflow

This application is strictly configured to use the **P-256** curve (`prime256v1`). Run these commands using OpenSSL to generate keys in the specific formats required by the Java security providers.

### 1. Generate the Raw EC Private Key
```bash
openssl ecparam -name prime256v1 -genkey -noout -out private_key.pem
```
**Explanation of Parameters:**
*   `ecparam`: The OpenSSL command for Elliptic Curve parameter manipulation.
*   `-name prime256v1`: Specifies the **P-256** curve. This is mandatory as the application code uses `ES256`, which is mathematically tied to this specific curve.
*   `-genkey`: Tells OpenSSL to generate a new private key based on the specified curve parameters.
*   `-noout`: Prevents printing the curve parameters to the console; we only want the key file.
*   `-out private_key.pem`: Saves the resulting private key in PEM (textual) format.

---

### 2. Convert to PKCS#8 Format (Required by Java)
```bash
openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key.pem -nocrypt -out private_key.der
```
**Explanation of Parameters:**
*   `pkcs8`: The command for PKCS#8 format management.
*   `-topk8`: Converts the input key into a PKCS#8 structured key.
*   `-inform PEM`: Specifies that the input file is in the PEM (textual) format generated in step 1.
*   `-outform DER`: Specifies the output should be in **DER** (binary) format. Java's `KeyFactory` and `PKCS8EncodedKeySpec` expect binary DER data.
*   `-nocrypt`: Generates an **unencrypted** PKCS#8 file. This is required because the application loads the key directly from environment variables without a password prompt.
*   `-in` / `-out`: Specifies the input and output file paths.

---

### 3. Extract the Public Key (X.509 Format)
```bash
openssl ec -in private_key.pem -pubout -outform DER -out public_key.der
```
**Explanation of Parameters:**
*   `ec`: The command for processing EC keys.
*   `-pubout`: Instructs OpenSSL to extract only the **public key** from the provided private key.
*   `-outform DER`: Saves the public key in the binary DER format.
*   **X.509 Note:** The `pubout` command automatically uses the **X.509 SubjectPublicKeyInfo** structure, which is the exact format required by Java's `X509EncodedKeySpec`.

---

### 4. Convert to single-line Base64
The application configuration requires these keys to be provided as single-line strings.
```bash
base64 -w 0 private_key.der > private_key_base64.txt
base64 -w 0 public_key.der > public_key_base64.txt
```
**Explanation of Parameters:**
*   `-w 0`: (Critical) Disables line-wrapping. By default, `base64` inserts newlines every 64-76 characters. Newlines will break `application.yml` and environment variable parsing. This flag ensures the entire key is on a **single line**.

> [!CAUTION]
> **CRITICAL: NEVER open `.der` files in a text editor (vi, nano, notepad, etc.).**
> These are binary files. Opening them in a text editor will mangle the cryptographic bytes and add invisible line endings, causing the application to fail with `InvalidKeySpecException`. Only use the `base64` command to process them.

---

## Configuration

Copy the strings from the `.txt` files into your `application.yml` or set them as environment variables:

```yaml
jwt:
  secret-key:
    private: <CONTENT_OF_private_key_base64.txt>
    public: <CONTENT_OF_public_key_base64.txt>
```

---

## Troubleshooting

### Error: `java.security.spec.InvalidKeySpecException: ... not expected 48`
If you see this error during application startup, it means the Java key parser found an ASN.1 `SEQUENCE` tag where it expected a version `INTEGER`.

**The Cause:**
You are providing the raw **SEC1** encoded private key (the output of Step 1) or a **corrupted binary file** (likely from opening it in `vi`) instead of the required **PKCS#8** encoded key (the output of Step 2).

**The Fix:**
Re-run the generation steps and ensure that in **Step 4**, you are base64-encoding the **`private_key.der`** file, and that you **NEVER** opened it in an editor.

---

> **Security Warning:** Never commit `.pem`, `.der`, or `.txt` key files to version control. Always use environment variables or a secure secret manager for production.

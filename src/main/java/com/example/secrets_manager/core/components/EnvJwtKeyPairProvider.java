package com.example.secrets_manager.core.components;

import com.example.secrets_manager.core.services.JwtKeyPairProvider;
import io.jsonwebtoken.io.Decoders;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Production implementation that loads JWT EC keys from environment variables. */
@Component
@Slf4j
@ConditionalOnProperty(name = "jwt.secret-key.source", havingValue = "env", matchIfMissing = true)
public class EnvJwtKeyPairProvider implements JwtKeyPairProvider {

  @Value("${jwt.secret-key.private}")
  private String privateKeyBase64;

  @Value("${jwt.secret-key.public}")
  private String publicKeyBase64;

  @Override
  public KeyPair getKeyPair() {
    try {
      var kf = KeyFactory.getInstance("EC");

      var pkcs8Spec = new PKCS8EncodedKeySpec(Decoders.BASE64.decode(privateKeyBase64.trim()));
      var privateKey = kf.generatePrivate(pkcs8Spec);

      var x509Spec = new X509EncodedKeySpec(Decoders.BASE64.decode(publicKeyBase64.trim()));
      var publicKey = kf.generatePublic(x509Spec);

      return new KeyPair(publicKey, privateKey);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IllegalStateException("Failed to load JWT EC keys: " + e.getMessage(), e);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Invalid Base64 format for JWT EC keys.", e);
    }
  }
}

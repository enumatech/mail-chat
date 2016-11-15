package io.enuma.app.keystoretest;

import android.*;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;

/**
 * Created by llunesu on 14/11/2016.
 */

public final class Keychain {
    CancellationSignal cancellationSignal = new CancellationSignal();

    static int ConfirmRequestId = 1;

    void showAuthenticationScreen(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(null, null);
            if (intent != null)
            {
                //startActivityForResult(intent, ConfirmRequestId);
            }
            return;
        }

        fingerprintManager.authenticate(null, cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {

            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                Log.v("main", "onAuthenticationSucceeded");
            }

            @Override
            public void	onAuthenticationError(int errorCode, CharSequence errString) {
                Log.v("main", "onAuthenticationError");
            }

            @Override
            public void onAuthenticationFailed() {
                Log.v("main", "onAuthenticationFailed");
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                Log.v("main", "onAuthenticationHelp");
            }
        }, null);
    }


    private void generate()
            throws java.security.NoSuchAlgorithmException, NoSuchProviderException,
            InvalidAlgorithmParameterException, InvalidKeyException, KeyStoreException,
            IOException, CertificateException, UnrecoverableKeyException, InvalidKeySpecException, SignatureException {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");

        keyPairGenerator.initialize(
                new KeyGenParameterSpec.Builder(
                        "key1",
                        KeyProperties.PURPOSE_SIGN)
                        .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        // Only permit the private key to be used if the user authenticated
                        // within the last five minutes.
                        .setUserAuthenticationRequired(true)
                        .setUserAuthenticationValidityDurationSeconds(5 * 60)
                        .build());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
    }

    private String sign()
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
            UnrecoverableKeyException, InvalidKeyException, SignatureException, InvalidKeySpecException, NoSuchProviderException {
        // The key pair can also be obtained from the Android Keystore any time as follows:
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey("key1", null);

        KeyFactory factory = KeyFactory.getInstance(privateKey.getAlgorithm(), "AndroidKeyStore");
        KeyInfo keyInfo = factory.getKeySpec(privateKey, KeyInfo.class);
        boolean isit = keyInfo.isInsideSecureHardware();
        PublicKey publicKey = keyStore.getCertificate("key1").getPublicKey();
        byte[] data = privateKey.getEncoded();
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update("sdfg".getBytes("UTF-8"));
        byte[] sig = signature.sign();
        return Base64.encodeToString(sig, 0);
    }

}

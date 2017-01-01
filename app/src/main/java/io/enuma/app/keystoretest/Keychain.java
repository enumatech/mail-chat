package io.enuma.app.keystoretest;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.security.auth.x500.X500Principal;

import static android.util.Base64.NO_WRAP;
import static io.enuma.app.keystoretest.ChatThreadListActivity.bytesToHex;

/**
 * Created by llunesu on 14/11/2016.
 */

public final class Keychain {

    private static final String AndroidKeyStore = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private final static byte[] FIXED_IV = "TmpKeyAnyway".getBytes();//12 bytes
    private static final String RSA_MODE =  "RSA/ECB/PKCS1Padding";

    public static byte[] encryptAES(SecretKey secretKey, byte[] input)
            throws Exception {
        Cipher c = Cipher.getInstance(AES_MODE);
        c.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, FIXED_IV));
        return c.doFinal(input);
    }

    public static byte[] decryptAES(SecretKey secretKey, byte[] encryptedBytes)
            throws Exception {
        Cipher c2 = Cipher.getInstance(AES_MODE);
        c2.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, FIXED_IV));
        return c2.doFinal(encryptedBytes);
    }

    public static byte[] encryptRSA(Key publicKey, byte[] input) throws Exception{
        Cipher inputCipher = Cipher.getInstance(RSA_MODE);//, "AndroidOpenSSL");
        inputCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return inputCipher.doFinal(input);
    }

    public static byte[] decryptRSA(Key privateKey, byte[] input) throws Exception{
        Cipher inputCipher = Cipher.getInstance(RSA_MODE);//, "AndroidOpenSSL");
        inputCipher.init(Cipher.DECRYPT_MODE, privateKey);
        return inputCipher.doFinal(input);
    }


    private static KeyPair generateKeyPair(Context context, String alias)
            throws Exception {
        // Generate a key pair for encryption
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 30);
        KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(alias)
                .setSubject(new X500Principal("CN=" + alias))
                .setSerialNumber(BigInteger.TEN)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, AndroidKeyStore);
        kpg.initialize(spec);
        return kpg.generateKeyPair();
    }


    private static SecretKey generateSecretKey()
            throws Exception {
        KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null);
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // for example
        SecretKey secretKey = keyGen.generateKey();
        //keyStore.setKeyEntry(alias, secretKey, null, null);
        return secretKey;
    }


    private static SecretKey generateSecretKey(String alias)
            throws Exception {

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            // crash
            throw new NoSuchAlgorithmException();
        }
        else {

            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore);
            keyGenerator.init(
                    new KeyGenParameterSpec.Builder(alias,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setRandomizedEncryptionRequired(false)
                            .build());
            return keyGenerator.generateKey();
        }
    }

/*
    private static Key getKeyPair() {
        KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null);
        return keyStore.getKey(alias, null);
    }
*/

    public static final String APPLICATION_TAG_RSA = "Generated preferences RSA key";
    public static final String APPLICATION_TAG_AES = "Generated preferences AES key";


    public static void deleteAllKeys()
            throws Exception {

        KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null);
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            deleteKey(aliases.nextElement());
        }
    }


    public static void deleteKey(String alias)
            throws Exception {

        KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null);
        keyStore.deleteEntry(alias);
    }


    public static SecretKey findOrGenerateSecretKey(String alias)
            throws Exception {

        KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null);

        if (!keyStore.containsAlias(alias)) {

            return generateSecretKey(alias);
        }

        return (SecretKey) keyStore.getKey(alias, null);
    }


    public static Key findOrGenerateKeyPair(String alias, Context context)
            throws Exception {

        KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null);

        if (!keyStore.containsAlias(alias)) {

            return generateKeyPair(context, alias).getPrivate();
        }

        return keyStore.getKey(alias, null);
    }


    public static Key findOrGenerateKey(Context context)
            throws Exception {

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {

            return findOrGenerateKeyPair(APPLICATION_TAG_RSA, context);
        }
        else {

            return findOrGenerateSecretKey(APPLICATION_TAG_AES);
        }
    }


    public static SecretKey getSecretKey(String alias)
            throws Exception {
        KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null);
        return (SecretKey) keyStore.getKey(alias, null);
    }


    public static String decryptString(String base64)
            throws Exception {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {

            PrivateKey secretKey = Keychain.getPrivateKey(APPLICATION_TAG_RSA);
            return new String(Keychain.decryptRSA(secretKey, Base64.decode(base64, 0)), utf8);
        }
        else {

            SecretKey secretKey = Keychain.getSecretKey(APPLICATION_TAG_AES);
            return new String(Keychain.decryptAES(secretKey, Base64.decode(base64, 0)), utf8);
        }
    }


    private static Charset utf8 = Charset.defaultCharset();


    public static String encryptString(String str)
            throws Exception {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {

            PublicKey secretKey = Keychain.getPublicKey(APPLICATION_TAG_RSA);
            return Base64.encodeToString(Keychain.encryptRSA(secretKey, str.getBytes(utf8)), NO_WRAP);
        }
        else {

            SecretKey secretKey = Keychain.getSecretKey(APPLICATION_TAG_AES);
            return Base64.encodeToString(Keychain.encryptAES(secretKey, str.getBytes(utf8)), NO_WRAP);
        }
    }


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


    public static KeyPair generate(String alias )
            throws java.security.NoSuchAlgorithmException, NoSuchProviderException,
            InvalidAlgorithmParameterException, InvalidKeyException, KeyStoreException,
            IOException, CertificateException, UnrecoverableKeyException, InvalidKeySpecException, SignatureException {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");

        keyPairGenerator.initialize(
                new KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_SIGN)
                        .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        // Only permit the private key to be used if the user authenticated
                        // within the last five minutes.
                        .setUserAuthenticationRequired(true)
                        .setUserAuthenticationValidityDurationSeconds(5 * 60)
                        .build());
        return keyPairGenerator.generateKeyPair();
    }

    public static String getPubkeyHash(PublicKey pubkey)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] data = pubkey.getEncoded();
        md.update(data, 0, data.length);
        byte[] sha1hash = md.digest();
        return "0x" + bytesToHex(Arrays.copyOfRange(sha1hash, 12, 20));
    }

    public static PrivateKey getPrivateKey(String alias)
            throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return ((KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, null)).getPrivateKey();
    }

    public static PublicKey getPublicKey(String alias)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return keyStore.getCertificate(alias).getPublicKey();
    }

    public static String sign(String alias, byte[] bytes)
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException,
            UnrecoverableKeyException, InvalidKeyException, SignatureException, InvalidKeySpecException, NoSuchProviderException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, null);

        KeyFactory factory = KeyFactory.getInstance(privateKey.getAlgorithm(), "AndroidKeyStore");
        KeyInfo keyInfo = factory.getKeySpec(privateKey, KeyInfo.class);
        //boolean isit = keyInfo.isInsideSecureHardware(); must be true
        //byte[] data = privateKey.getEncoded(); must be null

        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(bytes);
        byte[] sig = signature.sign();
        return Base64.encodeToString(sig, 0);
    }

}

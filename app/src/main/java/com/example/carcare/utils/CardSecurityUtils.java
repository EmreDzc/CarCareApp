package com.example.carcare.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CardSecurityUtils {

    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Kart numarasını SHA-256 ile hashler
     * @param cardNumber Ham kart numarası (boşluksuz)
     * @return Hashlenmiş kart numarası
     */
    public static String hashCardNumber(String cardNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            // Salt ekleyerek güvenliği artırın (opsiyonel)
            String saltedCardNumber = cardNumber + "CarCareApp2025"; // Sabit salt

            byte[] hashBytes = digest.digest(saltedCardNumber.getBytes());

            // Byte array'i hex string'e çevir
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algoritması bulunamadı", e);
        }
    }

    /**
     * Kart numarasının son 4 hanesini alır
     * @param cardNumber Ham kart numarası
     * @return Son 4 hane
     */
    public static String getLastFourDigits(String cardNumber) {
        String cleanNumber = cardNumber.replace(" ", "");
        if (cleanNumber.length() >= 4) {
            return cleanNumber.substring(cleanNumber.length() - 4);
        }
        return cleanNumber;
    }

    /**
     * Maskelenmiş kart numarası oluşturur
     * @param lastFourDigits Son 4 hane
     * @return Maskelenmiş numara (örn: **** **** **** 1234)
     */
    public static String createMaskedCardNumber(String lastFourDigits) {
        return "**** **** **** " + lastFourDigits;
    }

    /**
     * CVV'yi hashler (opsiyonel güvenlik için)
     * @param cvv CVV kodu
     * @return Hashlenmiş CVV
     */
    public static String hashCvv(String cvv) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            String saltedCvv = cvv + "CVV_SALT_2025";
            byte[] hashBytes = digest.digest(saltedCvv.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algoritması bulunamadı", e);
        }
    }
}
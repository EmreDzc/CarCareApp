package com.example.carcare.utils;

public class CardValidationUtils {

    /**
     * Ödeme sırasında girilen kart numarasının kayıtlı kartla eşleşip eşleşmediğini kontrol eder
     * @param enteredCardNumber Kullanıcının girdiği kart numarası
     * @param storedHashedCardNumber Veritabanında saklanan hashlenmiş kart numarası
     * @return true eğer kartlar eşleşiyorsa
     */
    public static boolean validateCardNumber(String enteredCardNumber, String storedHashedCardNumber) {
        String enteredCardHash = CardSecurityUtils.hashCardNumber(enteredCardNumber.replace(" ", ""));
        return enteredCardHash.equals(storedHashedCardNumber);
    }

    /**
     * CVV doğrulaması yapar
     * @param enteredCvv Kullanıcının girdiği CVV
     * @param storedHashedCvv Veritabanında saklanan hashlenmiş CVV
     * @return true eğer CVV'ler eşleşiyorsa
     */
    public static boolean validateCvv(String enteredCvv, String storedHashedCvv) {
        String enteredCvvHash = CardSecurityUtils.hashCvv(enteredCvv);
        return enteredCvvHash.equals(storedHashedCvv);
    }

    /**
     * Kart numarasının Luhn algoritmasına göre geçerli olup olmadığını kontrol eder
     * @param cardNumber Kontrol edilecek kart numarası
     * @return true eğer geçerliyse
     */
    public static boolean isValidCardNumber(String cardNumber) {
        String cleanNumber = cardNumber.replace(" ", "");

        if (cleanNumber.length() < 13 || cleanNumber.length() > 19) {
            return false;
        }

        // Luhn algoritması
        int sum = 0;
        boolean alternate = false;

        for (int i = cleanNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cleanNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }

    /**
     * CVV formatının doğru olup olmadığını kontrol eder
     * @param cvv CVV kodu
     * @return true eğer geçerliyse
     */
    public static boolean isValidCvv(String cvv) {
        return cvv != null && cvv.matches("\\d{3,4}");
    }

    /**
     * Son kullanma tarihinin geçerli olup olmadığını kontrol eder
     * @param month Ay (01-12)
     * @param year Yıl (YYYY formatında)
     * @return true eğer geçerliyse
     */
    public static boolean isValidExpiryDate(String month, String year) {
        try {
            int monthInt = Integer.parseInt(month);
            int yearInt = Integer.parseInt(year);

            if (monthInt < 1 || monthInt > 12) {
                return false;
            }

            java.util.Calendar now = java.util.Calendar.getInstance();
            int currentYear = now.get(java.util.Calendar.YEAR);
            int currentMonth = now.get(java.util.Calendar.MONTH) + 1; // Calendar.MONTH is 0-based

            if (yearInt < currentYear) {
                return false;
            }

            if (yearInt == currentYear && monthInt < currentMonth) {
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
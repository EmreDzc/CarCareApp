package com.example.carcare.models;

import com.google.firebase.firestore.Exclude;

public class CardModel {
    @Exclude
    private String documentId;

    private String cardName;
    private String cardHolderName;
    private String maskedCardNumber; // Örn: **** **** **** 1234
    private String lastFourDigits;
    private String expiryMonth;
    private String expiryYear;
    private String cardType; // VISA, MASTERCARD etc.
    private boolean masterpassOptIn;
    private boolean isDefault;
    private String bankName; // Yeni alan


    public CardModel() {}

    // Tüm alanlar için Getter ve Setter'lar
    @Exclude
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }

    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

    public String getMaskedCardNumber() { return maskedCardNumber; }
    public void setMaskedCardNumber(String maskedCardNumber) { this.maskedCardNumber = maskedCardNumber; }

    public String getLastFourDigits() { return lastFourDigits; }
    public void setLastFourDigits(String lastFourDigits) { this.lastFourDigits = lastFourDigits; }

    public String getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(String expiryMonth) { this.expiryMonth = expiryMonth; }

    public String getExpiryYear() { return expiryYear; }
    public void setExpiryYear(String expiryYear) { this.expiryYear = expiryYear; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public boolean isMasterpassOptIn() { return masterpassOptIn; }
    public void setMasterpassOptIn(boolean masterpassOptIn) { this.masterpassOptIn = masterpassOptIn; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
}
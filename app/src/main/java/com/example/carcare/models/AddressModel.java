package com.example.carcare.models;

import com.google.firebase.firestore.Exclude; // Firestore'a kaydedilmeyecek alanlar için

public class AddressModel {
    @Exclude // Bu alan Firestore'a kaydedilmeyecek, sadece kod içinde kullanılacak
    private String documentId;

    private String title;
    private String province;
    private String district;
    private String neighborhood;
    private String street;
    private String buildingNo;
    private String floorNo;
    private String doorNo;
    private String description;
    private String addressType;
    private boolean isDefaultAddress;


    // Sadece teslimat adresleri için
    private String recipientName;
    private String recipientSurname;
    private String recipientPhone;

    // Firestore için public boş constructor gerekli
    public AddressModel() {}

    // Getter ve Setter'lar
    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getBuildingNo() {
        return buildingNo;
    }

    public void setBuildingNo(String buildingNo) {
        this.buildingNo = buildingNo;
    }

    public String getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(String floorNo) {
        this.floorNo = floorNo;
    }

    public String getDoorNo() {
        return doorNo;
    }

    public void setDoorNo(String doorNo) {
        this.doorNo = doorNo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientSurname() {
        return recipientSurname;
    }

    public void setRecipientSurname(String recipientSurname) {
        this.recipientSurname = recipientSurname;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public boolean isDefaultAddress() {
        return isDefaultAddress;
    }

    public void setDefaultAddress(boolean defaultAddress) {
        isDefaultAddress = defaultAddress;
    }

    // Kolay gösterim için (opsiyonel)
    public String getFullAddressLine() {
        StringBuilder sb = new StringBuilder();
        if (street != null && !street.isEmpty()) sb.append(street);
        if (buildingNo != null && !buildingNo.isEmpty()) sb.append(" No:").append(buildingNo);
        if (doorNo != null && !doorNo.isEmpty()) sb.append(" D:").append(doorNo);
        if (floorNo != null && !floorNo.isEmpty()) sb.append(" Kat:").append(floorNo);
        if (sb.length() > 0) sb.append("\n");
        if (neighborhood != null && !neighborhood.isEmpty()) sb.append(neighborhood);
        if (district != null && !district.isEmpty()) {
            if (sb.length() > 0 && neighborhood != null) sb.append(" / "); else if (sb.length() > 0) sb.append(" ");
            sb.append(district);
        }
        if (province != null && !province.isEmpty()) {
            if (sb.length() > 0 && (district != null || neighborhood != null)) sb.append(" / "); else if (sb.length() > 0) sb.append(" ");
            sb.append(province);
        }
        return sb.toString();
    }

    public String getAddressLine1() { // Örneğin tv_address_line için
        StringBuilder sb = new StringBuilder();
        if (getStreet() != null && !getStreet().isEmpty()) sb.append(getStreet());
        if (getBuildingNo() != null && !getBuildingNo().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("No:").append(getBuildingNo());
        }
        if (getDoorNo() != null && !getDoorNo().isEmpty()) {
            if (sb.length() > 0 && getBuildingNo() != null) sb.append(" D:").append(getDoorNo());
            else if (sb.length() > 0) sb.append(", D:").append(getDoorNo());
            else sb.append("D:").append(getDoorNo());
        }
        if (getFloorNo() != null && !getFloorNo().isEmpty()) {
            if (sb.length() > 0) sb.append(" Kat:").append(getFloorNo());
            else sb.append("Kat:").append(getFloorNo());
        }
        return sb.toString();
    }

    public String getAddressLine2() { // Örneğin tv_address_district için
        StringBuilder sb = new StringBuilder();
        if (getNeighborhood() != null && !getNeighborhood().isEmpty()) sb.append(getNeighborhood());
        if (getDistrict() != null && !getDistrict().isEmpty()) {
            if (sb.length() > 0 && getNeighborhood() != null) sb.append(" / "); else if (sb.length() > 0) sb.append(" ");
            sb.append(getDistrict());
        }
        if (getProvince() != null && !getProvince().isEmpty()) {
            if (sb.length() > 0 && (getDistrict() != null || getNeighborhood() != null)) sb.append(", "); else if (sb.length() > 0) sb.append(" ");
            sb.append(getProvince());
        }
        return sb.toString();
    }
}
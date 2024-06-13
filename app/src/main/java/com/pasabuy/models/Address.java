package com.pasabuy.models;

import java.util.Objects;

public class Address {
    private String addressID;
    private String houseStreet;
    private String barangay;
    private String municipality;
    private String province;

    public Address() {
        // Default constructor required for calls to DataSnapshot.getValue(Address.class)
    }

    public Address(String addressID, String houseStreet, String barangay, String municipality, String province) {
        this.addressID = addressID;
        this.houseStreet = houseStreet;
        this.barangay = barangay;
        this.municipality = municipality;
        this.province = province;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(addressID, address.addressID) &&
                Objects.equals(houseStreet, address.houseStreet) &&
                Objects.equals(barangay, address.barangay) &&
                Objects.equals(municipality, address.municipality) &&
                Objects.equals(province, address.province);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressID, houseStreet, barangay, municipality, province);
    }
    public String getAddressID() {
        return addressID;
    }

    public void setAddressID(String addressID) {
        this.addressID = addressID;
    }

    public String getHouseStreet() {
        return houseStreet;
    }

    public void setHouseStreet(String houseStreet) {
        this.houseStreet = houseStreet;
    }

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public String getMunicipality() {
        return municipality;
    }

    public void setMunicipality(String municipality) {
        this.municipality = municipality;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }
}

package com.premisave.property.entity;

import lombok.Data;

@Data
public class EmergencyContact {

    private String fullName;
    private String relationship;
    private String phoneNumber;
    private String email;
}
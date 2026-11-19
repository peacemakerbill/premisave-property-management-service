package com.premisave.property.dto.response;

/** What an owner sees when searching for a tenant: enough to recognise the person, with contact details partly hidden. */
public record TenantSummary(String id, String fullName, String email, String phoneNumber, Boolean isBlacklisted) {
}

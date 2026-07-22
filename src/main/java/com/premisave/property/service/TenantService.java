package com.premisave.property.service;

import com.premisave.property.client.AuthServiceClient;
import com.premisave.property.dto.request.TenantRegistrationRequest;
import com.premisave.property.dto.request.UpdateTenantRequest;
import com.premisave.property.dto.response.AddressResponse;
import com.premisave.property.dto.response.ProfileSyncStatusResponse;
import com.premisave.property.dto.response.TenantResponse;
import com.premisave.property.dto.response.UserDto;
import com.premisave.property.entity.Address;
import com.premisave.property.entity.Tenant;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.repository.TenantRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AuthServiceClient authServiceClient;

    @Transactional
    public TenantResponse registerTenant(TenantRegistrationRequest request, String userId) {
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new BadRequestException("fullName is required");
        }
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            throw new BadRequestException("phoneNumber is required");
        }

        tenantRepository.findByUserId(userId).ifPresent(existing -> {
            throw new ConflictException("A tenant profile already exists for this user");
        });

        if (request.getIdNumber() != null) {
            tenantRepository.findByIdNumber(request.getIdNumber()).ifPresent(existing -> {
                throw new ConflictException("A tenant with this ID number is already registered");
            });
        }
        tenantRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            throw new ConflictException("A tenant with this email is already registered");
        });

        Tenant tenant = new Tenant();
        tenant.setUserId(userId);
        tenant.setFullName(request.getFullName());
        tenant.setPhoneNumber(request.getPhoneNumber());
        tenant.setEmail(request.getEmail());
        tenant.setIdNumber(request.getIdNumber());
        tenant.setCurrentAddress(toAddress(request.getCurrentAddress()));
        tenant.setIsActive(true);
        tenant.setIsBlacklisted(false);

        return toResponse(tenantRepository.save(tenant));
    }

    /**
     * One-click tenant registration — pulls fullName/phoneNumber/email
     * directly from the user's auth-service account (via their own JWT,
     * forwarded to auth-service's /profile/me) instead of requiring the
     * person to retype them. idNumber/occupation/currentAddress aren't
     * collected here (auth-service doesn't have idNumber, and they're all
     * optional on Tenant); add them afterward via updateTenantByUserId().
     */
    @Transactional
    public TenantResponse quickRegisterTenant(String userId, String authHeader) {
        tenantRepository.findByUserId(userId).ifPresent(existing -> {
            throw new ConflictException("A tenant profile already exists for this user");
        });

        UserDto authUser = fetchAuthUser(authHeader);
        requireCompleteAuthProfile(authUser);

        if (authUser.getEmail() != null) {
            tenantRepository.findByEmail(authUser.getEmail()).ifPresent(existing -> {
                throw new ConflictException("A tenant with this email is already registered");
            });
        }

        Tenant tenant = new Tenant();
        tenant.setUserId(userId);
        tenant.setFullName(authUser.getFullName());
        tenant.setPhoneNumber(authUser.getPhoneNumber());
        tenant.setEmail(authUser.getEmail());
        tenant.setIsActive(true);
        tenant.setIsBlacklisted(false);

        return toResponse(tenantRepository.save(tenant));
    }

    /**
     * Overwrites fullName/phoneNumber/email on the tenant profile to match
     * whatever is currently on file in auth-service. Auth-service is
     * always treated as the source of truth for these fields — any local
     * edits made directly on the tenant profile are discarded on sync.
     */
    @Transactional
    public TenantResponse syncTenantWithAuthProfile(String userId, String authHeader) {
        Tenant tenant = findTenantByUserIdOrThrow(userId);

        UserDto authUser = fetchAuthUser(authHeader);
        requireCompleteAuthProfile(authUser);

        tenant.setFullName(authUser.getFullName());
        tenant.setPhoneNumber(authUser.getPhoneNumber());
        tenant.setEmail(authUser.getEmail());

        return toResponse(tenantRepository.save(tenant));
    }

    /**
     * Lets the frontend check whether the tenant profile has drifted from
     * auth-service before prompting the user to sync, rather than syncing
     * unconditionally on every page load.
     */
    public ProfileSyncStatusResponse checkTenantSyncStatus(String userId, String authHeader) {
        Tenant tenant = findTenantByUserIdOrThrow(userId);
        UserDto authUser = fetchAuthUser(authHeader);

        List<String> outOfSync = new ArrayList<>();
        if (!Objects.equals(tenant.getFullName(), authUser.getFullName())) {
            outOfSync.add("fullName");
        }
        if (!Objects.equals(tenant.getPhoneNumber(), authUser.getPhoneNumber())) {
            outOfSync.add("phoneNumber");
        }
        if (!Objects.equals(tenant.getEmail(), authUser.getEmail())) {
            outOfSync.add("email");
        }

        ProfileSyncStatusResponse response = new ProfileSyncStatusResponse();
        response.setInSync(outOfSync.isEmpty());
        response.setOutOfSyncFields(outOfSync);
        response.setLatestFullName(authUser.getFullName());
        response.setLatestPhoneNumber(authUser.getPhoneNumber());
        response.setLatestEmail(authUser.getEmail());
        return response;
    }

    /**
     * Fetches the caller's own profile from auth-service by forwarding
     * their JWT to /profile/me — auth-service enforces its own
     * authentication on this call rather than us trusting a shared
     * internal API key.
     */
    private UserDto fetchAuthUser(String authHeader) {
        UserDto authUser = authServiceClient.getMyProfile(authHeader);
        if (authUser == null) {
            throw new ResourceNotFoundException("User account not found in auth service");
        }
        return authUser;
    }

    // Defensive guard — quick-register/sync bypass TenantRegistrationRequest's
    // own manual validation entirely, so this re-checks the same invariant
    // against whatever auth-service actually returned.
    private void requireCompleteAuthProfile(UserDto authUser) {
        if (authUser.getFullName() == null || authUser.getFullName().isBlank()) {
            throw new BadRequestException(
                    "Your account is missing a full name. Please update your profile before continuing.");
        }
        if (authUser.getPhoneNumber() == null || authUser.getPhoneNumber().isBlank()) {
            throw new BadRequestException(
                    "Your account is missing a phone number. Please update your profile before continuing.");
        }
    }

    public TenantResponse getTenantById(String id) {
        return toResponse(findTenantOrThrow(id));
    }

    public TenantResponse getTenantByUserId(String userId) {
        return toResponse(findTenantByUserIdOrThrow(userId));
    }

    @Transactional
    public TenantResponse updateTenantByUserId(UpdateTenantRequest request, String userId) {
        Tenant tenant = findTenantByUserIdOrThrow(userId);

        if (request.getFullName() != null) {
            tenant.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            tenant.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmail() != null) {
            tenant.setEmail(request.getEmail());
        }
        if (request.getOccupation() != null) {
            tenant.setOccupation(request.getOccupation());
        }

        return toResponse(tenantRepository.save(tenant));
    }

    // Called by BlacklistedTenantService to keep the flag on Tenant in sync
    @Transactional
    public void setBlacklisted(String tenantId, boolean blacklisted) {
        Tenant tenant = findTenantOrThrow(tenantId);
        tenant.setIsBlacklisted(blacklisted);
        tenantRepository.save(tenant);
    }

    private Tenant findTenantOrThrow(String id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    private Tenant findTenantByUserIdOrThrow(String userId) {
        return tenantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant profile not found"));
    }

    private Address toAddress(com.premisave.property.dto.request.AddressRequest request) {
        if (request == null) {
            return null;
        }
        Address address = new Address();
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setPostalCode(request.getPostalCode());
        return address;
    }

    private AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        AddressResponse response = new AddressResponse();
        response.setStreet(address.getStreet());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setCountry(address.getCountry());
        response.setPostalCode(address.getPostalCode());
        response.setLandmark(address.getLandmark());
        return response;
    }

    private TenantResponse toResponse(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setUserId(tenant.getUserId());
        response.setFullName(tenant.getFullName());
        response.setPhoneNumber(tenant.getPhoneNumber());
        response.setEmail(tenant.getEmail());
        response.setIdNumber(tenant.getIdNumber());
        response.setOccupation(tenant.getOccupation());
        response.setCurrentAddress(toAddressResponse(tenant.getCurrentAddress()));
        response.setIsActive(tenant.getIsActive());
        response.setIsBlacklisted(tenant.getIsBlacklisted());
        response.setCreatedAt(tenant.getCreatedAt());
        response.setUpdatedAt(tenant.getUpdatedAt());
        return response;
    }
}
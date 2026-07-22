package com.premisave.property.service;

import com.premisave.property.client.AuthServiceClient;
import com.premisave.property.dto.request.AddressRequest;
import com.premisave.property.dto.request.BankDetailsRequest;
import com.premisave.property.dto.request.CreateOwnerRequest;
import com.premisave.property.dto.request.UpdateOwnerRequest;
import com.premisave.property.dto.response.AddressResponse;
import com.premisave.property.dto.response.BankDetailsResponse;
import com.premisave.property.dto.response.OwnerResponse;
import com.premisave.property.dto.response.ProfileSyncStatusResponse;
import com.premisave.property.dto.response.UserDto;
import com.premisave.property.entity.Address;
import com.premisave.property.entity.BankDetails;
import com.premisave.property.entity.Owner;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.exception.UnauthorizedException;
import com.premisave.property.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final AuthServiceClient authServiceClient;

    // Internal API key for server-to-server calls to auth-service (see
    // AuthServiceClient's /internal/** endpoints). Passed explicitly per
    // call rather than via a Feign interceptor.
    @Value("${app.api-key}")
    private String internalApiKey;

    @Transactional
    public OwnerResponse createOwner(CreateOwnerRequest request, String userId) {
        ownerRepository.findByUserId(userId).ifPresent(existing -> {
            throw new ConflictException("Owner profile already exists for this user");
        });

        Owner owner = new Owner();
        owner.setUserId(userId);
        owner.setFullName(request.getFullName());
        owner.setPhoneNumber(request.getPhoneNumber());
        owner.setEmail(request.getEmail());
        owner.setAddress(toAddress(request.getAddress()));
        owner.setIsActive(true);
        owner.setIsVerified(false);

        return toResponse(ownerRepository.save(owner));
    }

    /**
     * One-click owner profile creation — pulls fullName/phoneNumber/email
     * directly from the user's auth-service account instead of requiring
     * the person to retype them. Bank details aren't collected here; add
     * them afterward via updateOwner().
     */
    @Transactional
    public OwnerResponse quickCreateOwner(String userId) {
        ownerRepository.findByUserId(userId).ifPresent(existing -> {
            throw new ConflictException("Owner profile already exists for this user");
        });

        UserDto authUser = fetchAuthUser(userId);
        requireCompleteAuthProfile(authUser);

        Owner owner = new Owner();
        owner.setUserId(userId);
        owner.setFullName(authUser.getFullName());
        owner.setPhoneNumber(authUser.getPhoneNumber());
        owner.setEmail(authUser.getEmail());
        owner.setIsActive(true);
        owner.setIsVerified(false);

        return toResponse(ownerRepository.save(owner));
    }

    /**
     * Overwrites fullName/phoneNumber/email on the owner profile to match
     * whatever is currently on file in auth-service. Auth-service is
     * always treated as the source of truth for these fields — any local
     * edits made directly on the owner profile are discarded on sync.
     */
    @Transactional
    public OwnerResponse syncOwnerWithAuthProfile(String userId) {
        Owner owner = ownerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found"));

        UserDto authUser = fetchAuthUser(userId);
        requireCompleteAuthProfile(authUser);

        owner.setFullName(authUser.getFullName());
        owner.setPhoneNumber(authUser.getPhoneNumber());
        owner.setEmail(authUser.getEmail());

        return toResponse(ownerRepository.save(owner));
    }

    /**
     * Lets the frontend check whether the owner profile has drifted from
     * auth-service before prompting the user to sync, rather than syncing
     * unconditionally on every page load.
     */
    public ProfileSyncStatusResponse checkOwnerSyncStatus(String userId) {
        Owner owner = ownerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found"));

        UserDto authUser = fetchAuthUser(userId);

        List<String> outOfSync = new ArrayList<>();
        if (!Objects.equals(owner.getFullName(), authUser.getFullName())) {
            outOfSync.add("fullName");
        }
        if (!Objects.equals(owner.getPhoneNumber(), authUser.getPhoneNumber())) {
            outOfSync.add("phoneNumber");
        }
        if (!Objects.equals(owner.getEmail(), authUser.getEmail())) {
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

    private UserDto fetchAuthUser(String userId) {
        UserDto authUser = authServiceClient.getUserById(userId, internalApiKey);
        if (authUser == null) {
            throw new ResourceNotFoundException("User account not found in auth service");
        }
        return authUser;
    }

    // Defensive guard — quick-create/sync bypass CreateOwnerRequest's own
    // @NotBlank validation entirely, so this re-checks the same invariant
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

    public OwnerResponse getOwnerById(String id) {
        return toResponse(findOwnerOrThrow(id));
    }

    public OwnerResponse getOwnerByUserId(String userId) {
        return ownerRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found"));
    }

    public boolean existsByUserId(String userId) {
        return ownerRepository.findByUserId(userId).isPresent();
    }

    @Transactional
    public OwnerResponse updateOwner(String id, UpdateOwnerRequest request, String userId) {
        Owner owner = findOwnerOrThrow(id);

        if (!owner.getUserId().equals(userId)) {
            throw new UnauthorizedException("You do not have access to this owner profile");
        }

        if (request.getFullName() != null) {
            owner.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            owner.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmail() != null) {
            owner.setEmail(request.getEmail());
        }
        if (request.getAddress() != null) {
            owner.setAddress(toAddress(request.getAddress()));
        }
        if (request.getBankDetails() != null) {
            owner.setBankDetails(toBankDetails(request.getBankDetails()));
        }

        return toResponse(ownerRepository.save(owner));
    }

    private Owner findOwnerOrThrow(String id) {
        return ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));
    }

    private Address toAddress(AddressRequest request) {
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

    private BankDetails toBankDetails(BankDetailsRequest request) {
        BankDetails bankDetails = new BankDetails();
        bankDetails.setBankName(request.getBankName());
        bankDetails.setAccountNumber(request.getAccountNumber());
        bankDetails.setAccountName(request.getAccountName());
        bankDetails.setBranchCode(request.getBranchCode());
        return bankDetails;
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

    private BankDetailsResponse toBankDetailsResponse(BankDetails bankDetails) {
        if (bankDetails == null) {
            return null;
        }
        BankDetailsResponse response = new BankDetailsResponse();
        response.setBankName(bankDetails.getBankName());
        response.setAccountName(bankDetails.getAccountName());
        response.setAccountNumberMasked(maskAccountNumber(bankDetails.getAccountNumber()));
        return response;
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }
        return "*".repeat(accountNumber.length() - 4) + accountNumber.substring(accountNumber.length() - 4);
    }

    private OwnerResponse toResponse(Owner owner) {
        OwnerResponse response = new OwnerResponse();
        response.setId(owner.getId());
        response.setFullName(owner.getFullName());
        response.setPhoneNumber(owner.getPhoneNumber());
        response.setEmail(owner.getEmail());
        response.setAddress(toAddressResponse(owner.getAddress()));
        response.setBankDetails(toBankDetailsResponse(owner.getBankDetails()));
        response.setIsActive(owner.getIsActive());
        response.setIsVerified(owner.getIsVerified());
        return response;
    }
}
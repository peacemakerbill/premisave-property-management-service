package com.premisave.property.service;

import com.premisave.property.dto.request.AddressRequest;
import com.premisave.property.dto.request.BankDetailsRequest;
import com.premisave.property.dto.request.CreateOwnerRequest;
import com.premisave.property.dto.request.UpdateOwnerRequest;
import com.premisave.property.dto.response.AddressResponse;
import com.premisave.property.dto.response.BankDetailsResponse;
import com.premisave.property.dto.response.OwnerResponse;
import com.premisave.property.entity.Address;
import com.premisave.property.entity.BankDetails;
import com.premisave.property.entity.Owner;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.exception.UnauthorizedException;
import com.premisave.property.repository.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OwnerService {

    private final OwnerRepository ownerRepository;

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
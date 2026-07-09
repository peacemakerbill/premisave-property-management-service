package com.premisave.property.service;

import com.premisave.property.dto.request.AddressRequest;
import com.premisave.property.dto.request.CreatePropertyRequest;
import com.premisave.property.dto.request.UpdatePropertyRequest;
import com.premisave.property.dto.response.AddressResponse;
import com.premisave.property.dto.response.PropertyResponse;
import com.premisave.property.entity.Address;
import com.premisave.property.entity.GeoLocation;
import com.premisave.property.entity.Property;
import com.premisave.property.enums.LeaseStatus;
import com.premisave.property.enums.UnitStatus;
import com.premisave.property.exception.BadRequestException;
import com.premisave.property.exception.ConflictException;
import com.premisave.property.exception.ResourceNotFoundException;
import com.premisave.property.exception.UnauthorizedException;
import com.premisave.property.repository.LeaseRepository;
import com.premisave.property.repository.PropertyRepository;
import com.premisave.property.repository.RentalUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final RentalUnitRepository rentalUnitRepository;
    private final LeaseRepository leaseRepository;

    @Transactional
    public PropertyResponse createProperty(CreatePropertyRequest request, String ownerId) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("title is required");
        }
        if (request.getRegistrationNumber() == null || request.getRegistrationNumber().isBlank()) {
            throw new BadRequestException("registrationNumber is required");
        }
        if (request.getRegistrationType() == null) {
            throw new BadRequestException("registrationType is required");
        }

        if (propertyRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new ConflictException(
                    "A property with this registration number is already registered");
        }

        Property property = new Property();
        property.setOwnerId(ownerId);
        property.setTitle(request.getTitle());
		property.setDescription(request.getDescription());
		property.setPropertyType(request.getPropertyType());
        property.setAddress(toAddress(request.getAddress()));
        property.setRegistrationNumber(request.getRegistrationNumber());
        property.setRegistrationType(request.getRegistrationType());

        if (request.getLatitude() != null && request.getLongitude() != null) {
            GeoLocation location = new GeoLocation();
            location.setLatitude(request.getLatitude());
            location.setLongitude(request.getLongitude());
            property.setLocation(location);
        }

        property.setTotalUnits(0);
        property.setAvailableUnits(0);
        property.setIsActive(true);

        return toResponse(propertyRepository.save(property));
    }

    public List<PropertyResponse> getOwnerProperties(String ownerId) {
        return propertyRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public PropertyResponse getPropertyById(String id) {
        return toResponse(findPropertyOrThrow(id));
    }

    @Transactional
    public PropertyResponse updateProperty(String id, UpdatePropertyRequest request, String ownerId) {
        Property property = findPropertyOrThrow(id);

        if (!property.getOwnerId().equals(ownerId)) {
            throw new UnauthorizedException("You do not have access to this property");
        }

        if (request.getTitle() != null) {
            property.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            property.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            property.setIsActive(request.getIsActive());
        }

        return toResponse(propertyRepository.save(property));
    }

    @Transactional
    public void deleteProperty(String id, String ownerId) {
        Property property = findPropertyOrThrow(id);

        if (!property.getOwnerId().equals(ownerId)) {
            throw new UnauthorizedException("You do not have access to this property");
        }

        boolean hasOccupiedUnits = rentalUnitRepository.findByPropertyId(id).stream()
                .anyMatch(unit -> unit.getStatus() == UnitStatus.OCCUPIED);

        if (hasOccupiedUnits) {
            throw new ConflictException(
                    "This property has occupied units and cannot be deleted. Terminate active leases first.");
        }

        if (leaseRepository.existsByPropertyIdAndStatus(id, LeaseStatus.ACTIVE)) {
            throw new ConflictException(
                    "This property has an active lease and cannot be deleted. Terminate it first.");
        }

        propertyRepository.delete(property);
    }

    private Property findPropertyOrThrow(String id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
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

    private PropertyResponse toResponse(Property property) {
        PropertyResponse response = new PropertyResponse();
        response.setId(property.getId());
        response.setOwnerId(property.getOwnerId());
        response.setTitle(property.getTitle());
        response.setDescription(property.getDescription());
        response.setPropertyType(property.getPropertyType());
        response.setAddress(toAddressResponse(property.getAddress()));
        response.setRegistrationNumber(property.getRegistrationNumber());
        response.setRegistrationType(property.getRegistrationType());
        response.setIsActive(property.getIsActive());
        response.setIsVerified(property.getIsVerified());
        response.setTotalUnits(property.getTotalUnits());
        response.setAvailableUnits(property.getAvailableUnits());
        if (property.getLocation() != null) {
            response.setLatitude(property.getLocation().getLatitude());
            response.setLongitude(property.getLocation().getLongitude());
        }
        response.setCreatedAt(property.getCreatedAt());
        return response;
    }
}
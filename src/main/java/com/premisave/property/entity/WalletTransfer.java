package com.premisave.property.entity;

import com.premisave.property.enums.WalletTransferPurpose;
import com.premisave.property.enums.WalletTransferStatus;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Local ledger of every money movement property-service asks wallet-service
 * to make (tenant wallet -> property owner wallet). It exists so that:
 *  - a retry with the same reference never debits the tenant twice,
 *  - "money moved but payment not booked" is detectable and recoverable,
 *  - every payment can be reconciled against wallet-service by reference.
 *
 * The unique index on {@code reference} is created at startup by MongoConfig
 * (auto-index-creation is off by default in Spring Data MongoDB 4+).
 */
@Data
@Document(collection = "wallet_transfers")
public class WalletTransfer {

    @Id
    private String id;

    // Full wallet-service reference: PRP-{tenantId}-{clientReference}
    private String reference;

    private WalletTransferPurpose purpose;
    private String targetId;          // leaseId / rentalUnitId / utility billId

    private String tenantId;
    private String ownerId;
    private String propertyId;

    private String senderUserId;      // tenant's auth userId
    private String recipientAccount;  // owner's wallet account number (auth email)

    private BigDecimal amount;
    private String description;

    private WalletTransferStatus status = WalletTransferStatus.INITIATED;
    private String walletTransactionId;
    private String failureReason;
    private String bookedRecordId;    // LeaseRentUnitPayment / UnitRentPayment / UtilityBill id

    // While set and in the future, another request is actively processing
    // this reference. Expires on its own if a process dies mid-flight.
    private LocalDateTime inFlightUntil;

    // Optimistic lock: two concurrent claims of the same reference cannot both win.
    @Version
    private Long version;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
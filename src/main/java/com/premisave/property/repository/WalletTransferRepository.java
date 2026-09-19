package com.premisave.property.repository;

import com.premisave.property.entity.WalletTransfer;
import com.premisave.property.enums.WalletTransferStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransferRepository extends MongoRepository<WalletTransfer, String> {

    Optional<WalletTransfer> findByReference(String reference);

    // For reconciliation: transfers stuck in a non-terminal state.
    List<WalletTransfer> findByStatusInAndUpdatedAtBefore(List<WalletTransferStatus> statuses, LocalDateTime cutoff);
}
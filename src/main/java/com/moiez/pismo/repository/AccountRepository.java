package com.moiez.pismo.repository;

import com.moiez.pismo.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByDocumentNumber(String documentNumber);
}
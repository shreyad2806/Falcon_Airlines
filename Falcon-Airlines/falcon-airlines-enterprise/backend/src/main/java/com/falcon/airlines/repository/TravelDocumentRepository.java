package com.falcon.airlines.repository;

import com.falcon.airlines.entity.TravelDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TravelDocumentRepository extends JpaRepository<TravelDocument, Long>, JpaSpecificationExecutor<TravelDocument> {
}

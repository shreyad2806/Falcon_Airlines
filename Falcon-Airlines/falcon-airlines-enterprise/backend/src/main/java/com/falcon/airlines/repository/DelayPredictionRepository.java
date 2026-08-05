package com.falcon.airlines.repository;

import com.falcon.airlines.entity.DelayPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DelayPredictionRepository extends JpaRepository<DelayPrediction, Long>, JpaSpecificationExecutor<DelayPrediction> {

    List<DelayPrediction> findByFlightId(Long flightId);
}

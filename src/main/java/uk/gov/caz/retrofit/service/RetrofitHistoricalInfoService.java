package uk.gov.caz.retrofit.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.retrofit.dto.RetrofitInfoHistoricalRequest;
import uk.gov.caz.retrofit.dto.RetrofitVehicleHistoricalInfo;
import uk.gov.caz.retrofit.dto.RetrofitVehicleHistory;
import uk.gov.caz.retrofit.repository.RetrofitVehicleHistoryPostgresRepository;

/**
 * A class that is responsible for managing vehicle's licences historical data using postgres
 * repository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrofitHistoricalInfoService {

  private final RetrofitVehicleHistoryPostgresRepository retrofitVehicleHistoryPostgresRepository;

  /**
   * Finds all {@link RetrofitVehicleHistoricalInfo} entities for a given vrm and date range.
   *
   * @param request {@link RetrofitInfoHistoricalRequest}
   * @return {@link RetrofitVehicleHistoricalInfo} .
   */
  public RetrofitVehicleHistoricalInfo findByVrnInRange(String vrn,
      RetrofitInfoHistoricalRequest request) {
    List<RetrofitVehicleHistory> changes = retrofitVehicleHistoryPostgresRepository
        .findByVrnInRange(vrn, request.getLocalStartDate(), request.getLocalEndDate(),
            request.getPageSize(), request.getPageNumber());
    return RetrofitVehicleHistoricalInfo.builder()
        .changes(changes)
        .totalChangesCount(getTotalChangesCount(vrn, request, changes))
        .build();
  }

  /**
   * Helper method to provide total count.
   */
  private int getTotalChangesCount(String vrn, RetrofitInfoHistoricalRequest request,
      List<RetrofitVehicleHistory> changes) {
    return request.getPageNumber() == 0 && changes.size() < request.getPageSize()
        ? changes.size()
        : retrofitVehicleHistoryPostgresRepository
            .count(vrn, request.getLocalStartDate(), request.getLocalEndDate()).intValue();
  }
}

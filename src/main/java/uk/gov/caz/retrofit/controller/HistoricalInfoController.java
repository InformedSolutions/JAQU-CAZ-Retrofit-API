package uk.gov.caz.retrofit.controller;

import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.retrofit.dto.RetrofitInfoHistoricalRequest;
import uk.gov.caz.retrofit.dto.RetrofitInfoHistoricalResponse;
import uk.gov.caz.retrofit.dto.RetrofitInfoHistoricalResponse.Change;
import uk.gov.caz.retrofit.dto.RetrofitVehicleHistoricalInfo;
import uk.gov.caz.retrofit.service.RetrofitHistoricalInfoService;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HistoricalInfoController implements HistoricalInfoControllerApiSpec {
  public static final String BASE_PATH =
      "/v1/retrofit/{vrn}/retrofit-info-historical";

  private final RetrofitHistoricalInfoService retrofitHistoricalInfoService;

  @Override
  public ResponseEntity<RetrofitInfoHistoricalResponse> search(String vrn,
      RetrofitInfoHistoricalRequest request) {
    request.validate();
    RetrofitVehicleHistoricalInfo vehicleHistorical = retrofitHistoricalInfoService
        .findByVrnInRange(vrn, request);

    RetrofitInfoHistoricalResponse response = RetrofitInfoHistoricalResponse.builder()
        .page(request.getPageNumber())
        .pageCount(
            calculatePageCount(vehicleHistorical.getTotalChangesCount(), request.getPageSize()))
        .perPage(request.getPageSize())
        .totalChangesCount(vehicleHistorical.getTotalChangesCount())
        .changes(vehicleHistorical.getChanges().stream()
            .map(Change::from)
            .collect(Collectors.toList()))
        .build();
    return ResponseEntity.ok(response);
  }

  /**
   * Helper method to calculate pages count.
   */
  protected int calculatePageCount(int totalCount, int perPage) {
    return (totalCount + perPage - 1) / perPage;
  }
}

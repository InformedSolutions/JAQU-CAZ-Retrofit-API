package uk.gov.caz.retrofit.controller;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.retrofit.dto.RetrofitStatusResponse;
import uk.gov.caz.retrofit.service.RetrofitVehicleService;

@RestController
@AllArgsConstructor
public class RetrofitVehicleController implements RetrofitVehicleControllerApiSpec {
  public static final String BASE_PATH = "/v1/retrofit/vehicles";

  @Autowired
  private RetrofitVehicleService retrofitVehicleService;

  @Override
  public ResponseEntity<RetrofitStatusResponse> fetchRetrofitStatus(String correlationId,
      String vrn) {
    if (retrofitVehicleService.existsByVrn(vrn)) {
      return ResponseEntity.ok(new RetrofitStatusResponse(true));
    }

    return ResponseEntity.notFound().build();
  }
}

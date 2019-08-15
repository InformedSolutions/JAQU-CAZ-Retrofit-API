package uk.gov.caz.taxiregister;

import static java.util.stream.Collectors.toList;
import static uk.gov.caz.testutils.TestObjects.API_CALL_REGISTER_JOB_ID;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import uk.gov.caz.taxiregister.annotation.IntegrationTest;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.service.SourceAwareRegisterService;
import uk.gov.caz.taxiregister.util.DatabaseInitializer;

/**
 * This class provides storage-specific methods for inserting Vehicles. Please check {@link
 * RegisterVehiclesAbstractTest} to get better understanding of tests steps that will be executed.
 *
 * It uses(and thus tests) {@link uk.gov.caz.taxiregister.service.RegisterFromRestApiCommand}
 * command.
 */
@IntegrationTest
@Import(DatabaseInitializer.class)
@Slf4j
public class RegisterByAPITestIT extends RegisterVehiclesAbstractTest {

  private static final List<VehicleDto> UPLOADER_1_VEHICLES = Lists.newArrayList(
      buildVehicle("OI64EFO,2019-04-30,2019-05-22,taxi,la-3,\"a\"\",b\",true"),
      buildVehicle("OI64EFO,2019-04-27,2019-06-23,PHV,la-3,a & b'c & d,true"),
      buildVehicle("DS98UDG,2019-03-12,2019-06-25,taxi,la-2,hragF,true"),
      buildVehicle("DS98UDG,2019-03-11,2019-07-24,PHV,la-1,iragG,true"),
      buildVehicle("ND84VSX,2019-04-14,2019-06-13,taxi,la-1,Oretr,false"),
      buildVehicle("ZC62OMB,2019-04-15,2019-05-17,PHV,la-2,beBCC"),
      buildVehicle("BW91HUN,2019-03-09,2019-05-06,taxi,la-1,yGSJC,true"),
      buildVehicle("SV57THC,2019-05-22,2019-06-16,taxi,la-2,VDNIv,true"),
      buildVehicle("LE35LMK,2019-06-17,2019-07-11,PHV,la-1,FhMxv,true"),
      buildVehicle("NO03KNT,2019-03-23,2019-05-29,taxi,la-3,EGsZU,true"),
      buildVehicle("LV98HYW,2019-03-02,2019-03-17,PHV,la-1,kAejQ"),
      buildVehicle("ZA14APJ,2019-03-27,2019-06-19,PHV,la-2,TrxdV,true"),
      buildVehicle("RG35XNP,2019-04-04,2019-04-15,taxi,la-1,cBate,false"),
      buildVehicle("EF31PRO,2019-06-03,2019-06-29,taxi,la-3,tCMMZ,true"),
      buildVehicle("PC00SNK,2019-03-17,2019-05-25,PHV,la-3,DhkZj,false"),
      buildVehicle("SW40NRN,2019-04-06,2019-06-16,taxi,la-2,rKXry,false"),
      buildVehicle("SW40NRN,2019-05-06,2019-08-16,taxi,la-2,rKXry"),
      buildVehicle("PC00SNK,2019-05-17,2019-08-25,PHV,la-3,DhkZj")
  );

  private static final List<VehicleDto> UPLOADER_1_VEHICLES_ALL_BUT_FIVE_AND_FIVE_MODIFIED =
      modifyFive(UPLOADER_1_VEHICLES.stream().skip(5).collect(toList()));

  private static final List<VehicleDto> UPLOADER_2_VEHICLES = Lists.newArrayList(
      buildVehicle("OX84LFX,2019-03-14,2019-05-01,PHV,la-1,VCmGQ,false"),
      buildVehicle("CF23YPG,2019-04-11,2019-05-02,taxi,la-2,MwXrE,false"),
      buildVehicle("JP21YSO,2019-04-24,2019-06-15,taxi,la-1,PUdee,true"),
      buildVehicle("KP37QPV,2019-05-26,2019-06-07,taxi,la-2,ixwvm,true"),
      buildVehicle("EP64TKJ,2019-04-27,2019-07-30,taxi,la-3,XgcgB,true"),
      buildVehicle("ST73VOI,2019-03-24,2019-06-22,PHV,la-2,DgVPLyBtch,false")
  );

  private static final List<VehicleDto> UPLOADER_2_VEHICLES_ALL_AND_FIVE_MODIFIED =
      modifyFive(new ArrayList<>(UPLOADER_2_VEHICLES));

  @Autowired
  private SourceAwareRegisterService registerService;

  @Override
  void whenThreeVehiclesAreUpdatedByBothUploaders() {
    register(UPLOADER_1_VEHICLES_ALL_BUT_FIVE_AND_FIVE_MODIFIED, FIRST_UPLOADER_ID);
    register(UPLOADER_2_VEHICLES_ALL_AND_FIVE_MODIFIED, SECOND_UPLOADER_ID);
  }

  @Override
  void whenVehiclesAreRegisteredBySecondUploaderWithNoDataFromTheFirstOne() {
    register(UPLOADER_2_VEHICLES, SECOND_UPLOADER_ID);
  }

  @Override
  void whenThereAreFiveVehicleLessRegisteredByFirstUploader() {
    List<VehicleDto> allButFive = UPLOADER_1_VEHICLES.stream().skip(5).collect(toList());
    register(allButFive, FIRST_UPLOADER_ID);
  }

  @Override
  void whenVehiclesAreRegisteredAgainstEmptyDatabaseByFirstUploader() {
    register(UPLOADER_1_VEHICLES, FIRST_UPLOADER_ID);
  }

  @Override
  int getFirstUploaderTotalVehiclesCount() {
    return UPLOADER_1_VEHICLES.size();
  }

  @Override
  int getSecondUploaderTotalVehiclesCount() {
    return UPLOADER_2_VEHICLES.size();
  }

  @Override
  int getRegisterJobId() {
    return API_CALL_REGISTER_JOB_ID;
  }

  private void register(List<VehicleDto> vehicles, UUID uploaderId) {
    String correlationId = UUID.randomUUID().toString();

    registerService.register(
        vehicles,
        uploaderId,
        getRegisterJobId(),
        correlationId
    );
  }

  private static VehicleDto buildVehicle(String rawVehicle) {
    String[] rawVehicleDetails = rawVehicle.split(",");

    Boolean wheelchairAccessibleVehicle = rawVehicleDetails.length == 7
        ? Boolean.getBoolean(rawVehicleDetails[6])
        : null;

    return VehicleDto.builder()
        .vrm(rawVehicleDetails[0])
        .start(rawVehicleDetails[1])
        .end(rawVehicleDetails[2])
        .taxiOrPhv(rawVehicleDetails[3])
        .licensingAuthorityName(rawVehicleDetails[4])
        .licensePlateNumber(rawVehicleDetails[5])
        .wheelchairAccessibleVehicle(wheelchairAccessibleVehicle)
        .build();
  }

  private static List<VehicleDto> modifyFive(List<VehicleDto> vehicles) {
    for (int i = 0; i < 5; i++) {
      VehicleDto vehicleDto = vehicles.get(i);
      vehicles.remove(vehicleDto);
      vehicles.add(vehicleDto.toBuilder().wheelchairAccessibleVehicle(
          changeWheelchairAccessibleVehicleAttribute(i, vehicleDto)
      ).build());
    }
    return vehicles;
  }

  private static Boolean changeWheelchairAccessibleVehicleAttribute(int i, VehicleDto vehicleDto) {
    boolean randomBoolean = i % 2 == 0;
    if (vehicleDto.getWheelchairAccessibleVehicle() == null) {
      return randomBoolean;
    }
    return randomBoolean ? null : !vehicleDto.getWheelchairAccessibleVehicle();
  }
}

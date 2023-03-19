package com.udacity.catpoint.application;

import com.udacity.catpoint.data.*;
import com.udacity.catpoint.service.ImageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private ImageService imageService;
    private SecurityService securityService;
    static private SecurityRepository securityRepository;


    @BeforeAll
    static void initAll(){
        securityRepository =  new PretendDatabaseSecurityRepositoryImpl();
    }
    @BeforeEach
    void init(){
        securityService = new SecurityService(securityRepository, imageService);
        securityService.setAlarmStatus(AlarmStatus.NO_ALARM);
        securityService.setArmingStatus(ArmingStatus.DISARMED);
    }
    @AfterEach
    void clean(){
        Set<Sensor> sensors  = new HashSet<>(securityService.getSensors());
        for (Sensor sensor:sensors) {
            securityService.removeSensor(sensor);
        }
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,
                names = {"ARMED_AWAY", "ARMED_HOME"})
    @DisplayName("1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.")
    void changeSensorActivationStatus_pending_sensor_active(ArmingStatus armingStatus) {
        securityService.setArmingStatus(armingStatus);
        assertNotEquals(securityService.getArmingStatus(), ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(new Sensor("Home door", SensorType.DOOR), true);
        assertEquals(AlarmStatus.PENDING_ALARM, securityService.getAlarmStatus());
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,
            names = {"ARMED_AWAY", "ARMED_HOME"})
    @DisplayName("2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.")
    void changeSensorActivationStatus_alarm_sensor_active(ArmingStatus armingStatus) {
        securityService.setArmingStatus(armingStatus);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        assertNotEquals(securityService.getArmingStatus(), ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(new Sensor("Home door", SensorType.DOOR), true);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    @Test
    @DisplayName("3. If pending alarm and all sensors are inactive, return to no alarm state.")
    void changeSensorActivationStatus_no_alarm_sensor_inactive(){
        Sensor window = new Sensor("Home window", SensorType.WINDOW);
        Sensor door = new Sensor("Home door", SensorType.DOOR);

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.changeSensorActivationStatus(door, true);
        securityService.changeSensorActivationStatus(window, true);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(door, false);
        securityService.changeSensorActivationStatus(door, false);
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    @Test
    @DisplayName("4. If alarm is active, change in sensor state should not affect the alarm state.")
    void changeSensorActivation_is_not_affect_alarm_state(){
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);
        securityService.setAlarmStatus(AlarmStatus.ALARM);

        Sensor window = new Sensor("Home window", SensorType.WINDOW);
        Sensor door = new Sensor("Home door", SensorType.DOOR);

        securityService.changeSensorActivationStatus(window, true);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());

        securityService.changeSensorActivationStatus(door, true);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());

        securityService.changeSensorActivationStatus(door, false);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    @RepeatedTest(value = 3, name = "sensor is activated while already active {currentRepetition}/{totalRepetitions}")
    @DisplayName("5. (no Alarm)If a sensor is activated while already active and the system is in pending state, change it to alarm state.")
    void changeSensorActivation_while_already_active_system_from_pending_to_alarm_is_not_alarm(RepetitionInfo repetitionInfo){
        Sensor window = new Sensor("Home window", SensorType.WINDOW);

        securityService.changeSensorActivationStatus(window, true);

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(window, false);
        assertNotEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());

    }

    @Test
    @DisplayName("5. (Alarm)If a sensor is activated while already active and the system is in pending state, change it to alarm state.")
    void changeSensorActivation_while_already_active_system_from_pending_to_alarm_is_alarm(){
        Sensor window = new Sensor("Home window", SensorType.WINDOW);
        Sensor door = new Sensor("Home door", SensorType.DOOR);

        securityService.changeSensorActivationStatus(door, true);

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(window, true);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class)
    @DisplayName("6. (different states)If a sensor is deactivated while already inactive, make no changes to the alarm state.")
    void changeSensorActivationStatus_inactive_sensor_to_inactive_no_state_changes_changes_alarm_states(AlarmStatus alarmStatus) {
        Sensor window = new Sensor("Home window", SensorType.WINDOW);
        Sensor door = new Sensor("Home door", SensorType.DOOR);
        Sensor motion = new Sensor("Door motion", SensorType.MOTION);
        securityService.changeSensorActivationStatus(window, false);
        securityService.changeSensorActivationStatus(motion, false);
        securityService.changeSensorActivationStatus(door, true);
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);
        securityService.setAlarmStatus(alarmStatus);

        securityService.changeSensorActivationStatus(window, false);
        assertEquals(alarmStatus, securityService.getAlarmStatus());

        securityService.changeSensorActivationStatus(motion, false);
        assertEquals(alarmStatus, securityService.getAlarmStatus());
    }

    @RepeatedTest(value = 3, name = "sensor is deactivated while already inactive {currentRepetition}/{totalRepetitions}")
    @DisplayName("6. (repeated)If a sensor is deactivated while already inactive, make no changes to the alarm state.")
    void changeSensorActivationStatus_inactive_sensor_to_inactive_no_state_changes() {
        Sensor window = new Sensor("Home window", SensorType.WINDOW);
        Sensor door = new Sensor("Home door", SensorType.DOOR);
        Sensor motion = new Sensor("Door motion", SensorType.MOTION);
        securityService.changeSensorActivationStatus(window, false);
        securityService.changeSensorActivationStatus(motion, false);
        securityService.changeSensorActivationStatus(door, true);
        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);

        securityService.changeSensorActivationStatus(window, false);
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());

        securityService.changeSensorActivationStatus(motion, false);
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    @Test
    @DisplayName("7. (Activated)If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.")
    void detection_cat_while_system_at_home(){
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(new BufferedImage(100,100,1));
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }
    @Test
    @DisplayName("7. (Deactivated)If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.")
    void detection_cat_while_system_deactivated(){
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(new BufferedImage(100,100,1));
        assertNotEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    @Test
    @DisplayName("8. If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.")
    void cat_not_found_and_inactive_sensors(){

        BufferedImage testImage = new BufferedImage(100,100,1);
        Sensor window = new Sensor("Home window", SensorType.WINDOW);
        Sensor door = new Sensor("Home door", SensorType.DOOR);
        Sensor motion = new Sensor("Door motion", SensorType.MOTION);

        securityService.changeSensorActivationStatus(window, false);
        securityService.changeSensorActivationStatus(door, false);
        securityService.changeSensorActivationStatus(motion, false);

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.setAlarmStatus(AlarmStatus.ALARM);

        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);
        securityService.processImage(testImage);
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());

        securityService.changeSensorActivationStatus(door, true);
        securityService.changeSensorActivationStatus(motion, true);
        securityService.processImage(testImage);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class,
                names = {"NO_ALARM", "PENDING_ALARM"})
    @DisplayName("9. If the system is disarmed, set the status to no alarm.")
    void system_is_not_armed_is_not_alarm(AlarmStatus alarmStatus){
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.setAlarmStatus(alarmStatus);
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());

    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,
            names = {"ARMED_HOME", "ARMED_AWAY"})
    @DisplayName("10. If the system is armed, reset all sensors to inactive.")
    void system_is_disarmed_all_sensors_inactive(ArmingStatus armingStatus){
        Sensor window = new Sensor("Home window", SensorType.WINDOW);
        Sensor door = new Sensor("Home door", SensorType.DOOR);
        Sensor motion = new Sensor("Door motion", SensorType.MOTION);

        securityService.changeSensorActivationStatus(window, true);
        securityService.changeSensorActivationStatus(door, true);
        securityService.changeSensorActivationStatus(motion, true);
        securityService.setArmingStatus(armingStatus);

        for (Sensor sensor: securityService.getSensors()) {
            assertEquals(false, sensor.getActive());
        }
    }

    @ParameterizedTest
    @MethodSource("argumentProvider")
    @DisplayName("11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.")
    void system_armed_home_while_camera_does_not_show_cat_set_status_alarm(ArmingStatus armingStatusArg, AlarmStatus alarmStatusExp){

        BufferedImage testImage = new BufferedImage(100,100,1);

        securityService.setArmingStatus(armingStatusArg);
        securityService.setAlarmStatus(AlarmStatus.NO_ALARM);

        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(testImage);
        assertEquals(alarmStatusExp, securityService.getAlarmStatus());
    }

    private static Stream<Arguments> argumentProvider(){
        return Stream.of(
                Arguments.of(ArmingStatus.DISARMED, AlarmStatus.NO_ALARM),
                Arguments.of(ArmingStatus.ARMED_AWAY, AlarmStatus.NO_ALARM),
                Arguments.of(ArmingStatus.ARMED_HOME, AlarmStatus.ALARM)
        );
    }

    @Test
    @DisplayName("11. (change state)If the system is armed-home while the camera shows a cat, set the alarm status to alarm.")
    void change_state_after_cat_and_armed_away_state(){
        BufferedImage testImage = new BufferedImage(100,100,1);

        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        securityService.processImage(testImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }
}
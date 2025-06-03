package com.example.carcare; // Paket adını kendi projenle eşleştir

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleOBD2Manager {
    private static final String TAG = "SimpleOBD2Manager";

    // --- OBD2 PID Sabitleri ---
    private static final String COOLANT_TEMP_PID = "0105";
    private static final String ENGINE_RPM_PID = "010C";
    private static final String VEHICLE_SPEED_PID = "010D";
    private static final String FUEL_LEVEL_PID = "012F";
    private static final String ENGINE_LOAD_PID = "0104";
    private static final String THROTTLE_POS_PID = "0111";
    private static final String INTAKE_TEMP_PID = "010F";
    private static final String MAF_AIR_FLOW_PID = "0110";
    private static final String DTC_REQUEST_PID = "03";      // Request current DTCs
    private static final String VEHICLE_VIN_PID = "0902";    // Request Vehicle Identification Number

    // --- Diğer Sabitler ---
    private static final int MAX_CONSECUTIVE_FAILURES = 8;
    private static final long COMMAND_INTERVAL_MS = 250;
    private static final long READ_RESPONSE_TIMEOUT_MS = 5000;
    private static final long INIT_COMMAND_RESPONSE_TIMEOUT_MS = 7000;
    private static final int SCHEDULER_INITIAL_DELAY_S = 2;
    private static final int SCHEDULER_PERIOD_S = 3;

    // --- Kritik Eşik Değerleri (Başlangıç için sabit, daha sonra ayarlanabilir) ---
    private static final double THRESHOLD_ENGINE_TEMP_HIGH = 110.0; // Celsius
    private static final double THRESHOLD_FUEL_LEVEL_LOW = 15.0;  // Yüzde %

    private CriticalDataAlertListener criticalDataAlertListener;
    private List<VehicleData.DTC> previousDTCs = new ArrayList<>(); // Önceki DTC'leri takip etmek için
    private final Context context;
    private final BluetoothManager bluetoothManager;
    private ScheduledExecutorService scheduler;
    private final Handler mainHandler;
    private final VehicleData vehicleData;
    private DataUpdateListener dataUpdateListener;

    private volatile boolean isReading = false;
    private int consecutiveFailures = 0;

    private final Queue<String> commandQueue = new LinkedList<>();
    private final AtomicBoolean isProcessingQueue = new AtomicBoolean(false);

    // Selected OBD2 protocol for ATSP command (default auto)
    private String selectedProtocol = "0";

    // --- DTC Açıklamaları ---
    private static final Map<String, String> DTC_DESCRIPTIONS_RAW = new HashMap<>();
    static {
        DTC_DESCRIPTIONS_RAW.put("P0000", "No Trouble Code Detected or End of List");
        DTC_DESCRIPTIONS_RAW.put("P0100", "Mass or Volume Air Flow Circuit Malfunction");
        DTC_DESCRIPTIONS_RAW.put("P0101", "Mass or Volume Air Flow Circuit Range/Performance Problem");
        DTC_DESCRIPTIONS_RAW.put("P0102", "Mass or Volume Air Flow Circuit Low Input");
        DTC_DESCRIPTIONS_RAW.put("P0103", "Mass or Volume Air Flow Circuit High Input");
        DTC_DESCRIPTIONS_RAW.put("P0113", "Intake Air Temperature Sensor 1 Circuit High Input");
        DTC_DESCRIPTIONS_RAW.put("P0118", "Engine Coolant Temperature Sensor 1 Circuit High");
        DTC_DESCRIPTIONS_RAW.put("P0121", "Throttle/Pedal Position Sensor/Switch A Circuit Range/Performance");
        DTC_DESCRIPTIONS_RAW.put("P0128", "Coolant Thermostat (Coolant Temperature Below Thermostat Regulating Temperature)");
        DTC_DESCRIPTIONS_RAW.put("P0131", "O2 Sensor Circuit Low Voltage (Bank 1 Sensor 1)");
        DTC_DESCRIPTIONS_RAW.put("P0135", "O2 Sensor Heater Circuit Malfunction (Bank 1 Sensor 1)");
        DTC_DESCRIPTIONS_RAW.put("P0171", "System Too Lean (Bank 1)");
        DTC_DESCRIPTIONS_RAW.put("P0172", "System Too Rich (Bank 1)");
        DTC_DESCRIPTIONS_RAW.put("P0300", "Random/Multiple Cylinder Misfire Detected");
        DTC_DESCRIPTIONS_RAW.put("P0301", "Cylinder 1 Misfire Detected");
        DTC_DESCRIPTIONS_RAW.put("P0302", "Cylinder 2 Misfire Detected");
        DTC_DESCRIPTIONS_RAW.put("P0303", "Cylinder 3 Misfire Detected");
        DTC_DESCRIPTIONS_RAW.put("P0304", "Cylinder 4 Misfire Detected");
        DTC_DESCRIPTIONS_RAW.put("P0401", "Exhaust Gas Recirculation Flow Insufficient Detected");
        DTC_DESCRIPTIONS_RAW.put("P0420", "Catalyst System Efficiency Below Threshold (Bank 1)");
        DTC_DESCRIPTIONS_RAW.put("P0440", "Evaporative Emission Control System Malfunction");
        DTC_DESCRIPTIONS_RAW.put("P0442", "Evaporative Emission Control System Leak Detected (Small Leak)");
        DTC_DESCRIPTIONS_RAW.put("P0455", "Evaporative Emission Control System Leak Detected (Gross Leak)");
        DTC_DESCRIPTIONS_RAW.put("P0500", "Vehicle Speed Sensor Malfunction");
        // YENİ EKLENEN RAW AÇIKLAMALAR (Kullanıcı dostu olanlar zaten DTC_USER_FRIENDLY_DESCRIPTIONS içinde daha detaylı)
        DTC_DESCRIPTIONS_RAW.put("P0010", "A Camshaft Position Actuator Circuit (Bank 1)");
        DTC_DESCRIPTIONS_RAW.put("P0011", "A Camshaft Position - Timing Over-Advanced or System Performance (Bank 1)");
        DTC_DESCRIPTIONS_RAW.put("P0030", "HO2S Heater Control Circuit (Bank 1 Sensor 1)");
        DTC_DESCRIPTIONS_RAW.put("P0036", "HO2S Heater Control Circuit (Bank 1 Sensor 2)");
        DTC_DESCRIPTIONS_RAW.put("P0043", "HO2S Heater Control Circuit Low (Bank 1 Sensor 3)"); // Bu zaten vardı, teyit amaçlı
        DTC_DESCRIPTIONS_RAW.put("P0105", "Manifold Absolute Pressure/Barometric Pressure Circuit Malfunction");
        DTC_DESCRIPTIONS_RAW.put("P0110", "Intake Air Temperature Sensor 1 Circuit Malfunction");
        DTC_DESCRIPTIONS_RAW.put("P0115", "Engine Coolant Temperature Sensor 1 Circuit Malfunction");
        DTC_DESCRIPTIONS_RAW.put("P0120", "Throttle/Pedal Position Sensor/Switch 'A' Circuit Malfunction");
        DTC_DESCRIPTIONS_RAW.put("P0125", "Insufficient Coolant Temperature for Closed Loop Fuel Control");
        DTC_DESCRIPTIONS_RAW.put("P0130", "O2 Sensor Circuit Malfunction (Bank 1 Sensor 1)");
        DTC_DESCRIPTIONS_RAW.put("P0136", "O2 Sensor Circuit Malfunction (Bank 1 Sensor 2)");
        DTC_DESCRIPTIONS_RAW.put("P0141", "O2 Sensor Heater Circuit Malfunction (Bank 1 Sensor 2)");
        DTC_DESCRIPTIONS_RAW.put("P0170", "Fuel Trim Malfunction (Bank 1)");
        DTC_DESCRIPTIONS_RAW.put("P0325", "Knock Sensor 1 Circuit Malfunction (Bank 1 or Single Sensor)");
        DTC_DESCRIPTIONS_RAW.put("P0335", "Crankshaft Position Sensor 'A' Circuit Malfunction");
        DTC_DESCRIPTIONS_RAW.put("P0340", "Camshaft Position Sensor 'A' Circuit Malfunction (Bank 1 or Single Sensor)");
        DTC_DESCRIPTIONS_RAW.put("P0400", "Exhaust Gas Recirculation Flow Malfunction");
        DTC_DESCRIPTIONS_RAW.put("P0403", "Exhaust Gas Recirculation Control Circuit Malfunction");
        DTC_DESCRIPTIONS_RAW.put("P0410", "Secondary Air Injection System Malfunction");
        DTC_DESCRIPTIONS_RAW.put("P0430", "Catalyst System Efficiency Below Threshold (Bank 2)");
        DTC_DESCRIPTIONS_RAW.put("P0441", "Evaporative Emission Control System Incorrect Purge Flow");
        DTC_DESCRIPTIONS_RAW.put("P0443", "Evaporative Emission System Purge Control Valve Circuit");
        DTC_DESCRIPTIONS_RAW.put("P0446", "Evaporative Emission System Vent Control Circuit");
        DTC_DESCRIPTIONS_RAW.put("P0456", "Evaporative Emission System Leak Detected (Very Small Leak)");
        DTC_DESCRIPTIONS_RAW.put("P0505", "Idle Air Control System Malfunction");
        DTC_DESCRIPTIONS_RAW.put("P0700", "Transmission Control System (MIL Request)");
        DTC_DESCRIPTIONS_RAW.put("P0705", "Transmission Range Sensor Circuit Malfunction (PRNDL Input)");
        DTC_DESCRIPTIONS_RAW.put("C0031", "Left Front Wheel Speed Sensor Circuit");
        DTC_DESCRIPTIONS_RAW.put("C0034", "Right Front Wheel Speed Sensor Circuit");
        DTC_DESCRIPTIONS_RAW.put("C0035", "Left Front Wheel Speed Sensor Circuit Malfunction"); // Zaten vardı
        DTC_DESCRIPTIONS_RAW.put("C0040", "Right Front Wheel Speed Sensor Malfunction");
        DTC_DESCRIPTIONS_RAW.put("C0051", "Steering Wheel Position Sensor Circuit Malfunction");
        DTC_DESCRIPTIONS_RAW.put("B0001", "Driver Frontal Stage 1 Deployment Control");
        DTC_DESCRIPTIONS_RAW.put("B0012", "Driver Frontal Stage 2 Deployment Control (Subfault)"); // Zaten vardı
        DTC_DESCRIPTIONS_RAW.put("B1000", "ECU Malfunction");
        DTC_DESCRIPTIONS_RAW.put("U0073", "Control Module Communication Bus 'A' Off");
        DTC_DESCRIPTIONS_RAW.put("U0100", "Lost Communication With ECM/PCM 'A'"); // Zaten vardı
        DTC_DESCRIPTIONS_RAW.put("U0101", "Lost Communication with TCM");
        DTC_DESCRIPTIONS_RAW.put("U0121", "Lost Communication With Anti-Lock Brake System (ABS) Control Module");
        DTC_DESCRIPTIONS_RAW.put("U0140", "Lost Communication With Body Control Module");
        DTC_DESCRIPTIONS_RAW.put("U0155", "Lost Communication With Instrument Panel Cluster (IPC) Control Module");
    }

    private static final Map<String, String> DTC_USER_FRIENDLY_DESCRIPTIONS = new HashMap<>();
    static {
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0000", "No Trouble Code Detected or End of List. This typically means the system check is complete and no active faults were found in the monitored systems. Your vehicle is likely operating normally (as far as the OBD2 system can tell).");
        // --- Yakıt ve Hava Ölçümleme ---
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0010", "A Camshaft Position Actuator Circuit (Bank 1): Problem with the circuit controlling the camshaft position actuator, which adjusts camshaft timing for better performance and efficiency. May cause rough idle, poor performance, or stalling. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0011", "A Camshaft Position - Timing Over-Advanced or System Performance (Bank 1): Camshaft timing is too far advanced or there's a performance issue with the system. Can be caused by incorrect oil viscosity, issues with the oil control valve, or problems with the phaser. Symptoms: rough running, reduced power. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0012", "A Camshaft Position - Timing Over-Retarded (Bank 1): Camshaft timing is too far retarded. Similar causes and symptoms to P0011. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0030", "HO2S Heater Control Circuit (Bank 1 Sensor 1): Malfunction in the heater circuit for the upstream oxygen sensor (before catalytic converter) on Bank 1. The heater helps the sensor reach operating temperature quickly. Failure can lead to increased emissions and poor fuel economy, especially when cold. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0031", "HO2S Heater Control Circuit Low (Bank 1 Sensor 1): Heater circuit for O2 sensor (Bank 1, Sensor 1) has low voltage. Could be a short or faulty heater element. See P0030 for implications.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0036", "HO2S Heater Control Circuit (Bank 1 Sensor 2): Malfunction in the heater circuit for the downstream oxygen sensor (after catalytic converter) on Bank 1. This sensor primarily monitors catalyst efficiency. Heater failure can delay this monitoring. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0043", "HO2S Heater Control Circuit Low (Bank 1 Sensor 3): If your vehicle has a third O2 sensor on Bank 1 (less common), this indicates a low voltage in its heater circuit. Implications similar to other O2 heater faults.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0100", "Mass Air Flow (MAF) Sensor Circuit Malfunction: The MAF sensor measures air entering your engine, crucial for correct fuel mixture. A 'Circuit Malfunction' means an electrical problem (wiring, sensor). Leads to poor fuel economy, reduced power, rough idle. Check Engine Light on. Prolonged issue can damage catalytic converter.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0101", "Mass Air Flow (MAF) Sensor Circuit Range/Performance: MAF sensor readings are outside the expected range. Caused by a dirty sensor, vacuum leaks, or failing sensor. Similar symptoms to P0100. Needs inspection.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0102", "Mass Air Flow (MAF) Sensor Circuit Low Input: MAF sensor signals very low airflow. Often a wiring issue, faulty sensor, or intake blockage. Noticeable performance problems. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0103", "Mass Air Flow (MAF) Sensor Circuit High Input: MAF sensor signals excessively high airflow. Can confuse ECU, leading to incorrect fuel mixture, poor performance. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0105", "Manifold Absolute Pressure (MAP)/Barometric Pressure Circuit Malfunction: The MAP sensor measures engine vacuum. A fault can lead to incorrect fuel delivery and ignition timing. Symptoms: rough idle, poor fuel economy, stalling. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0106", "Manifold Absolute Pressure (MAP)/Barometric Pressure Circuit Range/Performance: MAP sensor readings are out of expected range. Could be a faulty sensor, vacuum leak, or clogged line. Similar symptoms to P0105.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0110", "Intake Air Temperature (IAT) Sensor 1 Circuit Malfunction: IAT sensor measures incoming air temp. A circuit malfunction indicates an electrical issue with the sensor or wiring. Can affect fuel mixture and timing. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0113", "Intake Air Temperature (IAT) Sensor 1 Circuit High Input: IAT sensor reading abnormally high temperature (or open circuit). Affects air-fuel mix, ignition. Reduced performance, poor fuel economy, hard starting. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0115", "Engine Coolant Temperature (ECT) Sensor 1 Circuit Malfunction: ECT sensor tells ECU engine temp. Circuit malfunction implies electrical problem. Can cause starting issues, poor performance, fans running constantly. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0118", "Engine Coolant Temperature (ECT) Sensor 1 Circuit High: ECT sensor reports extremely high temp (or electrical issue). Can cause incorrect fuel mix (ECU thinks engine is hotter), starting problems, fans running always. Check Engine Light on. If engine IS overheating, stop driving to prevent damage.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0120", "Throttle/Pedal Position Sensor/Switch 'A' Circuit Malfunction: Problem with the electrical circuit of the sensor that detects accelerator pedal position or throttle blade position. Can cause hesitation, erratic acceleration, or limp mode. Check Engine Light on. Safety concern.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0121", "Throttle/Pedal Position Sensor/Switch 'A' Circuit Range/Performance: Sensor's signal is erratic or doesn't match other inputs. Hesitant acceleration, sudden power changes, unstable idle. Difficult/unsafe to drive. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0122", "Throttle/Pedal Position Sensor/Switch 'A' Circuit Low Input: Sensor signal is too low. May result in no throttle response or engine stuck at idle. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0123", "Throttle/Pedal Position Sensor/Switch 'A' Circuit High Input: Sensor signal is too high. May cause unintended acceleration or high idle. Check Engine Light on. Potentially dangerous.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0125", "Insufficient Coolant Temperature for Closed Loop Fuel Control: Engine is not warming up enough for the ECU to switch to 'closed loop' mode (where O2 sensors fine-tune fuel). Often a faulty thermostat stuck open. Results in higher fuel consumption and emissions. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0128", "Coolant Thermostat (Coolant Temperature Below Thermostat Regulating Temperature): Thermostat likely stuck open, engine runs too cool. Increased fuel consumption, emissions, poor heater performance. Check Engine Light on. Fix for efficiency.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0130", "O2 Sensor Circuit Malfunction (Bank 1 Sensor 1): Electrical problem with the upstream O2 sensor on Bank 1. This sensor is key for fuel mixture control. Can lead to poor fuel economy, increased emissions. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0131", "O2 Sensor Circuit Low Voltage (Bank 1 Sensor 1): Upstream O2 sensor (Bank 1) detects rich fuel mix (too much fuel) or sensor is failing. Poor fuel economy, high emissions, rotten egg smell. Can damage catalytic converter long-term. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0132", "O2 Sensor Circuit High Voltage (Bank 1 Sensor 1): Upstream O2 sensor (Bank 1) detects lean fuel mix (too much air) or sensor is failing. Can cause misfires, poor performance. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0135", "O2 Sensor Heater Circuit Malfunction (Bank 1 Sensor 1): Heater for upstream O2 sensor (Bank 1) isn't working. Sensor takes longer to give accurate readings, affecting fuel economy/emissions when cold. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0136", "O2 Sensor Circuit Malfunction (Bank 1 Sensor 2): Electrical problem with the downstream O2 sensor (after catalyst) on Bank 1. This sensor monitors catalyst efficiency. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0137", "O2 Sensor Circuit Low Voltage (Bank 1 Sensor 2): Downstream O2 sensor (Bank 1) reading low. May indicate a rich condition after the catalyst or a failing sensor. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0138", "O2 Sensor Circuit High Voltage (Bank 1 Sensor 2): Downstream O2 sensor (Bank 1) reading high. May indicate a lean condition or a failing sensor, or possibly an issue with the catalyst. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0141", "O2 Sensor Heater Circuit Malfunction (Bank 1 Sensor 2): Heater for downstream O2 sensor (Bank 1) isn't working. See P0135 for implications on sensor warm-up. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0150", "O2 Sensor Circuit Malfunction (Bank 2 Sensor 1): Same as P0130, but for Bank 2 (if your engine has two banks, e.g., V6, V8).");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0151", "O2 Sensor Circuit Low Voltage (Bank 2 Sensor 1): Same as P0131, but for Bank 2.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0155", "O2 Sensor Heater Circuit Malfunction (Bank 2 Sensor 1): Same as P0135, but for Bank 2.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0156", "O2 Sensor Circuit Malfunction (Bank 2 Sensor 2): Same as P0136, but for Bank 2.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0161", "O2 Sensor Heater Circuit Malfunction (Bank 2 Sensor 2): Same as P0141, but for Bank 2.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0170", "Fuel Trim Malfunction (Bank 1): General fault with the fuel trim system on Bank 1, meaning the ECU is having trouble maintaining the ideal air/fuel ratio. Could be lean or rich. Needs further diagnosis. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0171", "System Too Lean (Bank 1): Engine on 'Bank 1' gets too much air, not enough fuel. Caused by vacuum leaks, faulty MAF, low fuel pressure. Rough idle, misfires, poor fuel economy. Check Engine Light on. Long-term lean can damage engine/catalyst. Prompt diagnosis needed.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0172", "System Too Rich (Bank 1): Engine on 'Bank 1' gets too much fuel, not enough air. Caused by leaking injector, faulty fuel pressure regulator, bad MAF/O2 sensor. Black smoke, poor fuel economy, fuel smell. Check Engine Light on. Can foul plugs, damage catalyst.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0173", "Fuel Trim Malfunction (Bank 2): Same as P0170, but for Bank 2.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0174", "System Too Lean (Bank 2): Same as P0171, but for Bank 2.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0175", "System Too Rich (Bank 2): Same as P0172, but for Bank 2.");
        // --- Ateşleme Sistemi ---
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0300", "Random/Multiple Cylinder Misfire Detected: One or more cylinders aren't firing correctly. Due to worn plugs, faulty coils/wires, injector problems, low compression. Rough engine, shaking, power loss, flashing Check Engine Light (severe, can damage catalyst). If flashing, reduce speed/load, check ASAP.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0301", "Cylinder 1 Misfire Detected: Cylinder #1 is misfiring. See P0300 for causes/symptoms. Focus diagnosis on cylinder 1 components.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0302", "Cylinder 2 Misfire Detected: Cylinder #2 is misfiring. See P0300 for causes/symptoms. Focus diagnosis on cylinder 2 components.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0303", "Cylinder 3 Misfire Detected: Cylinder #3 is misfiring. See P0300 for causes/symptoms. Focus diagnosis on cylinder 3 components.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0304", "Cylinder 4 Misfire Detected: Cylinder #4 is misfiring. See P0300 for causes/symptoms. Focus diagnosis on cylinder 4 components.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0305", "Cylinder 5 Misfire Detected: Cylinder #5 is misfiring. See P0300 for causes/symptoms. Focus diagnosis on cylinder 5 components.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0306", "Cylinder 6 Misfire Detected: Cylinder #6 is misfiring. See P0300 for causes/symptoms. Focus diagnosis on cylinder 6 components.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0325", "Knock Sensor 1 Circuit Malfunction (Bank 1 or Single Sensor): The knock sensor detects engine 'knock' or 'pinging' (pre-ignition). A circuit malfunction means an electrical issue with the sensor or wiring. Can lead to reduced power as ECU may retard timing to prevent knock. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0335", "Crankshaft Position Sensor 'A' Circuit Malfunction: This sensor monitors crankshaft speed and position, vital for ignition timing and fuel injection. A circuit fault can cause the engine to not start, stall, or run very poorly. Check Engine Light on. Critical sensor.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0340", "Camshaft Position Sensor 'A' Circuit Malfunction (Bank 1 or Single Sensor): This sensor tracks camshaft position, helping with fuel injection timing and variable valve timing. A fault can cause hard starting, no start, rough running, or reduced power. Check Engine Light on.");
        // --- Yardımcı Emisyon Kontrolleri ---
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0400", "Exhaust Gas Recirculation (EGR) Flow Malfunction: General fault in the EGR system, which recirculates exhaust to reduce emissions. Could be insufficient or excessive flow. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0401", "Exhaust Gas Recirculation (EGR) Flow Insufficient Detected: Not enough exhaust gas recirculated. Often a clogged EGR valve/passages. Engine pinging, failed emissions. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0403", "Exhaust Gas Recirculation (EGR) Control Circuit Malfunction: Electrical problem with the circuit that controls the EGR valve. The valve may not open or close correctly. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0404", "Exhaust Gas Recirculation (EGR) Control Circuit Range/Performance: EGR valve position or flow is not what the ECU expects. Valve might be sticking or sensor is inaccurate. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0410", "Secondary Air Injection System Malfunction: This system pumps fresh air into the exhaust during cold starts to help the catalytic converter warm up faster and reduce emissions. A malfunction means the system isn't working. Typically won't affect drivability but will cause failed emissions. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0420", "Catalyst System Efficiency Below Threshold (Bank 1): Catalytic converter on 'Bank 1' isn't reducing emissions efficiently. Often means converter is failing, but can be due to bad O2 sensor or engine problems damaging converter. Failed emissions test, possible reduced performance if clogged. Check Engine Light on. Converters are costly, so diagnose properly.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0430", "Catalyst System Efficiency Below Threshold (Bank 2): Same as P0420, but for Bank 2.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0440", "Evaporative Emission (EVAP) Control System Malfunction: EVAP system prevents fuel vapors from escaping. General fault (leak or other). Usually a loose gas cap, cracked hose, bad valve. Check Engine Light, slight fuel smell. Important for emissions.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0441", "Evaporative Emission (EVAP) Control System Incorrect Purge Flow: The EVAP system is not purging fuel vapors from the charcoal canister into the engine correctly (either no flow or flow when it shouldn't). Often a faulty purge valve. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0442", "Evaporative Emission (EVAP) Control System Leak Detected (Small Leak): Small leak in EVAP system. Most common: loose/damaged gas cap. Check cap first! Else, small crack in hose, faulty valve. Check Engine Light, failed emissions.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0443", "Evaporative Emission (EVAP) Control System Purge Control Valve Circuit Malfunction: Electrical problem with the circuit for the EVAP purge valve. Valve may not operate. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0446", "Evaporative Emission (EVAP) Control System Vent Control Circuit Malfunction: Electrical problem with the circuit for the EVAP vent valve/solenoid. This valve is crucial for sealing the system during leak tests and allowing air in/out of the canister. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0455", "Evaporative Emission (EVAP) Control System Leak Detected (Gross Leak/Large Leak): Significant EVAP leak. Gas cap off, large hose crack, disconnected line, faulty component. Strong fuel smell. Check Engine Light. Emissions issue, potential fire hazard.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0456", "Evaporative Emission (EVAP) Control System Leak Detected (Very Small Leak): Even smaller leak than P0442. Can be hard to find. Might be a pinhole in a hose or a slightly faulty seal. Check gas cap. Check Engine Light on.");
        // --- Araç Hızı, Rölanti Kontrolü ve Diğer Girdiler ---
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0500", "Vehicle Speed Sensor (VSS) 'A' Malfunction: VSS tells ECU vehicle speed. Used for speedometer, cruise, ABS, transmission. Malfunction: inaccurate/no speedo, cruise/ABS issues, erratic shifts. Check Engine Light. Safety concern.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0505", "Idle Air Control (IAC) System Malfunction: The IAC system controls engine idle speed. A malfunction can cause the idle to be too high, too low, or erratic, and can lead to stalling. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0506", "Idle Air Control (IAC) System RPM Lower Than Expected: Engine idle speed is consistently lower than the target set by the ECU. Could be a dirty throttle body, faulty IAC valve, or vacuum leak. May cause stalling. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0507", "Idle Air Control (IAC) System RPM Higher Than Expected: Engine idle speed is too high. Could be a faulty IAC valve, vacuum leak, or issue with the throttle position sensor. Wastes fuel. Check Engine Light on.");
        // --- Şanzıman (Transmission) P-Kodları ---
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0700", "Transmission Control System (TCS) Malfunction: This is a general fault code indicating that the Transmission Control Module (TCM) has detected a problem and has stored its own specific diagnostic trouble codes. You'll need a scanner capable of reading TCM codes to find the exact issue. The Check Engine Light will be on, and the transmission may behave erratically or go into 'limp mode' (stuck in one gear).");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0705", "Transmission Range Sensor Circuit Malfunction (PRNDL Input): The range sensor tells the TCM which gear is selected (Park, Reverse, Neutral, Drive, Low). A malfunction can cause incorrect gear display, inability to start (if it doesn't see Park/Neutral), or erratic shifting. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0715", "Input/Turbine Speed Sensor 'A' Circuit Malfunction: This sensor measures the rotational speed of the transmission's input shaft. This data is critical for shift timing and torque converter clutch operation. A fault can lead to harsh shifting, incorrect gear selection, or transmission slippage. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0720", "Output Speed Sensor Circuit Malfunction: This sensor measures the speed of the transmission's output shaft, which correlates to vehicle speed. Used for shift scheduling and speedometer. A fault can cause erratic shifting, incorrect speedometer readings. Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0730", "Incorrect Gear Ratio: The TCM has detected a mismatch between the expected gear ratio and the actual gear ratio being achieved. This often indicates internal transmission problems like slipping clutches or a failing torque converter. Serious issue, may lead to significant transmission damage if ignored. Check Engine Light on, drivability issues likely.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("P0740", "Torque Converter Clutch Circuit Malfunction: Problem with the electrical circuit that controls the torque converter clutch, which locks up at cruising speeds for better fuel efficiency. Failure can result in poor fuel economy and a sensation like the transmission is slipping. Check Engine Light on.");
        // === ŞASİ (C) KODLARI ===
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("C0021", "Brake Booster Performance: Problem with the brake booster, which assists in applying brake force. May result in a hard brake pedal or reduced braking effectiveness. ABS warning light may be on. Safety issue, inspect immediately.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("C0031", "Left Front Wheel Speed Sensor Circuit: Fault in the circuit for the left front wheel speed sensor. This will likely disable ABS and traction/stability control. ABS/TCS warning lights on. Conventional braking will work, but without anti-lock. Safety concern.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("C0032", "Left Front Wheel Speed Sensor Signal Erratic: Left front wheel speed sensor is providing an inconsistent or jumpy signal. See C0031 for implications.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("C0034", "Right Front Wheel Speed Sensor Circuit: Fault in the circuit for the right front wheel speed sensor. See C0031 for implications.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("C0035", "Left Front Wheel Speed Sensor Circuit Malfunction: (Duplicate, already well-described) Issue with the wheel speed sensor on the front left, affecting ABS or traction control. ABS/TCS lights on. Safety concern.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("C0037", "Left Rear Wheel Speed Sensor Circuit: Fault in the circuit for the left rear wheel speed sensor. See C0031 for implications.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("C0038", "Left Rear Wheel Speed Sensor Signal Erratic: Left rear wheel speed sensor is providing an inconsistent signal. See C0031 for implications.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("C0040", "Right Front Wheel Speed Sensor Malfunction: (Often a general fault) Problem with the right front wheel speed sensor. See C0031 for implications.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("C0051", "Steering Wheel Position Sensor Circuit Malfunction: This sensor monitors the steering wheel's angle. It's crucial for Electronic Stability Control (ESC) and sometimes adaptive headlights. A fault will likely disable ESC and illuminate its warning light. Can be a safety issue.");
        // === GÖVDE (B) KODLARI ===
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("B0001", "Driver Frontal Stage 1 Deployment Control: Issue with the primary deployment circuit for the driver's airbag. Airbag warning light will be on. CRITICAL safety issue: airbag may not deploy in an accident. Immediate professional attention required.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("B0012", "Driver Frontal Stage 2 Deployment Control (Subfault): (Duplicate, already well-described) Problem with driver's airbag (second stage). Airbag warning light on. Airbag may not deploy correctly. Serious safety issue, immediate attention needed.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("B1000", "ECU Malfunction: Generic code indicating an internal fault within an electronic control unit (e.g., Body Control Module, Airbag Module). Specific module needs to be identified. May cause various B-system features to fail. Further diagnostics needed.");
        // === AĞ (U) KODLARI ===
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("U0073", "Control Module Communication Bus 'A' Off: The main communication network (CAN bus) between various control modules in the vehicle is not functioning. This is a serious network problem. The vehicle may not start, or many systems may fail simultaneously. Requires advanced diagnostics.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("U0100", "Lost Communication With ECM/PCM 'A': (Duplicate, already well-described) Other modules lost communication with Engine/Powertrain Control Module. Can cause no-start, poor running. Advanced diagnostics needed. Check Engine Light and other warning lights likely on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("U0101", "Lost Communication With TCM (Transmission Control Module): The TCM is not communicating with other modules. May result in the transmission going into limp mode, harsh shifts, or Check Engine Light on.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("U0121", "Lost Communication With Anti-Lock Brake System (ABS) Control Module: The ABS module is not communicating. ABS and possibly traction/stability control will be disabled. ABS warning light on. Safety concern.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("U0140", "Lost Communication With Body Control Module (BCM): The BCM controls many body-related functions (lights, windows, locks, etc.). Loss of communication can cause these features to malfunction. Various symptoms possible. Diagnostics needed.");
        DTC_USER_FRIENDLY_DESCRIPTIONS.put("U0155", "Lost Communication With Instrument Panel Cluster (IPC) Control Module: The instrument cluster is not communicating. Gauges, warning lights, and displays on your dashboard may not work correctly or at all. Diagnostics needed.");
    }

    public static VehicleData.DTC getInterpretedDTC(String dtcCode) {
        String rawDescription = DTC_DESCRIPTIONS_RAW.getOrDefault(dtcCode.toUpperCase(), "Unknown or manufacturer-specific trouble code. Check service manual.");
        String userFriendlyDescription = DTC_USER_FRIENDLY_DESCRIPTIONS.get(dtcCode.toUpperCase());

        if (userFriendlyDescription != null && !userFriendlyDescription.isEmpty()) {
            return new VehicleData.DTC(dtcCode, userFriendlyDescription, true);
        } else {
            return new VehicleData.DTC(dtcCode, rawDescription, false);
        }
    }

    public SimpleOBD2Manager(Context context, BluetoothManager bluetoothManager) {
        this.context = context;
        this.bluetoothManager = bluetoothManager;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.vehicleData = new VehicleData();
    }

    public void setDataUpdateListener(DataUpdateListener listener) {
        this.dataUpdateListener = listener;
    }

    public void setSelectedProtocol(String proto) {
        if (proto != null && !proto.isEmpty()) {
            this.selectedProtocol = proto;
        } else {
            this.selectedProtocol = "0";
        }
    }

    public String getSelectedProtocol() {
        return selectedProtocol;
    }

    private boolean initializeELM327() {
        Log.i(TAG, "ELM327 başlatılıyor... seçilen protokol: " + selectedProtocol);
        try {
            InputStream in = bluetoothManager.getInputStream();
            OutputStream out = bluetoothManager.getOutputStream();

            if (in == null || out == null) {
                Log.e(TAG, "initializeELM327: InputStream veya OutputStream null!");
                return false;
            }

            clearInputStream(in);

            String[] initCommands = {
                    "ATZ", "ATE0", "ATL0", "ATH0", "ATS0", "ATSP" + selectedProtocol
            };

            for (String cmd : initCommands) {
                String response = sendCommandAndReadResponse(cmd, out, in, INIT_COMMAND_RESPONSE_TIMEOUT_MS);
                Log.d(TAG, "INIT CMD: " + cmd + " -> RESPONSE: [" + (response != null ? response.replace("\r", "\\r").replace("\n", "\\n") : "null") + "]");
                if (response == null ||
                        (!response.toUpperCase().contains("OK") &&
                                !(cmd.equalsIgnoreCase("ATZ") && (response.toUpperCase().contains("ELM") || response.contains("?"))) &&
                                !(cmd.startsWith("ATSP") && response.toUpperCase().contains("OK"))
                        )
                ) {
                    Log.e(TAG, "Başlatma komutu başarısız veya beklenmedik yanıt: " + cmd + " -> [" + response + "]");
                    if (cmd.startsWith("ATSP") && (response == null || !response.toUpperCase().contains("OK"))) {
                        return false;
                    }
                }
                Thread.sleep(300);
            }

            String protocolCheckResponse = sendCommandAndReadResponse("ATDP", out, in, INIT_COMMAND_RESPONSE_TIMEOUT_MS);
            Log.i(TAG, "Protokol kontrol (ATDP): " + protocolCheckResponse);
            if (protocolCheckResponse == null || protocolCheckResponse.toUpperCase().contains("AUTO") || protocolCheckResponse.toUpperCase().contains("SEARCHING")) {
                Log.w(TAG, "ATDP yanıtı 'AUTO' veya 'SEARCHING' içeriyor, bu protokolün henüz tam oturmadığını gösterebilir.");
            }

            String pidSupportResponse = sendCommandAndReadResponse("0100", out, in, INIT_COMMAND_RESPONSE_TIMEOUT_MS);
            Log.d(TAG, "INIT CMD: 0100 -> RESPONSE: [" + (pidSupportResponse != null ? pidSupportResponse.replace("\r", "\\r").replace("\n", "\\n") : "null") + "]");
            if (pidSupportResponse == null || !pidSupportResponse.replaceAll("\\s", "").toUpperCase().startsWith("4100")) {
                Log.w(TAG, "Temel OBD2 iletişimi (0100) yanıtı beklenmedik: [" + pidSupportResponse + "]");
            }

            Log.i(TAG, "ELM327 başlatma denemesi tamamlandı.");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "initializeELM327: Genel hata", e);
            return false;
        }
    }

    private void clearInputStream(InputStream in) throws IOException {
        int available = in.available();
        if (available > 0) {
            byte[] buffer = new byte[available];
            int bytesRead = in.read(buffer);
            Log.d(TAG, "InputStream temizlendi, " + bytesRead + " byte atıldı.");
        }
    }

    private String sendCommandAndReadResponse(String command, OutputStream out, InputStream in, long timeoutMs) throws IOException, InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            Log.w(TAG, "sendCommandAndReadResponse: Thread kesildi, komut gönderilmiyor.");
            throw new InterruptedException("Command sending interrupted");
        }
        String commandWithCR = command.trim() + "\r";
        Log.d(TAG, "Gönderiliyor: [" + commandWithCR.replace("\r", "\\r") + "]");
        out.write(commandWithCR.getBytes(StandardCharsets.US_ASCII));
        out.flush();
        return readResponse(in, timeoutMs);
    }

    private String readResponse(InputStream inputStream, long timeoutMs) throws IOException {
        StringBuilder responseBuffer = new StringBuilder();
        StringBuilder rawHexBuffer = new StringBuilder();
        long startTime = System.currentTimeMillis();
        int consecutiveNoDataReads = 0;
        final int MAX_CONSECUTIVE_NO_DATA_BEFORE_SLEEP = 20;
        final int SLEEP_INTERVAL_NO_DATA = 10;
        final int SLEEP_INTERVAL_HAS_DATA = 1;

        Log.v(TAG, "Yanıt bekleniyor (timeout: " + timeoutMs + "ms)...");

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (Thread.currentThread().isInterrupted()) {
                Log.w(TAG, "readResponse: Thread kesildi, okuma durduruluyor.");
                throw new IOException("Read operation interrupted by thread interruption");
            }

            if (inputStream.available() > 0) {
                consecutiveNoDataReads = 0;
                int dataByte = inputStream.read();

                if (dataByte == -1) {
                    Log.w(TAG, "readResponse: Stream sonu (-1).");
                    break;
                }

                rawHexBuffer.append(String.format("%02X ", dataByte));
                char character = (char) dataByte;

                if (character == '>') {
                    Log.v(TAG, "readResponse: Prompt '>' bulundu.");
                    break;
                }
                if (character != '\r' && character != '\n' && dataByte != 0x00) {
                    responseBuffer.append(character);
                }
                try { Thread.sleep(SLEEP_INTERVAL_HAS_DATA); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break;}

            } else {
                consecutiveNoDataReads++;
                try {
                    Thread.sleep(SLEEP_INTERVAL_NO_DATA);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.w(TAG, "readResponse: Bekleme kesildi.", e);
                    break;
                }
            }
        }

        Log.d(TAG, "readResponse - Ham Hex: [" + rawHexBuffer.toString().trim() + "]");
        String finalResponse = responseBuffer.toString().replaceAll("\\s+", " ").trim();
        Log.d(TAG, "readResponse - İşlenmiş: [" + finalResponse + "]");

        if (System.currentTimeMillis() - startTime >= timeoutMs && !rawHexBuffer.toString().contains("3E")) {
            Log.w(TAG, "readResponse: Timeout! Alınan (işlenmiş): [" + finalResponse + "]");
        }
        return finalResponse;
    }

    public void startReading() {
        if (!bluetoothManager.isConnected()) {
            showToast("OBD2 cihazına bağlı değil!");
            Log.w(TAG, "startReading: Bluetooth bağlı değil.");
            if (dataUpdateListener != null) mainHandler.post(dataUpdateListener::onConnectionLost);
            return;
        }
        if (isReading) {
            Log.d(TAG, "startReading: Zaten okunuyor.");
            return;
        }

        Log.i(TAG, "Veri okuma başlatılıyor...");
        isReading = true;
        consecutiveFailures = 0;

        // Yeni bir okuma seansı başladığında, önceki DTC listesini temizle.
        // Bu, bir önceki bağlantıdan kalan DTC'lerin yanlışlıkla "yeni" olarak
        // algılanmasını önler. İlk DTC okumasında hepsi yeni gibi gelecektir, bu doğru.
        if (previousDTCs != null) {
            previousDTCs.clear();
        } else {
            previousDTCs = new ArrayList<>(); // Null ise başlat
        }


        if (scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "OBDScheduler"));
        }

        new Thread(() -> {
            if (initializeELM327()) {
                mainHandler.post(() -> showToast("ELM327 başarıyla başlatıldı"));
                Log.i(TAG, "ELM327 başarıyla başlatıldı, periyodik okuma planlanıyor.");

                fillCommandQueue();
                processNextCommandFromQueue(); // İlk komut setini hemen başlat

                if (scheduler == null || scheduler.isShutdown()) { // Tekrar kontrol et, thread içinde değişmiş olabilir
                    scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "OBDScheduler"));
                }
                try {
                    // fillCommandQueueAndProcess, kuyruğu doldurup processNextCommandFromQueue'u çağıracak.
                    scheduler.scheduleWithFixedDelay(this::fillCommandQueueAndProcess,
                            SCHEDULER_INITIAL_DELAY_S, SCHEDULER_PERIOD_S, TimeUnit.SECONDS);
                    Log.d(TAG, "Scheduler planlandı. Initial: " + SCHEDULER_INITIAL_DELAY_S + "s, Period: " + SCHEDULER_PERIOD_S + "s");
                } catch (Exception e) {
                    Log.e(TAG, "Scheduler başlatılırken hata", e);
                    stopReadingInternally(); // Hata durumunda okumayı durdur
                }
            } else {
                mainHandler.post(() -> {
                    showToast("ELM327 başlatılamadı.");
                    Log.e(TAG, "ELM327 başlatılamadı.");
                });
                stopReadingInternally(); // Başlatma başarısız olursa okumayı durdur
            }
        }, "OBDInitThread").start();
    }

    private void stopReadingInternally() {
        isReading = false;
        clearQueueAndStopProcessingIfNeeded();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1500, TimeUnit.MILLISECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        // Önceki DTC listesini temizle, böylece bir sonraki bağlantıda
        // mevcut tüm DTC'ler "yeni" olarak algılanmaz.
        if (previousDTCs != null) {
            previousDTCs.clear();
        }
        Log.i(TAG, "OBD2 veri okuma dahili olarak durduruldu.");
        if (dataUpdateListener != null) {
            mainHandler.post(dataUpdateListener::onConnectionLost);
        }
    }


    private void fillCommandQueueAndProcess() {
        if (!isReading || !bluetoothManager.isConnected() || Thread.currentThread().isInterrupted()) {
            Log.d(TAG, "fillCommandQueueAndProcess: Okuma aktif değil, BT bağlı değil veya thread kesildi. İşlem yapılmayacak.");
            return;
        }
        Log.d(TAG, "Periyodik görev: Kuyruk dolduruluyor ve işlem başlatılıyor.");
        fillCommandQueue();
        if (!isProcessingQueue.get()) {
            processNextCommandFromQueue();
        }
    }

    private void fillCommandQueue() {
        synchronized (commandQueue) {
            commandQueue.clear();
            commandQueue.offer(VEHICLE_SPEED_PID);
            commandQueue.offer(ENGINE_RPM_PID);
            commandQueue.offer(COOLANT_TEMP_PID);
            commandQueue.offer(ENGINE_LOAD_PID);
            commandQueue.offer(THROTTLE_POS_PID);
            if (FUEL_LEVEL_PID != null) commandQueue.offer(FUEL_LEVEL_PID);
            if (INTAKE_TEMP_PID != null) commandQueue.offer(INTAKE_TEMP_PID);
            if (MAF_AIR_FLOW_PID != null) commandQueue.offer(MAF_AIR_FLOW_PID);
            commandQueue.offer(DTC_REQUEST_PID);
            commandQueue.offer(VEHICLE_VIN_PID); // VIN komutu eklendi
            Log.d(TAG, "Kuyruğa " + commandQueue.size() + " komut eklendi: " + commandQueue.toString());
        }
    }

    private void processNextCommandFromQueue() {
        if (!isReading) { Log.d(TAG, "processNext: Okuma durdurulmuş."); isProcessingQueue.set(false); return; }
        if (!bluetoothManager.isConnected()) { Log.w(TAG, "processNext: BT bağlı değil."); isProcessingQueue.set(false); handleConnectionIssue(); clearQueueAndStopProcessingIfNeeded(); return; }
        if (Thread.currentThread().isInterrupted()) { Log.w(TAG, "processNext: Thread kesildi."); isProcessingQueue.set(false); return; }

        if (!isProcessingQueue.compareAndSet(false, true)) {
            return;
        }

        String pidToProcess;
        synchronized (commandQueue) {
            if (commandQueue.isEmpty()) {
                isProcessingQueue.set(false);
                Log.d(TAG, "Kuyruk boş.");
                return;
            }
            pidToProcess = commandQueue.poll();
            Log.d(TAG, "Kuyruktan alındı ve işlenecek: " + pidToProcess);
        }

        if (pidToProcess == null) {
            isProcessingQueue.set(false);
            return;
        }

        try {
            InputStream in = bluetoothManager.getInputStream();
            OutputStream out = bluetoothManager.getOutputStream();
            if (in == null || out == null) {
                Log.e(TAG, "processNext: InputStream veya OutputStream null!");
                isProcessingQueue.set(false);
                handleConnectionIssue();
                clearQueueAndStopProcessingIfNeeded();
                return;
            }

            String response = sendCommandAndReadResponse(pidToProcess, out, in, READ_RESPONSE_TIMEOUT_MS);
            parseAndStoreData(pidToProcess, response);
            Thread.sleep(COMMAND_INTERVAL_MS);
        } catch (IOException e) {
            Log.e(TAG, "Komut (" + pidToProcess + ") işlenirken IOException", e);
            handleConnectionIssue();
        } catch (InterruptedException e) {
            Log.w(TAG, "Komut (" + pidToProcess + ") işlenirken InterruptedException", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Log.e(TAG, "Komut (" + pidToProcess + ") işlenirken genel hata", e);
            consecutiveFailures++;
            checkConsecutiveFailures();
        } finally {
            isProcessingQueue.set(false);
            if (isReading && bluetoothManager.isConnected() && !Thread.currentThread().isInterrupted()) {
                synchronized (commandQueue) {
                    if (!commandQueue.isEmpty()) {
                        processNextCommandFromQueue();
                    } else {
                        Log.d(TAG, "Kuyruk boşaldı, scheduler'ın bir sonraki çalıştırması beklenecek.");
                    }
                }
            } else {
                clearQueueAndStopProcessingIfNeeded();
            }
        }
    }

    private void clearQueueAndStopProcessingIfNeeded() {
        if (!isReading || !bluetoothManager.isConnected() || Thread.currentThread().isInterrupted()) {
            Log.d(TAG, "clearQueue: Okuma durdu/bağlantı kesildi/thread kesildi, kuyruk temizleniyor.");
            synchronized (commandQueue) {
                commandQueue.clear();
            }
            isProcessingQueue.set(false);
        }
    }

    // SimpleOBD2Manager.java

// ... (Diğer importlar ve sınıfın başındaki değişken tanımlamaları,
// THRESHOLD_ENGINE_TEMP_HIGH, THRESHOLD_FUEL_LEVEL_LOW,
// criticalDataAlertListener ve previousDTCs dahil) ...

    private void parseAndStoreData(String pidWithoutSuffix, String rawResponse) {
        boolean dataUpdatedThisParse = false;
        boolean currentPidSuccessfullyProcessed = false;

        if (rawResponse == null || rawResponse.isEmpty()) {
            Log.w(TAG, "PID (" + pidWithoutSuffix + ") için null veya boş yanıt alındı.");
            consecutiveFailures++;
            updateVehicleDataWithError(pidWithoutSuffix, false);
            checkConsecutiveFailures();
            return;
        }

        String cleanedResponse = rawResponse.replaceAll("[\\s\\p{Cntrl}]", "").toUpperCase();

        boolean isNoDataResponse = cleanedResponse.contains("NODATA");
        boolean isErrorResponse = cleanedResponse.contains("ERROR") ||
                cleanedResponse.contains("?") ||
                cleanedResponse.contains("UNABLETOCONNECT");
        boolean isSearchingWithoutData = cleanedResponse.contains("SEARCHING") &&
                !pidWithoutSuffix.equals(DTC_REQUEST_PID) && !pidWithoutSuffix.equals(VEHICLE_VIN_PID) &&
                !cleanedResponse.matches(".*41" + pidWithoutSuffix.substring(2) + "[0-9A-F]{2,}.*");

        if (isNoDataResponse && !cleanedResponse.matches(".*[1-9A-F].*") && !isErrorResponse) {
            Log.i(TAG, "PID (" + pidWithoutSuffix + ") için 'NO DATA' yanıtı alındı.");
            vehicleData.setSpecificFieldToNull(pidWithoutSuffix);
            currentPidSuccessfullyProcessed = true;
            dataUpdatedThisParse = true;
        } else if (isErrorResponse || isSearchingWithoutData) {
            Log.w(TAG, "PID (" + pidWithoutSuffix + ") için geçersiz veya hatalı yanıt: [" + rawResponse + "] -> Temizlenmiş: [" + cleanedResponse + "]");
            consecutiveFailures++;
            updateVehicleDataWithError(pidWithoutSuffix, false);
            checkConsecutiveFailures();
            return;
        }

        Double parsedValue = null;
        boolean parseAttemptedForThisPid = true;

        try {
            if (pidWithoutSuffix.equals(DTC_REQUEST_PID)) {
                parseDTCsFromResponse(rawResponse);
                // parseDTCsFromResponse içindeki mantığa göre consecutiveFailures yönetilir.
                // Eğer "NO DATA" veya hata değilse, bir şey geldi demektir.
                if (!isErrorResponse && !isNoDataResponse) {
                    currentPidSuccessfullyProcessed = true; // Başarılı parse parseDTCsFromResponse içinde belirlenir.
                }
                dataUpdatedThisParse = true;
            } else if (pidWithoutSuffix.equals(VEHICLE_VIN_PID)) {
                parseVinFromResponse(rawResponse);
                // parseVinFromResponse içindeki mantığa göre consecutiveFailures yönetilir.
                if (vehicleData.getVin() != null && !vehicleData.getVin().isEmpty()) {
                    currentPidSuccessfullyProcessed = true;
                } else if (isResponsePotentiallyValidForNull(cleanedResponse, VEHICLE_VIN_PID)){ // NO DATA gibi bir durum
                    currentPidSuccessfullyProcessed = true;
                }
                dataUpdatedThisParse = true;
            } else {
                // Diğer PID'ler için switch-case yapısı
                switch (pidWithoutSuffix) {
                    case VEHICLE_SPEED_PID:
                        parsedValue = parseSpeedFromResponseInternal(cleanedResponse);
                        if (parsedValue != null) vehicleData.setSpeed(parsedValue);
                        else updateVehicleDataWithError(pidWithoutSuffix, true);
                        break;
                    case ENGINE_RPM_PID:
                        parsedValue = parseRpmFromResponseInternal(cleanedResponse);
                        if (parsedValue != null) vehicleData.setRpm(parsedValue);
                        else updateVehicleDataWithError(pidWithoutSuffix, true);
                        break;
                    case COOLANT_TEMP_PID:
                        parsedValue = parseTempFromResponseInternal(cleanedResponse, COOLANT_TEMP_PID);
                        if (parsedValue != null) vehicleData.setEngineTemp(parsedValue);
                        else updateVehicleDataWithError(pidWithoutSuffix, true);
                        break;
                    case ENGINE_LOAD_PID:
                        parsedValue = parsePercentageFromResponseInternal(cleanedResponse, ENGINE_LOAD_PID);
                        if (parsedValue != null) vehicleData.setEngineLoad(parsedValue);
                        else updateVehicleDataWithError(pidWithoutSuffix, true);
                        break;
                    case THROTTLE_POS_PID:
                        parsedValue = parsePercentageFromResponseInternal(cleanedResponse, THROTTLE_POS_PID);
                        if (parsedValue != null) vehicleData.setThrottlePosition(parsedValue);
                        else updateVehicleDataWithError(pidWithoutSuffix, true);
                        break;
                    case FUEL_LEVEL_PID: // Bu PID zaten vardı
                        parsedValue = parsePercentageFromResponseInternal(cleanedResponse, FUEL_LEVEL_PID);
                        vehicleData.setFuelLevel(parsedValue); // Null olabileceği için direkt ata
                        if (parsedValue == null && !isResponsePotentiallyValidForNull(cleanedResponse, FUEL_LEVEL_PID)) {
                            updateVehicleDataWithError(pidWithoutSuffix, true);
                        }
                        break;
                    case INTAKE_TEMP_PID: // Bu PID zaten vardı
                        parsedValue = parseTempFromResponseInternal(cleanedResponse, INTAKE_TEMP_PID);
                        vehicleData.setIntakeTemp(parsedValue); // Null olabileceği için direkt ata
                        if (parsedValue == null && !isResponsePotentiallyValidForNull(cleanedResponse, INTAKE_TEMP_PID)) {
                            updateVehicleDataWithError(pidWithoutSuffix, true);
                        }
                        break;
                    case MAF_AIR_FLOW_PID: // Bu PID zaten vardı
                        parsedValue = parseMafFromResponseInternal(cleanedResponse);
                        vehicleData.setMafAirFlow(parsedValue); // Null olabileceği için direkt ata
                        if (parsedValue == null && !isResponsePotentiallyValidForNull(cleanedResponse, MAF_AIR_FLOW_PID)) {
                            updateVehicleDataWithError(pidWithoutSuffix, true);
                        }
                        break;
                    // FUEL_RATE_PID ve FUEL_TYPE_PID ile ilgili case'ler kaldırıldı.
                    default:
                        Log.w(TAG, "parseAndStoreData: Bilinmeyen veya işlenmeyen PID: " + pidWithoutSuffix + " (Yanıt: " + cleanedResponse + ")");
                        parseAttemptedForThisPid = false;
                        break;
                }

                if (parseAttemptedForThisPid) {
                    if (parsedValue != null) {
                        Log.d(TAG, "PID (" + pidWithoutSuffix + ") BAŞARIYLA parse edildi. Değer: " + parsedValue);
                        currentPidSuccessfullyProcessed = true;
                        dataUpdatedThisParse = true;
                    } else if (pidWithoutSuffix.equals(FUEL_LEVEL_PID) || pidWithoutSuffix.equals(INTAKE_TEMP_PID) ||
                            pidWithoutSuffix.equals(MAF_AIR_FLOW_PID) ) {
                        // Bu PID'ler için null dönmesi, 'NO DATA' anlamına gelebilir ve bir "başarı"dır.
                        if (isResponsePotentiallyValidForNull(cleanedResponse, pidWithoutSuffix)) {
                            Log.i(TAG, "PID (" + pidWithoutSuffix + ") için veri alınamadı (null döndü ama geçerli yanıt).");
                            currentPidSuccessfullyProcessed = true;
                            dataUpdatedThisParse = true;
                        } else {
                            Log.w(TAG, "PID (" + pidWithoutSuffix + ") parse edilemedi (geçersiz yanıt ve null değer).");
                        }
                    } else {
                        Log.w(TAG, "PID (" + pidWithoutSuffix + ") parse edilemedi (parsedValue null).");
                    }
                }
            } // switch-case sonu (else bloğu)

            if (currentPidSuccessfullyProcessed) {
                consecutiveFailures = 0;
                Log.v(TAG, "PID (" + pidWithoutSuffix + ") başarıyla işlendi veya 'NO DATA' olarak kabul edildi, hata sayacı sıfırlandı.");
            } else if (!isErrorResponse && !isNoDataResponse) {
                Log.w(TAG, "PID (" + pidWithoutSuffix + ") için işlem başarısız oldu ancak hata/NO DATA değil. Yanıt: ["+cleanedResponse+"]");
            }

        } catch (Exception e) {
            Log.e(TAG, "PID (" + pidWithoutSuffix + ") parse edilirken KRİTİK HATA: [" + rawResponse + "]", e);
            consecutiveFailures++;
            updateVehicleDataWithError(pidWithoutSuffix, false);
        }

        checkConsecutiveFailures();

        if (isReading && dataUpdateListener != null) {
            VehicleData snapshot = vehicleData.deepCopy();
            mainHandler.post(() -> dataUpdateListener.onDataUpdate(snapshot));

            if (dataUpdatedThisParse || pidWithoutSuffix.equals(DTC_REQUEST_PID)) {
                checkForCriticalConditions(vehicleData);
            }
        }
    }

    public void setCriticalDataAlertListener(CriticalDataAlertListener listener) {
        this.criticalDataAlertListener = listener;
    }

    // parseAndStoreData metodunun sonunda bu kontrolü ekleyebiliriz
    private void checkForCriticalConditions(VehicleData currentData) {
        if (criticalDataAlertListener == null) {
            return; // Listener set edilmemişse bir şey yapma
        }

        // 1. Yüksek Motor Sıcaklığı Kontrolü
        if (currentData.getEngineTemp() != null && currentData.getEngineTemp() > THRESHOLD_ENGINE_TEMP_HIGH) {
            criticalDataAlertListener.onHighEngineTemperature(currentData.getEngineTemp(), THRESHOLD_ENGINE_TEMP_HIGH);
        }

        // 2. Düşük Yakıt Seviyesi Kontrolü
        if (currentData.getFuelLevel() != null && currentData.getFuelLevel() >= 0 && currentData.getFuelLevel() < THRESHOLD_FUEL_LEVEL_LOW) {
            criticalDataAlertListener.onLowFuelLevel(currentData.getFuelLevel(), THRESHOLD_FUEL_LEVEL_LOW);
        }

        // 3. Yeni DTC Kontrolü
        List<VehicleData.DTC> currentDtcs = currentData.getDiagnosticTroubleCodes();
        if (currentDtcs != null) {
            List<VehicleData.DTC> newDetectedDtcs = new ArrayList<>();
            for (VehicleData.DTC currentDtc : currentDtcs) {
                boolean isNew = true;
                if (previousDTCs != null) { // previousDTCs null olabilir ilk çalıştırmada
                    for (VehicleData.DTC prevDtc : previousDTCs) {
                        if (prevDtc.code.equals(currentDtc.code)) {
                            isNew = false;
                            break;
                        }
                    }
                }
                if (isNew) {
                    newDetectedDtcs.add(currentDtc);
                }
            }

            if (!newDetectedDtcs.isEmpty()) {
                criticalDataAlertListener.onNewDtcDetected(newDetectedDtcs, currentDtcs);
            }
            // Bir sonraki kontrol için mevcut DTC'leri kaydet
            previousDTCs = new ArrayList<>(currentDtcs); // Derin kopya
        }
    }

    private void parseDTCsFromResponse(String rawResponse) {
        Log.d(TAG, "parseDTCsFromResponse çağrıldı. Gelen yanıt: [" + rawResponse + "]");
        vehicleData.clearDiagnosticTroubleCodes();
        boolean dataSuccessfullyParsed = false;

        if (rawResponse == null || rawResponse.isEmpty()) {
            Log.w(TAG, "parseDTCsFromResponse: DTC için null veya boş yanıt.");
            return;
        }

        String cleanedResponse = rawResponse.replaceAll("[\\s\\p{Cntrl}]", "").toUpperCase();
        Log.d(TAG, "parseDTCsFromResponse: DTC Yanıtı (işleniyor): " + cleanedResponse);

        if (cleanedResponse.contains("NODATA") || cleanedResponse.contains("ERROR") || cleanedResponse.contains("?") || cleanedResponse.contains("UNABLETOCONNECT")) {
            if (cleanedResponse.contains("NODATA") && !cleanedResponse.matches(".*[1-9A-F].*") && !cleanedResponse.contains("ERROR")) {
                Log.i(TAG, "DTC için 'NO DATA', muhtemelen arıza yok.");
                dataSuccessfullyParsed = true;
            } else {
                Log.w(TAG, "DTC yanıtı hata içeriyor: " + cleanedResponse);
            }
            if(dataSuccessfullyParsed) consecutiveFailures = 0;
            return;
        }

        String dtcDataSegment = cleanedResponse;
        if (cleanedResponse.startsWith("43")) {
            dtcDataSegment = cleanedResponse.substring(2);
        } else if (cleanedResponse.matches("^[0-9A-F]+$") && cleanedResponse.length() % 4 == 0) {
            Log.d(TAG, "DTC yanıtı '43' ile başlamıyor ama geçerli hex segmenti gibi görünüyor: " + cleanedResponse);
        } else if (cleanedResponse.contains("P") || cleanedResponse.contains("C") || cleanedResponse.contains("B") || cleanedResponse.contains("U")) {
            Log.w(TAG, "DTC yanıtı metin tabanlı görünüyor, şu an için sadece hex tabanlı işleme: " + cleanedResponse);
            return;
        }

        List<VehicleData.DTC> foundDtcs = new ArrayList<>();
        if (dtcDataSegment.length() >= 4 && dtcDataSegment.matches("^[0-9A-F]+$")) {
            for (int i = 0; i + 3 < dtcDataSegment.length(); i += 4) {
                String hexCodePart = dtcDataSegment.substring(i, i + 4);
                if (hexCodePart.equals("0000")) {
                    continue;
                }
                int firstByteValue = Integer.parseInt(hexCodePart.substring(0, 2), 16);
                String actualDtcType;
                if ((firstByteValue >> 6) == 0) actualDtcType = "P";
                else if ((firstByteValue >> 6) == 1) actualDtcType = "C";
                else if ((firstByteValue >> 6) == 2) actualDtcType = "B";
                else actualDtcType = "U";
                String finalFormattedDtcCode = actualDtcType + hexCodePart;

                VehicleData.DTC interpretedDTC = getInterpretedDTC(finalFormattedDtcCode);
                foundDtcs.add(interpretedDTC);
                Log.i(TAG, "parseDTCsFromResponse: Bulunan ve Yorumlanan DTC: " + interpretedDTC.code + " - " + interpretedDTC.description);
                dataSuccessfullyParsed = true;
            }
        } else if (!dtcDataSegment.isEmpty() && !dtcDataSegment.matches("^[0-9A-F]+$") &&
                (dtcDataSegment.contains("P") || dtcDataSegment.contains("C") || dtcDataSegment.contains("B") || dtcDataSegment.contains("U"))) {
            Log.d(TAG, "Metin tabanlı DTC parse etme denemesi: " + dtcDataSegment);
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([PCBU][0-9A-F]{3,4})");
            java.util.regex.Matcher matcher = pattern.matcher(dtcDataSegment);
            while (matcher.find()) {
                String code = matcher.group(1).toUpperCase();
                if(code.matches("[PCBU][0-9A-F]{3}")){
                    code = code.charAt(0) + "0" + code.substring(1);
                } else if (!code.matches("[PCBU][0-9A-F]{4}")){
                    Log.w(TAG, "Tanınmayan metin tabanlı DTC formatı: " + code);
                    continue;
                }
                VehicleData.DTC interpretedDTC = getInterpretedDTC(code);
                foundDtcs.add(interpretedDTC);
                Log.i(TAG, "Bulunan ve Yorumlanan Metin Tabanlı DTC: " + interpretedDTC.code + " - " + interpretedDTC.description);
                dataSuccessfullyParsed = true;
            }
            if (!dataSuccessfullyParsed && !dtcDataSegment.isEmpty()){
                Log.w(TAG, "Metin tabanlı DTC parse edilemedi: " + dtcDataSegment);
            }
        } else if (dtcDataSegment.isEmpty() || dtcDataSegment.equals("00") || dtcDataSegment.matches("0{1,8}")) {
            Log.i(TAG, "DTC yanıtı (veya segmenti) boş veya sıfır, arıza yok olarak yorumlandı.");
            dataSuccessfullyParsed = true;
        } else if (!dtcDataSegment.isEmpty()){
            Log.w(TAG, "DTC verisi parse edilemedi veya tanınmadı: [" + dtcDataSegment + "]");
        }

        vehicleData.setDiagnosticTroubleCodes(foundDtcs);
        Log.d(TAG, "parseDTCsFromResponse: VehicleData'ya " + foundDtcs.size() + " DTC eklendi.");
        if (dataSuccessfullyParsed) {
            consecutiveFailures = 0;
        }
    }
    // SimpleOBD2Manager.java

// ... (Diğer importlar ve sınıfın başındaki değişken tanımlamaları) ...

    private void parseVinFromResponse(String rawResponse) {
        Log.d(TAG, "parseVinFromResponse çağrıldı. Gelen yanıt: [" + rawResponse + "]");
        vehicleData.setVin(null); // Önceki VIN'i temizle
        boolean vinSuccessfullyParsed = false;

        if (rawResponse == null || rawResponse.isEmpty()) {
            Log.w(TAG, "VIN için null veya boş yanıt.");
            return;
        }

        String[] lines = rawResponse.split("\\r?\\n");
        StringBuilder relevantResponseBuilder = new StringBuilder();
        for (String line : lines) {
            String trimmedLine = line.replaceAll("[\\s]", "").toUpperCase();
            if (trimmedLine.contains("4902") ||
                    (trimmedLine.matches("^[0-9A-F:]+$") &&
                            !trimmedLine.contains("BUSINIT") &&
                            !trimmedLine.contains("ELM") &&
                            !trimmedLine.contains("OK") &&
                            !trimmedLine.contains("SEARCHING") &&
                            !trimmedLine.contains("STOPPED") &&
                            !trimmedLine.contains("NODATA") &&
                            !trimmedLine.contains("UNABLETOCONNECT") &&
                            !trimmedLine.contains("?") )) {
                relevantResponseBuilder.append(trimmedLine);
            } else if (trimmedLine.contains("NODATA") || trimmedLine.contains("ERROR") || trimmedLine.contains("?")) {
                Log.d(TAG, "VIN parse - Yanıtta potansiyel hata/veri yok satırı: " + trimmedLine);
            }
        }
        String cleanedResponse = relevantResponseBuilder.toString();
        Log.d(TAG, "parseVinFromResponse: VIN Yanıtı (işleniyor ve birleştirilmiş): " + cleanedResponse);

        if (cleanedResponse.isEmpty() || cleanedResponse.contains("NODATA") || cleanedResponse.contains("ERROR") || cleanedResponse.contains("?")) {
            if (cleanedResponse.contains("NODATA") && !cleanedResponse.contains("ERROR")) {
                Log.i(TAG, "VIN için 'NO DATA' yanıtı alındı.");
                vinSuccessfullyParsed = true;
            } else {
                Log.w(TAG, "VIN yanıtı hata içeriyor veya işlenecek veri yok: " + cleanedResponse);
            }
            if(vinSuccessfullyParsed) consecutiveFailures = 0;
            return;
        }

        StringBuilder vinHexPayloadBuilder = new StringBuilder();
        String[] parts = cleanedResponse.split(":");

        for (String part : parts) {
            if (part.isEmpty()) continue;

            Log.v(TAG, "VIN parse - İşlenen Ham Part: " + part);
            StringBuilder partVinHex = new StringBuilder();
            boolean vinCharStartedInPart = false;
            int startIndexForPart = 0;

            // Eğer part "4902" ile başlıyorsa, VIN verisi genellikle belirli bir ofsetten sonra başlar.
            // Örn: "490201W0V..." -> "W0V..." kısmı hex "573056..."
            // "4902" (4 karakter) + "XX" (mesaj sayısı/tipi, 2 karakter) = 6 karakter ofset.
            // Bazen "4902XXXX[VIN_DATA]" olabilir, bu durumda 8 karakter ofset.
            // İlk geçerli VIN karakterini arayarak daha güvenli olabiliriz.
            if (part.startsWith("4902")) {
                boolean foundVinStart = false;
                for (int vinSearchIndex = 4; (vinSearchIndex + 1) < part.length(); vinSearchIndex += 2) { // "4902" sonrasından başla
                    String potentialPair = part.substring(vinSearchIndex, vinSearchIndex + 2);
                    try {
                        int charVal = Integer.parseInt(potentialPair, 16);
                        if ((charVal >= 0x30 && charVal <= 0x39) || // 0-9
                                (charVal >= 0x41 && charVal <= 0x5A) || // A-Z
                                (charVal >= 0x61 && charVal <= 0x7A)) { // a-z
                            startIndexForPart = vinSearchIndex;
                            foundVinStart = true;
                            Log.d(TAG, "VIN parse - 4902 Part içinde VIN başlangıcı " + startIndexForPart + ". index'te bulundu: " + potentialPair);
                            break;
                        }
                    } catch (NumberFormatException ignored) {}
                }
                if (!foundVinStart) {
                    Log.w(TAG, "VIN parse - 4902 Part içinde geçerli VIN hex başlangıcı bulunamadı: " + part);
                    continue; // Bu part'tan VIN verisi alamadık, sonraki part'a geç.
                }
            }
            // Değilse, startIndexForPart = 0 olarak kalır, part'ın başından itibaren taranır.

            for (int j = startIndexForPart; (j + 1) < part.length(); j += 2) {
                String hexPair = part.substring(j, j + 2);
                try {
                    int charVal = Integer.parseInt(hexPair, 16);
                    if ((charVal >= 0x30 && charVal <= 0x39) ||
                            (charVal >= 0x41 && charVal <= 0x5A) ||
                            (charVal >= 0x61 && charVal <= 0x7A)) {
                        partVinHex.append(hexPair);
                        vinCharStartedInPart = true;
                    } else if (vinCharStartedInPart) {
                        Log.d(TAG, "VIN parse - Part içinde VIN sonrası potansiyel gürültü/geçersiz hex: " + hexPair + " in " + part);
                    } else {
                        Log.v(TAG, "VIN parse - Part başında VIN olmayan hex (atlandı): " + hexPair + " in " + part);
                    }
                } catch (NumberFormatException nfe) {
                    Log.w(TAG, "VIN parse - Part içinde geçersiz hex çifti: " + hexPair + " in " + part);
                }
            }
            if (partVinHex.length() > 0) {
                vinHexPayloadBuilder.append(partVinHex);
                Log.d(TAG, "VIN parse - Part'tan VIN Hex'i eklendi: " + partVinHex.toString());
            }
        }
        String vinHexPayload = vinHexPayloadBuilder.toString();

        if (vinHexPayload.isEmpty()) {
            Log.w(TAG, "VIN için işlenecek SON hex veri bulunamadı. İşlenmiş Orijinal: " + cleanedResponse);
            return;
        }
        Log.d(TAG, "VIN için birleştirilmiş/işlenmiş SON Hex Verisi: [" + vinHexPayload + "]");

        StringBuilder vinBuilder = new StringBuilder();
        int charCount = 0;
        for (int k = 0; (k + 1) < vinHexPayload.length() && charCount < 17; k += 2) {
            String hexPair = vinHexPayload.substring(k, k + 2);
            try {
                int charValue = Integer.parseInt(hexPair, 16);
                if (charValue == 0x00 && vinBuilder.length() > 0) {
                    Log.d(TAG, "VIN içinde null byte (00) bulundu, VIN sonlandırılıyor.");
                    break;
                }
                if ((charValue >= 0x30 && charValue <= 0x39) ||
                        (charValue >= 0x41 && charValue <= 0x5A) ||
                        (charValue >= 0x61 && charValue <= 0x7A)) {
                    vinBuilder.append((char) charValue);
                    charCount++;
                } else if (charValue != 0x00) {
                    Log.w(TAG, "VIN parse ederken ASCII olmayan veya geçersiz hex çifti atlandı (son karakter çevrimi): " + hexPair + " (decimal: " + charValue + ")");
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "VIN parse ederken geçersiz hex çifti (son karakter çevrimi): " + hexPair + " Hata: " + e.getMessage());
            }
        }

        String extractedVin = vinBuilder.toString().toUpperCase();
        if (extractedVin.length() == 17) {
            vehicleData.setVin(extractedVin);
            Log.i(TAG, "VIN BAŞARIYLA parse edildi (17 karakter): " + extractedVin);
            vinSuccessfullyParsed = true;
        } else if (extractedVin.length() > 0) {
            Log.w(TAG, "Kısmi VIN parse edildi (" + extractedVin.length() + " karakter): " + extractedVin + ". Son Hex payload: [" + vinHexPayload +"]");
        } else {
            Log.w(TAG, "VIN segmentinden geçerli ASCII karakter çıkarılamadı. Son Hex Payload: [" + vinHexPayload + "], İşlenmiş Yanıt: [" + cleanedResponse +"]");
        }

        if (vinSuccessfullyParsed) {
            consecutiveFailures = 0;
        }
    }

// ... (Sınıfın geri kalanı) ...


    private boolean isResponsePotentiallyValidForNull(String cleanedResponse, String pid) {
        return cleanedResponse.startsWith("41" + pid.substring(2));
    }

    private void updateVehicleDataWithError(String pid, boolean incrementFailureCounter) {
        Log.d(TAG, "PID ("+pid+") için hata durumu, veri 'null' olarak ayarlanıyor.");
        if (incrementFailureCounter) {
            consecutiveFailures++;
            Log.d(TAG, "Hata sayacı artırıldı: " + consecutiveFailures);
        }
        vehicleData.setSpecificFieldToNull(pid);
        if (dataUpdateListener != null) {
            VehicleData snapshot = vehicleData.deepCopy();
            mainHandler.post(() -> dataUpdateListener.onDataUpdate(snapshot));
        }
        if (incrementFailureCounter) {
            checkConsecutiveFailures();
        }
    }

    private String extractRelevantData(String cleanedResponse, String expectedPidConstant) {
        String pidDigits = expectedPidConstant.substring(2);
        String expectedHeader = "41" + pidDigits;
        int startIndex = cleanedResponse.indexOf(expectedHeader);
        if (startIndex != -1) {
            return cleanedResponse.substring(startIndex + expectedHeader.length());
        }
        Log.w(TAG, "extractRelevantData: PID ("+expectedPidConstant+") için beklenen başlık ("+expectedHeader+") yanıtta bulunamadı: [" + cleanedResponse + "]");
        return null;
    }

    private Double parseSpeedFromResponseInternal(String cleanedResponse) {
        String dataHex = extractRelevantData(cleanedResponse, VEHICLE_SPEED_PID);
        if (dataHex != null && dataHex.length() >= 2) {
            try {
                return (double) Integer.parseInt(dataHex.substring(0, 2), 16);
            } catch (NumberFormatException e) {
                Log.e(TAG, "parseSpeedInternal NFE: " + dataHex.substring(0, 2) + " from " + cleanedResponse, e);
            }
        }
        return null;
    }

    private Double parseRpmFromResponseInternal(String cleanedResponse) {
        String dataHex = extractRelevantData(cleanedResponse, ENGINE_RPM_PID);
        if (dataHex != null && dataHex.length() >= 4) {
            try {
                int a = Integer.parseInt(dataHex.substring(0, 2), 16);
                int b = Integer.parseInt(dataHex.substring(2, 4), 16);
                return ((a * 256.0) + b) / 4.0;
            } catch (NumberFormatException e) {
                Log.e(TAG, "parseRpmInternal NFE: " + dataHex.substring(0, 4) + " from " + cleanedResponse, e);
            }
        }
        return null;
    }

    private Double parseTempFromResponseInternal(String cleanedResponse, String expectedPidConstant) {
        String dataHex = extractRelevantData(cleanedResponse, expectedPidConstant);
        if (dataHex != null && dataHex.length() >= 2) {
            try {
                return (double) (Integer.parseInt(dataHex.substring(0, 2), 16) - 40);
            } catch (NumberFormatException e) {
                Log.e(TAG, "parseTempInternal ("+expectedPidConstant+") NFE: " + dataHex.substring(0, 2) + " from " + cleanedResponse, e);
            }
        }
        return null;
    }

    private Double parsePercentageFromResponseInternal(String cleanedResponse, String expectedPidConstant) {
        String dataHex = extractRelevantData(cleanedResponse, expectedPidConstant);
        if (dataHex != null && dataHex.length() >= 2) {
            try {
                return (Integer.parseInt(dataHex.substring(0, 2), 16) * 100.0) / 255.0;
            } catch (NumberFormatException e) {
                Log.e(TAG, "parsePercentageInternal ("+expectedPidConstant+") NFE: " + dataHex.substring(0, 2) + " from " + cleanedResponse, e);
            }
        }
        return null;
    }

    private Double parseMafFromResponseInternal(String cleanedResponse) {
        String dataHex = extractRelevantData(cleanedResponse, MAF_AIR_FLOW_PID);
        if (dataHex != null && dataHex.length() >= 4) {
            try {
                int a = Integer.parseInt(dataHex.substring(0, 2), 16);
                int b = Integer.parseInt(dataHex.substring(2, 4), 16);
                return ((a * 256.0) + b) / 100.0;
            } catch (NumberFormatException e) {
                Log.e(TAG, "parseMafInternal NFE: " + dataHex.substring(0, 4) + " from " + cleanedResponse, e);
            }
        }
        return null;
    }

    private void handleConnectionIssue() {
        consecutiveFailures++;
        Log.w(TAG, "handleConnectionIssue çağrıldı. Hata sayısı: " + consecutiveFailures);
        checkConsecutiveFailures();
    }

    private void checkConsecutiveFailures() {
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            Log.e(TAG, "Maksimum hata (" + consecutiveFailures + "). Okuma durduruluyor.");
            if (isReading) {
                mainHandler.post(() -> showToast("OBD2 bağlantısı koptu veya veri alınamıyor!"));
                stopReadingInternally();
            }
            consecutiveFailures = 0;
        }
    }

    public void stopReading() {
        Log.i(TAG, "stopReading çağrıldı (API)...");
        if (!isReading && (scheduler == null || scheduler.isShutdown())) {
            Log.d(TAG, "Okuma zaten duruk veya scheduler kapalı.");
            return;
        }
        stopReadingInternally();
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    public interface DataUpdateListener {
        void onDataUpdate(VehicleData data);
        void onConnectionLost();
    }

    public static class VehicleData {
        // DTC inner class
        public static class DTC {
            public String code;
            public String description;
            public boolean isUserUnderstandable;

            public DTC(String code, String description, boolean isUserUnderstandable) {
                this.code = code;
                this.description = description;
                this.isUserUnderstandable = isUserUnderstandable;
            }
            @Override
            public String toString() {
                return code + ": " + description;
            }
        }

        private Double speed = 0.0;
        private Double rpm = 0.0;
        private Double engineTemp = null;
        private Double fuelLevel = null;
        private Double engineLoad = null;
        private Double throttlePosition = null;
        private Double intakeTemp = null;
        private Double mafAirFlow = null;
        private List<DTC> diagnosticTroubleCodes = new ArrayList<>();
        private String vin = null; // VIN alanı eklendi

        public VehicleData() {}

        public VehicleData(VehicleData other) {
            this.speed = other.speed;
            this.rpm = other.rpm;
            this.engineTemp = other.engineTemp;
            this.fuelLevel = other.fuelLevel;
            this.engineLoad = other.engineLoad;
            this.throttlePosition = other.throttlePosition;
            this.intakeTemp = other.intakeTemp;
            this.mafAirFlow = other.mafAirFlow;
            this.diagnosticTroubleCodes = new ArrayList<>(other.diagnosticTroubleCodes);
            this.vin = other.vin; // VIN kopyalandı
        }

        public VehicleData deepCopy() {
            return new VehicleData(this);
        }

        public void setSpecificFieldToNull(String pid) {
            switch (pid) {
                case VEHICLE_SPEED_PID: this.speed = 0.0; break;
                case ENGINE_RPM_PID: this.rpm = 0.0; break;
                case COOLANT_TEMP_PID: this.engineTemp = null; break;
                case ENGINE_LOAD_PID: this.engineLoad = null; break;
                case THROTTLE_POS_PID: this.throttlePosition = null; break;
                case FUEL_LEVEL_PID: this.fuelLevel = null; break;
                case INTAKE_TEMP_PID: this.intakeTemp = null; break;
                case MAF_AIR_FLOW_PID: this.mafAirFlow = null; break;
                case DTC_REQUEST_PID:
                    if (this.diagnosticTroubleCodes != null) {
                        this.diagnosticTroubleCodes.clear();
                    }
                    break;
                case VEHICLE_VIN_PID: this.vin = null; break; // VIN için eklendi
            }
        }

        public void clearDiagnosticTroubleCodes() {
            if (this.diagnosticTroubleCodes != null) {
                this.diagnosticTroubleCodes.clear();
            }
        }

        public void clearVin() { this.vin = null; } // VIN için temizleme metodu

        // Getter ve Setter'lar
        public Double getSpeed() { return speed; }
        public void setSpeed(Double speed) { this.speed = speed; }
        public Double getRpm() { return rpm; }
        public void setRpm(Double rpm) { this.rpm = rpm; }
        public Double getEngineTemp() { return engineTemp; }
        public void setEngineTemp(Double engineTemp) { this.engineTemp = engineTemp; }
        public Double getFuelLevel() { return fuelLevel; }
        public void setFuelLevel(Double fuelLevel) { this.fuelLevel = fuelLevel; }
        public Double getEngineLoad() { return engineLoad; }
        public void setEngineLoad(Double engineLoad) { this.engineLoad = engineLoad; }
        public Double getThrottlePosition() { return throttlePosition; }
        public void setThrottlePosition(Double throttlePosition) { this.throttlePosition = throttlePosition; }
        public Double getIntakeTemp() { return intakeTemp; }
        public void setIntakeTemp(Double intakeTemp) { this.intakeTemp = intakeTemp; }
        public Double getMafAirFlow() { return mafAirFlow; }
        public void setMafAirFlow(Double mafAirFlow) { this.mafAirFlow = mafAirFlow; }
        public List<DTC> getDiagnosticTroubleCodes() { return diagnosticTroubleCodes; }
        public void setDiagnosticTroubleCodes(List<DTC> codes) { this.diagnosticTroubleCodes = codes; }
        public String getVin() { return vin; } // VIN getter
        public void setVin(String vin) { this.vin = vin; } // VIN setter
    }
}
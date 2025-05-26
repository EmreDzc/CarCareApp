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
import java.util.LinkedList;
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

    // --- Diğer Sabitler ---
    private static final int MAX_CONSECUTIVE_FAILURES = 8; // Biraz daha toleranslı olmak için 10-12 yapılabilir
    private static final long COMMAND_INTERVAL_MS = 250; // Bazı araçlar için 300-500ms daha iyi olabilir
    private static final long READ_RESPONSE_TIMEOUT_MS = 5000;
    private static final long INIT_COMMAND_RESPONSE_TIMEOUT_MS = 7000;
    private static final int SCHEDULER_INITIAL_DELAY_S = 2;
    private static final int SCHEDULER_PERIOD_S = 3; // Veya 2 saniye

    private final Context context;
    private final BluetoothManager bluetoothManager;
    private ScheduledExecutorService scheduler; // Yeniden oluşturulabilir olması için final değil
    private final Handler mainHandler;
    private final VehicleData vehicleData;
    private DataUpdateListener dataUpdateListener;

    private volatile boolean isReading = false; // volatile eklendi
    private int consecutiveFailures = 0;

    private final Queue<String> commandQueue = new LinkedList<>();
    private final AtomicBoolean isProcessingQueue = new AtomicBoolean(false);

    public SimpleOBD2Manager(Context context, BluetoothManager bluetoothManager) {
        this.context = context;
        this.bluetoothManager = bluetoothManager;
        // scheduler burada başlatılmayacak, startReading içinde başlatılacak
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.vehicleData = new VehicleData();
    }

    public void setDataUpdateListener(DataUpdateListener listener) {
        this.dataUpdateListener = listener;
    }

    private boolean initializeELM327() {
        Log.i(TAG, "ELM327 başlatılıyor...");
        try {
            InputStream in = bluetoothManager.getInputStream();
            OutputStream out = bluetoothManager.getOutputStream();

            if (in == null || out == null) {
                Log.e(TAG, "initializeELM327: InputStream veya OutputStream null!");
                return false;
            }

            // Önceki bir oturumdan kalmış olabilecek verileri temizle
            clearInputStream(in);

            String[] initCommands = {
                    "ATZ",    // Reset
                    "ATE0",   // Echo off
                    "ATL0",   // Linefeeds off
                    "ATH0",   // Headers off (Bazı adaptörler daha iyi çalışır, bazıları için ATH1 gerekebilir)
                    // ATH1 olursa parse metotları gelen header'ı (örn: 7E8) atlamalı. Şimdilik ATH0 ile devam.
                    "ATS0",   // Spaces off (Bu, yanıtların boşluksuz gelmesine neden olabilir, parse metotları bunu dikkate almalı)
                    "ATSP0"   // Set protocol to auto and save it
            };

            for (String cmd : initCommands) {
                String response = sendCommandAndReadResponse(cmd, out, in, INIT_COMMAND_RESPONSE_TIMEOUT_MS);
                Log.d(TAG, "INIT CMD: " + cmd + " -> RESPONSE: [" + (response != null ? response.replace("\r", "\\r").replace("\n", "\\n") : "null") + "]");
                if (response == null ||
                        (!response.toUpperCase().contains("OK") && // Genel OK kontrolü
                                !(cmd.equalsIgnoreCase("ATZ") && (response.toUpperCase().contains("ELM") || response.contains("?"))) && // ATZ özel durumu
                                !(cmd.equalsIgnoreCase("ATSP0") && response.toUpperCase().contains("OK")) // ATSP0 OK bekler
                        )
                ) {
                    Log.e(TAG, "Başlatma komutu başarısız veya beklenmedik yanıt: " + cmd + " -> [" + response + "]");
                    if (cmd.equalsIgnoreCase("ATSP0") && (response == null || !response.toUpperCase().contains("OK"))) {
                        return false; // ATSP0 kritik
                    }
                    // Diğer komutlar için devam etmeyi dene
                }
                Thread.sleep(300); // Komutlar arası bekleme
            }

            // Protokolün gerçekten ayarlandığını doğrulamak için bir test komutu
            String protocolCheckResponse = sendCommandAndReadResponse("ATDP", out, in, INIT_COMMAND_RESPONSE_TIMEOUT_MS);
            Log.i(TAG, "Protokol kontrol (ATDP): " + protocolCheckResponse);
            if (protocolCheckResponse == null || protocolCheckResponse.toUpperCase().contains("AUTO") || protocolCheckResponse.toUpperCase().contains("SEARCHING")) {
                Log.w(TAG, "ATDP yanıtı 'AUTO' veya 'SEARCHING' içeriyor, bu protokolün henüz tam oturmadığını gösterebilir.");
            }


            // Temel PID destek sorgusu
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
        StringBuilder rawHexBuffer = new StringBuilder(); // Ham gelen byte'ları hex olarak loglamak için
        long startTime = System.currentTimeMillis();
        int consecutiveNoDataReads = 0;
        final int MAX_CONSECUTIVE_NO_DATA_BEFORE_SLEEP = 20; // Daha kısa süre bekle
        final int SLEEP_INTERVAL_NO_DATA = 10; // ms
        final int SLEEP_INTERVAL_HAS_DATA = 1; // ms, veri varsa daha hızlı kontrol et

        Log.v(TAG, "Yanıt bekleniyor (timeout: " + timeoutMs + "ms)...");

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (Thread.currentThread().isInterrupted()) {
                Log.w(TAG, "readResponse: Thread kesildi, okuma durduruluyor.");
                throw new IOException("Read operation interrupted by thread interruption");
            }

            if (inputStream.available() > 0) {
                consecutiveNoDataReads = 0; // Veri geldi, sayacı sıfırla
                int dataByte = inputStream.read();

                if (dataByte == -1) { // Stream sonu
                    Log.w(TAG, "readResponse: Stream sonu (-1).");
                    break;
                }

                rawHexBuffer.append(String.format("%02X ", dataByte)); // Log için hex
                char character = (char) dataByte;

                if (character == '>') { // Komut istemi '>' bulundu, yanıt bitti
                    Log.v(TAG, "readResponse: Prompt '>' bulundu.");
                    break;
                }

                // Kontrol karakterlerini ve null byte'ı atla, diğerlerini ekle
                if (character != '\r' && character != '\n' && dataByte != 0x00) {
                    responseBuffer.append(character);
                }
                try { Thread.sleep(SLEEP_INTERVAL_HAS_DATA); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break;}

            } else { // Veri yok
                consecutiveNoDataReads++;
                try {
                    Thread.sleep(SLEEP_INTERVAL_NO_DATA);
                    if (consecutiveNoDataReads > MAX_CONSECUTIVE_NO_DATA_BEFORE_SLEEP) {
                        // Çok uzun süre veri gelmediyse, küçük bir mola daha ver
                        // Log.v(TAG, "readResponse: Uzun süre veri yok, kısa ek bekleme.");
                        // Thread.sleep(50);
                        consecutiveNoDataReads = 0; // Sayacı sıfırla ki sürekli uzun beklemesin
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.w(TAG, "readResponse: Bekleme kesildi.", e);
                    break; // InterruptedException durumunda döngüden çık
                }
            }
        }

        Log.d(TAG, "readResponse - Ham Hex: [" + rawHexBuffer.toString().trim() + "]");
        String finalResponse = responseBuffer.toString().replaceAll("\\s+", " ").trim(); // İç boşlukları tek boşluğa indir
        Log.d(TAG, "readResponse - İşlenmiş: [" + finalResponse + "]");

        if (System.currentTimeMillis() - startTime >= timeoutMs && !rawHexBuffer.toString().contains("3E")) { // 3E = '>'
            Log.w(TAG, "readResponse: Timeout! Alınan (işlenmiş): [" + finalResponse + "]");
            // Timeout durumunda null yerine kısmi yanıtı döndürebilir veya özel bir hata fırlatabiliriz.
            // Şimdilik kısmi yanıtı döndürmek, parse metodlarının bunu ele almasını sağlar.
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
        consecutiveFailures = 0; // Her yeni başlangıçta sıfırla

        // Scheduler'ı burada oluştur ve başlat
        if (scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "OBDScheduler"));
        }


        new Thread(() -> {
            if (initializeELM327()) {
                mainHandler.post(() -> showToast("ELM327 başarıyla başlatıldı"));
                Log.i(TAG, "ELM327 başarıyla başlatıldı, periyodik okuma planlanıyor.");

                // İlk komut setini hemen işle
                fillCommandQueue();
                processNextCommandFromQueue(); // Bu, kuyruktaki tüm komutları işlemeye başlar

                // Periyodik olarak kuyruğu doldur ve işlemeyi tetikle
                // Scheduler'ın canlı olduğundan emin ol
                if (scheduler == null || scheduler.isShutdown()) {
                    scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "OBDScheduler"));
                }
                try {
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
                stopReadingInternally(); // Başlatma başarısızsa okumayı durdur
            }
        }, "OBDInitThread").start();
    }

    private void stopReadingInternally() {
        isReading = false; // Önce isReading'i false yap
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
        // scheduler = null; // İsteğe bağlı: bir sonraki startReading'de yeniden oluşturulacak
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
        // processNextCommandFromQueue() zaten kuyruk boş değilse çalışmaya devam eder.
        // Eğer bir önceki döngü bitmişse ve kuyruk boşsa, bu tetikleme yeni komutları işler.
        if (!isProcessingQueue.get()) { // Eğer aktif bir işlem yoksa başlat
            processNextCommandFromQueue();
        }
    }

    private void fillCommandQueue() {
        synchronized (commandQueue) {
            if (!commandQueue.isEmpty()) {
                // Log.d(TAG, "Kuyruk zaten dolu veya işleniyor, yeniden doldurulmayacak.");
                // return; // Eğer kuyrukta hala eleman varsa, üzerine yazmamak için.
                // Ya da mevcut strateji: her periyotta kuyruğu temizle ve yeniden doldur. Bu daha basit.
            }
            commandQueue.clear(); // Her döngüde kuyruğu temizleyip yeniden dolduruyoruz
            commandQueue.offer(VEHICLE_SPEED_PID);
            commandQueue.offer(ENGINE_RPM_PID);
            commandQueue.offer(COOLANT_TEMP_PID);
            commandQueue.offer(ENGINE_LOAD_PID);
            commandQueue.offer(THROTTLE_POS_PID);
            if (FUEL_LEVEL_PID != null) commandQueue.offer(FUEL_LEVEL_PID);
            if (INTAKE_TEMP_PID != null) commandQueue.offer(INTAKE_TEMP_PID);
            if (MAF_AIR_FLOW_PID != null) commandQueue.offer(MAF_AIR_FLOW_PID);
            Log.d(TAG, "Kuyruğa " + commandQueue.size() + " komut eklendi: " + commandQueue.toString());
        }
    }

    private void processNextCommandFromQueue() {
        if (!isReading) { Log.d(TAG, "processNext: Okuma durdurulmuş."); isProcessingQueue.set(false); return; }
        if (!bluetoothManager.isConnected()) { Log.w(TAG, "processNext: BT bağlı değil."); isProcessingQueue.set(false); handleConnectionIssue(); clearQueueAndStopProcessingIfNeeded(); return; }
        if (Thread.currentThread().isInterrupted()) { Log.w(TAG, "processNext: Thread kesildi."); isProcessingQueue.set(false); return; }


        // Eğer zaten bir komut işleniyorsa veya kuyruk boşsa çık.
        // AtomicBoolean ile sadece bir thread'in aynı anda işlem yapmasını sağla.
        if (!isProcessingQueue.compareAndSet(false, true)) {
            // Log.d(TAG, "processNext: Zaten bir komut işleniyor veya kuyruk boş, çıkılıyor.");
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
        }

        if (pidToProcess == null) { // Ekstra kontrol, normalde olmamalı
            isProcessingQueue.set(false);
            return;
        }

        try {
            InputStream in = bluetoothManager.getInputStream();
            OutputStream out = bluetoothManager.getOutputStream();
            if (in == null || out == null) {
                Log.e(TAG, "processNext: InputStream veya OutputStream null!");
                isProcessingQueue.set(false);
                handleConnectionIssue(); // Bağlantı sorunu olarak ele al
                clearQueueAndStopProcessingIfNeeded();
                return;
            }

            String response = sendCommandAndReadResponse(pidToProcess, out, in, READ_RESPONSE_TIMEOUT_MS);
            parseAndStoreData(pidToProcess, response); // Yanıt null olsa bile parse etmeye gönder
            Thread.sleep(COMMAND_INTERVAL_MS); // Komutlar arası bekleme
        } catch (IOException e) {
            Log.e(TAG, "Komut (" + pidToProcess + ") işlenirken IOException", e);
            handleConnectionIssue(); // Bağlantı sorunu olarak ele al
        } catch (InterruptedException e) {
            Log.w(TAG, "Komut (" + pidToProcess + ") işlenirken InterruptedException", e);
            Thread.currentThread().interrupt(); // Kesme bayrağını yeniden ayarla
            // isReading = false; // Okumayı durdurabilir veya devam etmeyi deneyebilir
        } catch (Exception e) { // Diğer beklenmedik hatalar
            Log.e(TAG, "Komut (" + pidToProcess + ") işlenirken genel hata", e);
            consecutiveFailures++; // Genel hatalar için de sayacı artır
            checkConsecutiveFailures();
        } finally {
            isProcessingQueue.set(false); // Bu komutun işlenmesi bitti, bir sonrakine izin ver
            if (isReading && bluetoothManager.isConnected() && !Thread.currentThread().isInterrupted()) {
                // Kuyrukta hala eleman varsa veya scheduler yeni eleman eklediyse devam et
                synchronized (commandQueue) {
                    if (!commandQueue.isEmpty()) {
                        processNextCommandFromQueue(); // Bir sonraki komutu işle
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
            isProcessingQueue.set(false); // İşlem bayrağını sıfırla
            // stopReading çağrılmadıysa ve sadece thread interrupt ise, scheduler devam edebilir.
            // Eğer isReading false ise veya BT bağlı değilse, stopReading zaten çağrılmış olmalı veya çağrılacak.
        }
    }

    private void parseAndStoreData(String pidWithoutSuffix, String rawResponse) {
        boolean dataUpdatedThisParse = false;

        if (rawResponse == null || rawResponse.isEmpty()) {
            Log.w(TAG, "PID (" + pidWithoutSuffix + ") için null veya boş yanıt alındı.");
            consecutiveFailures++; // Null/boş yanıt bir hatadır
            updateVehicleDataWithError(pidWithoutSuffix, true); // Hata sayacını artır
            checkConsecutiveFailures();
            return;
        }

        String cleanedResponse = rawResponse.replaceAll("[\\s\\p{Cntrl}]", "").toUpperCase();

        // "NO DATA" gibi durumları önce kontrol et
        boolean isNoDataResponse = cleanedResponse.contains("NODATA");
        boolean isErrorResponse = cleanedResponse.contains("ERROR") ||
                cleanedResponse.contains("?") ||
                cleanedResponse.contains("UNABLETOCONNECT");
        // "SEARCHING" ama içinde PID verisi yoksa bu da bir tür hatadır
        boolean isSearchingWithoutData = cleanedResponse.contains("SEARCHING") && !cleanedResponse.matches(".*41" + pidWithoutSuffix.substring(2) + "[0-9A-F]{2,}.*");


        if (isNoDataResponse || isErrorResponse || isSearchingWithoutData) {
            if (isNoDataResponse) {
                Log.i(TAG, "PID (" + pidWithoutSuffix + ") için 'NO DATA' yanıtı alındı. Bu PID araç tarafından desteklenmiyor olabilir.");
                // NO DATA bir hata değildir, bu yüzden consecutiveFailures ARTIRILMAZ.
                vehicleData.setSpecificFieldToNull(pidWithoutSuffix);
                dataUpdatedThisParse = true; // UI'ı N/A ile güncellemek için
            } else {
                // Diğer hata durumları (ERROR, ?, UNABLE, SEARCHING without data)
                Log.w(TAG, "PID (" + pidWithoutSuffix + ") için geçersiz veya hatali yanıt: [" + rawResponse + "] -> Temizlenmiş: [" + cleanedResponse + "]");
                consecutiveFailures++; // Bu bir hatadır
                updateVehicleDataWithError(pidWithoutSuffix, false); // Hata sayacını artırmaz, çünkü burada zaten artırıldı
            }
            // checkConsecutiveFailures() her durumda çağrılmalı, çünkü bir hata oluşmuş olabilir (NO DATA hariç)
            if (!isNoDataResponse) { // Sadece gerçek hatalarda check et
                checkConsecutiveFailures();
            }
            // dataUpdateListener'ı UI'ı güncellemek için çağır (NO DATA veya hata durumunda)
            if (dataUpdateListener != null && dataUpdatedThisParse) { // dataUpdatedThisParse NO DATA için true olacak
                VehicleData snapshot = vehicleData.deepCopy();
                mainHandler.post(() -> dataUpdateListener.onDataUpdate(snapshot));
            } else if (dataUpdateListener != null && (isErrorResponse || isSearchingWithoutData)) {
                // Hata durumunda da UI güncellenmeli (N/A göstermek için)
                VehicleData snapshot = vehicleData.deepCopy(); // updateVehicleDataWithError zaten null yaptı
                mainHandler.post(() -> dataUpdateListener.onDataUpdate(snapshot));
            }
            return; // İşlem burada biter
        }


        // Buraya kadar geldiyse, yanıt geçerli bir veri içermeli
        Double parsedValue = null;
        boolean parseAttempted = true;
        try {
            switch (pidWithoutSuffix) {
                case VEHICLE_SPEED_PID:
                    parsedValue = parseSpeedFromResponseInternal(cleanedResponse);
                    if (parsedValue != null) vehicleData.setSpeed(parsedValue);
                    else updateVehicleDataWithError(pidWithoutSuffix, true); // Parse hatası
                    break;
                case ENGINE_RPM_PID:
                    parsedValue = parseRpmFromResponseInternal(cleanedResponse);
                    if (parsedValue != null) vehicleData.setRpm(parsedValue);
                    else updateVehicleDataWithError(pidWithoutSuffix, true); // Parse hatası
                    break;
                case COOLANT_TEMP_PID:
                    parsedValue = parseTempFromResponseInternal(cleanedResponse, COOLANT_TEMP_PID);
                    if (parsedValue != null) vehicleData.setEngineTemp(parsedValue);
                    else updateVehicleDataWithError(pidWithoutSuffix, true); // Parse hatası
                    break;
                case ENGINE_LOAD_PID:
                    parsedValue = parsePercentageFromResponseInternal(cleanedResponse, ENGINE_LOAD_PID);
                    if (parsedValue != null) vehicleData.setEngineLoad(parsedValue);
                    else updateVehicleDataWithError(pidWithoutSuffix, true); // Parse hatası
                    break;
                case THROTTLE_POS_PID: // Bu zaten NO DATA olarak geliyordu, yukarıda ele alındı.
                    // Eğer NO DATA dışında bir şey gelip parse edilemezse burası çalışır.
                    parsedValue = parsePercentageFromResponseInternal(cleanedResponse, THROTTLE_POS_PID);
                    if (parsedValue != null) vehicleData.setThrottlePosition(parsedValue);
                    else updateVehicleDataWithError(pidWithoutSuffix, true); // Parse hatası
                    break;
                case FUEL_LEVEL_PID:
                    parsedValue = parsePercentageFromResponseInternal(cleanedResponse, FUEL_LEVEL_PID);
                    vehicleData.setFuelLevel(parsedValue); // null gelebilir, bu bir hata değil
                    if (parsedValue == null) {
                        // Eğer parse metodu null döndürdüyse ve cleanedResponse "NO DATA" içermiyorsa bu bir parse hatasıdır.
                        // Ama NO DATA durumu yukarıda zaten ele alındı. Bu PID için null normal olabilir.
                        // Sadece gerçekten parse edilemeyen bir formatta gelirse hata say.
                        if (!isResponsePotentiallyValidForNull(cleanedResponse, FUEL_LEVEL_PID)) {
                            updateVehicleDataWithError(pidWithoutSuffix, true); // Parse hatası
                        } else {
                            vehicleData.setFuelLevel(null); // Veriyi null yap
                        }
                    }
                    break;
                case INTAKE_TEMP_PID:
                    parsedValue = parseTempFromResponseInternal(cleanedResponse, INTAKE_TEMP_PID);
                    vehicleData.setIntakeTemp(parsedValue); // null gelebilir
                    if (parsedValue == null) {
                        if (!isResponsePotentiallyValidForNull(cleanedResponse, INTAKE_TEMP_PID)) {
                            updateVehicleDataWithError(pidWithoutSuffix, true);
                        } else {
                            vehicleData.setIntakeTemp(null);
                        }
                    }
                    break;
                case MAF_AIR_FLOW_PID:
                    parsedValue = parseMafFromResponseInternal(cleanedResponse);
                    vehicleData.setMafAirFlow(parsedValue); // null gelebilir
                    if (parsedValue == null) {
                        if (!isResponsePotentiallyValidForNull(cleanedResponse, MAF_AIR_FLOW_PID)) {
                            updateVehicleDataWithError(pidWithoutSuffix, true);
                        } else {
                            vehicleData.setMafAirFlow(null);
                        }
                    }
                    break;
                default:
                    Log.w(TAG, "Bilinmeyen PID parse edilmeye çalışıldı: " + pidWithoutSuffix);
                    parseAttempted = false;
                    break;
            }

            if (parseAttempted) {
                if (parsedValue != null) {
                    Log.d(TAG, "PID (" + pidWithoutSuffix + ") BAŞARIYLA parse edildi. Yanıt: ["+rawResponse+"], Temizlenmiş: ["+cleanedResponse+"], Değer: " + parsedValue);
                    dataUpdatedThisParse = true;
                    consecutiveFailures = 0; // Başarılı parse, sayacı sıfırla
                } else {
                    // Eğer buraya geldiyse ve parsedValue null ise, bu bir parse hatasıdır (NO DATA hariç)
                    // ve updateVehicleDataWithError zaten yukarıda çağrılmış olmalı.
                    // Log.w(TAG, "PID (" + pidWithoutSuffix + ") parse edilemedi (parsedValue null). Yanıt: [" + rawResponse + "]");
                    // consecutiveFailures++; // updateVehicleDataWithError'da artırılmalı
                    // checkConsecutiveFailures(); // updateVehicleDataWithError'dan sonra çağrılacak
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "PID (" + pidWithoutSuffix + ") parse edilirken KRİTİK HATA: [" + rawResponse + "]", e);
            consecutiveFailures++; // Kritik hata, sayacı artır
            updateVehicleDataWithError(pidWithoutSuffix, false); // Hata sayacını tekrar artırma
        }

        // Her PID işlendikten sonra, eğer parse hatası olduysa (ve NO DATA değilse) consecutiveFailures artmış olacak.
        // Bu yüzden checkConsecutiveFailures'ı burada genel olarak çağırmak iyi olabilir.
        // Ancak sadece gerçekten parse hatası olan durumlarda çağrılmalı.
        // consecutiveFailures > 0 ise ve bu artış bu PID'den kaynaklanıyorsa kontrol et.
        // Şimdilik, checkConsecutiveFailures zaten updateVehicleDataWithError içinde veya hata bloklarında çağrılıyor.

        if (dataUpdatedThisParse && dataUpdateListener != null) {
            VehicleData snapshot = vehicleData.deepCopy();
            mainHandler.post(() -> dataUpdateListener.onDataUpdate(snapshot));
        }
    }

    private boolean isResponsePotentiallyValidForNull(String cleanedResponse, String pid) {
        // Bu PID'ler için null değerler, "NO DATA" olmasa bile kabul edilebilir olabilir
        // (örneğin, araç bu veriyi o an sağlamıyorsa ama PID'yi tamamen reddetmiyorsa).
        // Ya da yanıt formatı beklenenden farklı ama "NO DATA" değil.
        // Şimdilik basitçe, eğer "41" + PID ile başlıyorsa ama parse edilemiyorsa,
        // bunu "NO DATA" gibi değerlendirmiyoruz, parse hatası sayıyoruz.
        // Bu metod, bazı PID'lerin null değer döndürmesinin normal olup olmadığını belirlemek için genişletilebilir.
        return cleanedResponse.startsWith("41" + pid.substring(2)); // Eğer beklenen başlıkla başlıyorsa ama parse edilemiyorsa, bu bir parse hatasıdır.
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
        // checkConsecutiveFailures burada da çağrılabilir eğer incrementFailureCounter true ise
        // Ancak zaten parseAndStoreData'nın sonunda veya hata bloklarında çağrılıyor. Tekrardan kaçın.
        if (incrementFailureCounter) { // Sadece gerçekten bir hata oluştuğunda (örn. parse hatası) çağır.
            checkConsecutiveFailures();
        }
    }

    // ------ YENİ PARSE METOTLARI (INTERNAL) ------
    // Bu metotlar temizlenmiş, boşluksuz, büyük harf yanıt bekler.

    private String extractRelevantData(String cleanedResponse, String expectedPidConstant) {
        String pidDigits = expectedPidConstant.substring(2); // "010D" -> "0D"
        String expectedHeader = "41" + pidDigits; // "410D"

        int startIndex = cleanedResponse.indexOf(expectedHeader);
        if (startIndex != -1) {
            // Header'dan sonraki kısmı al
            return cleanedResponse.substring(startIndex + expectedHeader.length());
        }
        // Bazen yanıt 7E8 04 41 0D 00 gibi gelebilir (ATH1 ile).
        // Bu durumda "410D"yi bulup sonrasını almak daha mantıklı olabilir.
        // Ancak ATH0 ile bu beklenmez.
        Log.w(TAG, "extractRelevantData: PID ("+expectedPidConstant+") için beklenen başlık ("+expectedHeader+") yanıtta bulunamadı: [" + cleanedResponse + "]");
        return null;
    }


    private Double parseSpeedFromResponseInternal(String cleanedResponse) {
        String dataHex = extractRelevantData(cleanedResponse, VEHICLE_SPEED_PID);
        if (dataHex != null && dataHex.length() >= 2) { // Speed = 1 byte (AA)
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
        if (dataHex != null && dataHex.length() >= 4) { // RPM = 2 bytes (AABB)
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
        if (dataHex != null && dataHex.length() >= 2) { // Temp = 1 byte (AA)
            try {
                // Sıcaklık -40 ile +215 arası olabilir.
                return (double) (Integer.parseInt(dataHex.substring(0, 2), 16) - 40);
            } catch (NumberFormatException e) {
                Log.e(TAG, "parseTempInternal ("+expectedPidConstant+") NFE: " + dataHex.substring(0, 2) + " from " + cleanedResponse, e);
            }
        }
        return null;
    }

    private Double parsePercentageFromResponseInternal(String cleanedResponse, String expectedPidConstant) {
        String dataHex = extractRelevantData(cleanedResponse, expectedPidConstant);
        if (dataHex != null && dataHex.length() >= 2) { // Percentage = 1 byte (AA)
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
        if (dataHex != null && dataHex.length() >= 4) { // MAF = 2 bytes (AABB)
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
        checkConsecutiveFailures(); // Bu, gerekirse stopReading'i tetikleyebilir
    }


    private void checkConsecutiveFailures() {
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            Log.e(TAG, "Maksimum hata (" + consecutiveFailures + "). Okuma durduruluyor.");
            if (isReading) { // Sadece okuma aktifken bu işlemleri yap
                mainHandler.post(() -> showToast("OBD2 bağlantısı koptu veya veri alınamıyor!"));
                stopReadingInternally(); // Bu, isReading'i false yapar ve scheduler'ı durdurur ve onConnectionLost'u çağırır.
            }
            consecutiveFailures = 0; // Sayacı sıfırla ki bir sonraki bağlantıda tekrar denenebilsin
        }
    }

    public void stopReading() {
        Log.i(TAG, "stopReading çağrıldı (API)...");
        if (!isReading && (scheduler == null || scheduler.isShutdown())) {
            Log.d(TAG, "Okuma zaten duruk veya scheduler kapalı.");
            return;
        }
        // isReading'i false yap, bu scheduler ve komut işleme döngülerini durdurmalı.
        // Ardından kaynakları temizle.
        stopReadingInternally(); // Bu metod hem isReading'i false yapar hem de scheduler'ı durdurur.
        // BluetoothManager.disconnect() burada çağrılmamalı,
        // bu sadece OBD2 okumasını durdurur, Bluetooth bağlantısını değil.
        // Bluetooth bağlantısını kesmek CarActivity'nin sorumluluğunda olmalı.
    }


    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    public interface DataUpdateListener {
        void onDataUpdate(VehicleData data);
        void onConnectionLost();
    }

    public static class VehicleData {
        private Double speed = 0.0;
        private Double rpm = 0.0;
        private Double engineTemp = null;
        private Double fuelLevel = null;
        private Double engineLoad = null;
        private Double throttlePosition = null;
        private Double intakeTemp = null;
        private Double mafAirFlow = null;

        public VehicleData() {}

        // Kopyalama kurucusu
        public VehicleData(VehicleData other) {
            this.speed = other.speed;
            this.rpm = other.rpm;
            this.engineTemp = other.engineTemp;
            this.fuelLevel = other.fuelLevel;
            this.engineLoad = other.engineLoad;
            this.throttlePosition = other.throttlePosition;
            this.intakeTemp = other.intakeTemp;
            this.mafAirFlow = other.mafAirFlow;
        }

        public VehicleData deepCopy() {
            return new VehicleData(this);
        }

        public void setSpecificFieldToNull(String pid) {
            switch (pid) {
                case VEHICLE_SPEED_PID: this.speed = 0.0; break; // Hız için 0.0 daha iyi olabilir
                case ENGINE_RPM_PID: this.rpm = 0.0; break;     // RPM için 0.0
                case COOLANT_TEMP_PID: this.engineTemp = null; break;
                case ENGINE_LOAD_PID: this.engineLoad = null; break;
                case THROTTLE_POS_PID: this.throttlePosition = null; break;
                case FUEL_LEVEL_PID: this.fuelLevel = null; break;
                case INTAKE_TEMP_PID: this.intakeTemp = null; break;
                case MAF_AIR_FLOW_PID: this.mafAirFlow = null; break;
            }
        }


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
    }
}
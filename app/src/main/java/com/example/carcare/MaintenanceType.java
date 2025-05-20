package com.example.carcare;

import java.util.Calendar;

public enum MaintenanceType {
    MOTOR_OIL("Motor Yağı ve Filtre Değişimi Hatırlatması",
            "Lütfen motor yağınızı ve filtrenizi değiştirmeyi unutmayın!",
            Calendar.FEBRUARY, 1, 1, 10),

    AIR_FILTER("Hava Filtresi Değişimi Hatırlatması",
            "Lütfen hava filtrenizi değiştirmeyi unutmayın!",
            Calendar.MARCH, 1, 2, 11),

    FUEL_FILTER("Yakıt Filtresi Değişimi Hatırlatması",
            "Lütfen yakıt filtrenizi değiştirmeyi unutmayın!",
            Calendar.APRIL, 1, 4, 12),

    BRAKE_SYSTEM("Fren Sistemi Kontrolü Hatırlatması",
            "Lütfen fren sisteminizi kontrol etmeyi unutmayın!",
            Calendar.MAY, 1, 2, 13),

    SPARK_PLUG("Bujilerin Değişimi Hatırlatması",
            "Lütfen bujilerinizi değiştirmeyi unutmayın!",
            Calendar.JUNE, 1, 3, 14),

    SUMMER_MAINTENANCE("Yaz Bakımı Hatırlatması",
            "Lütfen klima bakımı, lastik kontrolü ve diğer yazlık bakımlarınızı yapmayı unutmayın!",
            Calendar.JUNE, 1, 1, 6),

    WINTER_MAINTENANCE("Kış Bakımı Hatırlatması",
            "Lütfen kış lastiği değişimi, antifriz kontrolü ve diğer kışlık bakımlarınızı gerçekleştirin!",
            Calendar.DECEMBER, 1, 1, 5),

    TRAFFIC_INSURANCE("Zorunlu Trafik Sigortası Yenileme",
            "Lütfen zorunlu trafik sigortanızı yenilemeyi unutmayın!",
            Calendar.JANUARY, 1, 1, 4),

    MAIN_MAINTENANCE("TÜVTÜRK Araç Bakım Hatırlatması",
            "Aracınızın bakımını yapmayı unutmayın! Lütfen TÜVTÜRK kontrolünüzü gerçekleştirin.",
            Calendar.JANUARY, 1, 2, 1),

    TRAFFIC_FINE_JANUARY("Trafik Cezası Hatırlatması",
            "Trafik cezası sorgulamanızı yapmayı unutmayın!",
            Calendar.JANUARY, 1, 1, 3),

    TRAFFIC_FINE_JULY("Trafik Cezası Hatırlatması",
            "Trafik cezası sorgulamanızı yapmayı unutmayın!",
            Calendar.JULY, 1, 1, 3);

    private final String title;
    private final String message;
    private final int month;
    private final int day;
    private final int yearInterval;
    private final int notificationId;

    MaintenanceType(String title, String message, int month, int day, int yearInterval, int notificationId) {
        this.title = title;
        this.message = message;
        this.month = month;
        this.day = day;
        this.yearInterval = yearInterval;
        this.notificationId = notificationId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public int getYearInterval() {
        return yearInterval;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public String getWorkerId() {
        return this.name().toLowerCase() + "_worker";
    }
}
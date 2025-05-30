package com.example.carcare;

import java.util.Calendar;

public enum MaintenanceType {
    MOTOR_OIL("Engine Oil & Filter Change Reminder",
            "Please remember to change your engine oil and filter!",
            Calendar.FEBRUARY, 1, 1, 10), // Notification ID: 10

    AIR_FILTER("Air Filter Change Reminder",
            "Please remember to change your air filter!",
            Calendar.MARCH, 1, 2, 11), // Notification ID: 11

    FUEL_FILTER("Fuel Filter Change Reminder",
            "Please remember to change your fuel filter!",
            Calendar.APRIL, 1, 4, 12), // Notification ID: 12

    BRAKE_SYSTEM("Brake System Check Reminder",
            "Please remember to check your brake system!",
            Calendar.MAY, 1, 2, 13), // Notification ID: 13

    SPARK_PLUG("Spark Plugs Replacement Reminder",
            "Please remember to replace your spark plugs!",
            Calendar.JUNE, 1, 3, 14), // Notification ID: 14

    SUMMER_MAINTENANCE("Summer Maintenance Reminder",
            "Please remember your summer maintenance: A/C check, tire inspection, etc.!",
            Calendar.JUNE, 1, 1, 6), // Notification ID: 6

    WINTER_MAINTENANCE("Winter Maintenance Reminder",
            "Please perform your winter maintenance: winter tires, antifreeze check, etc.!",
            Calendar.DECEMBER, 1, 1, 5), // Notification ID: 5

    TRAFFIC_INSURANCE("Mandatory Traffic Insurance Renewal",
            "Please remember to renew your mandatory traffic insurance!",
            Calendar.JANUARY, 1, 1, 4), // Notification ID: 4

    // "TÜVTÜRK" Türkiye'ye özgü bir kurum olduğu için genel bir ifade kullandım.
    // Hedef kitlenize göre "Vehicle Inspection Reminder" veya "MOT Reminder" (UK) gibi ifadeler kullanabilirsiniz.
    MAIN_MAINTENANCE("Vehicle Inspection Reminder(TÜVTÜRK)",
            "Don't forget your vehicle's inspection(TÜVTÜRK)! Please ensure it's up to date.",
            Calendar.JANUARY, 1, 2, 1), // Notification ID: 1

    // Trafik cezası sorgulaması da ülkeye göre değişir. Genel bir ifade kullandım.
    TRAFFIC_FINE_JANUARY("Traffic Fine Check Reminder (January)",
            "Don't forget to check for any outstanding traffic fines!",
            Calendar.JANUARY, 1, 1, 3), // Notification ID: 3

    TRAFFIC_FINE_JULY("Traffic Fine Check Reminder (July)",
            "Don't forget to check for any outstanding traffic fines!",
            Calendar.JULY, 1, 1, 3); // Notification ID: 3 (Aynı ID, farklı zamanlama)

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
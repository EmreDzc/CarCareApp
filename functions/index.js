const {onSchedule} = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendAnnualMaintenanceNotification = onSchedule("0 0 1 1 *", {
  timeZone: "Europe/Istanbul",
}, async (event) => {
  const payload = {
    notification: {
      title: "Yýllýk Araba Bakým Zamaný!",
      body: "Arabanýzýn yýllýk bakýmýný yapmayý unutmayýn!",
    },
  };

  await admin.messaging().sendToTopic("maintenance", payload);

  const db = admin.firestore();
  await db.collection("notifications").add({
    title: payload.notification.title,
    message: payload.notification.body,
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
  });

  return null;
});

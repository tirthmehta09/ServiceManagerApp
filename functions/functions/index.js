const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const APP_SECRET = "SM_SECRET_2026";

exports.sendFCM = functions.https.onRequest(async (req, res) => {
    console.log("Headers:", JSON.stringify(req.headers));
    console.log("Body:", JSON.stringify(req.body));

    // 1. Verify Custom Secret Header
    const secret = req.headers["x-fcm-secret"];
    if (!secret || secret !== APP_SECRET) {
        console.warn(`Unauthorized: Expected ${APP_SECRET}, got ${secret}`);
        res.status(401).send("Unauthorized");
        return;
    }

    try {
        const payload = req.body;

        const message = {
            topic: payload.to.replace("/topics/", ""),
            notification: {
                title: payload.notification.title,
                body: payload.notification.body,
            },
            data: {
                title: payload.data.title || payload.notification.title,
                body: payload.data.body || payload.notification.body,
                serviceId: payload.data.serviceId || "",
            },
            android: {
                notification: {
                    channel_id: "service_manager_channel",
                    priority: "high",
                },
            },
        };

        const response = await admin.messaging().send(message);
        res.status(200).send({ success: true, response });
    } catch (error) {
        console.error("FCM Send Error:", error);
        res.status(500).send({ error: error.message });
    }
});
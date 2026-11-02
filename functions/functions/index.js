const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// The shared secret the app sends in X-FCM-Secret. Set it with
//     firebase functions:config:set app.secret="<a long random string>"
// or as the APP_SECRET environment variable. It is never committed.
const APP_SECRET = process.env.APP_SECRET ||
    (functions.config().app && functions.config().app.secret);

exports.sendFCM = functions.https.onRequest(async (req, res) => {
    console.log("Headers:", JSON.stringify(req.headers));
    console.log("Body:", JSON.stringify(req.body));

    // 1. Verify Custom Secret Header
    const secret = req.headers["x-fcm-secret"];
    // Fail closed: with no secret configured the function rejects everything,
    // rather than accepting every caller. And never log the expected value --
    // the old line printed it into the function log on each failed attempt.
    if (!APP_SECRET || !secret || secret !== APP_SECRET) {
        console.warn("Unauthorized: bad or missing X-FCM-Secret");
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
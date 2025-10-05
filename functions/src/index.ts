import {onDocumentCreated} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

export const checkForMatches = onDocumentCreated("items/{itemId}", async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
        logger.log("Nema podataka o novom predmetu.");
        return;
    }
    const newItem = snapshot.data();
    const newItemId = event.params.itemId;

    if (newItem.lostStatus === true) {
        logger.log("Novi predmet je 'izgubljen', ne tražimo poklapanja.");
        return;
    }

    logger.log(`Pronađen predmet dodat: ${newItemId}. Tražim poklapanja...`);

    const allItemsSnapshot = await db.collection("items").get();

    for (const doc of allItemsSnapshot.docs) {
        if (doc.id === newItemId) continue;
        
        const existingItem = doc.data();

        if (existingItem.lostStatus === true &&
            existingItem.category === newItem.category &&
            existingItem.secretDetails.toLowerCase() === newItem.secretDetails.toLowerCase()
        ) {
            logger.log(`POKLAPANJE PRONAĐENO! Izgubljen: ${doc.id}, Pronađen: ${newItemId}`);

            const ownerId = existingItem.userId;
            const finderId = newItem.userId;

            await sendNotification(ownerId,
                "Moguće poklapanje za vaš predmet!",
                `Pronađen je predmet iz kategorije '${newItem.category}' koji se možda poklapa sa vašim. Pogledajte listu predmeta za više detalja.`
            );

            await sendNotification(finderId,
                "Moguće poklapanje za predmet koji ste pronašli!",
                `Prijavili ste pronalazak predmeta iz kategorije '${newItem.category}'. Moguće je da smo pronašli vlasnika. Pogledajte listu predmeta.`
            );
            return;
        }
    }
    logger.log("Nema poklapanja.");
});

async function sendNotification(userId: string, title: string, body: string) {
    const userDoc = await db.collection("users").doc(userId).get();
    const userData = userDoc.data();

    if (userData && userData.fcmToken) {
        const fcmToken = userData.fcmToken;
        const payload: admin.messaging.Message = {
            token: fcmToken,
            notification: {
                title: title,
                body: body,
            },
        };
        logger.log(`Šaljem notifikaciju korisniku ${userId} na token ${fcmToken}`);
        return messaging.send(payload);
    } else {
        logger.log(`Korisnik ${userId} nema FCM token.`);
        return null;
    }
}
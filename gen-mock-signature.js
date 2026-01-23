const crypto = require("crypto");

// ===== MOCK INPUT =====
const WEBHOOK_SECRET = "webhook_secret"; // giả lập process.env.WEBHOOK_SECRET

// payload PHẢI là string (đúng format backend verify)
const payload = JSON.stringify({
    type: "POST",
    ownerId: "6893bcd8-bd5e-42f9-9e10-61a639136734",
    fileId: "mock",
    image: {
        url: "mock_url",
        contentType: "image/png"
    }
});

// ===== GENERATE SIGNATURE =====
const signature = crypto
    .createHmac("sha256", WEBHOOK_SECRET)
    .update(payload)
    .digest("hex");

// ===== OUTPUT =====
console.log("Payload:");
console.log(payload);
console.log("\nSignature:");
console.log(signature);

const queueList = document.getElementById("queueList");
const totalPatients = document.getElementById("totalPatients");
const waitingCount = document.getElementById("waitingCount");
const modal = document.getElementById("modal");
const toast = document.getElementById("toast");

async function loadPatients() {
    try {
        const response = await fetch("/api/patients");
        const patients = await response.json();

        renderQueue(patients);
        waitingCount.textContent = patients.length;
        totalPatients.textContent = patients.length;
    } catch (error) {
        queueList.innerHTML =
            '<div class="empty">Could not connect to the Java server.</div>';
    }
}

function renderQueue(patients) {
    if (!patients.length) {
        queueList.innerHTML =
            '<div class="empty">No patients are currently waiting.</div>';
        return;
    }

    queueList.innerHTML = patients.map((patient, index) => `
        <div class="queue-item">
            <div class="queue-number">${index + 1}</div>

            <div>
                <h4>${escapeHtml(patient.name)}</h4>
                <p>
                    ${escapeHtml(patient.id)}
                    • Age ${patient.age}
                    • ${escapeHtml(patient.symptoms || "No symptoms recorded")}
                </p>
            </div>

            <span class="badge">Waiting</span>
        </div>
    `).join("");
}

async function callNext() {
    try {
        const response = await fetch("/api/next", {
            method: "POST"
        });

        const data = await response.json();

        if (!response.ok) {
            showToast(data.error || "No patient is waiting.");
            return;
        }

        showToast(`Now consulting: ${data.name} (${data.id})`);
        loadPatients();

    } catch (error) {
        showToast("Could not connect to the server.");
    }
}

document.getElementById("patientForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const params = new URLSearchParams({
        id: document.getElementById("id").value,
        name: document.getElementById("name").value,
        age: document.getElementById("age").value,
        phone: document.getElementById("phone").value,
        symptoms: document.getElementById("symptoms").value
    });

    try {
        const response = await fetch("/api/patients", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: params
        });

        const data = await response.json();

        if (!response.ok) {
            showToast(data.error || "Could not add patient.");
            return;
        }

        document.getElementById("patientForm").reset();

        closeModal();

        showToast("Patient added successfully.");

        loadPatients();

    } catch (error) {
        showToast("Could not connect to the server.");
    }
});

document.getElementById("searchForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const id = document.getElementById("searchId").value.trim();

    const result = document.getElementById("searchResult");

    try {
        const response = await fetch(
            `/api/search?id=${encodeURIComponent(id)}`
        );

        const data = await response.json();

        result.classList.remove("hidden");

        if (!response.ok) {
            result.innerHTML = `
                <strong>Patient not found</strong>
                <span>${escapeHtml(data.error)}</span>
            `;

            return;
        }

        result.innerHTML = `
            <strong>${escapeHtml(data.name)}</strong>

            <span>
                ID: ${escapeHtml(data.id)}<br>
                Age: ${data.age}<br>
                Phone: ${escapeHtml(data.phone || "Not provided")}<br>
                Symptoms: ${escapeHtml(data.symptoms || "Not provided")}
            </span>
        `;

    } catch (error) {

        result.classList.remove("hidden");

        result.innerHTML = `
            <strong>Server error</strong>
        `;
    }
});

function openModal() {
    modal.classList.remove("hidden");

    document.getElementById("id").focus();
}

function closeModal() {
    modal.classList.add("hidden");
}

modal.addEventListener("click", (event) => {

    if (event.target === modal) {
        closeModal();
    }

});

function showToast(message) {

    toast.textContent = message;

    toast.classList.add("show");

    setTimeout(() => {
        toast.classList.remove("show");
    }, 2800);
}

function escapeHtml(value) {

    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

loadPatients();
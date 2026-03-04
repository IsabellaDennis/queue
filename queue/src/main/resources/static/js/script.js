let allTokens = [];
let pieChart, barChart;

async function apiFetch(url) {
    let res = await fetch(url, { credentials: 'include' });
    if (res.status === 401 || res.status === 403) {
        window.location.href = "/login";
        return null;
    }
    if (!res.ok) {
        return null;
    }
    return res;
}

// Persist counter count across browser sessions
let deskCount = parseInt(localStorage.getItem('deskCount') || '3');

function addCounterCard(num) {
    const colors = ['var(--stat-1)', 'var(--stat-2)', 'var(--stat-3)', 'var(--stat-4)'];
    const color = colors[(num - 1) % colors.length];
    let grid = document.getElementById("staffGrid");
    let newDesk = document.createElement("div");
    newDesk.className = "staff-card";
    newDesk.innerHTML = `
        <div class="staff-avatar" style="background: ${color};">C${num}</div>
        <div class="staff-info">
            <h4>Counter ${num}</h4>
            <span>Waiting for patient</span>
        </div>
        <span class="status-badge status-WAITING" style="margin-left: auto;">Idle</span>
    `;
    grid.appendChild(newDesk);
}

function addNewDesk() {
    deskCount++;
    localStorage.setItem('deskCount', deskCount);
    addCounterCard(deskCount);
}

async function resetCounters() {
    const summaryRes = await apiFetch('/queue/summary');
    if (!summaryRes) return;
    const data = await summaryRes.json();
    const serving = Number(data.serving);

    if (serving > 3) {
        alert(`⚠️ Cannot reset counters!\n\n${serving} tokens are currently being served. Resetting to 3 counters would leave ${serving - 3} tokens with no counter.\n\nPlease complete or reset the queue first.`);
        return;
    }

    // serving <= 3: reset silently, no confirmation needed
    let grid = document.getElementById("staffGrid");
    let cards = grid.getElementsByClassName("staff-card");
    while (cards.length > 3) {
        grid.removeChild(cards[3]);
    }
    deskCount = 3;
    localStorage.removeItem('deskCount');
    refresh();
}

// Restore extra counters added in previous sessions
(function restoreCounters() {
    for (let i = 4; i <= deskCount; i++) {
        addCounterCard(i);
    }
})();

function showSection(section, el) {
    document.getElementById("dashboardSection").style.display = "none";
    document.getElementById("tokensSection").style.display = "none";
    document.getElementById("countersSection").style.display = "none";

    document.getElementById(section + "Section").style.display = "block";

    document.querySelectorAll(".menu-item").forEach(i => i.classList.remove("active"));
    el.classList.add("active");

    if (section === "tokens") loadHistory();
}

async function generateToken() { await apiFetch('/queue/generate'); refresh(); }

async function serveNext() {
    const summaryRes = await apiFetch('/queue/summary');
    if (!summaryRes) return;
    const data = await summaryRes.json();

    if (data.waiting === 0 || data.waiting === '0') {
        alert("No tokens are waiting in the queue.");
        return;
    }

    if (data.serving >= deskCount) {
        alert("All counters are busy! Please complete a token first or add a new counter.");
        return;
    }

    await apiFetch('/queue/serve');
    refresh();
}

async function completeToken() {
    let token = document.getElementById("completeToken").value;
    if (!token) return alert("Enter token");
    await apiFetch('/queue/complete/' + token);
    document.getElementById("completeToken").value = "";
    refresh();
}

async function resetQueue() { await apiFetch('/queue/reset'); refresh(); }
async function logout() { await fetch('/logout', { credentials: 'include' }); window.location = "/login"; }

async function loadSummary() {
    const res = await apiFetch('/queue/summary');
    if (!res) return;
    const data = await res.json();
    document.getElementById("total").innerText = data.total;
    document.getElementById("waiting").innerText = data.waiting;
    document.getElementById("serving").innerText = data.serving;
    document.getElementById("completed").innerText = data.completed;
}

async function loadServing() {
    const res = await apiFetch('/queue/serving');
    if (!res) return;
    const data = await res.json();

    // 1. Update the Main Dashboard "Currently Serving" display
    const tokenElement = document.getElementById("currentlyServing");
    if (data && data.length > 0) {
        tokenElement.innerText = data.map(t => t.tokenNumber).join(" ");
    } else {
        tokenElement.innerText = "--";
    }

    // 2. Distribute Serving Tokens to all active Service Desks
    let grid = document.getElementById("staffGrid");
    if (grid) {
        let desks = grid.getElementsByClassName("staff-card");
        let tokenIndex = 0;

        for (let i = 0; i < desks.length; i++) {
            let deskInfo = desks[i].querySelector('.staff-info span');
            let deskStatusBadge = desks[i].querySelector('.status-badge');

            if (data && tokenIndex < data.length) {
                let servingToken = data[tokenIndex].tokenNumber;
                deskInfo.innerHTML = `Serving Token <strong style="color:var(--primary)">${servingToken}</strong>`;
                deskStatusBadge.className = "status-badge status-SERVING";
                deskStatusBadge.innerText = "Busy";
                tokenIndex++;
            } else {
                deskInfo.innerText = "Waiting for patient";
                deskStatusBadge.className = "status-badge status-COMPLETED";
                deskStatusBadge.innerText = "Idle";
            }
        }
    }
}

async function loadHistory() {
    const res = await apiFetch('/queue/all');
    if (!res) return;
    allTokens = await res.json();
    renderTable();
}

function formatTime(timeData) {
    if (!timeData) return "--";
    try {
        let date;
        if (Array.isArray(timeData)) {
            date = new Date(timeData[0], timeData[1] - 1, timeData[2], timeData[3], timeData[4], timeData[5] || 0);
        } else {
            date = new Date(timeData);
        }
        return date.toLocaleTimeString('en-IN', { timeZone: 'Asia/Kolkata', hour: '2-digit', minute: '2-digit', hour12: true });
    } catch (e) { return "--"; }
}

function renderTable() {
    let search = document.getElementById("searchInput").value.toUpperCase();
    let status = document.getElementById("statusFilter").value;
    let historyDiv = document.getElementById("history");

    let filtered = allTokens.filter(t => {
        return t.tokenNumber.toUpperCase().includes(search) &&
            (status === "" || t.status === status);
    });

    if (filtered.length === 0) {
        historyDiv.innerHTML = `<div style='text-align:center; padding: 40px; color:var(--text-muted); font-size:16px;'>No tokens match your search criteria.</div>`;
        return;
    }

    let html = `<div class="token-cards-grid">`;
    filtered.forEach(t => {
        let created = formatTime(t.createdTime);
        let served = t.status === 'WAITING' ? '--' : formatTime(t.servedTime);

        html += `
        <div class="token-history-card">
            <div class="thc-header">
                <h3>${t.tokenNumber}</h3>
                <span class="status-badge status-${t.status}">${t.status}</span>
            </div>
            <div class="thc-body">
                <div class="time-block">
                    <span class="time-label">Generated</span>
                    <span class="time-value">${created}</span>
                </div>
                <div class="time-block">
                    <span class="time-label">Served</span>
                    <span class="time-value">${served}</span>
                </div>
            </div>
        </div>
        `;
    });
    html += `</div>`;
    historyDiv.innerHTML = html;
}

document.getElementById("searchInput").addEventListener("keyup", renderTable);
document.getElementById("statusFilter").addEventListener("change", renderTable);

function refresh() {
    loadSummary();
    loadServing();
}

setInterval(refresh, 3000);
refresh();

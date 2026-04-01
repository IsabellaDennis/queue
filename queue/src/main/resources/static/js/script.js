let allTokens = [];
let pieChart, barChart;

// Inject serving card styles directly so they always apply regardless of CSS caching
(function injectStyles() {
    const style = document.createElement('style');
    style.textContent = `
        @keyframes tokenPulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.75; }
        }
        @keyframes liveDot {
            0%, 100% { transform: scale(1); opacity: 1; }
            50% { transform: scale(1.4); opacity: 0.6; }
        }
        #currentlyServing {
            display: flex !important;
            flex-wrap: wrap;
            justify-content: center;
            gap: 20px;
            padding: 4px 0;
        }
        .dash-serving-card {
            background: #ffffff;
            border-radius: 12px;
            border: 1px solid #fee2e2;
            border-top: 5px solid #dc2626;
            box-shadow: 0 4px 20px rgba(0,0,0,0.05);
            padding: 16px 20px 24px;
            min-width: 170px;
            text-align: center;
            position: relative;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            overflow: hidden;
        }
        .dash-serving-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 12px 30px rgba(220, 38, 38, 0.12);
        }
        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 12px;
            padding-bottom: 8px;
            border-bottom: 1px solid #f1f5f9;
        }
        .live-status {
            font-size: 9px;
            font-weight: 800;
            color: #22c55e;
            display: flex;
            align-items: center;
            gap: 4px;
            letter-spacing: 0.5px;
        }
        .live-dot {
            width: 6px;
            height: 6px;
            background: #22c55e;
            border-radius: 50%;
            animation: liveDot 1.2s ease-in-out infinite;
        }
        .dash-serving-card .desk-badge {
            color: #475569;
            font-size: 10px;
            font-family: 'Inter', sans-serif;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 1px;
        }
        .dash-serving-card .now-serving-label {
            display: block;
            color: #94a3b8;
            font-size: 9px;
            font-family: 'Inter', sans-serif;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 2px;
            margin-bottom: 4px;
        }
        .dash-serving-card .token-number {
            display: block;
            font-family: 'Outfit', sans-serif;
            font-size: 72px;
            font-weight: 800;
            color: #dc2626;
            letter-spacing: -2px;
            line-height: 1;
            animation: tokenPulse 2s ease-in-out infinite;
        }
    `;
    document.head.appendChild(style);
})();











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

    const tokenElement = document.getElementById("currentlyServing");
    let grid = document.getElementById("staffGrid");
    let currentlyServingCards = [];

    // Load persistent token-to-desk assignments
    let tokenDesks = JSON.parse(localStorage.getItem('tokenDesks') || '{}');
    let servingNumbers = data ? data.map(t => t.tokenNumber) : [];

    // 1. Clean up completed tokens from assignments
    for (let t in tokenDesks) {
        if (!servingNumbers.includes(t)) {
            delete tokenDesks[t];
        }
    }

    // 2. Assign desks to newly serving tokens
    let usedDesks = Object.values(tokenDesks);
    for (let t of servingNumbers) {
        if (!tokenDesks[t]) {
            let assigned = 1;
            while (usedDesks.includes(assigned) && assigned <= deskCount) {
                assigned++;
            }
            tokenDesks[t] = assigned;
            usedDesks.push(assigned);
        }
    }
    localStorage.setItem('tokenDesks', JSON.stringify(tokenDesks));

    if (grid) {
        let desks = grid.getElementsByClassName("staff-card");

        for (let i = 0; i < desks.length; i++) {
            let deskNum = i + 1;
            let deskInfo = desks[i].querySelector('.staff-info span');
            let deskStatusBadge = desks[i].querySelector('.status-badge');
            let deskName = desks[i].querySelector('h4').innerText;

            let tokenObj = data ? data.find(t => tokenDesks[t.tokenNumber] === deskNum) : null;

            if (tokenObj) {
                let servingToken = tokenObj.tokenNumber;
                deskInfo.innerHTML = `Serving Token <strong style="color:var(--primary)">${servingToken}</strong>`;
                deskStatusBadge.className = "status-badge status-SERVING";
                deskStatusBadge.innerText = "Busy";
                
                currentlyServingCards.push(`
                    <div class="dash-serving-card">
                        <div class="card-header">
                            <span class="desk-badge">${deskName}</span>
                            <span class="live-status"><span class="live-dot"></span>LIVE</span>
                        </div>
                        <span class="now-serving-label">Now Serving</span>
                        <span class="token-number">${servingToken}</span>
                    </div>
                `);

            } else {
                deskInfo.innerText = "Waiting for patient";
                deskStatusBadge.className = "status-badge status-COMPLETED";
                deskStatusBadge.innerText = "Idle";
            }
        }
    }

    if (currentlyServingCards.length > 0) {
        tokenElement.style.display = "flex";
        tokenElement.style.flexWrap = "wrap";
        tokenElement.style.justifyContent = "center";
        tokenElement.style.gap = "20px";
        tokenElement.innerHTML = currentlyServingCards.join("");
    } else {
        tokenElement.style.display = "block";
        tokenElement.innerHTML = "--";
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

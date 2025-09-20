// Suspicious Activity Modal logic
const suspiciousActivityModal = document.getElementById('suspiciousActivityModal');
const suspiciousActivityBody = document.getElementById('suspiciousActivityBody');
const suspiciousActivityTitle = document.getElementById('suspiciousActivityTitle');
const closeSuspiciousActivity = document.getElementById('closeSuspiciousActivity');
const viewSuspiciousActivityBtn = document.getElementById('viewSuspiciousActivityBtn');
const deleteAllUsersBtn = document.getElementById('deleteAllUsersBtn');

async function fetchSuspiciousActivityLogs() {
    suspiciousActivityBody.innerHTML = '<div style="text-align:center;"><i class="fas fa-spinner fa-spin"></i> Loading logs...</div>';
    try {
        const response = await fetch(`${API_BASE_URL}/api/admin/suspicious-activity`, { headers: getAuthHeaders() });
        if (!response.ok) throw new Error('Failed to fetch logs');
        const logs = await response.json();
        if (!Array.isArray(logs) || logs.length === 0) {
            suspiciousActivityBody.innerHTML = '<div style="text-align:center;">No suspicious activity found.</div>';
            return;
        }
        suspiciousActivityBody.innerHTML = logs.map(log => {
            const username = log.username || 'N/A';
            const userId = log.userId != null ? log.userId : 'N/A';
            const action = log.action || 'N/A';
            let time = 'N/A';
            if (log.timestamp) {
                const dateObj = new Date(log.timestamp);
                time = isNaN(dateObj.getTime()) ? 'N/A' : dateObj.toLocaleString();
            }
            return `<div class="suspicious-log-item" style="padding:8px 0;border-bottom:1px solid #eee;">
                <b>User:</b> ${username} (ID: ${userId})<br>
                <b>Action:</b> ${action}<br>
                <b>Time:</b> ${time}
            </div>`;
        }).join('');
    } catch (err) {
        suspiciousActivityBody.innerHTML = `<div style="color:#b91c1c;text-align:center;"><i class="fas fa-exclamation-triangle"></i> ${err.message}</div>`;
    }
}

if (viewSuspiciousActivityBtn) {
    viewSuspiciousActivityBtn.addEventListener('click', () => {
        suspiciousActivityModal.style.display = 'flex';
        suspiciousActivityModal.style.opacity = '1';
        document.body.classList.add('no-scroll');
        fetchSuspiciousActivityLogs();
    });
}
if (deleteAllUsersBtn) {
    deleteAllUsersBtn.addEventListener('click', () => {
        showModal({
            title: 'Delete All Users',
            body: 'This will mark all non-admin users as deleted. This action cannot be undone. Continue?',
            okText: 'Delete All',
            okClass: 'delete',
            onOk: async () => {
                try {
                    const response = await fetch(`${API_BASE_URL}/api/admin/users`, { method: 'DELETE', headers: getAuthHeaders() });
                    const data = await response.json();
                    if (!response.ok || data.error) throw new Error(data.error || 'Failed to delete users');
                    showToast(`Deleted ${data.deleted || 0} users`, 'success');
                    fetchUsers();
                } catch (err) {
                    showToast(err.message, 'error');
                }
            }
        });
    });
}
if (closeSuspiciousActivity) {
    closeSuspiciousActivity.addEventListener('click', () => {
        suspiciousActivityModal.style.display = 'none';
        suspiciousActivityModal.style.opacity = '1';
        document.body.classList.remove('no-scroll');
    });
    // Escape key closes suspicious activity modal
    document.addEventListener('keydown', function (e) {
        if (suspiciousActivityModal.style.display !== 'none' && (e.key === 'Escape' || e.key === 'Esc')) {
            suspiciousActivityModal.style.display = 'none';
            suspiciousActivityModal.style.opacity = '1';
            document.body.classList.remove('no-scroll');
        }
    });
}

// Theme toggle for admin-login.html
if (window.location.pathname.endsWith('admin-login.html')) {
    const themeToggle = document.getElementById('themeToggle');
    if (themeToggle) {
        themeToggle.addEventListener('click', () => {
            const isDark = document.body.classList.toggle('dark');
            const theme = isDark ? 'dark' : 'light';
            localStorage.setItem('qr-studio-theme', theme);
            themeToggle.style.color = isDark ? '#059669' : '#059669';
            themeToggle.style.background = '#fff';
            themeToggle.style.border = '2px solid #059669';
            showToast('Theme changed to ' + theme + ' mode', 'success');
        });
        // Initialize theme on admin-login.html
        const savedTheme = localStorage.getItem('qr-studio-theme') || 'light';
        document.body.classList.toggle('dark', savedTheme === 'dark');
        themeToggle.style.color = savedTheme === 'dark' ? '#059669' : '#059669';
        themeToggle.style.background = '#fff';
        themeToggle.style.border = '2px solid #059669';
    }
}
// Redirect to admin-login.html if not admin
function getJwtPayload(token) {
    if (!token) return null;
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function (c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        return null;
    }
}

if (window.location.pathname.endsWith('admin.html')) {
    const token = localStorage.getItem('authToken');
    const payload = getJwtPayload(token);
    if (!payload || payload.role !== 'admin') {
        window.location.href = 'admin-login.html';
    }
}
const API_BASE_URL = window.location.origin;
const userTableBody = document.getElementById('userTableBody');
const themeToggle = document.getElementById('themeToggle');
const toastContainer = document.getElementById('toastContainer');
const adminModal = document.getElementById('adminModal');
const adminModalTitle = document.getElementById('adminModalTitle');
const adminModalBody = document.getElementById('adminModalBody');
const adminModalOk = document.getElementById('adminModalOk');
const adminModalCancel = document.getElementById('adminModalCancel');

function getAuthHeaders() {
    const token = localStorage.getItem('authToken');
    return token ? {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    } : {
        'Content-Type': 'application/json'
    };
}

function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    let icon = 'check-circle';
    if (type === 'error') icon = 'exclamation-circle';
    else if (type === 'warning') icon = 'exclamation-triangle';
    else if (type === 'info') icon = 'info-circle';
    toast.innerHTML = `<div style="display: flex; align-items: center; gap: 10px;"><i class="fas fa-${icon}"></i><span>${message}</span></div>`;
    toastContainer.appendChild(toast);
    setTimeout(() => { toast.style.opacity = '0'; toast.style.transform = 'translateX(100%)'; setTimeout(() => toast.remove(), 300); }, 3000);
}

function showModal({ title = 'Confirm', body = '', okText = 'OK', cancelText = 'Cancel', okClass = '', onOk = null, onCancel = null }) {
    adminModalTitle.textContent = title;
    adminModalBody.textContent = body;
    adminModalOk.textContent = okText;
    adminModalCancel.textContent = cancelText;
    adminModalOk.className = 'custom-modal-btn ' + (okClass || 'ok');
    adminModal.style.display = 'flex';
    adminModal.style.opacity = '1';
    document.body.classList.add('no-scroll');
    const cleanup = () => {
        adminModal.style.display = 'none';
        adminModal.style.opacity = '1';
        document.body.classList.remove('no-scroll');
        adminModalOk.onclick = null;
        adminModalCancel.onclick = null;
    };
    adminModalOk.onclick = () => { cleanup(); if (onOk) onOk(); };
    adminModalCancel.onclick = () => { cleanup(); if (onCancel) onCancel(); };
    adminModal.addEventListener('mousedown', function (e) { if (e.target === adminModal) cleanup(); });
    // Escape key closes admin modal
    document.addEventListener('keydown', function (e) {
        if (adminModal.style.display !== 'none' && (e.key === 'Escape' || e.key === 'Esc')) {
            cleanup();
        }
    });
}

function updateThemeIcon(theme) {
    const icon = themeToggle.querySelector('i');
    if (theme === 'dark') {
        icon.className = 'fas fa-moon';
        themeToggle.style.color = '#059669';
        themeToggle.style.background = '#fff';
        themeToggle.style.border = '2px solid #059669';
    } else {
        icon.className = 'fas fa-sun';
        themeToggle.style.background = '#fff';
        themeToggle.style.color = '#059669';
        themeToggle.style.border = '2px solid #059669';
    }
}

function initializeTheme() {
    const savedTheme = localStorage.getItem('qr-studio-theme') || 'light';
    document.body.classList.toggle('dark', savedTheme === 'dark');
    updateThemeIcon(savedTheme);
}
themeToggle.addEventListener('click', () => {
    const isDark = document.body.classList.toggle('dark');
    const theme = isDark ? 'dark' : 'light';
    localStorage.setItem('qr-studio-theme', theme);
    updateThemeIcon(theme);
    showToast('Theme changed to ' + theme + ' mode', 'success');
});
initializeTheme();

async function fetchUsers() {
    userTableBody.innerHTML = `<tr><td colspan="5" class="empty-state"><i class="fas fa-spinner fa-spin"></i> Loading users...</td></tr>`;
    try {
        const response = await fetch(`${API_BASE_URL}/api/admin/users`, { headers: getAuthHeaders() });
        if (!response.ok) throw new Error('Failed to fetch users');
        const users = await response.json();
        if (!Array.isArray(users) || users.length === 0) {
            userTableBody.innerHTML = `<tr><td colspan="5" class="empty-state"><i class="fas fa-users"></i> No users found</td></tr>`;
            return;
        }
        userTableBody.innerHTML = users
            .filter(user => user.status !== 'deleted')
            .map(user => {
                const suspiciousBtn = user.status === 'suspicious'
                    ? `<button class="admin-btn ok" onclick="unmarkSuspicious('${user.id}')"><i class="fas fa-check-circle"></i> Unmark Suspicious</button>`
                    : `<button class="admin-btn suspicious" onclick="markSuspicious('${user.id}')"><i class="fas fa-exclamation-triangle"></i> Mark Suspicious</button>`;
                return `<tr>
                    <td>${user.username}</td>
                    <td>${user.email}</td>
                    <td><span class="user-status ${user.role}">${user.role.charAt(0).toUpperCase() + user.role.slice(1)}</span></td>
                    <td><span class="user-status ${user.status}">${user.status === 'suspicious' ? 'Suspicious' : 'Active'}</span></td>
                    <td class="user-actions">
                        ${suspiciousBtn}
                        <button class="admin-btn delete" onclick="deleteUser('${user.id}')"><i class="fas fa-trash"></i> Delete</button>
                    </td>
                </tr>`;
            }).join('');
        window.unmarkSuspicious = function (id) {
            showModal({
                title: 'Unmark User as Suspicious',
                body: 'Are you sure you want to unmark this user as suspicious?',
                okText: 'Unmark',
                okClass: 'ok',
                onOk: async () => {
                    try {
                        const response = await fetch(`${API_BASE_URL}/api/admin/users/${id}/suspicious`, { method: 'DELETE', headers: getAuthHeaders() });
                        const data = await response.json();
                        if (!response.ok || data.error) throw new Error(data.error || 'Failed to unmark user as suspicious');
                        showToast('User unmarked as suspicious', 'success');
                        fetchUsers();
                    } catch (err) {
                        showToast(err.message, 'error');
                    }
                }
            });
        };
    } catch (err) {
        userTableBody.innerHTML = `<tr><td colspan="5" class="empty-state"><i class="fas fa-exclamation-triangle"></i> ${err.message}</td></tr>`;
        showToast(err.message, 'error');
    }
}

window.deleteUser = function (id) {
    showModal({
        title: 'Delete User',
        body: 'Are you sure you want to delete this user account?',
        okText: 'Delete',
        okClass: 'delete',
        onOk: async () => {
            try {
                const response = await fetch(`${API_BASE_URL}/api/admin/users/${id}`, { method: 'DELETE', headers: getAuthHeaders() });
                const data = await response.json();
                if (!response.ok || data.error) throw new Error(data.error || 'Failed to delete user');
                showToast('User deleted successfully', 'success');
                fetchUsers();
            } catch (err) {
                showToast(err.message, 'error');
            }
        }
    });
};

window.markSuspicious = function (id) {
    showModal({
        title: 'Mark User as Suspicious',
        body: 'Are you sure you want to mark this user as suspicious?',
        okText: 'Mark Suspicious',
        okClass: 'suspicious',
        onOk: async () => {
            try {
                const response = await fetch(`${API_BASE_URL}/api/admin/users/${id}/suspicious`, { method: 'POST', headers: getAuthHeaders() });
                const data = await response.json();
                if (!response.ok || data.error) throw new Error(data.error || 'Failed to mark user as suspicious');
                showToast('User marked as suspicious', 'success');
                fetchUsers();
            } catch (err) {
                showToast(err.message, 'error');
            }
        }
    });
};

document.addEventListener('DOMContentLoaded', fetchUsers);
document.getElementById('adminLogoutBtn').addEventListener('click', () => {
    localStorage.removeItem('authToken');
    showToast('Logged out successfully', 'success');
    setTimeout(() => { window.location.href = 'admin-login.html'; }, 800);
});

// Listen for login form submission and show warning toast if deleted
if (window.location.pathname.endsWith('login.html')) {
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            try {
                const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });
                const data = await response.json();
                if (!data.success) {
                    if (data.message && data.message.includes('deleted by admin')) {
                        showToast(data.message, 'warning');
                    } else {
                        showToast(data.message || 'Login failed', 'error');
                    }
                } else {
                    localStorage.setItem('authToken', data.token);
                    showToast('Login successful', 'success');
                    setTimeout(() => { window.location.href = 'index.html'; }, 800);
                }
            } catch (err) {
                showToast('Login error', 'error');
            }
        });
    }
}

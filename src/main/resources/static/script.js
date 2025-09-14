// Base URL for API requests (configurable for public/private networks)
const API_BASE_URL = window.location.origin; // Default to same origin; override in production if needed

// DOM elements
const qrTextInput = document.getElementById('qrText');
const generateBtn = document.getElementById('generateBtn');
const qrFileInput = document.getElementById('qrFile');
const qrImageInput = document.getElementById('qrImageInput');
const qrImageUploadArea = document.getElementById('qrImageUploadArea');
const docUploadArea = document.getElementById('docUploadArea');
const docFileInput = document.getElementById('docFileInput');

// Result elements
const qrResult = document.getElementById('qrResult');
const scanResult = document.getElementById('scanResult');
const historyList = document.getElementById('historyList');

// UI elements
const uploadArea = document.getElementById('uploadArea');
const loadingOverlay = document.getElementById('loadingOverlay');
const toastContainer = document.getElementById('toastContainer');
const themeToggle = document.getElementById('themeToggle');
const clearHistoryBtn = document.getElementById('clearHistoryBtn');

// Stats elements
const totalGeneratedEl = document.getElementById('totalGenerated');
const totalScannedEl = document.getElementById('totalScanned');
const lastActivityEl = document.getElementById('lastActivity');
const charCount = document.getElementById('charCount');

let stats = {
    totalGenerated: 0,
    totalScanned: 0,
    lastActivity: null
};

// Helper function for authenticated API calls
function getAuthHeaders() {
    const token = localStorage.getItem('authToken');
    return token ? {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    } : {
        'Content-Type': 'application/json'
    };
}

// Helper function for authenticated fetch with FormData
function getAuthHeadersForFormData() {
    const token = localStorage.getItem('authToken');
    return token ? {
        'Authorization': `Bearer ${token}`
    } : {};
}

// Initialize localStorage for history if not already set
function initializeLocalStorage() {
    if (!localStorage.getItem('qrHistory')) {
        localStorage.setItem('qrHistory', JSON.stringify([]));
    }
    if (!localStorage.getItem('qrStats')) {
        localStorage.setItem('qrStats', JSON.stringify({ totalGenerated: 0, totalScanned: 0 }));
    }
}

function initializeTheme() {
    const savedTheme = localStorage.getItem('qr-studio-theme') || 'light';
    document.body.classList.toggle('dark', savedTheme === 'dark');
    updateThemeIcon(savedTheme);
    updateThemeToggleStyle(savedTheme);
}

function toggleTheme() {
    const isDark = document.body.classList.toggle('dark');
    const theme = isDark ? 'dark' : 'light';
    localStorage.setItem('qr-studio-theme', theme);
    updateThemeIcon(theme);
    updateThemeToggleStyle(theme);
    showToast('Theme changed to ' + theme + ' mode', 'success');
}

function updateThemeToggleStyle(theme) {
    themeToggle.style.padding = '12px 16px';
    themeToggle.style.border = '2px solid #ffffff';
    themeToggle.style.borderRadius = '8px';
    themeToggle.style.boxShadow = '0 4px 12px rgba(255, 255, 255, 0.3)';
    themeToggle.style.color = '#ffffff';
    themeToggle.style.fontSize = '1.2rem';
    themeToggle.style.cursor = 'pointer';
    themeToggle.style.transition = 'background 0.3s ease';

    if (theme === 'dark') {
        themeToggle.style.background = 'linear-gradient(45deg, #2C3E50, #34495E)';
    } else {
        themeToggle.style.background = 'linear-gradient(45deg, #ECF0F1, #3498DB)';
        themeToggle.style.color = '#2C3E50';
    }
}

function updateThemeIcon(theme) {
    const icon = themeToggle.querySelector('i');
    if (theme === 'dark') {
        icon.className = 'material-symbols-outlined';
        icon.textContent = 'light_mode';
        icon.style.color = '#ffffff';
    } else {
        icon.className = 'fas fa-moon';
        icon.textContent = '';
        icon.style.color = 'dark';
    }
}

function updateCharCount() {
    const count = qrTextInput.value.length;
    charCount.textContent = count;
    charCount.style.color = count > 450 ? '#ef4444' : 'var(--text-muted)';
}

// Add this function to ensure proper dropdown positioning
function ensureDropdownVisibility() {
    const profileDropdown = document.getElementById('profileDropdown');
    if (profileDropdown) {
        profileDropdown.style.zIndex = '10000';
        profileDropdown.style.position = 'absolute';
    }
}

// QR Generation from Text (Backend)
async function generateQR() {
    const text = qrTextInput.value.trim();
    if (!text) {
        showToast('Please enter text to generate QR code', 'error');
        qrTextInput.focus();
        return;
    }

    showLoading('Generating QR code...');
    generateBtn.disabled = true;

    try {
        const response = await fetch(`${API_BASE_URL}/api/qr/generate`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ text })
        });
        const data = await response.json();
        if (!response.ok || data.error) throw new Error(data.error || 'Failed to generate QR code');

        const historyItem = {
            id: data.id,
            text: data.text,
            image: data.image,
            type: data.type,
            timestamp: data.timestamp
        };
        displayQRCode(historyItem);
        qrTextInput.value = '';
        updateCharCount();
        await loadHistory();
        await loadStats();
        showToast('QR code generated successfully!', 'success');
    } catch (error) {
        showError(qrResult, 'Error: ' + error.message);
        showToast('Failed to generate QR code', 'error');
    } finally {
        hideLoading();
        generateBtn.disabled = false;
    }
}

// QR Generation from Image (Backend: treat as scan, then re-generate QR)
async function generateQRFromImage(file) {
    if (!file.type.startsWith('image/')) {
        showToast('Please upload an image file.', 'error');
        return;
    }
    if (file.size > 100 * 1024 * 1024) {
        showToast('File size exceeds 100MB limit.', 'error');
        return;
    }

    showLoading('Generating QR code from image...');
    try {
        const formData = new FormData();
        formData.append('file', file);
        const response = await fetch(`${API_BASE_URL}/api/qr/generate-image`, {
            method: 'POST',
            headers: getAuthHeadersForFormData(),
            body: formData
        });
        const data = await response.json();
        if (!response.ok || data.error) throw new Error(data.error || 'Failed to generate QR code from image');

        const historyItem = {
            id: data.id,
            text: data.text,
            image: data.image,
            type: data.type,
            timestamp: data.timestamp
        };
        displayQRCode(historyItem);
        await loadHistory();
        await loadStats();
        showToast('QR code generated from image successfully!', 'success');
    } catch (error) {
        showError(qrResult, 'Error: ' + error.message);
        showToast('Failed to generate QR code from image', 'error');
    } finally {
        hideLoading();
    }
}

// Display QR Code
function displayQRCode(data) {
    // Escape single quotes and newlines for safe inline JS
    const safeText = (data.text || '').replace(/'/g, "\\'").replace(/\n/g, ' ');
    qrResult.innerHTML = `
        <div class="qr-success">
            <img src="${data.image}" alt="Generated QR Code" style="animation: slideUp 0.5s ease-out;">
            <div style="margin-top: 20px;">
                <p style="color: var(--primary-color); font-weight: 600; margin-bottom: 8px;">
                    <i class="fas fa-check-circle"></i> QR Code Generated!
                </p>
                <p style="font-size: 0.9rem; color: var(--text-secondary); word-break: break-all;">
                    ${safeText}
                </p>
                <div style="margin-top: 15px; display: flex; gap: 10px; justify-content: center;">
                    <button onclick="downloadQR('${data.image}', '${safeText}')"
                            style="padding: 8px 16px; background: var(--gradient-secondary); color: white; border: none; border-radius: var(--radius); cursor: pointer; font-size: 0.9rem;">
                        <i class="fas fa-download"></i> Download
                    </button>
                    <button onclick="copyToClipboard('${safeText}')"
                            style="padding: 8px 16px; background: var(--gradient-primary); color: white; border: none; border-radius: var(--radius); cursor: pointer; font-size: 0.9rem;">
                        <i class="fas fa-copy"></i> Copy Text
                    </button>
                </div>
            </div>
        </div>
    `;
}

// QR Scanning with Image Upload (Backend)
async function handleImageUpload(file) {
    if (!file.type.startsWith('image/')) {
        showToast('Please upload an image file.', 'error');
        return;
    }
    if (file.size > 100 * 1024 * 1024) {
        showToast('File size exceeds 100MB limit.', 'error');
        return;
    }

    showLoading('Scanning QR code...');
    try {
        const formData = new FormData();
        formData.append('file', file);
        const response = await fetch(`${API_BASE_URL}/api/qr/scan`, {
            method: 'POST',
            headers: getAuthHeadersForFormData(),
            body: formData
        });
        const data = await response.json();
        if (!response.ok || data.error) throw new Error(data.error || 'QR code not readable');

        displayScanResult(data.text);
        await loadHistory();
        await loadStats();
        showToast('QR code detected successfully!', 'success');
    } catch (error) {
        showError(scanResult, 'QR code not readable');
        showToast('QR code not readable', 'warning');
    } finally {
        hideLoading();
    }
}

// Display Scan Result
function displayScanResult(data) {
    const isUrl = isValidUrl(data);
    scanResult.innerHTML = `
        <div class="scan-success success" style="animation: slideUp 0.5s ease-out;">
            <div style="margin-bottom: 15px;">
                <i class="fas fa-check-circle" style="font-size: 2rem; color: #10b981; margin-bottom: 10px;"></i>
                <h3 style="color: var(--text-primary); margin-bottom: 10px;">QR Code Detected!</h3>
            </div>
            <div style="background: var(--surface-elevated); padding: 15px; border-radius: var(--radius); margin-bottom: 15px;">
                <p style="word-break: break-all; font-family: monospace; font-size: 0.9rem; color: var(--text-primary);">${data}</p>
            </div>
            <div style="display: flex; gap: 10px; justify-content: center; flex-wrap: wrap;">
                <button onclick="copyToClipboard('${data}')"
                        style="padding: 8px 16px; background: var(--gradient-primary); color: white; border: none; border-radius: var(--radius); cursor: pointer; font-size: 0.9rem;">
                    <i class="fas fa-copy"></i> Copy
                </button>
                ${isUrl ? `<button onclick="window.open('${data}', '_blank')"
                                  style="padding: 8px 16px; background: var(--gradient-secondary); color: white; border: none; border-radius: var(--radius); cursor: pointer; font-size: 0.9rem;">
                            <i class="fas fa-external-link-alt"></i> Open Link
                          </button>` : ''}
            </div>
        </div>
    `;
}

// Drag and drop handlers for QR scanning
function handleDragOver(e) {
    e.preventDefault();
    uploadArea.classList.add('dragover');
}

function handleDragLeave(e) {
    e.preventDefault();
    uploadArea.classList.remove('dragover');
}

function handleDrop(e) {
    e.preventDefault();
    uploadArea.classList.remove('dragover');
    const files = e.dataTransfer.files;
    if (files.length > 0) {
        handleImageUpload(files[0]);
    }
}

// Drag and drop handlers for QR image generation
function handleImageDragOver(e) {
    e.preventDefault();
    qrImageUploadArea.classList.add('dragover');
}

function handleImageDragLeave(e) {
    e.preventDefault();
    qrImageUploadArea.classList.remove('dragover');
}

function handleImageDrop(e) {
    e.preventDefault();
    qrImageUploadArea.classList.remove('dragover');
    const files = e.dataTransfer.files;
    if (files.length > 0) {
        generateQRFromImage(files[0]);
    }
}

// Load history from backend
async function loadHistory() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/qr/history`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errorText || 'Failed to fetch history'}`);
        }
        const history = await response.json();
        if (!Array.isArray(history) || history.length === 0) {
            historyList.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-inbox"></i>
                    <h3>No activity yet</h3>
                    <p>Generated or scanned QR codes will appear here</p>
                </div>
            `;
            return;
        }
        historyList.innerHTML = history.map((item, index) => {
            const typeLabel = item.type === 'scanned' ? '<span class="history-type scanned">Scanned</span>' : '<span class="history-type generated">Generated</span>';
            const exactTime = new Date(item.timestamp).toLocaleString();
            // Escape single quotes and newlines for safe inline JS
            const safeText = (item.text || '').replace(/'/g, "\\'").replace(/\n/g, ' ');
            return `
            <div class="history-item" style="animation-delay: ${index * 0.1}s;">
                <div class="history-text">
                    <div class="text-content">${truncateText(item.text, 60)}</div>
                    <div class="history-meta">
                        ${typeLabel}
                        <span class="history-time"><i class="fas fa-clock"></i> ${formatDate(item.timestamp)} <span class="history-exact-time" title="${exactTime}">(${exactTime})</span></span>
                    </div>
                    <img src="${item.image || ''}" alt="QR Code" style="max-width: 50px; max-height: 50px; margin-top: 5px;">
                </div>
                <div style="display: flex; gap: 10px;">
                    <button onclick="regenerateQR('${safeText}')"
                            style="padding: 6px 12px; background: var(--gradient-primary); color: white; border: none; border-radius: var(--radius); cursor: pointer; font-size: 0.8rem;">
                        <i class='fas fa-pen-to-square'></i>
                    </button>
                    <button class="delete-btn" onclick="deleteQR(${item.id})">
                        <i class="fas fa-trash-can"></i>
                    </button>
                </div>
            </div>
            `;
        }).join('');
    } catch (error) {
        console.error('Error loading history:', error);
        historyList.innerHTML = `
            <div class="error">
                <i class="fas fa-exclamation-triangle" style="margin-right: 10px;"></i>
                Failed to load history: ${error.message}
            </div>
        `;
        showToast('Failed to load history', 'error');
    }
}

async function deleteQR(id) {
    showModal({
        title: 'Delete QR Code',
        body: 'Are you sure you want to delete this QR code?',
        okText: 'Delete',
        okClass: 'ok',
        onOk: async () => {
            try {
                const response = await fetch(`${API_BASE_URL}/api/qr/${id}`, {
                    method: 'DELETE',
                    headers: getAuthHeaders()
                });
                const data = await response.json();
                if (!response.ok || data.error) throw new Error(data.error || 'Failed to delete QR code');
                await loadHistory();
                await loadStats();
                showToast('QR code deleted successfully', 'success');
            } catch (error) {
                showToast('Failed to delete QR code', 'error');
            }
        }
    });
}

async function clearAllHistory() {
    showModal({
        title: 'Clear All History',
        body: 'Are you sure you want to clear all history? This action cannot be undone.',
        okText: 'Clear All',
        okClass: 'ok',
        onOk: async () => {
            try {
                const response = await fetch(`${API_BASE_URL}/api/qr/history`, {
                    headers: getAuthHeaders()
                });
                const history = await response.json();
                for (const item of history) {
                    await fetch(`${API_BASE_URL}/api/qr/${item.id}`, {
                        method: 'DELETE',
                        headers: getAuthHeaders()
                    });
                }
                await loadHistory();
                await loadStats();
                showToast('History cleared successfully', 'success');
            } catch (error) {
                showToast('Failed to clear history', 'error');
            }
        }
    });
}

// Show custom modal for confirmations
function showModal({ title = 'Confirm', body = '', okText = 'OK', cancelText = 'Cancel', okClass = '', onOk = null, onCancel = null }) {
    const modal = document.getElementById('customModal');
    const modalTitle = document.getElementById('customModalTitle');
    const modalBody = document.getElementById('customModalBody');
    const btnOk = document.getElementById('customModalOk');
    const btnCancel = document.getElementById('customModalCancel');

    modalTitle.textContent = title;
    modalBody.textContent = body;
    btnOk.textContent = okText;
    btnCancel.textContent = cancelText;
    btnOk.className = 'custom-modal-btn ' + (okClass || 'ok');

    modal.style.display = 'flex';

    const cleanup = () => {
        modal.style.display = 'none';
        btnOk.onclick = null;
        btnCancel.onclick = null;
    };

    btnOk.onclick = () => {
        cleanup();
        if (onOk) onOk();
    };
    btnCancel.onclick = () => {
        cleanup();
        if (onCancel) onCancel();
    };
}

// Utility functions
function regenerateQR(text) {
    qrTextInput.value = text;
    updateCharCount();
    qrTextInput.focus();
}

function downloadQR(dataUrl, filename) {
    const link = document.createElement('a');
    link.download = `qr-code-${filename.substring(0, 20)}.png`;
    link.href = dataUrl;
    link.click();
    showToast('QR code downloaded', 'success');
}

async function copyToClipboard(text) {
    try {
        await navigator.clipboard.writeText(text);
        showToast('Copied to clipboard', 'success');
    } catch (error) {
        showToast('Failed to copy to clipboard', 'error');
    }
}

function isValidUrl(string) {
    try {
        new URL(string);
        return true;
    } catch (_) {
        return false;
    }
}

function truncateText(text, maxLength) {
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
}

function formatDate(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;

    return date.toLocaleDateString();
}

// Stats management (Backend)
async function loadStats() {
    try {
        const response = await fetch(`${API_BASE_URL}/api/qr/stats`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to fetch stats');
        const statsData = await response.json();
        stats.totalGenerated = statsData.generated || 0;
        stats.totalScanned = statsData.scanned || 0;
        // Get last activity from history
        const historyResp = await fetch(`${API_BASE_URL}/api/qr/history`, {
            headers: getAuthHeaders()
        });
        if (!historyResp.ok) throw new Error('Failed to fetch history for stats');
        const history = await historyResp.json();
        if (Array.isArray(history) && history.length > 0) {
            stats.lastActivity = history[0].timestamp;
        } else {
            stats.lastActivity = null;
        }
        updateStatsDisplay();
    } catch (e) {
        console.error('Error loading stats:', e);
        stats.totalGenerated = 0;
        stats.totalScanned = 0;
        stats.lastActivity = null;
        updateStatsDisplay();
    }
}

function updateStatsDisplay() {
    totalGeneratedEl.textContent = stats.totalGenerated;
    totalScannedEl.textContent = stats.totalScanned;
    lastActivityEl.textContent = stats.lastActivity
        ? formatDate(stats.lastActivity)
        : 'Never';
}

// UI helpers
function showLoading(message = 'Loading...') {
    loadingOverlay.querySelector('p').textContent = message;
    loadingOverlay.classList.add('show');
}

function hideLoading() {
    loadingOverlay.classList.remove('show');
}

function showError(element, message) {
    element.innerHTML = `
        <div class="error" style="animation: slideUp 0.3s ease-out;">
            <i class="fas fa-exclamation-triangle" style="margin-right: 10px;"></i>
            ${message}
        </div>
    `;
}

function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let icon = 'check-circle';
    if (type === 'error') icon = 'exclamation-circle';
    else if (type === 'warning') icon = 'exclamation-triangle';
    else if (type === 'info') icon = 'info-circle';

    toast.innerHTML = `
        <div style="display: flex; align-items: center; gap: 10px;">
            <i class="fas fa-${icon}"></i>
            <span>${message}</span>
        </div>
    `;

    toastContainer.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Authentication functions
async function checkAuthStatus() {
    const token = localStorage.getItem('authToken');
    const userMenu = document.getElementById('userMenu');
    const authBtn = document.getElementById('authBtn');
    const currentUsername = document.getElementById('currentUsername');
    const profileName = document.getElementById('profileName');
    const profileEmail = document.getElementById('profileEmail');

    if (!token) {
        // User is not logged in - show login button
        userMenu.style.display = 'none';
        authBtn.style.display = 'block';
        authBtn.textContent = 'Login';
        authBtn.onclick = () => window.location.href = 'login.html';
        return false;
    }

    try {
        // Fetch user profile from backend
        const response = await fetch('/api/auth/profile', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            }
        });

        const data = await response.json();

        if (data.success && response.ok) {
            // User is logged in - show profile dropdown
            userMenu.style.display = 'block';
            authBtn.style.display = 'none';
            currentUsername.textContent = data.username;
            profileName.textContent = data.username;
            profileEmail.textContent = data.email;

            // Initialize profile dropdown functionality
            initializeProfileDropdown();
            return true;
        } else {
            // Token is invalid, remove it and show login
            localStorage.removeItem('authToken');
            userMenu.style.display = 'none';
            authBtn.style.display = 'block';
            authBtn.textContent = 'Login';
            authBtn.onclick = () => window.location.href = 'login.html';
            return false;
        }
    } catch (error) {
        console.error('Error checking auth status:', error);
        // On error, assume not authenticated
        localStorage.removeItem('authToken');
        userMenu.style.display = 'none';
        authBtn.style.display = 'block';
        authBtn.textContent = 'Login';
        authBtn.onclick = () => window.location.href = 'login.html';
        return false;
    }
} function initializeProfileDropdown() {
    const profileBtn = document.getElementById('profileBtn');
    const profileDropdown = document.getElementById('profileDropdown');
    const logoutBtn = document.getElementById('logoutBtn');
    const editProfile = document.getElementById('editProfile');

    // Ensure dropdown visibility properties
    ensureDropdownVisibility();

    // Toggle dropdown
    profileBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        profileBtn.classList.toggle('active');
        profileDropdown.classList.toggle('show');

        // Re-ensure visibility properties on toggle
        ensureDropdownVisibility();
    });

    // Close dropdown when clicking outside
    document.addEventListener('click', (e) => {
        if (!profileBtn.contains(e.target) && !profileDropdown.contains(e.target)) {
            profileBtn.classList.remove('active');
            profileDropdown.classList.remove('show');
        }
    });

    // Handle menu items
    logoutBtn.addEventListener('click', (e) => {
        e.preventDefault();
        logout();
    });

    editProfile.addEventListener('click', (e) => {
        e.preventDefault();
        profileDropdown.classList.remove('show');
        profileBtn.classList.remove('active');
        // Open the edit profile modal
        if (!localStorage.getItem('authToken')) {
            showToast('Please log in to edit your profile', 'error');
            return;
        }
        openEditProfileModal();
    });
}

function logout() {
    localStorage.removeItem('authToken');
    showToast('Logged out successfully', 'success');
    setTimeout(() => {
        window.location.href = 'login.html';
    }, 1000);
}

// Attach event listeners
function attachEventListeners() {
    generateBtn.addEventListener('click', generateQR);
    qrTextInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') generateQR();
    });
    qrTextInput.addEventListener('input', updateCharCount);
    uploadArea.addEventListener('click', () => qrFileInput.click());
    qrFileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            handleImageUpload(e.target.files[0]);
        }
    });
    uploadArea.addEventListener('dragover', handleDragOver);
    uploadArea.addEventListener('dragleave', handleDragLeave);
    uploadArea.addEventListener('drop', handleDrop);
    qrImageUploadArea.addEventListener('click', () => qrImageInput.click());
    qrImageInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            generateQRFromImage(e.target.files[0]);
        }
    });
    qrImageUploadArea.addEventListener('dragover', handleImageDragOver);
    qrImageUploadArea.addEventListener('dragleave', handleImageDragLeave);
    qrImageUploadArea.addEventListener('drop', handleImageDrop);
    // document upload handlers
    docUploadArea.addEventListener('click', () => docFileInput.click());
    docFileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            uploadDocument(e.target.files[0]);
        }
    });
    docUploadArea.addEventListener('dragover', (e) => { e.preventDefault(); docUploadArea.classList.add('dragover'); });
    docUploadArea.addEventListener('dragleave', (e) => { e.preventDefault(); docUploadArea.classList.remove('dragover'); });
    docUploadArea.addEventListener('drop', (e) => { e.preventDefault(); docUploadArea.classList.remove('dragover'); const files = e.dataTransfer.files; if (files.length > 0) uploadDocument(files[0]); });
    themeToggle.addEventListener('click', toggleTheme);
    clearHistoryBtn.addEventListener('click', clearAllHistory);
}

// Initialize the app
document.addEventListener('DOMContentLoaded', () => {
    checkAuthStatus().then(isAuthenticated => {
        const mainContent = document.getElementById('mainContent');
        const historySection = document.getElementById('historySection');
        const loginPromptSection = document.getElementById('loginPromptSection');
        const goToLoginBtn = document.getElementById('goToLoginBtn');
        if (!isAuthenticated) {
            if (mainContent) mainContent.style.display = 'none';
            if (historySection) historySection.style.display = 'none';
            if (loginPromptSection) loginPromptSection.style.display = 'block';
            if (goToLoginBtn) goToLoginBtn.onclick = () => window.location.href = 'login.html';
        } else {
            if (mainContent) mainContent.style.display = '';
            if (historySection) historySection.style.display = '';
            if (loginPromptSection) loginPromptSection.style.display = 'none';
            attachEventListeners();
            loadHistory();
            loadStats();
        }
        initializeTheme();
        setTimeout(ensureDropdownVisibility, 100);
    });
});

// Upload document and generate QR
async function uploadDocument(file) {
    const allowed = ['application/pdf', 'text/plain', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
    if (!allowed.includes(file.type)) {
        showToast('Unsupported document type', 'error');
        return;
    }
    if (file.size > 50 * 1024 * 1024) {
        showToast('File size exceeds 50MB limit.', 'error');
        return;
    }
    showLoading('Uploading document...');
    try {
        const formData = new FormData();
        formData.append('file', file);
        const response = await fetch(`${API_BASE_URL}/api/qr/upload-doc`, {
            method: 'POST',
            headers: getAuthHeadersForFormData(),
            body: formData
        });
        const data = await response.json();
        if (!response.ok || data.error) throw new Error(data.error || 'Failed to upload document');
        const historyItem = {
            id: data.id,
            text: data.text,
            image: data.image,
            type: data.type,
            timestamp: data.timestamp
        };
        displayQRCode(historyItem);
        await loadHistory();
        await loadStats();
        showToast('Document uploaded and QR generated successfully!', 'success');
    } catch (error) {
        showError(qrResult, 'Error: ' + error.message);
        showToast('Failed to upload document', 'error');
    } finally {
        hideLoading();
    }
}

/* Edit Profile Modal: fetch, populate, validation, and update */
// Elements
const editProfileModal = document.getElementById('editProfileModal');
const editUsername = document.getElementById('editUsername');
const editEmail = document.getElementById('editEmail');
const emailValidationFeedback = document.getElementById('emailValidationFeedback');
const newPassword = document.getElementById('newPassword');
const confirmNewPassword = document.getElementById('confirmNewPassword');
const saveProfileChanges = document.getElementById('saveProfileChanges');
const cancelEditProfile = document.getElementById('cancelEditProfile');
const closeEditProfileModal = document.getElementById('closeEditProfileModal');
// currentPassword removed by design
const toggleNewPassword = document.getElementById('toggleNewPassword');
const toggleConfirmPassword = document.getElementById('toggleConfirmPassword');

let profileToken = localStorage.getItem('authToken') || null;

// Open modal when Edit Profile clicked

async function openEditProfileModal() {
    const response = await fetch(`${API_BASE_URL}/api/auth/profile`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${profileToken}`
        }
    });
    const data = await response.json();
    if (!response.ok || !data.success) throw new Error(data.message || 'Failed to fetch profile');

    editUsername.value = data.username || '';
    editEmail.value = data.email || '';
    emailValidationFeedback.textContent = '';
    // currentPassword removed from modal
    newPassword.value = '';
    confirmNewPassword.value = '';

    editProfileModal.style.display = 'flex';
}

function closeEditModal() {
    editProfileModal.style.display = 'none';
}

closeEditProfileModal.addEventListener('click', closeEditModal);
cancelEditProfile.addEventListener('click', closeEditModal);

// Password toggle handlers (new and confirm fields)
toggleNewPassword?.addEventListener('click', () => togglePasswordField(newPassword, toggleNewPassword));
toggleConfirmPassword?.addEventListener('click', () => togglePasswordField(confirmNewPassword, toggleConfirmPassword));

function togglePasswordField(field, btn) {
    if (!field || !btn) return;
    const isPassword = field.getAttribute('type') === 'password';
    field.setAttribute('type', isPassword ? 'text' : 'password');
    const icon = btn.querySelector('i');
    if (!icon) return;
    if (isPassword) {
        icon.classList.remove('fa-eye');
        icon.classList.add('fa-eye-slash');
    } else {
        icon.classList.remove('fa-eye-slash');
        icon.classList.add('fa-eye');
    }
}

// Enhanced password validation and UX
function initializeProfileModalEnhancements() {
    const newPasswordField = document.getElementById('newPassword');
    const confirmPasswordField = document.getElementById('confirmNewPassword');
    const passwordRequirements = document.getElementById('passwordRequirements');
    const lengthReq = document.getElementById('lengthReq');
    const letterReq = document.getElementById('letterReq');
    const numberReq = document.getElementById('numberReq');

    if (!newPasswordField || !passwordRequirements) return;

    // Show/hide password requirements based on password field focus
    newPasswordField.addEventListener('focus', () => {
        passwordRequirements.style.display = 'block';
    });

    newPasswordField.addEventListener('blur', () => {
        if (!newPasswordField.value) {
            passwordRequirements.style.display = 'none';
        }
    });

    // Real-time password validation
    newPasswordField.addEventListener('input', () => {
        const password = newPasswordField.value;

        if (password.length === 0) {
            passwordRequirements.style.display = 'none';
            return;
        }

        passwordRequirements.style.display = 'block';

        // Length requirement
        if (lengthReq) {
            if (password.length >= 6) {
                lengthReq.classList.add('valid');
                lengthReq.querySelector('i').className = 'fas fa-check';
            } else {
                lengthReq.classList.remove('valid');
                lengthReq.querySelector('i').className = 'fas fa-times';
            }
        }

        // Letter requirement
        if (letterReq) {
            if (/[a-zA-Z]/.test(password)) {
                letterReq.classList.add('valid');
                letterReq.querySelector('i').className = 'fas fa-check';
            } else {
                letterReq.classList.remove('valid');
                letterReq.querySelector('i').className = 'fas fa-times';
            }
        }

        // Number requirement
        if (numberReq) {
            if (/[0-9]/.test(password)) {
                numberReq.classList.add('valid');
                numberReq.querySelector('i').className = 'fas fa-check';
            } else {
                numberReq.classList.remove('valid');
                numberReq.querySelector('i').className = 'fas fa-times';
            }
        }

        // Validate confirm password if it has a value
        if (confirmPasswordField && confirmPasswordField.value) {
            validatePasswordMatch();
        }
    });

    // Confirm password validation
    if (confirmPasswordField) {
        confirmPasswordField.addEventListener('input', validatePasswordMatch);
    }

    function validatePasswordMatch() {
        if (!newPasswordField || !confirmPasswordField) return;

        const password = newPasswordField.value;
        const confirmPassword = confirmPasswordField.value;

        if (confirmPassword && password !== confirmPassword) {
            confirmPasswordField.style.borderColor = '#ef4444';
            confirmPasswordField.style.boxShadow = '0 0 0 3px rgba(239, 68, 68, 0.1)';
        } else if (confirmPassword) {
            confirmPasswordField.style.borderColor = 'var(--primary-color)';
            confirmPasswordField.style.boxShadow = '0 0 0 3px rgba(5, 150, 105, 0.1)';
        } else {
            confirmPasswordField.style.borderColor = 'var(--border-color)';
            confirmPasswordField.style.boxShadow = 'none';
        }
    }
}

// Email validation enhancement
function initializeEmailValidation() {
    const emailField = document.getElementById('editEmail');
    const feedback = document.getElementById('emailValidationFeedback');

    if (!emailField || !feedback) return;

    let debounceTimer;

    emailField.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        const email = emailField.value.trim();

        if (!email) {
            feedback.style.display = 'none';
            emailField.style.borderColor = 'var(--border-color)';
            return;
        }

        debounceTimer = setTimeout(() => {
            validateEmailFormat(email);
        }, 500);
    });

    function validateEmailFormat(email) {
        const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

        if (emailRegex.test(email)) {
            feedback.textContent = '✓ Valid email format';
            feedback.className = 'email-validation-feedback success';
            emailField.style.borderColor = 'var(--primary-color)';
        } else {
            feedback.textContent = '✗ Please enter a valid email address';
            feedback.className = 'email-validation-feedback error';
            emailField.style.borderColor = '#ef4444';
        }
        feedback.style.display = 'block';
    }
}

// Initialize enhancements when modal opens
const originalOpenEditProfileModal = openEditProfileModal;
window.openEditProfileModal = async function () {
    await originalOpenEditProfileModal();
    setTimeout(() => {
        initializeProfileModalEnhancements();
        initializeEmailValidation();
    }, 100);
};

// Email format + availability check
let emailCheckTimeoutEdit;
editEmail?.addEventListener('input', () => {
    clearTimeout(emailCheckTimeoutEdit);
    emailValidationFeedback.textContent = '';
    emailCheckTimeoutEdit = setTimeout(async () => {
        const email = editEmail.value.trim();
        if (!email) return;
        const isValid = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email);
        if (!isValid) {
            emailValidationFeedback.textContent = 'Invalid email format';
            emailValidationFeedback.style.color = 'var(--text-secondary)';
            return;
        }
        try {
            const resp = await fetch('/api/auth/check-email', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email }) });
            const json = await resp.json();
            if (json.available) {
                emailValidationFeedback.textContent = 'Email available';
                emailValidationFeedback.style.color = '#059669';
            } else {
                emailValidationFeedback.textContent = json.message || 'Email already in use';
                emailValidationFeedback.style.color = '#ef4444';
            }
        } catch (e) {
            emailValidationFeedback.textContent = 'Error checking email';
            emailValidationFeedback.style.color = '#ef4444';
        }
    }, 500);
});

// Save profile changes
saveProfileChanges.addEventListener('click', async () => {
    const usernameVal = editUsername.value.trim();
    const emailVal = editEmail.value.trim();
    // currentPassword removed by design
    const newPass = newPassword.value;
    const confirmPass = confirmNewPassword.value;

    if (!usernameVal || !emailVal) {
        showToast('Username and email are required', 'error');
        return;
    }

    if (newPass && newPass.length < 6) {
        showToast('New password must be at least 6 characters', 'error');
        return;
    }

    if (newPass && newPass !== confirmPass) {
        showToast('New passwords do not match', 'error');
        return;
    }

    try {
        saveProfileChanges.disabled = true;
        saveProfileChanges.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';

        const resp = await fetch('/api/auth/profile/update', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${profileToken}`
            },
            body: JSON.stringify({ username: usernameVal, email: emailVal, newPassword: newPass })
        });
        const json = await resp.json();
        if (!resp.ok || !json.success) throw new Error(json.message || 'Failed to update profile');

        // Update local UI and token if email changed (optional: server may return a refreshed token in future)
        document.getElementById('profileName').textContent = json.username || usernameVal;
        document.getElementById('profileEmail').textContent = json.email || emailVal;

        showToast(json.message || 'Profile updated', 'success');
        setTimeout(() => closeEditModal(), 900);
    } catch (err) {
        showToast(err.message || 'Error updating profile', 'error');
    } finally {
        saveProfileChanges.disabled = false;
        saveProfileChanges.innerHTML = '<i class="fas fa-save"></i> Save Changes';
    }
});

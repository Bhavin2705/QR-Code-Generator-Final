document.addEventListener('DOMContentLoaded', () => {
    const loginBtn = document.getElementById('loginBtn');
    const adminUsername = document.getElementById('adminUsername');
    const adminPassword = document.getElementById('adminPassword');
    const passwordToggle = document.getElementById('passwordToggle');
    const toast = document.getElementById('toast');

    passwordToggle.addEventListener('click', () => {
        const type = adminPassword.getAttribute('type') === 'password' ? 'text' : 'password';
        adminPassword.setAttribute('type', type);
        passwordToggle.classList.toggle('fa-eye');
        passwordToggle.classList.toggle('fa-eye-slash');
    });

    function showToast(message, type = 'success') {
        toast.textContent = message;
        toast.style.background = type === 'success' ? '#10b981' : '#ef4444';
        toast.style.display = 'block';
        setTimeout(() => toast.style.display = 'none', 3000);
    }

    async function login() {
        const username = adminUsername.value.trim();
        const password = adminPassword.value.trim();
        if (!username || !password) {
            showToast('Please fill in all fields', 'error');
            return;
        }
        try {
            const response = await fetch('/api/auth/admin-login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });
            const data = await response.json();
            if (data.success && data.token) {
                localStorage.setItem('authToken', data.token);
                showToast('Login successful!');
                // If backend returns role, check it (future extensibility)
                setTimeout(() => { window.location.href = 'admin.html'; }, 1000);
            } else {
                showToast(data.message || 'Login failed', 'error');
            }
        } catch (error) {
            showToast('Network error. Please try again.', 'error');
        }
    }

    loginBtn.addEventListener('click', login);
    adminUsername.addEventListener('keypress', (e) => { if (e.key === 'Enter') login(); });
    adminPassword.addEventListener('keypress', (e) => { if (e.key === 'Enter') login(); });
});

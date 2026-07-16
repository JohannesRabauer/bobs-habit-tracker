// Auto-dismiss flash messages after 4 seconds
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.flash-message').forEach(function (el) {
        setTimeout(function () {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(el);
            bsAlert.close();
        }, 4000);
    });

    // Confirm dialog before habit deletion
    document.querySelectorAll('.confirm-delete').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!confirm('Delete this habit? This cannot be undone.')) {
                e.preventDefault();
            }
        });
    });
});

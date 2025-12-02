// Events Platform Main JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // Initialize tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Initialize popovers
    var popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    var popoverList = popoverTriggerList.map(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });

    // Form validation
    const forms = document.querySelectorAll('.needs-validation');
    Array.from(forms).forEach(form => {
        form.addEventListener('submit', event => {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        }, false);
    });

    // Event search functionality
    const searchForm = document.getElementById('eventSearchForm');
    if (searchForm) {
        searchForm.addEventListener('submit', function(e) {
            e.preventDefault();
            const searchTerm = document.getElementById('searchInput').value;
            // Implement search logic here or redirect to search page
            window.location.href = `/events?search=${encodeURIComponent(searchTerm)}`;
        });
    }

    // Load more events functionality
    let currentPage = 1;
    const loadMoreBtn = document.getElementById('loadMoreBtn');
    if (loadMoreBtn) {
        loadMoreBtn.addEventListener('click', function() {
            currentPage++;
            loadMoreEvents(currentPage);
        });
    }

    // Category filter
    const categoryFilter = document.getElementById('categoryFilter');
    if (categoryFilter) {
        categoryFilter.addEventListener('change', function() {
            const categoryId = this.value;
            filterEventsByCategory(categoryId);
        });
    }

    // Date filter
    const dateFilter = document.getElementById('dateFilter');
    if (dateFilter) {
        dateFilter.addEventListener('change', function() {
            const date = this.value;
            filterEventsByDate(date);
        });
    }
});

// AJAX functions
async function loadMoreEvents(page) {
    try {
        const response = await fetch(`/api/v1/public/event?page=${page}&size=6`);
        const events = await response.json();
        // Append events to the list
        displayEvents(events);
    } catch (error) {
        console.error('Error loading more events:', error);
    }
}

async function filterEventsByCategory(categoryId) {
    try {
        const url = categoryId ?
            `/api/v1/public/event?categoryId=${categoryId}` :
            '/api/v1/public/event';

        const response = await fetch(url);
        const events = await response.json();
        displayEvents(events);
    } catch (error) {
        console.error('Error filtering events:', error);
    }
}

async function filterEventsByDate(date) {
    try {
        const url = date ?
            `/api/v1/public/event?date=${date}` :
            '/api/v1/public/event';

        const response = await fetch(url);
        const events = await response.json();
        displayEvents(events);
    } catch (error) {
        console.error('Error filtering events by date:', error);
    }
}

function displayEvents(events) {
    const eventsContainer = document.getElementById('eventsContainer');
    if (events.length === 0) {
        eventsContainer.innerHTML = '<div class="col-12"><p class="text-center">Мероприятия не найдены</p></div>';
        return;
    }

    eventsContainer.innerHTML = events.map(event => `
        <div class="col-md-4 mb-4">
            <div class="card event-card h-100">
                <div class="card-body">
                    <h5 class="card-title">${event.name}</h5>
                    <p class="card-text">
                        <small class="text-muted">
                            <i class="fas fa-calendar me-1"></i>
                            ${new Date(event.startTime).toLocaleDateString('ru-RU')}
                        </small>
                    </p>
                    <p class="card-text">
                        <i class="fas fa-map-marker-alt me-1"></i>
                        ${event.location.city}, ${event.location.street}
                    </p>
                    <a href="/event/${event.id}" class="btn btn-primary">Подробнее</a>
                </div>
            </div>
        </div>
    `).join('');
}

// Utility functions
function showLoading(button) {
    const originalText = button.innerHTML;
    button.innerHTML = '<span class="loading"></span> Загрузка...';
    button.disabled = true;
    return originalText;
}

function hideLoading(button, originalText) {
    button.innerHTML = originalText;
    button.disabled = false;
}

function showToast(message, type = 'success') {
    // Implement toast notifications
    const toast = document.createElement('div');
    toast.className = `toast align-items-center text-white bg-${type} border-0`;
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${message}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;

    document.body.appendChild(toast);
    new bootstrap.Toast(toast).show();

    setTimeout(() => toast.remove(), 3000);
}
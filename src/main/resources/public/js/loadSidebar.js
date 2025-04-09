// En /public/js/loadSidebar.js
function loadSidebar() {
    fetch('/html/sidebar.html')
        .then(response => response.text())
        .then(html => {
            document.body.insertAdjacentHTML('afterbegin', html);
        });
}

function addMobileFunctionality() {
    // Crear botón de hamburguesa
    const toggleBtn = document.createElement('button');
    toggleBtn.id = 'sidebarToggle';
    toggleBtn.className = 'btn btn-orange position-fixed';
    toggleBtn.style = 'left: 10px; top: 10px; z-index: 999;';
    toggleBtn.innerHTML = '<i class="bi bi-list"></i>';

    document.body.prepend(toggleBtn);

    // Event listener para mostrar/ocultar
    toggleBtn.addEventListener('click', () => {
        document.querySelector('.sidebar').classList.toggle('active');
    });
}


// Ejecutar al cargar la página
document.addEventListener('DOMContentLoaded', loadSidebar);
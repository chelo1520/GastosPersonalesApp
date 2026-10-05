(function () {
    const root = document.getElementById('simulacion');
    if (!root) return;

    const lista = document.getElementById('sim-lista');
    const form = document.getElementById('sim-form');
    const presupuesto = parseFloat(root.dataset.presupuesto) || 0;

    const fmt = (n) => '$ ' + Math.round(n).toLocaleString('es-AR');

    const el = {
        total: document.getElementById('sim-total'),
        presupuesto: document.getElementById('sim-presupuesto'),
        diferencia: document.getElementById('sim-diferencia'),
        label: document.getElementById('sim-diferencia-label'),
        valor: document.getElementById('sim-diferencia-valor'),
        barra: document.getElementById('sim-barra'),
        fill: document.getElementById('sim-barra-fill'),
        sugerencia: document.getElementById('sim-sugerencia'),
    };

    function recalcular() {
        const items = lista ? Array.from(lista.querySelectorAll('.app-sim-item')) : [];
        let total = 0;
        let mayorExtra = null;

        items.forEach((li) => {
            const marcado = li.querySelector('.app-sim-check').checked;
            if (!marcado) return;
            const importe = parseFloat(li.dataset.importe) || 0;
            total += importe;
            if (li.dataset.extraordinario === 'true' && (!mayorExtra || importe > mayorExtra.importe)) {
                mayorExtra = { importe, descripcion: li.dataset.descripcion };
            }
        });

        const diff = total - presupuesto;
        // Sin presupuesto, cualquier gasto ya lo excede: barra llena
        const pct = presupuesto > 0 ? (total / presupuesto) * 100 : (total > 0 ? 100 : 0);

        el.total.textContent = fmt(total);
        el.presupuesto.textContent = fmt(presupuesto);
        el.diferencia.classList.toggle('is-ok', diff <= 0);
        // Acentos como escapes Unicode: el JS se sirve sin charset y el navegador los rompe
        el.label.textContent = diff > 0 ? 'Te pasar\u00edas por' : 'Te sobrar\u00edan';
        el.valor.textContent = fmt(Math.abs(diff));
        el.fill.style.width = Math.min(pct, 100) + '%';
        el.barra.setAttribute('aria-valuenow', Math.min(Math.round(pct), 100));

        if (mayorExtra && presupuesto > 0) {
            const sin = total - mayorExtra.importe;
            const pctSin = Math.round((sin / presupuesto) * 100);
            el.sugerencia.textContent =
                'Desmarcando \u00ab' + mayorExtra.descripcion + '\u00bb quedar\u00edas en ' + fmt(sin) + ' (' + pctSin + ' %).';
            el.sugerencia.hidden = false;
        } else {
            el.sugerencia.hidden = true;
        }
    }

    function agregarExtraordinario(descripcion, monto) {
        const li = document.createElement('li');
        li.className = 'app-sim-item';
        li.dataset.importe = monto;
        li.dataset.descripcion = descripcion;
        li.dataset.extraordinario = 'true';

        const label = document.createElement('label');
        label.className = 'app-sim-item-label';

        const check = document.createElement('input');
        check.type = 'checkbox';
        check.className = 'app-sim-check';
        check.checked = true;

        const desc = document.createElement('span');
        desc.className = 'app-sim-descripcion';
        const texto = document.createElement('span');
        texto.textContent = descripcion;
        const tag = document.createElement('span');
        tag.className = 'app-sim-tag';
        tag.textContent = 'Extraordinario';
        desc.append(texto, tag);

        label.append(check, desc);

        const cat = document.createElement('span');
        cat.className = 'app-sim-categoria';
        cat.textContent = 'Extraordinario';

        const imp = document.createElement('span');
        imp.className = 'app-sim-importe';
        imp.textContent = Math.round(monto).toLocaleString('es-AR');

        li.append(label, cat, imp);
        lista.appendChild(li);
    }

    if (lista) {
        lista.addEventListener('change', (e) => {
            if (e.target.classList.contains('app-sim-check')) recalcular();
        });
    }

    form.addEventListener('submit', (e) => {
        e.preventDefault();
        const descInput = document.getElementById('sim-extra-descripcion');
        const montoInput = document.getElementById('sim-extra-monto');
        const descripcion = descInput.value.trim();
        const monto = parseFloat(montoInput.value);
        if (!descripcion || !(monto > 0) || !lista) return;

        agregarExtraordinario(descripcion, monto);
        descInput.value = '';
        montoInput.value = '';
        descInput.focus();
        recalcular();
    });

    recalcular();
})();
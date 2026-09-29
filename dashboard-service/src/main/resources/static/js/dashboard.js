(function () {
    'use strict';

    var data = window.GZ || {};
    var charts = {};
    var PLATFORM_COLORS = {
        BIGBASKET: '#84c225',
        BLINKIT: '#f2c230',
        ZEPTO: '#6b2fa3',
        INSTAMART: '#fc8019'
    };
    var CATEGORY_COLORS = ['#e53e3e', '#dd8b1f', '#3a5bd9', '#1e9e5a', '#805ad5', '#718096'];

    function noChartLib(canvasId) {
        var el = document.getElementById(canvasId);
        if (el) {
            el.parentNode.innerHTML = '<p class="muted">Chart.js could not be loaded.</p>';
        }
    }

    function hasData(series) {
        return series && series.labels && series.labels.length > 0;
    }

    var builders = {
        overview: function () {
            if (!window.Chart) { return noChartLib('ratingChart'); }
            var s = data.ratingByFlavor;
            if (!hasData(s)) { return; }
            charts.rating = new Chart(document.getElementById('ratingChart'), {
                type: 'bar',
                data: {
                    labels: s.labels,
                    datasets: [{ label: 'Avg rating', data: s.values, backgroundColor: '#0f2c59', borderRadius: 4 }]
                },
                options: {
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: { y: { min: 0, max: 5, title: { display: true, text: 'Stars' } } }
                }
            });
        },
        reviews: function () {
            if (!window.Chart) { return noChartLib('complaintChart'); }
            var s = data.complaintCategories;
            if (!hasData(s)) { return; }
            charts.complaints = new Chart(document.getElementById('complaintChart'), {
                type: 'doughnut',
                data: {
                    labels: s.labels,
                    datasets: [{ data: s.values, backgroundColor: CATEGORY_COLORS }]
                },
                options: { maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
            });
        },
        channels: function () {
            if (!window.Chart) { return noChartLib('priceChart'); }
            var p = data.priceChart;
            if (!p || !p.labels || !p.labels.length) { return; }
            charts.price = new Chart(document.getElementById('priceChart'), {
                type: 'bar',
                data: {
                    labels: p.labels,
                    datasets: p.series.map(function (s) {
                        return {
                            label: s.label,
                            data: s.prices,
                            backgroundColor: PLATFORM_COLORS[s.platform] || '#999',
                            borderRadius: 3
                        };
                    })
                },
                options: {
                    maintainAspectRatio: false,
                    plugins: {
                        tooltip: {
                            callbacks: {
                                label: function (ctx) {
                                    return ctx.parsed.y == null
                                        ? ctx.dataset.label + ': not listed'
                                        : ctx.dataset.label + ': Rs ' + ctx.parsed.y;
                                }
                            }
                        }
                    },
                    scales: { y: { beginAtZero: false, title: { display: true, text: 'Price (INR)' } } }
                }
            });
        }
    };

    function activate(tab) {
        document.querySelectorAll('.tab').forEach(function (b) {
            b.classList.toggle('active', b.dataset.tab === tab);
        });
        document.querySelectorAll('.panel').forEach(function (p) {
            p.classList.toggle('active', p.id === tab);
        });
        // Charts are built lazily so they measure a visible canvas.
        if (!charts['built_' + tab] && builders[tab]) {
            builders[tab]();
            charts['built_' + tab] = true;
        }
        history.replaceState(null, '', '#' + tab);
    }

    document.querySelectorAll('.tab').forEach(function (btn) {
        btn.addEventListener('click', function () { activate(btn.dataset.tab); });
    });

    document.querySelectorAll('.flavor-row.expandable').forEach(function (row) {
        row.addEventListener('click', function () {
            var detail = document.getElementById(row.dataset.target);
            if (detail) {
                detail.hidden = !detail.hidden;
                row.classList.toggle('open', !detail.hidden);
            }
        });
    });

    var initial = (location.hash || '#overview').substring(1);
    activate(builders[initial] ? initial : 'overview');
})();

const API = "/api";

let editingCustomer = null;
let activeMenu = null;
let toastTimer = null;

const $ = id => document.getElementById(id);

async function request(url, options = {}) {
    const response = await fetch(API + url, {
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        },
        ...options
    });

    let data = {};
    try {
        data = await response.json();
    } catch (_) {}

    if (!response.ok) {
        throw new Error(data.message || "Something went wrong.");
    }

    return data;
}

function showToast(message) {
    const toast = $("toast");
    toast.textContent = message;
    toast.classList.add("show");

    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        toast.classList.remove("show");
    }, 2800);
}

function showPage(page) {
    document.querySelectorAll(".page").forEach(p => {
        p.classList.add("hidden");
    });

    document.querySelectorAll(".nav-item").forEach(btn => {
        btn.classList.toggle("active", btn.dataset.page === page);
    });

    if (page === "view") {
        $("viewPage").classList.remove("hidden");
        loadCustomers();
    }

    if (page === "search") {
        $("searchPage").classList.remove("hidden");
        $("searchInput").value = "";
        searchCustomers();
    }

    if (page === "add" || page === "edit") {
        $("formPage").classList.remove("hidden");

        if (page === "add") {
            editingCustomer = null;
            $("customerForm").reset();
            $("formTitle").textContent = "Add Customer";
            $("formDescription").textContent =
                "Enter customer details and address information.";
            $("saveButton").textContent = "Save Customer";
        }
    }

    if (page === "recycle") {
        $("recyclePage").classList.remove("hidden");
        loadRecycleBin();
    }

    activeMenu = null;
}

function customerRow(c, recycle = false) {
    const id = Number(c.address_id);
    const customerId = Number(c.customer_id);

    const defaultValue = Number(c.is_default) === 1
        ? '<span class="default-badge">Default</span>'
        : '<span class="not-default">—</span>';

    let actions = "";

    if (recycle) {
        actions = `
            <button class="primary-button"
                onclick="restoreCustomer(${id})">Restore</button>
            <button class="more-button"
                title="Delete permanently"
                onclick="permanentDelete(${id})">⋮</button>
        `;
    } else {
        actions = `
            <div class="action-cell">
                <button class="more-button"
                    onclick="toggleMenu(event, ${id})">⋮</button>

                <div class="action-menu ${activeMenu === id ? "" : "hidden"}"
                     id="menu-${id}">
                    <button onclick="editCustomer(${customerId}, ${id})">
                        ✎ Edit
                    </button>
                    <button class="danger"
                        onclick="deleteCustomer(${id})">
                        ♲ Delete
                    </button>
                </div>
            </div>
        `;
    }

    return `
        <tr>
            <td>${escapeHTML(c.name)}</td>
            <td>${escapeHTML(c.phone)}</td>
            <td>${escapeHTML(c.email || "—")}</td>
            <td>${escapeHTML(c.house || "—")}</td>
            <td>${escapeHTML(c.street || "—")}</td>
            <td>${escapeHTML(c.city || "—")}</td>
            <td>${escapeHTML(c.pin || "—")}</td>
            ${recycle ? "" : `<td>${defaultValue}</td>`}
            <td>${actions}</td>
        </tr>
    `;
}

function escapeHTML(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;"
    })[char]);
}

async function loadCustomers() {
    try {
        const customers = await request("/customers");
        $("customerTable").innerHTML =
            customers.map(c => customerRow(c)).join("");

        $("customerCount").textContent =
            `${customers.length} Customers`;

        $("emptyView").classList.toggle(
            "hidden", customers.length > 0
        );

        $("customerTable").closest(".table-wrap")
            .classList.toggle("hidden", customers.length === 0);

    } catch (error) {
        showToast(error.message);
    }
}

async function searchCustomers() {
    const query = $("searchInput").value.trim();

    try {
        const customers = await request(
            "/customers?q=" + encodeURIComponent(query)
        );

        $("searchTable").innerHTML =
            customers.map(c => customerRow(c)).join("");

        $("emptySearch").classList.toggle(
            "hidden", customers.length > 0
        );

        $("searchTable").closest(".table-wrap")
            .classList.toggle("hidden", customers.length === 0);

    } catch (error) {
        showToast(error.message);
    }
}

async function loadRecycleBin() {
    try {
        const customers = await request("/recycle-bin");

        $("recycleTable").innerHTML =
            customers.map(c => customerRow(c, true)).join("");

        $("emptyRecycle").classList.toggle(
            "hidden", customers.length > 0
        );

        $("recycleTable").closest(".table-wrap")
            .classList.toggle("hidden", customers.length === 0);

    } catch (error) {
        showToast(error.message);
    }
}

function toggleMenu(event, id) {
    event.stopPropagation();

    const oldMenu = activeMenu;
    activeMenu = oldMenu === id ? null : id;

    document.querySelectorAll(".action-menu").forEach(menu => {
        menu.classList.add("hidden");
    });

    if (activeMenu !== null) {
        const menu = $("menu-" + id);
        if (menu) menu.classList.remove("hidden");
    }
}

document.addEventListener("click", () => {
    activeMenu = null;
    document.querySelectorAll(".action-menu").forEach(menu => {
        menu.classList.add("hidden");
    });
});

async function saveCustomer(event) {
    event.preventDefault();

    const customer = {
        name: $("name").value.trim(),
        phone: $("phone").value.trim(),
        email: $("email").value.trim(),
        house: $("house").value.trim(),
        street: $("street").value.trim(),
        city: $("city").value.trim(),
        pin: $("pin").value.trim(),
        isDefault: $("isDefault").checked
    };

    if (!/^\d{10}$/.test(customer.phone)) {
        showToast("Phone number must contain 10 digits.");
        return;
    }

    if (!/^\d{6}$/.test(customer.pin)) {
        showToast("PIN code must contain 6 digits.");
        return;
    }

    try {
        if (editingCustomer) {
            await request(
                `/customers/${editingCustomer.customerId}/addresses/${editingCustomer.addressId}`,
                {
                    method: "PUT",
                    body: JSON.stringify(customer)
                }
            );
            showToast("Customer updated successfully!");
        } else {
            await request("/customers", {
                method: "POST",
                body: JSON.stringify(customer)
            });
            showToast("Customer added successfully!");
        }

        editingCustomer = null;
        showPage("view");

    } catch (error) {
        showToast(error.message);
    }
}

async function editCustomer(customerId, addressId) {
    try {
        const customers = await request("/customers");
        const customer = customers.find(
            c => Number(c.customer_id) === customerId &&
                 Number(c.address_id) === addressId
        );

        if (!customer) {
            showToast("Customer not found.");
            return;
        }

        editingCustomer = { customerId, addressId };

        $("name").value = customer.name || "";
        $("phone").value = customer.phone || "";
        $("email").value = customer.email || "";
        $("house").value = customer.house || "";
        $("street").value = customer.street || "";
        $("city").value = customer.city || "";
        $("pin").value = customer.pin || "";
        $("isDefault").checked = Number(customer.is_default) === 1;

        $("formTitle").textContent = "Edit Customer";
        $("formDescription").textContent =
            "Update customer details and address information.";
        $("saveButton").textContent = "Update Customer";

        showPage("edit");

    } catch (error) {
        showToast(error.message);
    }
}

async function deleteCustomer(id) {
    if (!confirm("Move this customer to Recycle Bin?")) return;

    try {
        await request(`/addresses/${id}`, { method: "DELETE" });
        showToast("Customer moved to Recycle Bin.");
        loadCustomers();
    } catch (error) {
        showToast(error.message);
    }
}

async function restoreCustomer(id) {
    try {
        await request(`/addresses/${id}/restore`, {
            method: "PATCH"
        });

        showToast("Customer restored successfully!");
        loadRecycleBin();

    } catch (error) {
        showToast(error.message);
    }
}

async function permanentDelete(id) {
    if (!confirm("Permanently delete this customer address? This cannot be undone.")) {
        return;
    }

    try {
        await request(`/addresses/${id}/permanent`, {
            method: "DELETE"
        });

        showToast("Permanently deleted.");
        loadRecycleBin();

    } catch (error) {
        showToast(error.message);
    }
}

function toggleTheme() {
    document.body.classList.toggle("light");

    const light = document.body.classList.contains("light");

    $("themeIcon").textContent = light ? "☾" : "☀";
    $("themeText").textContent = light ? "Dark Mode" : "Light Mode";

    localStorage.setItem("addressly-theme", light ? "light" : "dark");
}

(function initializeTheme() {
    const theme = localStorage.getItem("addressly-theme");

    if (theme === "light") {
        document.body.classList.add("light");
        $("themeIcon").textContent = "☾";
        $("themeText").textContent = "Dark Mode";
    }
})();

showPage("view");
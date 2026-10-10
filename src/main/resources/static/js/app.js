(function () {
    "use strict";

    const API = "/api/movies";
    const TITLE_RE = /^[\p{L}\p{N}][\p{L}\p{N} .,:;!?'&()\-]*$/u;
    const GENRE_RE = /^\p{L}[\p{L} &/\-]*$/u;
    const PRICE_RE = /^\d{1,5}(\.\d{1,2})?$/;
    const ID_RE = /^[1-9]\d{0,17}$/;

    /* ---------- Tabs ---------- */
    const tabs = Array.from(document.querySelectorAll(".tab"));
    const desktop = window.matchMedia("(min-width: 961px)");

    function activateTab(tab, focus) {
        tabs.forEach(t => {
            const selected = t === tab;
            t.classList.toggle("active", selected);
            t.setAttribute("aria-selected", String(selected));
            t.tabIndex = selected ? 0 : -1;
            const panel = document.getElementById(t.getAttribute("aria-controls"));
            panel.hidden = !selected;
            panel.classList.toggle("active", selected);
        });
        if (focus) tab.focus();
        if (desktop.matches) {
            document.getElementById(tab.getAttribute("aria-controls"))
                .querySelector("input").focus({ preventScroll: true });
        }
    }

    tabs.forEach((tab, i) => {
        tab.addEventListener("click", () => activateTab(tab, false));
        tab.addEventListener("keydown", e => {
            if (e.key === "ArrowRight" || e.key === "ArrowLeft") {
                e.preventDefault();
                const next = (i + (e.key === "ArrowRight" ? 1 : tabs.length - 1)) % tabs.length;
                activateTab(tabs[next], true);
            }
        });
    });

    /* ---------- Validation ---------- */
    function validateId(value) {
        if (value === "") return "Movie ID is required.";
        if (!ID_RE.test(value)) return "Movie ID must be a positive whole number.";
        return null;
    }

    function validateMovie(data) {
        const errors = {};
        if (!data.title) errors.title = "Title is required.";
        else if (data.title.length > 100) errors.title = "Title must be at most 100 characters.";
        else if (!TITLE_RE.test(data.title)) errors.title = "Title must start with a letter or digit and may only contain letters, digits, spaces and . , : ; ! ? ' & ( ) -";

        if (!data.genre) errors.genre = "Genre is required.";
        else if (data.genre.length > 50) errors.genre = "Genre must be at most 50 characters.";
        else if (!GENRE_RE.test(data.genre)) errors.genre = "Genre must start with a letter and may only contain letters, spaces, & / and -";

        if (data.ticketPrice === "") errors.ticketPrice = "Ticket price is required.";
        else if (!PRICE_RE.test(data.ticketPrice)) errors.ticketPrice = "Enter a valid price with at most 2 decimal places.";
        else {
            const p = Number(data.ticketPrice);
            if (p <= 0) errors.ticketPrice = "Ticket price must be greater than 0.";
            else if (p > 10000) errors.ticketPrice = "Ticket price must not exceed 10000.00.";
        }
        return errors;
    }

    function readMovie(form) {
        return {
            title: form.elements.title.value.trim(),
            genre: form.elements.genre.value.trim(),
            ticketPrice: form.elements.ticketPrice.value.trim()
        };
    }

    /* ---------- UI helpers ---------- */
    function clearErrors(form) {
        form.querySelectorAll(".error").forEach(el => (el.textContent = ""));
        form.querySelectorAll(".invalid").forEach(el => {
            el.classList.remove("invalid");
            el.removeAttribute("aria-invalid");
        });
        const alert = form.querySelector(".alert");
        alert.className = "alert";
        alert.textContent = "";
    }

    function showErrors(form, errors) {
        let first = null;
        Object.entries(errors || {}).forEach(([field, msg]) => {
            const slot = form.querySelector('[data-error-for="' + field + '"]');
            if (slot) slot.textContent = msg;
            const input = form.elements[field];
            if (input) {
                input.classList.add("invalid");
                input.setAttribute("aria-invalid", "true");
                if (!first) first = input;
            }
        });
        if (first) first.focus();
    }

    function showAlert(form, ok, message) {
        const alert = form.querySelector(".alert");
        alert.className = "alert show " + (ok ? "success" : "failure");
        alert.textContent = message;
    }

    async function send(form, method, url, body) {
        const button = form.querySelector("button[type=submit]");
        const label = button.textContent;
        button.disabled = true;
        button.textContent = "Please wait…";
        try {
            const options = { method, headers: { "Accept": "application/json" } };
            if (body) {
                options.headers["Content-Type"] = "application/json";
                options.body = JSON.stringify(body);
            }
            const res = await fetch(url, options);
            let payload = {};
            try { payload = await res.json(); } catch (_) { /* empty body */ }
            if (res.ok) {
                showAlert(form, true, payload.message || "Done.");
                return true;
            }
            showErrors(form, payload.errors);
            showAlert(form, false, payload.message || ("Request failed (HTTP " + res.status + ")."));
        } catch (err) {
            showAlert(form, false, "Could not reach the server. Is CineBook running?");
        } finally {
            button.disabled = false;
            button.textContent = label;
        }
        return false;
    }

    function toBody(data) {
        return { title: data.title, genre: data.genre, ticketPrice: Number(data.ticketPrice) };
    }

    /* ---------- Add ---------- */
    const addForm = document.getElementById("add-form");
    addForm.addEventListener("submit", async e => {
        e.preventDefault();
        clearErrors(addForm);
        const data = readMovie(addForm);
        const errors = validateMovie(data);
        if (Object.keys(errors).length) { showErrors(addForm, errors); return; }
        if (await send(addForm, "POST", API, toBody(data))) {
            ["title", "genre", "ticketPrice"].forEach(n => (addForm.elements[n].value = ""));
        }
    });

    /* ---------- Update ---------- */
    const updateForm = document.getElementById("update-form");
    updateForm.addEventListener("submit", async e => {
        e.preventDefault();
        clearErrors(updateForm);
        const id = updateForm.elements.movieId.value.trim();
        const data = readMovie(updateForm);
        const errors = validateMovie(data);
        const idError = validateId(id);
        if (idError) errors.movieId = idError;
        if (Object.keys(errors).length) {
            showErrors(updateForm, idError ? Object.assign({ movieId: idError }, errors) : errors);
            return;
        }
        await send(updateForm, "PUT", API + "/" + encodeURIComponent(id), toBody(data));
    });

    /* ---------- Delete ---------- */
    const deleteForm = document.getElementById("delete-form");
    deleteForm.addEventListener("submit", async e => {
        e.preventDefault();
        clearErrors(deleteForm);
        const id = deleteForm.elements.movieId.value.trim();
        const errors = {};
        const idError = validateId(id);
        if (idError) errors.movieId = idError;
        if (!deleteForm.elements.confirm.checked) errors.confirm = "Please confirm the deletion.";
        if (Object.keys(errors).length) { showErrors(deleteForm, errors); return; }
        if (await send(deleteForm, "DELETE", API + "/" + encodeURIComponent(id))) {
            deleteForm.reset();
        }
    });
})();

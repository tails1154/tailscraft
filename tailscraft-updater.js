(function() {
	"use strict";

	const currentCommit = "__TAILSCRAFT_COMMIT__";
	const manifestURL = "update.json";
	let notice = null;

	function showUpdate(latestCommit) {
		if (notice || latestCommit === currentCommit) return;
		notice = document.createElement("div");
		notice.style.position = "fixed";
		notice.style.top = "12px";
		notice.style.right = "12px";
		notice.style.zIndex = "2147483647";
		notice.style.padding = "12px 14px";
		notice.style.background = "#171717";
		notice.style.color = "#ffffff";
		notice.style.border = "1px solid #c89b3c";
		notice.style.borderRadius = "4px";
		notice.style.boxShadow = "0 3px 12px rgba(0,0,0,.5)";
		notice.style.font = "14px sans-serif";
		notice.innerHTML = "<div style='font-weight:bold'>TailsCraft update available</div>"
			+ "<div style='margin-top:4px'>Refresh to load the latest client.</div>";
		const button = document.createElement("button");
		button.textContent = "Update";
		button.style.marginTop = "8px";
		button.style.cursor = "pointer";
		button.addEventListener("click", function() {
			window.location.replace(window.location.pathname + "?tailscraftUpdate=" + Date.now());
		});
		notice.appendChild(button);
		document.body.appendChild(notice);
	}

	async function checkForUpdate() {
		try {
			const response = await fetch(manifestURL + "?t=" + Date.now(), {
				cache: "no-store",
				headers: { "Cache-Control": "no-cache" }
			});
			if (response.ok) {
				const manifest = await response.json();
				if (manifest && typeof manifest.commit === "string") {
					showUpdate(manifest.commit.trim());
				}
			}
		} catch (ignored) {
			// Update checks are optional and must never interfere with the game.
		}
		window.setTimeout(checkForUpdate, 1000);
	}

	window.TailsCraftUpdater = {
		currentCommit: currentCommit,
		checkForUpdate: checkForUpdate
	};
	window.setTimeout(checkForUpdate, 1000);
})();

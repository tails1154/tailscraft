(function() {
	"use strict";

	const currentCommit = "__TAILSCRAFT_COMMIT__";
	const manifestURL = "update.json";
	let notice = null;
	const dismissedKey = "tailscraft.update.dismissed";

	function showUpdate(latestCommit) {
		if (notice || latestCommit === currentCommit || localStorage.getItem(dismissedKey) === latestCommit) return;
		notice = document.createElement("div");
		notice.style.position = "fixed";
		notice.style.bottom = "18px";
		notice.style.right = "18px";
		notice.style.zIndex = "2147483647";
		notice.style.width = "min(330px, calc(100vw - 36px))";
		notice.style.padding = "10px";
		notice.style.background = "linear-gradient(#3b3b3b, #202020)";
		notice.style.color = "#ffffff";
		notice.style.border = "2px solid #111";
		notice.style.outline = "1px solid #777";
		notice.style.boxShadow = "4px 4px 0 rgba(0,0,0,.45)";
		notice.style.font = "14px Arial, sans-serif";
		notice.setAttribute("role", "status");
		const title = document.createElement("div");
		title.textContent = "TailsCraft update available";
		title.style.fontWeight = "bold";
		title.style.textShadow = "2px 2px #222";
		const message = document.createElement("div");
		message.textContent = "A newer client is ready. Reload when convenient.";
		message.style.marginTop = "7px";
		message.style.color = "#d8d8d8";
		notice.appendChild(title);
		notice.appendChild(message);
		const actions = document.createElement("div");
		actions.style.display = "flex";
		actions.style.gap = "8px";
		actions.style.marginTop = "10px";
		const button = document.createElement("button");
		button.textContent = "Update";
		const styleButton = function(button, primary) {
			button.style.flex = "1";
			button.style.minHeight = "28px";
			button.style.padding = "3px 10px";
			button.style.cursor = "pointer";
			button.style.color = "#fff";
			button.style.background = primary ? "linear-gradient(#6b6b6b, #3f3f3f)" : "linear-gradient(#4b4b4b, #292929)";
			button.style.border = "2px solid #111";
			button.style.outline = "1px solid #888";
			button.style.font = "bold 13px Arial, sans-serif";
		};
		styleButton(button, true);
		button.addEventListener("click", function() {
			window.location.replace(window.location.pathname + "?tailscraftUpdate=" + Date.now());
		});
		const close = document.createElement("button");
		close.textContent = "Later";
		styleButton(close, false);
		close.addEventListener("click", function() {
			localStorage.setItem(dismissedKey, latestCommit);
			notice.remove();
			notice = null;
		});
		actions.appendChild(button);
		actions.appendChild(close);
		notice.appendChild(actions);
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

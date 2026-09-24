/* TailsForge: the WASM-GC-safe JavaScript mod API. */

/**
 * Creates the public API before the WASM module starts. Mods communicate with
 * the client through events and explicit bridges instead of patching WASM.
 * @param {?Object} opts
 */
function initializeTailsForge(opts) {
	if(window["TailsForge"]) return window["TailsForge"];
	var listeners = Object.create(null);
	var mods = Object.create(null);
	var storagePrefix = ((opts && typeof opts.localStorageNamespace === "string") ? opts.localStorageNamespace : "_eaglercraft_1.12") + ".";
	function decodeStored(value) {
		try {
			var binary = atob(value);
			var bytes = new Uint8Array(binary.length);
			for(var i = 0; i < binary.length; ++i) bytes[i] = binary.charCodeAt(i);
			return new TextDecoder("utf-8").decode(bytes);
		} catch(ex) { return null; }
	}
	var api = {
		version: "0.1.0-wasm-gc",
		mods: mods,
		game: {
			loaded: false,
			wasmExports: null,
			getWasmExports: function() { return api.game.wasmExports; }
		},
		on: function(event, callback) {
			if(typeof event !== "string" || typeof callback !== "function") return function() {};
			(listeners[event] || (listeners[event] = [])).push(callback);
			return function() { api.off(event, callback); };
		},
		off: function(event, callback) {
			var list = listeners[event];
			if(!list) return;
			for(var i = list.length - 1; i >= 0; --i) if(list[i] === callback) list.splice(i, 1);
		},
		once: function(event, callback) {
			var remove = api.on(event, function(data) {
				remove();
				callback(data);
			});
			return remove;
		},
		emit: function(event, data) {
			var list = listeners[event];
			if(!list) return;
			list = list.slice();
			for(var i = 0; i < list.length; ++i) {
				try { list[i](data); } catch(ex) { console.error("TailsForge mod error in " + event, ex); }
			}
		},
		register: function(mod) {
			if(!mod || typeof mod !== "object" || typeof mod.id !== "string" || !mod.id) {
				throw new Error("TailsForge mods need an id");
			}
			if(mods[mod.id]) throw new Error("TailsForge mod already registered: " + mod.id);
			mods[mod.id] = { id: mod.id, name: mod.name || mod.id, version: mod.version || "0.0.0", author: mod.author || "unknown" };
			if(typeof mod.init === "function") mod.init(api);
			api.emit("mod:loaded", mods[mod.id]);
			return mods[mod.id];
		},
		load: async function(url) {
			if(typeof url !== "string" || !url) throw new Error("TailsForge.load requires a URL");
			var source = await fetch(url).then(function(response) {
				if(!response.ok) throw new Error("HTTP " + response.status + " while loading " + url);
				return response.text();
			});
			(new Function("TailsForge", source + "\n//# sourceURL=" + url))(api);
			api.emit("mod:executed", { url: url });
		},
		storage: {
			get: function(key, fallback) {
				try {
					var value = window.localStorage.getItem(storagePrefix + "tailsforge." + key);
					var decoded = value === null ? null : decodeStored(value);
					return decoded === null ? fallback : JSON.parse(decoded);
				} catch(ex) { return fallback; }
			},
			set: function(key, value) {
				try {
					var bytes = new TextEncoder().encode(JSON.stringify(value));
					var binary = "";
					for(var i = 0; i < bytes.length; ++i) binary += String.fromCharCode(bytes[i]);
					window.localStorage.setItem(storagePrefix + "tailsforge." + key, btoa(binary));
				} catch(ex) {}
			},
			remove: function(key) {
				try { window.localStorage.removeItem(storagePrefix + "tailsforge." + key); } catch(ex) {}
			}
		},
		_setWasm: function(exports) {
			api.game.loaded = true;
			api.game.wasmExports = exports;
			api.emit("wasm:loaded", { exports: exports });
		}
	};
	window["TailsForge"] = api;
	api.emit("runtime:ready", { version: api.version });
	try {
		var storedIndex = decodeStored(window.localStorage.getItem(storagePrefix + "tailsforge.mods"));
		var storedMods = storedIndex ? JSON.parse(storedIndex) : [];
		for(var s = 0; s < storedMods.length; ++s) {
			var storedSource = decodeStored(window.localStorage.getItem(storagePrefix + "tailsforge.mod." + storedMods[s].key));
			if(storedSource) (new Function("TailsForge", storedSource + "\n//# sourceURL=tailsforge://" + storedMods[s].name))(api);
		}
	} catch(ex) { console.error("TailsForge stored mod failed to load", ex); }
	var frame = function(timestamp) {
		api.emit("frame", { timestamp: timestamp });
		window.requestAnimationFrame(frame);
	};
	window.requestAnimationFrame(frame);
	var configured = (opts && Array.isArray(opts.mods)) ? opts.mods.slice() : [];
	if(Array.isArray(window["tailsForgeMods"])) configured = configured.concat(window["tailsForgeMods"]);
	for(var i = 0; i < configured.length; ++i) {
		api.load(configured[i]).catch(function(ex) { console.error("TailsForge mod failed to load", ex); });
	}
	return api;
}

/*
 * Copyright (c) 2024 lax1dude, ayunami2000. All Rights Reserved.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */

const EVENT_VOICE_ICE = 0;
const EVENT_VOICE_DESC = 1;
const EVENT_VOICE_OPEN = 2;
const EVENT_VOICE_CLOSE = 3;

const platfVoiceClientName = "platformVoiceClient";

function initializePlatfVoiceClient(voiceClientImports) {

	/**
	 * @return {boolean}
	 */
	voiceClientImports["isSupported"] = function() {
		return typeof navigator.mediaDevices !== "undefined" && typeof navigator.mediaDevices.getUserMedia !== "undefined" && "srcObject" in HTMLAudioElement.prototype;
	};

	/**
	 * @param {string} desc
	 * @suppress {globalThis}
	 */
	function setRemoteDescriptionImpl(desc) {
		try {
			const remoteDesc = JSON.parse(desc);
			this["_peerConnection"].setRemoteDescription(remoteDesc).then(() => {
				if (remoteDesc.hasOwnProperty("type") && "offer" === remoteDesc["type"]) {
					this["_peerConnection"].createAnswer().then((desc) => {
						this["_peerConnection"].setLocalDescription(desc).then(() => {
							pushEvent(EVENT_TYPE_VOICE, EVENT_VOICE_DESC, {
								"objId": this["objId"],
								"data": JSON.stringify(desc)
							});
						}).catch((err) => {
							eagError("Failed to set local description for \"{}\"! {}", this["objId"], err.message);
							pushEvent(EVENT_TYPE_VOICE, EVENT_VOICE_CLOSE, {
								"objId": this["objId"]
							});
						});
					}).catch((err) => {
						eagError("Failed to create answer for \"{}\"! {}", this["objId"], err.message);
						pushEvent(EVENT_TYPE_VOICE, EVENT_VOICE_CLOSE, {
							"objId": this["objId"]
						});
					});
				}
			}).catch((err) => {
				eagError("Failed to set remote description for \"{}\"! {}", this["objId"], err.message);
				pushEvent(EVENT_TYPE_VOICE, EVENT_VOICE_CLOSE, {
					"objId": this["objId"]
				});
			});
		} catch (e) {
			eagError(e.message);
			pushEvent(EVENT_TYPE_VOICE, EVENT_VOICE_CLOSE, {
				"objId": this["objId"]
			});
		}
	}

	/**
	 * @param {string} ice
	 * @suppress {globalThis}
	 */
	function addRemoteICECandidateImpl(ice) {
		try {
			this["_peerConnection"].addIceCandidate(new RTCIceCandidate(/** @type {!RTCIceCandidateInit} */ (JSON.parse(ice)))).catch((err) => {
				eagError("Failed to parse ice candidate for \"{}\"! {}", this["objId"], err.message);
				pushEvent(EVENT_TYPE_VOICE, EVENT_VOICE_CLOSE, {
					"objId": this["objId"]
				});
			});
		} catch (e) {
			eagError(e.message);
			pushEvent(EVENT_TYPE_VOICE, EVENT_VOICE_CLOSE, {
				"objId": this["objId"]
			});
		}
	}

	/**
	 * @suppress {globalThis}
	 */
	function closeImpl() {
		this["_peerConnection"].close();
	}

	let idCounter = 0;

	/**
	 * @param {string} iceServers
	 * @param {number} offer
	 * @param {!MediaStream} localStream
	 * @return {Object}
	 */
	voiceClientImports["createRTCPeerConnection"] = function(iceServers, offer, localStream) {
		try {
			const peerId = idCounter++;
			var ret;
			const peerConnection = new RTCPeerConnection(/** @type {!RTCConfiguration} */ ({
				"iceServers": JSON.parse(iceServers),
				"optional": [
					{
						"DtlsSrtpKeyAgreement": true
					}
				]
			}));
			
			peerConnection.addEventListener("icecandidate", /** @type {function(Event)} */ ((/** RTCPeerConnectionIceEvent */ evt) => {
				if (evt.candidate) {
					pushEvent(EVENT_TYPE_VOICE, EVENT_VOICE_ICE, {
						"objId": peerId,
						"data": JSON.stringify({
							"sdpMLineIndex": "" + evt.candidate.sdpMLineIndex,
							"candidate": evt.candidate.candidate
						})
					});
				}
			}));
			peerConnection.addEventListener("track", /** @type {function(Event)} */ ((/** RTCTrackEvent */ evt) => {
				const rawStream = evt.streams[0];
				ret["_aud"] = document.createElement("audio");
				ret["_aud"]["autoplay"] = true;
				ret["_aud"]["muted"] = true;
				ret["_aud"]["srcObject"] = rawStream;
				pushEvent(EVENT_TYPE_VOICE, EVENT_VOICE_OPEN, {
					"objId": peerId,
					"stream": rawStream
				});
			}));

			localStream.getTracks().forEach(function(track) {
				peerConnection.addTrack(track, localStream);
			});
			if (offer) {
				peerConnection.createOffer().then((desc) => {
					peerConnection.setLocalDescription(desc).then(() => {
						pushEvent(EVENT_TYPE_VOICE, EVENT_VOICE_DESC, {
							"objId": peerId,
							"data": JSON.stringify(desc)
						});
					}).catch((err) => {
						eagError("Failed to set local description for \"{}\"! {}", peerId, err.message);
						pushEvent(EVENT_TYPE_VOICE, EVENT_VOICE_CLOSE, {
							"objId": peerId
						});
					});
				}).catch((err) => {
					eagError("Failed to set create offer for \"{}\"! {}", peerId, err.message);
					pushEvent(EVENT_TYPE_VOICE, EVENT_VOICE_CLOSE, {
						"objId": peerId
					});
				});
			}

			peerConnection.addEventListener("connectionstatechange", () => {
				const cs = peerConnection.connectionState;
				if ("disconnected" === cs || "failed" === cs) {
					pushEvent(EVENT_TYPE_VOICE, EVENT_VOICE_CLOSE, {
						"objId": peerId
					});
				}
			});

			return ret = {
				"objId": peerId,
				"_peerConnection": peerConnection,
				"setRemoteDescription": setRemoteDescriptionImpl,
				"addRemoteICECandidate": addRemoteICECandidateImpl,
				"closeHandle": closeImpl
			};		
		} catch (e) {
			eagError(e.message);
			return null;
		}
	};

	/* TailsCraft TCV1 bridge. This is deliberately separate from Eagler voice
	 * signaling so the server plugin can support an original, opt-in protocol. */
	if (!window["TailsCraftVoice"]) {
		const tcVoice = {
			_stream: null,
			_peers: {},
			_pending: [],
			_events: [],
			_deafened: false,
			_localPosition: null,
			_recorder: null,
			_relayPeers: {},
			"push": function(event) { this._events.push(event); },
			"poll": function() {
				const ret = JSON.stringify(this._events);
				this._events.length = 0;
				return ret;
			},
			"start": function() {
				if (this._stream) return;
				if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
					this.push({type:"error", data:"Microphone API is unavailable"});
					return;
				}
				navigator.mediaDevices.getUserMedia({audio:true, video:false}).then((stream) => {
					this._stream = stream;
					this._stream.getAudioTracks().forEach((track) => track.enabled = true);
					this.push({type:"ready"});
					const pending = this._pending.splice(0);
					pending.forEach((peer) => this._makePeer(peer.id, peer.offer));
				}).catch((err) => this.push({type:"error", data: String(err && err.message ? err.message : err)}));
			},
			"stop": function() {
				Object.keys(this._peers).forEach((id) => {
					const peer = this._peers[id];
					if (peer && peer._audio && typeof peer._audio.remove === "function") peer._audio.remove();
					if (peer && peer._pc && typeof peer._pc.close === "function") peer._pc.close();
				});
				this._peers = {};
				if (typeof this.stopRelay === "function") this.stopRelay();
				Object.keys(this._relayPeers).forEach((id) => {
					const relay = this._relayPeers[id];
					if (relay && relay.audio && typeof relay.audio.remove === "function") relay.audio.remove();
				});
				this._relayPeers = {};
				this._pending = [];
				this._events = [];
				if (this._stream) this._stream.getTracks().forEach((track) => track.stop());
				this._stream = null;
				this._localPosition = null;
			},
			"setMuted": function(muted) {
				if (this._stream) this._stream.getAudioTracks().forEach((track) => track.enabled = !muted);
			},
			"setDeafened": function(deafened) {
				this._deafened = !!deafened;
				Object.keys(this._peers).forEach((id) => {
					this._updateVolume(this._peers[id]);
				});
				Object.keys(this._relayPeers).forEach((id) => this._updateRelayVolume(this._relayPeers[id]));
			},
			"addPeer": function(id, offer) {
				if (this._peers[id]) return;
				if (!this._stream) {
					if (!this._pending.some((peer) => peer.id === id)) this._pending.push({id:id, offer:!!offer});
					return;
				}
				this._makePeer(id, !!offer);
			},
			"removePeer": function(id) {
				const peer = this._peers[id];
				if (peer) {
					if (peer._audio && typeof peer._audio.remove === "function") peer._audio.remove();
					if (peer._pc && typeof peer._pc.close === "function") peer._pc.close();
					delete this._peers[id];
				}
				const relay = this._relayPeers[id];
				if (relay && relay.audio) relay.audio.remove();
				delete this._relayPeers[id];
			},
			"setPeerMuted": function(id, muted) {
				const peer = this._peers[id];
				if (peer) { peer._muted = !!muted; this._updateVolume(peer); }
				const relay = this._relayPeers[id];
				if (relay) { relay._muted = !!muted; this._updateRelayVolume(relay); }
			},
			"setPeerPosition": function(id, x, y, z) {
				const peer = this._peers[id];
				if (peer) { peer._position = {x:x, y:y, z:z}; this._updateVolume(peer); }
				const relay = this._relayPeers[id];
				if (relay) { relay._position = {x:x, y:y, z:z}; this._updateRelayVolume(relay); }
			},
			"setLocalPosition": function(x, y, z) {
				this._localPosition = {x:x, y:y, z:z};
				Object.keys(this._peers).forEach((id) => this._updateVolume(this._peers[id]));
				Object.keys(this._relayPeers).forEach((id) => this._updateRelayVolume(this._relayPeers[id]));
			},
			"resumeAudio": function() {
				Object.keys(this._peers).forEach((id) => {
					const audio = this._peers[id]._audio;
					if (audio) {
						audio.muted = this._deafened || !!this._peers[id]._muted;
						audio.play().catch(() => this.push({type:"audioBlocked", id:id, data:"Remote audio is blocked; click the voice menu once"}));
					}
				});
				Object.keys(this._relayPeers).forEach((id) => {
					const audio = this._relayPeers[id].audio;
					if (audio) { audio.muted = this._deafened; audio.play().catch(() => {}); }
				});
			},
			"startRelay": function() {
				if (this._recorder || !this._stream) return;
				if (typeof MediaRecorder === "undefined") {
					this.push({type:"error", data:"Browser audio relay is unavailable"});
					return;
				}
				try {
					let options = {};
					if (MediaRecorder.isTypeSupported("audio/webm;codecs=opus")) options = {mimeType:"audio/webm;codecs=opus"};
					const recorder = new MediaRecorder(this._stream, options);
					recorder.ondataavailable = (evt) => {
						if (!evt.data || !evt.data.size) return;
						evt.data.arrayBuffer().then((buffer) => {
							const bytes = new Uint8Array(buffer);
							let binary = "";
							for (let i = 0; i < bytes.length; i += 8192) binary += String.fromCharCode.apply(null, bytes.subarray(i, Math.min(i + 8192, bytes.length)));
							this.push({type:"relayAudio", data:btoa(binary)});
						}).catch(() => {});
					};
					recorder.onerror = () => this.push({type:"error", data:"Browser audio relay failed"});
					recorder.start(120);
					this._recorder = recorder;
				} catch (err) { this.push({type:"error", data:"Browser audio relay failed: " + String(err)}); }
			},
			"stopRelay": function() {
				if (this._recorder) {
					try { this._recorder.stop(); } catch (err) {}
					this._recorder = null;
				}
			},
			"receiveRelayAudio": function(id, base64) {
				try {
					let relay = this._relayPeers[id];
					if (!relay) {
						const audio = document.createElement("audio");
						audio.autoplay = true; audio.playsInline = true; audio.muted = false;
						const mediaSource = new MediaSource();
						audio.src = URL.createObjectURL(mediaSource);
						relay = {audio:audio, source:mediaSource, buffer:null, queue:[], _position:null, _muted:false};
						this._relayPeers[id] = relay;
						mediaSource.addEventListener("sourceopen", () => {
							try {
								relay.buffer = mediaSource.addSourceBuffer("audio/webm; codecs=opus");
								relay.buffer.mode = "sequence";
								this._drainRelay(relay);
							} catch (err) { this.push({type:"error", id:id, data:"Relay audio codec unavailable"}); }
						});
					}
					this._updateRelayVolume(relay);
					const raw = atob(base64);
					const bytes = new Uint8Array(raw.length);
					for (let i = 0; i < raw.length; ++i) bytes[i] = raw.charCodeAt(i);
					relay.queue.push(bytes);
					this._drainRelay(relay);
					relay.audio.play().catch(() => this.push({type:"audioBlocked", id:id, data:"Relay audio is blocked; click the voice menu once"}));
				} catch (err) { this.push({type:"error", id:id, data:"Invalid relay audio"}); }
			},
			"_drainRelay": function(relay) {
				if (!relay || !relay.buffer || relay.buffer.updating || !relay.queue.length) return;
				relay.buffer.appendBuffer(relay.queue.shift());
				relay.buffer.addEventListener("updateend", () => this._drainRelay(relay), {once:true});
			},
			"_updateRelayVolume": function(relay) {
				if (!relay || !relay.audio) return;
				let volume = 1.0;
				if (this._localPosition && relay._position) {
					const dx = relay._position.x - this._localPosition.x;
					const dy = relay._position.y - this._localPosition.y;
					const dz = relay._position.z - this._localPosition.z;
					volume = Math.max(0.0, Math.min(1.0, 1.0 - Math.sqrt(dx * dx + dy * dy + dz * dz) / 128.0));
				}
				relay.audio.volume = volume;
				relay.audio.muted = this._deafened || !!relay._muted;
			},
			"_updateVolume": function(peer) {
				if (!peer || !peer._audio) return;
				let volume = 1.0;
				if (this._localPosition && peer._position) {
					const dx = peer._position.x - this._localPosition.x;
					const dy = peer._position.y - this._localPosition.y;
					const dz = peer._position.z - this._localPosition.z;
					const distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
					volume = Math.max(0.0, Math.min(1.0, 1.0 - distance / 128.0));
				}
				peer._audio.volume = volume;
				peer._audio.muted = this._deafened || !!peer._muted;
			},
			_makePeer: function(id, offer) {
				if (this._peers[id] || !this._stream) return;
				const pc = new RTCPeerConnection({iceServers: [{urls:"stun:stun.l.google.com:19302"}]});
				const peer = {_pc: pc, _audio: null, _remoteDescriptionSet: false, _icePending: [], _position: null, _muted: false};
				this._peers[id] = peer;
				pc.onicecandidate = (evt) => { if (evt.candidate) this.push({type:"ice", id:id, data:JSON.stringify(evt.candidate)}); };
				pc.ontrack = (evt) => {
					const audio = document.createElement("audio");
					audio.autoplay = true;
					audio.playsInline = true;
					audio.muted = false;
					// Some Chromium builds omit RTCTrackEvent.streams even when the
					// track is valid. Build a stream from the track in that case.
					audio.srcObject = evt.streams && evt.streams[0] ? evt.streams[0] : new MediaStream([evt.track]);
					audio.play().catch(() => this.push({type:"audioBlocked", id:id, data:"Click Enable voice chat or press V to allow remote audio"}));
					peer._audio = audio;
					this._updateVolume(peer);
					this.push({type:"open", id:id});
				};
				pc.onconnectionstatechange = () => {
					this.push({type:"state", id:id, data:pc.connectionState});
					if (pc.connectionState === "failed" || pc.connectionState === "closed" || pc.connectionState === "disconnected") this.push({type:"close", id:id});
				};
				pc.onicecandidateerror = (evt) => this.push({type:"error", id:id, data:"ICE: " + (evt.errorText || "candidate failed")});
				this._stream.getTracks().forEach((track) => pc.addTrack(track, this._stream));
				if (offer) pc.createOffer().then((desc) => pc.setLocalDescription(desc)).then(() => this.push({type:"offer", id:id, data:JSON.stringify(pc.localDescription)})).catch((err) => this.push({type:"error", id:id, data:String(err)}));
			},
			"signal": function(id, signalType, data) {
				const peer = this._peers[id];
				if (!peer) return;
				try {
					const parsed = JSON.parse(data);
					if (signalType === 3) {
						if (!peer._remoteDescriptionSet) peer._icePending.push(parsed);
						else peer._pc.addIceCandidate(parsed).catch((err) => this.push({type:"error", id:id, data:String(err)}));
					} else peer._pc.setRemoteDescription(parsed).then(() => {
						peer._remoteDescriptionSet = true;
						const queued = peer._icePending.splice(0);
						return Promise.all(queued.map((candidate) => peer._pc.addIceCandidate(candidate)));
					}).then(() => {
						if (signalType === 1) return peer._pc.createAnswer().then((answer) => peer._pc.setLocalDescription(answer));
					}).then(() => {
						if (signalType === 1) this.push({type:"answer", id:id, data:JSON.stringify(peer._pc.localDescription)});
					}).catch((err) => this.push({type:"error", id:id, data:String(err)}));
				} catch (err) { this.push({type:"error", id:id, data:String(err)}); }
			}
		};
		window["TailsCraftVoice"] = tcVoice;
	}

}

function initializeNoPlatfVoiceClient(voiceClientImports) {
	setUnsupportedFunc(voiceClientImports, platfVoiceClientName, "isSupported");
	setUnsupportedFunc(voiceClientImports, platfVoiceClientName, "createRTCPeerConnection");
}

import asyncio, gzip, hashlib, importlib.util, json, os, pathlib, shutil, ssl, struct, tempfile, uuid, zlib
import websockets

LIVE = os.environ.get("TC5_TEST_LIVE") == "1"
SERVER = "wss://tails1154.com:9842" if LIVE else "ws://127.0.0.1:19842"

def byte_tag(name, value):
    return b"\x01" + bytes([len(name)]) + name.encode() + bytes([value & 255])
def int_tag(name, value):
    return b"\x03" + struct.pack(">H", len(name)) + name.encode() + struct.pack(">i", value)
def long_tag(name, value):
    return b"\x04" + struct.pack(">H", len(name)) + name.encode() + struct.pack(">q", value)
def string_tag(name, value):
    raw = value.encode()
    return b"\x08" + struct.pack(">H", len(name)) + name.encode() + struct.pack(">H", len(raw)) + raw
def compound_tag(name, body):
    return b"\x0a" + struct.pack(">H", len(name)) + name.encode() + body + b"\x00"

def make_epk():
    nbt = (string_tag("LevelName", "TC5 protocol fixture") + long_tag("RandomSeed", 12345)
        + string_tag("generatorName", "default") + int_tag("generatorVersion", 1)
        + byte_tag("MapFeatures", 1) + int_tag("GameType", 0) + byte_tag("hardcore", 0)
        + byte_tag("Difficulty", 2) + byte_tag("DifficultyLocked", 0) + byte_tag("allowCommands", 1)
        + long_tag("Time", 0) + long_tag("DayTime", 0) + int_tag("SpawnX", 0)
        + int_tag("SpawnY", 64) + int_tag("SpawnZ", 0) + byte_tag("raining", 0)
        + int_tag("rainTime", 0) + byte_tag("thundering", 0) + int_tag("thunderTime", 0)
        + int_tag("clearWeatherTime", 0)
        + compound_tag("Version", string_tag("Name", "1.12.2") + int_tag("Id", 1343) + byte_tag("Snapshot", 0)))
    level = gzip.compress(b"\x0a\x00\x00" + nbt)
    def head(name, value):
        raw = value.encode()
        return b"HEAD" + bytes([len(name)]) + name.encode() + struct.pack(">i", len(raw)) + raw + b">"
    def file(name, value):
        raw = name.encode()
        crc = zlib.crc32(value) & 0xffffffff
        if crc >= 0x80000000: crc -= 0x100000000
        return b"FILE" + bytes([len(raw)]) + raw + struct.pack(">i", len(value) + 5) + struct.pack(">i", crc) + value + b":>"
    body = head("file-type", "epk/world188") + head("world-name", "TC5 protocol fixture") + file("level.dat", level) + b"END$"
    filename, comment = b"tc5-fixture.epk", b"TC5 protocol fixture"
    header = (b"EAGPKG$$" + bytes([len(b"ver2.0")]) + b"ver2.0" + bytes([len(filename)]) + filename
        + struct.pack(">H", len(comment)) + comment + struct.pack(">q", 0) + struct.pack(">i", 4) + b"0")
    return header + body + b":::YEE:>"

async def main():
    temp_root = None
    local_server = None
    if not LIVE:
        server_module_path = pathlib.Path("/home/tails1154/tailsconnect/server.py")
        spec = importlib.util.spec_from_file_location("tc5_test_server", server_module_path)
        server_module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(server_module)
        temp_root = pathlib.Path(tempfile.mkdtemp(prefix="tc5-test-"))
        server_module.TC5_ROOT = temp_root
        server_module.TC5_INDEX = temp_root / "index.json"
        server_module.tc5_worlds.clear()
        server_module.tc5_uploads.clear()
        server_module.tc5_hosts.clear()
        local_server = await websockets.serve(server_module.handle_client, "127.0.0.1", 19842)
    epk = make_epk()
    digest = hashlib.sha256(epk).hexdigest()
    ssl_context = ssl._create_unverified_context()
    try:
      connect_args = {"max_size": 8 * 1024 * 1024}
      if LIVE: connect_args["ssl"] = ssl_context
      async with websockets.connect(SERVER, **connect_args) as ws:
        async def recv(label):
            value = await asyncio.wait_for(ws.recv(), 30)
            print(label, value if isinstance(value, str) else "binary " + str(len(value)))
            return value
        await ws.send("TC3 HELLO " + json.dumps({"version": 3, "features": ["rooms", "relay"], "friendCode": "TEST0001"}))
        await recv("WELCOME")
        await ws.send("TC3 HOST " + json.dumps({"game": "tailscraft-1.12.2-packets-v2", "maxPlayers": 2,
            "advertisement": {"public": True, "name": "TC5 Protocol Fixture"}}))
        await recv("ROOM")
        await ws.send("TC5 HOST_WORLD " + json.dumps({"game": "tailscraft-1.12.2-packets-v2",
            "name": "TC5 Protocol Fixture", "size": len(epk), "sha256": digest, "maxPlayers": 2,
            "advertisement": {"public": True, "name": "TC5 Protocol Fixture"}}))
        await recv("UPLOAD_READY")
        await ws.send(b"TC5W" + struct.pack(">II", 0, 1) + epk)
        await recv("STORED")
        hosted = await recv("HOSTED")
        if not isinstance(hosted, str) or not hosted.startswith("TC5 HOSTED "):
            raise RuntimeError("TC5 daemon startup failed: " + str(hosted))
        print("RESULT TC5W upload, checksum, daemon startup, and hosted-room creation passed")
        await ws.send("TC3 LEAVE")
    finally:
      if local_server is not None:
        local_server.close()
        await local_server.wait_closed()
      if temp_root is not None:
        shutil.rmtree(temp_root, ignore_errors=True)

asyncio.run(main())

"""Compare the actual packaged resources with this checkout; reject simulator binaries."""
import hashlib
import plistlib
import struct
import sys
import zipfile
from pathlib import Path

resources = Path(__file__).resolve().parents[1] / "Millionaire/Resources"
with zipfile.ZipFile(sys.argv[1]) as archive:
    root = "Payload/Millionaire.app/"
    assert archive.testzip() is None
    assert not any("/_CodeSignature/" in n or n.endswith("embedded.mobileprovision") for n in archive.namelist())
    info = plistlib.loads(archive.read(root + "Info.plist"))
    assert info["CFBundleSupportedPlatforms"] == ["iPhoneOS"]
    assert info["UIDeviceFamily"] == [1]
    assert info["MinimumOSVersion"] == "17.0"
    executable = archive.read(root + info["CFBundleExecutable"])
    magic, cpu, _, file_type, commands, _, _, _ = struct.unpack_from("<8I", executable)
    assert (magic, cpu, file_type) == (0xFEEDFACF, 0x0100000C, 2)
    offset = 32
    platforms = []
    for _ in range(commands):
        cmd, size = struct.unpack_from("<II", executable, offset)
        assert size >= 8 and offset + size <= len(executable)
        assert cmd != 0x1D, "Code signature load command found"
        if cmd == 0x32:
            platforms.append(struct.unpack_from("<I", executable, offset + 8)[0])
        offset += size
    assert platforms == [2], f"Unexpected Mach-O platform {platforms}"
    for src in resources.rglob("*"):
        if src.is_file():
            assert archive.read(root + src.relative_to(resources).as_posix()) == src.read_bytes(), src
    assert root + "Assets.car" in archive.namelist()
    print("ZIP CRC, iPhoneOS arm64 Mach-O, minimum iOS 17, unsigned state, audio, questions, notices and compiled assets verified.")
    print("Executable SHA-256:", hashlib.sha256(executable).hexdigest())

"""Reassemble the recovered application Smali and sign an APK using its original assets."""
import argparse
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
from tempfile import TemporaryDirectory
from zipfile import ZipFile

ORIGINAL_SHA256 = "5747b7c062123a005d8c649a5d7941ecb9eb75f095b6178bf64476d4c78516c6"
DEX = re.compile(r"classes(?:[2-9]|[1-9]\d+)?\.dex")
V1_SIGNATURE = re.compile(r"META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))", re.I)
ROOT = Path(__file__).resolve().parents[1]


def run(*args):
    subprocess.run(args, check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("original", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--build-tools", type=Path, required=True)
    parser.add_argument("--keystore", type=Path, required=True)
    parser.add_argument("--key-alias", required=True)
    args = parser.parse_args()
    java = Path(os.environ["JAVA_HOME"]) / "bin/java"
    jar = Path(os.environ["REA_JADX_MCP_JAR"]).resolve()
    keystore = args.keystore.resolve()
    output = args.output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)

    with args.original.open("rb") as source:
        if hashlib.file_digest(source, "sha256").hexdigest() != ORIGINAL_SHA256:
            parser.error("The original APK does not match the expected SHA-256.")

    with TemporaryDirectory(prefix="xunji-build-", dir=output.parent) as tmp:
        work = Path(tmp)
        original_dex = work / "original"
        rebuilt_dex = work / "rebuilt"
        original_dex.mkdir()
        rebuilt_dex.mkdir()
        unsigned = work / "unsigned.apk"
        aligned = work / "aligned.apk"
        signed = work / "signed.apk"

        with ZipFile(args.original) as source:
            for entry in source.infolist():
                if DEX.fullmatch(entry.filename):
                    (original_dex / entry.filename).write_bytes(source.read(entry))
            run(java, "-Xmx3g", "--class-path", jar, ROOT / "tools/AssembleApp.java",
                ROOT / "smali", original_dex, rebuilt_dex)

            with ZipFile(unsigned, "w") as apk:
                for entry in source.infolist():
                    if V1_SIGNATURE.fullmatch(entry.filename):
                        continue
                    replacement = rebuilt_dex / entry.filename if DEX.fullmatch(entry.filename) else None
                    data = replacement.read_bytes() if replacement and replacement.exists() else source.read(entry)
                    apk.writestr(entry, data)

        run(args.build_tools / "zipalign", "-P", "16", "-f", "4", unsigned, aligned)
        run(args.build_tools / "apksigner", "sign", "--v1-signing-enabled", "false",
            "--v4-signing-enabled", "false", "--ks", keystore, "--ks-key-alias", args.key_alias,
            "--ks-pass", "env:XUNJI_KEYSTORE_PASSWORD", "--key-pass", "env:XUNJI_KEY_PASSWORD",
            "--out", signed, aligned)
        run(args.build_tools / "apksigner", "verify", "--verbose", signed)
        run(args.build_tools / "zipalign", "-P", "16", "-c", "4", signed)
        shutil.move(signed, output)
        print(output)


if __name__ == "__main__":
    main()

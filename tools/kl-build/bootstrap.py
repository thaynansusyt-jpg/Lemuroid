#!/usr/bin/env python3
"""Recover the Linux x86_64 Android build toolchain without system package installs.
Usage: python3 tools/kl-build/bootstrap.py /absolute/toolchain-directory
"""
from pathlib import Path
import os
import shutil
import subprocess
import sys
import tarfile
import urllib.parse
import zipfile

root = Path(sys.argv[1]).resolve()
root.mkdir(parents=True, exist_ok=True)
downloads = root / "downloads"
downloads.mkdir(exist_ok=True)
def download(url, name):
    path = downloads / name
    if not path.exists():
        subprocess.run(["curl", "--fail", "--location", "--retry", "2", "--max-time", "300", url, "-o", str(path)], check=True)
    return path

def unzip(url, name, destination):
    with zipfile.ZipFile(download(url, name)) as archive:
        archive.extractall(destination)

jdk = root / "jdk"
if not (jdk / "bin/javac").exists():
    archive = download("https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse", "jdk17.tar.gz")
    with tarfile.open(archive) as stream:
        # Extract only regular files/directories/symlinks beneath the archive root.
        stream.extractall(root / "jdk-unpack", filter="data")
    unpacked = next((root / "jdk-unpack").iterdir())
    shutil.move(unpacked, jdk)
if not (root / "gradle-8.10.2/bin/gradle").exists():
    unzip("https://services.gradle.org/distributions/gradle-8.10.2-bin.zip", "gradle.zip", root)
sdk = root / "sdk"
tools = sdk / "cmdline-tools/latest"
if not (tools / "bin/sdkmanager").exists():
    unzip("https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip", "sdk-tools.zip", root / "sdk-unpack")
    tools.parent.mkdir(parents=True, exist_ok=True)
    shutil.move(root / "sdk-unpack/cmdline-tools", tools)
for folder in (jdk / "bin", tools / "bin", root / "gradle-8.10.2/bin"):
    for binary in folder.iterdir():
        if binary.is_file():
            binary.chmod(binary.stat().st_mode | 0o111)
env = os.environ.copy()
env["JAVA_HOME"] = str(jdk)
env["ANDROID_HOME"] = str(sdk)
# Reuse system trust (including a managed proxy certificate) when available.
trust = Path("/etc/ssl/certs/java/cacerts")
if trust.exists():
    env["JAVA_OPTS"] = f"-Djavax.net.ssl.trustStore={trust}"
proxy = urllib.parse.urlsplit(env.get("HTTPS_PROXY", ""))
args = [str(tools / "bin/sdkmanager"), f"--sdk_root={sdk}"]
if proxy.hostname:
    args += ["--proxy=http", f"--proxy_host={proxy.hostname}", f"--proxy_port={proxy.port or 80}"]
subprocess.run(args + ["--licenses"], input="y\n" * 100, text=True, env=env, check=True)
subprocess.run(args + ["platforms;android-35", "build-tools;34.0.0", "platform-tools", "cmake;3.22.1", "ndk;27.2.12479018"], input="y\n" * 100, text=True, env=env, check=True)
subprocess.run([str(jdk / "bin/javac"), "-version"], check=True)
print("Toolchain ready:", root)

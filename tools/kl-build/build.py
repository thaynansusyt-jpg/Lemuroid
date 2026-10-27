#!/usr/bin/env python3
"""Build an ARM64 debug candidate with its own Gradle cache. No release credentials.
Usage: python3 tools/kl-build/build.py /absolute/toolchain-directory
"""
from pathlib import Path
import os
import subprocess
import shutil
import sys
import urllib.parse
root = Path(sys.argv[1]).resolve()
project = Path(__file__).resolve().parents[2]
subprocess.run([sys.executable, str(project / "tools/kl-build/prepare-test-cores.py")], check=True)
env = os.environ.copy()
env["JAVA_HOME"] = str(root / "jdk")
env["ANDROID_HOME"] = env["ANDROID_SDK_ROOT"] = str(root / "sdk")
env["GRADLE_USER_HOME"] = str(root / "gradle-cache")
env["PATH"] = str(root / "jdk/bin") + os.pathsep + env["PATH"]
args = [str(root / "gradle-8.10.2/bin/gradle"), "--no-daemon", "--console=plain", "--max-workers=2", "--no-parallel", "-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8", "-Pkotlin.compiler.execution.strategy=in-process", "--no-configuration-cache", "-PklArm64Only=true"]
if Path("/etc/ssl/certs/java/cacerts").exists():
    args += ["-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"]
proxy = urllib.parse.urlsplit(env.get("HTTPS_PROXY", ""))
if proxy.hostname:
    for protocol in ("http", "https"):
        args += [f"-D{protocol}.proxyHost={proxy.hostname}", f"-D{protocol}.proxyPort={proxy.port or 80}"]
# Restore missing bundled-core symlinks by copying only when that bundle entry is absent.
for binary in (project / "lemuroid-cores").glob("lemuroid_core_*/src/main/jniLibs/arm64-v8a/*.so"):
    bundled = project / "lemuroid-cores/bundled-cores/src/main/jniLibs/arm64-v8a" / binary.name
    if not bundled.is_file():
        destination = project / "lemuroid-app/src/main/jniLibs/arm64-v8a" / binary.name
        destination.parent.mkdir(parents=True, exist_ok=True)
        if not destination.is_file():
            shutil.copy2(binary, destination)
args += [":lemuroid-app:assembleFreeBundleDebug", ":lemuroid-app:testFreeBundleDebugUnitTest"]
subprocess.run(args, cwd=project, env=env, check=True)

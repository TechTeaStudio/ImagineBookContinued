"""Build ImagineBook Continued without Gradle, from the jars an installed MultiMC already has.

Why: the owner works over a slow, metered mobile link. A first Gradle + Loom build downloads about
460-500 MB (Gradle, JDK 25, Minecraft server jar and libraries). This script downloads about 6 MB once
(the ECJ compiler and the JUnit console launcher) and compiles against MultiMC's copy of Minecraft 26.3.
CI builds with Gradle (build.gradle); both read VERSION, which VersionDriftTest checks.

Usage (from the repository root):
    python tools/build_local.py            # jar + sources jar into build/libs
    python tools/build_local.py --test     # also compile and run the JUnit tests
Environment:
    MULTIMC_ROOT      MultiMC folder (default D:/UslessTrash/MultiMC)
    IMAGINEBOOK_JAVA  java.exe of a Java 25 runtime (a JRE is enough for ECJ)
"""
import argparse
import hashlib
import io
import json
import os
import shutil
import subprocess
import sys
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CACHE = ROOT / "tools" / ".cache"
OUT = ROOT / "build" / "local"
LIBS_OUT = ROOT / "build" / "libs"
MC = "26.3"
LOADER = "0.20.1"
WEBP = "webp-imageio-0.1.6.jar"

# name -> (url, sha1). Checksums pin exactly what was reviewed; a mismatch aborts the build.
DOWNLOADS = {
    "ecj-3.46.100.jar": ("https://repo1.maven.org/maven2/org/eclipse/jdt/ecj/3.46.100/ecj-3.46.100.jar",
                         "a06c482cdb8601a94328aae668c7236e3818fbe9"),
    "junit-platform-console-standalone-1.11.4.jar": (
        "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.11.4/"
        "junit-platform-console-standalone-1.11.4.jar", None),
    "webp-imageio-0.1.6-raw.jar": ("https://repo1.maven.org/maven2/org/sejda/imageio/webp-imageio/0.1.6/webp-imageio-0.1.6.jar",
                                   None),
}
DEFAULT_JAVA = Path.home() / "AppData/Roaming/ModrinthApp/meta/java_versions/zulu25.36.205-ca-jre25.0.4.1-win_x64/bin/java.exe"


def sha1(path):
    return hashlib.sha1(path.read_bytes()).hexdigest()


def cached(name):
    path = CACHE / name
    if path.exists():
        return path
    url, digest = DOWNLOADS[name]
    CACHE.mkdir(parents=True, exist_ok=True)
    print(f"downloading {name} from {url}")
    with urllib.request.urlopen(url, timeout=60) as r:
        data = r.read()
    tmp = path.with_suffix(".part")
    tmp.write_bytes(data)
    if digest and sha1(tmp) != digest:
        tmp.unlink()
        sys.exit(f"checksum mismatch for {name}")
    tmp.replace(path)
    return path


def nested_webp():
    """webp-imageio as Fabric expects a nested library: with a generated fabric.mod.json, like Loom's include."""
    path = CACHE / WEBP
    if path.exists():
        return path
    raw = cached("webp-imageio-0.1.6-raw.jar")
    meta = {"schemaVersion": 1, "id": "org_sejda_imageio_webp-imageio", "version": "0.1.6",
            "name": "webp-imageio", "custom": {"fabric-loom:generated": True}}
    with zipfile.ZipFile(raw) as src, zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as dst:
        for item in src.infolist():
            dst.writestr(item, src.read(item.filename))
        dst.writestr("fabric.mod.json", json.dumps(meta, indent=2))
    return path


def maven_path(libs, name):
    parts = name.split(":")
    group, artifact, version = parts[0], parts[1], parts[2]
    classifier = f"-{parts[3]}" if len(parts) > 3 else ""
    return libs / group.replace(".", "/") / artifact / version / f"{artifact}-{version}{classifier}.jar"


def classpath(multimc):
    libs = multimc / "libraries"
    jars = [libs / f"com/mojang/minecraft/{MC}/minecraft-{MC}-client.jar"]
    for meta in (multimc / f"meta/net.minecraft/{MC}.json", multimc / f"meta/net.fabricmc.fabric-loader/{LOADER}.json"):
        for lib in json.loads(meta.read_text(encoding="utf-8")).get("libraries", []):
            path = maven_path(libs, lib["name"])
            if path.exists():
                jars.append(path)
    jars.append(libs / f"net/fabricmc/fabric-loader/{LOADER}/fabric-loader-{LOADER}.jar")
    missing = [j for j in jars if not j.exists()]
    if missing:
        sys.exit("missing from MultiMC: " + ", ".join(map(str, missing)))
    return jars


def java_exe():
    java = Path(os.environ.get("IMAGINEBOOK_JAVA", DEFAULT_JAVA))
    if not java.exists():
        sys.exit(f"Java 25 not found at {java}; set IMAGINEBOOK_JAVA")
    return java


def ecj(java, cp, out, sources):
    out.mkdir(parents=True, exist_ok=True)
    cmd = [str(java), "-jar", str(cached("ecj-3.46.100.jar")), "-25", "-encoding", "UTF-8", "-nowarn",
           "-proc:none", "-cp", os.pathsep.join(map(str, cp)), "-d", str(out)] + [str(s) for s in sources]
    if subprocess.run(cmd).returncode != 0:
        sys.exit("compilation failed")


def version():
    return (ROOT / "VERSION").read_text(encoding="utf-8").strip() + f"+{MC}"


def package(classes):
    ver = version()
    LIBS_OUT.mkdir(parents=True, exist_ok=True)
    jar = LIBS_OUT / f"imaginebook-continued-fabric-{ver}.jar"
    resources = ROOT / "src/main/resources"
    with zipfile.ZipFile(jar, "w", zipfile.ZIP_DEFLATED) as z:
        for f in sorted(classes.rglob("*.class")):
            z.write(f, f.relative_to(classes).as_posix())
        for f in sorted(resources.rglob("*")):
            if f.is_dir() or f.suffix == ".psd":
                continue
            rel = f.relative_to(resources).as_posix()
            if rel == "fabric.mod.json":
                meta = json.loads(f.read_text(encoding="utf-8").replace("${version}", ver).replace("${minecraft}", MC))
                meta["jars"] = [{"file": f"META-INF/jars/{WEBP}"}]
                z.writestr(rel, json.dumps(meta, indent=2))
            else:
                z.write(f, rel)
        z.write(nested_webp(), f"META-INF/jars/{WEBP}")
        z.write(ROOT / "LICENSE", "LICENSE")
    sources = LIBS_OUT / f"imaginebook-continued-fabric-{ver}-sources.jar"
    with zipfile.ZipFile(sources, "w", zipfile.ZIP_DEFLATED) as z:
        for base in (ROOT / "src/main/java", resources):
            for f in sorted(base.rglob("*")):
                if f.is_file() and f.suffix != ".psd":
                    z.write(f, f.relative_to(base).as_posix())
        z.write(ROOT / "LICENSE", "LICENSE")
    print("built", jar)
    print("built", sources)
    return jar


def run_tests(java, cp, classes):
    console = cached("junit-platform-console-standalone-1.11.4.jar")
    test_classes = OUT / "test-classes"
    shutil.rmtree(test_classes, ignore_errors=True)
    ecj(java, cp + [classes, console], test_classes, sorted((ROOT / "src/test/java").rglob("*.java")))
    run_cp = os.pathsep.join(map(str, [test_classes, classes] + cp))
    cmd = [str(java), "-jar", str(console), "execute", "--class-path", run_cp,
           "--scan-class-path", str(test_classes), "--disable-banner", "--details=tree"]
    if subprocess.run(cmd, cwd=ROOT).returncode != 0:
        sys.exit("tests failed")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--test", action="store_true")
    args = ap.parse_args()
    multimc = Path(os.environ.get("MULTIMC_ROOT", "D:/UslessTrash/MultiMC"))
    java = java_exe()
    cp = classpath(multimc)
    classes = OUT / "classes"
    shutil.rmtree(classes, ignore_errors=True)
    ecj(java, cp, classes, sorted((ROOT / "src/main/java").rglob("*.java")))
    if args.test:
        run_tests(java, cp, classes)
    package(classes)


if __name__ == "__main__":
    main()

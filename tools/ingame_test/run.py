"""Run the in-game test: a real Minecraft 26.3 client with ImagineBook Continued + Stendhal + Fabric API.

The client runs offline (username "Tester", no Microsoft account) in an isolated game directory under
build/ingame, so the owner's worlds, options and account are never touched. It opens a book and
quill, takes screenshots, selects an image, resizes the screen, and exits. Needs a desktop session
(a window opens for about a minute).

Usage (from the repository root, after `python tools/build_local.py`):
    python tools/ingame_test/run.py [--stendhal-from <mods folder>] [--no-stendhal]
Prints every IMAGINEBOOK_TEST line and the screenshot paths; exit code 0 only on PASSED.
"""
import argparse
import json
import os
import shutil
import subprocess
import sys
import time
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import build_local as B  # noqa: E402

HERE = Path(__file__).resolve().parent
GAME = B.ROOT / "build" / "ingame"
DEFAULT_MODS = Path("D:/UslessTrash/MultiMC/instances/PEPELAND 10 26.3/.minecraft/mods")
# Only level.dat and data/ (a few KB) are copied from here; the source world is never written to.
WORLD_FROM = Path(os.environ.get("INGAME_WORLD_FROM", "D:/UslessTrash/MultiMC/instances/PEPELAND 10 26.3/.minecraft/saves/New World"))


def harness_jar(java, cp):
    classes = B.OUT / "ingame-classes"
    shutil.rmtree(classes, ignore_errors=True)
    B.ecj(java, cp + [B.OUT / "classes"], classes, sorted((HERE / "src").rglob("*.java")))
    jar = B.OUT / "imaginebook-ingame-test.jar"
    with zipfile.ZipFile(jar, "w", zipfile.ZIP_DEFLATED) as z:
        for f in sorted(classes.rglob("*.class")):
            z.write(f, f.relative_to(classes).as_posix())
        z.write(HERE / "fabric.mod.json", "fabric.mod.json")
    return jar


def runtime_classpath(multimc):
    libs = multimc / "libraries"
    jars = []
    for meta in (f"net.fabricmc.intermediary/{B.MC}.json", f"net.fabricmc.fabric-loader/{B.LOADER}.json", f"net.minecraft/{B.MC}.json"):
        for lib in json.loads((multimc / "meta" / meta).read_text(encoding="utf-8")).get("libraries", []):
            path = B.maven_path(libs, lib["name"])
            if path.exists() and path not in jars:
                jars.append(path)
    jars.append(libs / f"net/fabricmc/fabric-loader/{B.LOADER}/fabric-loader-{B.LOADER}.jar")
    jars.append(libs / f"com/mojang/minecraft/{B.MC}/minecraft-{B.MC}-client.jar")
    return jars


def prepare_game(mod_jar, harness, mods_from, stendhal):
    shutil.rmtree(GAME, ignore_errors=True)
    mods = GAME / "mods"
    mods.mkdir(parents=True)
    shutil.copy(mod_jar, mods)
    shutil.copy(harness, mods)
    # Sodium applies the AMD driver workarounds the owner's laptop needs; vanilla alone crashed natively
    # (0xC0000005) while loading fonts on the AMD Radeon 780M.
    wanted = ["fabric-api-", "sodium-fabric-"] + (["stendhal-"] if stendhal else [])
    for prefix in wanted:
        found = sorted(mods_from.glob(prefix + "*.jar"))
        if not found:
            sys.exit(f"{prefix}*.jar not found in {mods_from}")
        shutil.copy(found[-1], mods)
    # Skip first-launch onboarding, mute sound, fixed GUI scale so screenshots are comparable.
    (GAME / "options.txt").write_text("onboardAccessibility:false\nguiScale:2\nsoundCategory_master:0.0\n"
                                      "renderDistance:2\nnarrator:0\ntutorialStep:none\n", encoding="utf-8")
    # The editor needs a player, so the test plays in a world; terrain is generated afresh.
    world = GAME / "saves" / "TestWorld"
    world.mkdir(parents=True)
    shutil.copy(WORLD_FROM / "level.dat", world / "level.dat")
    shutil.copytree(WORLD_FROM / "data", world / "data")
    return sorted(p.name for p in mods.iterdir())


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--stendhal-from", type=Path, default=DEFAULT_MODS)
    ap.add_argument("--no-stendhal", action="store_true")
    args = ap.parse_args()
    multimc = Path(os.environ.get("MULTIMC_ROOT", "D:/UslessTrash/MultiMC"))
    java = B.java_exe()
    jars = sorted(B.LIBS_OUT.glob(f"imaginebook-continued-fabric-*+{B.MC}.jar"))
    if not jars:
        sys.exit("run tools/build_local.py first")
    harness = harness_jar(java, B.classpath(multimc))
    mods = prepare_game(jars[-1], harness, args.stendhal_from, not args.no_stendhal)
    print("mods:", ", ".join(mods))
    asset_index = json.loads((multimc / f"meta/net.minecraft/{B.MC}.json").read_text(encoding="utf-8"))["assetIndex"]["id"]
    # Launch like MultiMC does: javaw.exe plus Mojang's HeapDumpPath marker, by which GPU drivers
    # recognise Minecraft and apply their game profile. Plain java.exe crashed natively on the AMD
    # Radeon 780M right after OpenGL initialisation.
    javaw = java.with_name("javaw.exe") if java.with_name("javaw.exe").exists() else java
    cmd = [str(javaw), "-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump",
           "-Xms512M", "-Xmx1536M", "-Duser.language=en", "-cp", os.pathsep.join(map(str, runtime_classpath(multimc))),
           "net.fabricmc.loader.impl.launch.knot.KnotClient",
           "--username", "Tester", "--uuid", "00000000-0000-0000-0000-000000000001", "--accessToken", "0",
           "--version", B.MC, "--gameDir", str(GAME), "--assetsDir", str(multimc / "assets"),
           "--assetIndex", str(asset_index), "--width", "1280", "--height", "720",
           "--quickPlaySingleplayer", "TestWorld"]
    log = GAME / "run.log"
    started = time.time()
    # The AMD Radeon 780M (video memory carved out of the 16 GB system RAM) sometimes crashes natively
    # (0xC0000005) while the game uploads its first textures, before any mod code runs. Such a start is
    # retried; a crash after the test began is a real failure and is not retried.
    for attempt in range(1, 5):
        with open(log, "w", encoding="utf-8", errors="replace") as out:
            proc = subprocess.run(cmd, cwd=GAME, stdout=out, stderr=subprocess.STDOUT, timeout=600)
        text = log.read_text(encoding="utf-8", errors="replace")
        if proc.returncode == 0xC0000005 and "IMAGINEBOOK_TEST in world" not in text:
            print(f"attempt {attempt}: driver crashed during startup, retrying")
            continue
        break
    lines = log.read_text(encoding="utf-8", errors="replace").splitlines()
    for line in lines:
        if "IMAGINEBOOK_TEST" in line or "Exception" in line or "Caused by" in line or "Mixin apply" in line:
            print(line[:300])
    shots = sorted((GAME / "screenshots").glob("*.png")) if (GAME / "screenshots").exists() else []
    for s in shots:
        print("screenshot:", s)
    crashes = sorted((GAME / "crash-reports").glob("*.txt")) if (GAME / "crash-reports").exists() else []
    for c in crashes:
        print("CRASH REPORT:", c)
    print(f"exit code {proc.returncode} after {time.time() - started:.0f} s; full log {log}")
    passed = any(l.endswith("IMAGINEBOOK_TEST PASSED") for l in lines) and not crashes
    sys.exit(0 if passed else 1)


if __name__ == "__main__":
    main()

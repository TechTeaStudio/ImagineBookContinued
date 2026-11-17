"""Check that every ImagineBook Mixin applies, with Stendhal installed, without opening a game window.

The local twin of CI's `gradlew runMixinSmoke`. The development-only smoke mod (src/smoke) loads the
patched Minecraft classes in Fabric's preLaunch, before the window exists, and exits with 0 when every
Mixin applied (imaginebook.mixins.json sets defaultRequire 1, so a missed injection is an error).
Stendhal is loaded next to it because it patches BookEditScreen too.

Usage (from the repository root, after `python tools/build_local.py`):
    python tools/mixin_smoke.py [--mods-from <mods folder with fabric-api and stendhal>] [--no-stendhal]
"""
import argparse
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_local as B  # noqa: E402
from ingame_test.run import DEFAULT_MODS, runtime_classpath  # noqa: E402

GAME = B.ROOT / "build" / "mixin-smoke"


def smoke_jar(java, cp):
    classes = B.OUT / "smoke-classes"
    shutil.rmtree(classes, ignore_errors=True)
    B.ecj(java, cp + [B.OUT / "classes"], classes, sorted((B.ROOT / "src/smoke/java").rglob("*.java")))
    jar = B.OUT / "imaginebook-smoke-test.jar"
    with zipfile.ZipFile(jar, "w", zipfile.ZIP_DEFLATED) as z:
        for f in sorted(classes.rglob("*.class")):
            z.write(f, f.relative_to(classes).as_posix())
        z.write(B.ROOT / "src/smoke/resources/fabric.mod.json", "fabric.mod.json")
    return jar


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--mods-from", type=Path, default=DEFAULT_MODS)
    ap.add_argument("--no-stendhal", action="store_true")
    args = ap.parse_args()
    multimc = Path(os.environ.get("MULTIMC_ROOT", "D:/UslessTrash/MultiMC"))
    java = B.java_exe()
    jars = sorted(B.LIBS_OUT.glob(f"imaginebook-continued-fabric-*+{B.MC}.jar"))
    jars = [j for j in jars if not j.name.endswith("-sources.jar")]
    if not jars:
        sys.exit("run tools/build_local.py first")
    smoke = smoke_jar(java, B.classpath(multimc))
    shutil.rmtree(GAME, ignore_errors=True)
    mods = GAME / "mods"
    mods.mkdir(parents=True)
    shutil.copy(jars[-1], mods)
    for prefix in ["fabric-api-"] + ([] if args.no_stendhal else ["stendhal-"]):
        found = sorted(args.mods_from.glob(prefix + "*.jar"))
        if not found:
            sys.exit(f"{prefix}*.jar not found in {args.mods_from}")
        shutil.copy(found[-1], mods)
    print("mods:", ", ".join(sorted(p.name for p in mods.iterdir())))
    cmd = [str(java), f"-Dfabric.addMods={smoke}", "-Dmixin.debug.countInjections=true",
           "-cp", os.pathsep.join(map(str, runtime_classpath(multimc))),
           "net.fabricmc.loader.impl.launch.knot.KnotClient", "--version", B.MC, "--gameDir", str(GAME),
           "--accessToken", "0"]
    proc = subprocess.run(cmd, cwd=GAME, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=300)
    output = proc.stdout + proc.stderr
    (GAME / "smoke.log").write_text(output, encoding="utf-8")
    for line in output.splitlines():
        if "IMAGINEBOOK_" in line or "Mixin apply" in line or "InvalidInjection" in line or "Caused by" in line:
            print(line[:300])
    passed = proc.returncode == 0 and "IMAGINEBOOK_SMOKE_PASSED" in output
    print(("PASSED" if passed else "FAILED") + f", exit code {proc.returncode}; full log {GAME / 'smoke.log'}")
    sys.exit(0 if passed else 1)


if __name__ == "__main__":
    main()

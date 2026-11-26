# Battle Box importers (local tooling)

Scripts used to turn three downloaded Battle Box worlds into imported arenas (blib.py = Java Anvil -> npz, edrock_load.py = Bedrock LevelDB -> npz via amulet-leveldb, ed2java.py = Bedrock block states -> Java 1.21.1, mk.py = strip + write map.yml/blocks.mcmi.gz, uild_*.py = per-world arena definitions, oute.py = offline walk/seal check). Paths are hard-coded to a local temp folder; run them with python -I inside a venv (nbtlib, numpy, pillow, amulet-leveldb). The downloaded maps themselves are third-party data and are not part of the repo.


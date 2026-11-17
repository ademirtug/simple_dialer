"""Check 64-bit ELF LOAD/RELRO layout in APKs, AABs, AARs, or individual .so files.

See https://developer.android.com/guide/practices/page-sizes
APK ZIP alignment must additionally be checked with zipalign -c -P 16 4.
"""

import argparse
import struct
import zipfile
from pathlib import Path

PAGE = 16384
PT_LOAD = 1
PT_GNU_RELRO = 0x6474E552


def check_elf(name, data):
    if data[:4] != b"\x7fELF":
        raise ValueError(f"{name}: not an ELF library")
    if data[4] != 2:  # Android's 16 KB page-size support concerns 64-bit ABIs.
        print(f"SKIP {name}: 32-bit ELF")
        return []
    endian = {1: "<", 2: ">"}[data[5]]
    phoff = struct.unpack_from(endian + "Q", data, 32)[0]
    phsize, phnum = struct.unpack_from(endian + "HH", data, 54)
    headers = [struct.unpack_from(endian + "IIQQQQQQ", data, phoff + i * phsize)
               for i in range(phnum)]
    loads = [h for h in headers if h[0] == PT_LOAD]
    errors = []
    if not loads:
        errors.append(f"{name}: no LOAD segments")
    for _, flags, offset, address, _, filesz, memsz, alignment in loads:
        if alignment < PAGE or alignment % PAGE or (address - offset) % PAGE:
            errors.append(f"{name}: incompatible LOAD alignment {alignment:#x}")
    for relro in (h for h in headers if h[0] == PT_GNU_RELRO):
        start, end = relro[3], relro[3] + relro[6]
        if end % PAGE:
            # An isolated RELRO-only LOAD may end within a page, provided no
            # other writable LOAD shares that protected page.
            # RELRO may include padding beyond the LOAD's declared memory size.
            isolated = any(h[3] == start and h[3] + h[6] <= end for h in loads)
            rounded_end = (end + PAGE - 1) // PAGE * PAGE
            overlaps = any(h[1] & 2 and h[3] < rounded_end and h[3] + h[6] > end
                           for h in loads)
            if not isolated or overlaps:
                errors.append(f"{name}: RELRO ends at {end:#x}, not a 16 KB boundary")
    if not errors:
        print(f"PASS {name}: 16 KB LOAD and RELRO layout")
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("artifacts", type=Path, nargs="+")
    args = parser.parse_args()
    errors = []
    for artifact in args.artifacts:
        if artifact.suffix == ".so":
            errors.extend(check_elf(str(artifact), artifact.read_bytes()))
        else:
            with zipfile.ZipFile(artifact) as archive:
                for name in sorted(archive.namelist()):
                    if name.endswith(".so"):
                        errors.extend(check_elf(f"{artifact.name}:{name}", archive.read(name)))
    for error in errors:
        print(f"FAIL {error}")
    return bool(errors)


if __name__ == "__main__":
    raise SystemExit(main())
